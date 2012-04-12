/*
 * ============================================================================
 * GNU Lesser General Public License
 * ============================================================================
 *
 * Copyright (C) 2006-2009 Serotonin Software Technologies Inc. http://serotoninsoftware.com
 * @author Matthew Lohbihler
 * 
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307, USA.
 */
package gov.nrel.bacnet;

import java.nio.charset.Charset;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.Reader;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.logging.*;
import java.util.regex.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;   
import java.util.List;
import java.util.Vector;

import java.lang.Integer;

import com.serotonin.util.IpAddressUtils;
import com.serotonin.util.queue.*;
  
import com.serotonin.bacnet4j.LocalDevice;   
import com.serotonin.bacnet4j.RemoteDevice;   
import com.serotonin.bacnet4j.Network;
import com.serotonin.bacnet4j.RemoteObject;

import com.serotonin.bacnet4j.base.BACnetUtils;   

import com.serotonin.bacnet4j.event.DefaultDeviceEventListener;   
import com.serotonin.bacnet4j.event.DefaultExceptionListener;
import com.serotonin.bacnet4j.event.DeviceEventListener;


import com.serotonin.bacnet4j.util.PropertyReferences;   
import com.serotonin.bacnet4j.util.PropertyValues;   

import com.serotonin.bacnet4j.obj.BACnetObject;
import com.serotonin.bacnet4j.obj.FileObject;

import com.serotonin.bacnet4j.service.acknowledgement.ReadRangeAck;   

import com.serotonin.bacnet4j.service.confirmed.ReadRangeRequest;   
import com.serotonin.bacnet4j.service.confirmed.ReadRangeRequest.BySequenceNumber;   
import com.serotonin.bacnet4j.service.confirmed.ReinitializeDeviceRequest.ReinitializedStateOfDevice;

import com.serotonin.bacnet4j.service.unconfirmed.WhoIsRequest;

import com.serotonin.bacnet4j.type.Encodable;   

import com.serotonin.bacnet4j.type.primitive.ObjectIdentifier;   
import com.serotonin.bacnet4j.type.primitive.Real;
import com.serotonin.bacnet4j.type.primitive.SignedInteger;   
import com.serotonin.bacnet4j.type.primitive.UnsignedInteger;   

import com.serotonin.bacnet4j.type.enumerated.BinaryPV;
import com.serotonin.bacnet4j.type.enumerated.EngineeringUnits;
import com.serotonin.bacnet4j.type.enumerated.EventState;
import com.serotonin.bacnet4j.type.enumerated.EventType;
import com.serotonin.bacnet4j.type.enumerated.FileAccessMethod;
import com.serotonin.bacnet4j.type.enumerated.MessagePriority;
import com.serotonin.bacnet4j.type.enumerated.NotifyType;
import com.serotonin.bacnet4j.type.enumerated.ObjectType;   
import com.serotonin.bacnet4j.type.enumerated.PropertyIdentifier;   
import com.serotonin.bacnet4j.type.enumerated.Segmentation;

import com.serotonin.bacnet4j.type.constructed.Address;   
import com.serotonin.bacnet4j.type.constructed.Choice;
import com.serotonin.bacnet4j.type.constructed.DateTime;
import com.serotonin.bacnet4j.type.constructed.LogRecord;   
import com.serotonin.bacnet4j.type.constructed.ObjectPropertyReference;
import com.serotonin.bacnet4j.type.constructed.PropertyValue;
import com.serotonin.bacnet4j.type.constructed.SequenceOf;
import com.serotonin.bacnet4j.type.constructed.TimeStamp;

import com.serotonin.bacnet4j.type.notificationParameters.NotificationParameters;

import com.serotonin.bacnet4j.type.primitive.Boolean;
import com.serotonin.bacnet4j.type.primitive.CharacterString;
import com.serotonin.bacnet4j.type.primitive.ObjectIdentifier;
import com.serotonin.bacnet4j.type.primitive.SignedInteger;
import com.serotonin.bacnet4j.type.primitive.Unsigned16;
import com.serotonin.bacnet4j.type.primitive.UnsignedInteger;

import com.serotonin.bacnet4j.exception.BACnetException;


public class Scan {
  static class MyExceptionListener extends DefaultExceptionListener {  
    @Override  
      public void unimplementedVendorService(UnsignedInteger vendorId, UnsignedInteger serviceNumber, 
          ByteQueue queue) 
      {  
        if (vendorId.intValue() == 8 && serviceNumber.intValue() == 1) {  
          // do nothing  
        }  
        else  
          super.unimplementedVendorService(vendorId, serviceNumber, queue);  
      }  
  }

  static class SlaveDevice {
    private BACnetObject ai0;
    private BACnetObject ai1;
    private BACnetObject bi0;
    private BACnetObject bi1;

    private BACnetObject mso0;
    private BACnetObject ao0;
    private BACnetObject av0;
    private FileObject file0;
    private BACnetObject bv1;

    public SlaveDevice(LocalDevice ld)
    {
      try {
        ld.getConfiguration().setProperty(PropertyIdentifier.objectName,
            new CharacterString("BACnet4J slave device test"));
        ld.getConfiguration().setProperty(PropertyIdentifier.vendorIdentifier,
            new Unsigned16(513));
        ld.getConfiguration().setProperty(PropertyIdentifier.segmentationSupported,
            Segmentation.segmentedBoth);

        // Set up a few objects.
        ai0 = new BACnetObject(ld,
            ld.getNextInstanceObjectIdentifier(ObjectType.analogInput));
        ai0.setProperty(PropertyIdentifier.units, EngineeringUnits.centimeters);
        ld.addObject(ai0);

        ai1 = new BACnetObject(ld,
            ld.getNextInstanceObjectIdentifier(ObjectType.analogInput));
        ai1.setProperty(PropertyIdentifier.units, EngineeringUnits.percentObscurationPerFoot);
        ld.addObject(ai1);

        bi0 = new BACnetObject(ld,
            ld.getNextInstanceObjectIdentifier(ObjectType.binaryInput));
        ld.addObject(bi0);
        bi0.setProperty(PropertyIdentifier.objectName, new CharacterString("Off and on"));
        bi0.setProperty(PropertyIdentifier.inactiveText, new CharacterString("Off"));
        bi0.setProperty(PropertyIdentifier.activeText, new CharacterString("On"));

        bi1 = new BACnetObject(ld,
            ld.getNextInstanceObjectIdentifier(ObjectType.binaryInput));
        ld.addObject(bi1);
        bi1.setProperty(PropertyIdentifier.objectName, new CharacterString("Good and bad"));
        bi1.setProperty(PropertyIdentifier.inactiveText, new CharacterString("Bad"));
        bi1.setProperty(PropertyIdentifier.activeText, new CharacterString("Good"));

        mso0 = new BACnetObject(ld,
            ld.getNextInstanceObjectIdentifier(ObjectType.multiStateOutput));
        mso0.setProperty(PropertyIdentifier.objectName, new CharacterString("Vegetable"));
        mso0.setProperty(PropertyIdentifier.numberOfStates, new UnsignedInteger(4));
        mso0.setProperty(PropertyIdentifier.stateText, 1, new CharacterString("Tomato"));
        mso0.setProperty(PropertyIdentifier.stateText, 2, new CharacterString("Potato"));
        mso0.setProperty(PropertyIdentifier.stateText, 3, new CharacterString("Onion"));
        mso0.setProperty(PropertyIdentifier.stateText, 4, new CharacterString("Broccoli"));
        mso0.setProperty(PropertyIdentifier.presentValue, new UnsignedInteger(1));
        ld.addObject(mso0);

        ao0 = new BACnetObject(ld,
            ld.getNextInstanceObjectIdentifier(ObjectType.analogOutput));
        ao0.setProperty(PropertyIdentifier.objectName, new CharacterString("Settable analog"));
        ld.addObject(ao0);

        av0 = new BACnetObject(ld,
            ld.getNextInstanceObjectIdentifier(ObjectType.analogValue));
        av0.setProperty(PropertyIdentifier.objectName, new CharacterString("Command Priority Test"));
        av0.setProperty(PropertyIdentifier.relinquishDefault, new Real(3.1415F));
        ld.addObject(av0);

        file0 = new FileObject(ld, ld.getNextInstanceObjectIdentifier(ObjectType.file),
            new File("testFile.txt"), FileAccessMethod.streamAccess);
        file0.setProperty(PropertyIdentifier.fileType, new CharacterString("aTestFile"));
        file0.setProperty(PropertyIdentifier.archive, new Boolean(false));
        ld.addObject(file0);

        bv1 = new BACnetObject(ld,
            ld.getNextInstanceObjectIdentifier(ObjectType.binaryValue));
        bv1.setProperty(PropertyIdentifier.objectName, new CharacterString("A binary value"));
        bv1.setProperty(PropertyIdentifier.inactiveText, new CharacterString("Down"));
        bv1.setProperty(PropertyIdentifier.activeText, new CharacterString("Up"));
        ld.addObject(bv1);
      } catch (java.lang.Exception e) {
      }

    } 

    public void updateValues()
    {
      float ai0value = 0;
      float ai1value = 0;
      boolean bi0value = false;
      boolean bi1value = false;

      try {
        mso0.setProperty(PropertyIdentifier.presentValue, new UnsignedInteger(2));

        // Change the values.
        ai0value += 0.1;
        ai1value += 0.7;
        bi0value = !bi0value;
        bi1value = !bi1value;

        // Update the values in the objects.
        ai0.setProperty(PropertyIdentifier.presentValue, new Real(ai0value));
        ai1.setProperty(PropertyIdentifier.presentValue, new Real(ai1value));
        bi0.setProperty(PropertyIdentifier.presentValue, bi0value ? BinaryPV.active : BinaryPV.inactive);
        bi1.setProperty(PropertyIdentifier.presentValue, bi1value ? BinaryPV.active : BinaryPV.inactive);
      } catch (java.lang.Exception e) {
      }

    }   
  } 

  static class Handler extends DefaultDeviceEventListener {
    private BlockingQueue<RemoteDevice> m_devices;
    private HashSet<RemoteDevice> m_founddevices;
    private Logger m_logger;
    
    public Handler()
    {
      m_devices = new LinkedBlockingQueue<RemoteDevice>();
      m_founddevices = new HashSet<RemoteDevice>();
      m_logger = Logger.getLogger("Handler");
      m_logger.setParent(Logger.getLogger("BACnetScanner"));
    }

    @Override
      public void iAmReceived(RemoteDevice d) {
        m_logger.info("RemoteDevice found: " + d);

        try {
          if (!m_founddevices.contains(d))
          {
            m_founddevices.add(d);
            m_devices.put(d);
          } else {
            m_logger.info("Device already queued for scanning: " + d);
          }
        } catch (java.lang.InterruptedException e) {
        }
      }

    public BlockingQueue<RemoteDevice> remoteDevices()
    {
      return m_devices;
    }
  }

  


  private Logger m_logger;
  private Vector<DeviceFilter> m_filters;
  private int m_min;
  private int m_max;

  public Scan(int min, int max, Vector<DeviceFilter> filters)
  {
    m_logger = Logger.getLogger("BACnetScanner");
    m_filters = filters;
    m_min = min;
    m_max = max;
  }


  public static String readFile(String file, Charset cs)
    throws IOException {
    // No real need to close the BufferedReader/InputStreamReader
  // as they're only wrapping the stream
    FileInputStream stream = new FileInputStream(file);
    try {
      Reader reader = new BufferedReader(new InputStreamReader(stream, cs));
      StringBuilder builder = new StringBuilder();
      char[] buffer = new char[8192];
      int read;
      while ((read = reader.read(buffer, 0, buffer.length)) > 0) {
        builder.append(buffer, 0, read);
      }
      return builder.toString();
    } finally {
      // Potential issue here: if this throws an IOException,
      // it will mask any others. Normally I'd use a utility
      // method which would log exceptions and swallow them
      stream.close();
    }        
  }

  static class OIDFilter {
    String objectType;
    String instanceNumber;

    public boolean matches(ObjectIdentifier t_oid)
    {
      String strObjectType = t_oid.getObjectType().toString();
      String strInstanceNumber = Integer.toString(t_oid.getInstanceNumber());

      return Pattern.matches(objectType, strObjectType)
        && Pattern.matches(instanceNumber, strInstanceNumber);
    }
  }

  static class DeviceFilter {
    String instanceNumber;
    String networkNumber;
    String macAddress;
    String networkAddress;

    Vector<OIDFilter> oidFilters;

    public boolean matches(RemoteDevice t_rd)
    {
      String strInstanceNumber = Integer.toString(t_rd.getInstanceNumber());
      String strNetworkNumber = "";
      String strMacAddress = "";
      String strNetworkAddress = "";


      Address a = t_rd.getAddress();

      if (a != null)
      {
        strNetworkNumber = a.getNetworkNumber().toString();
        strMacAddress = a.getMacAddress().toString();
      }

      Network n = t_rd.getNetwork();

      if (n != null)
      {
        strNetworkAddress = n.getNetworkAddress().toString();
      }


      return Pattern.matches(instanceNumber, strInstanceNumber)
        && Pattern.matches(networkNumber, strNetworkNumber)
        && Pattern.matches(macAddress, strMacAddress)
        && Pattern.matches(networkAddress, strNetworkAddress);

    }
  }


  public static void main(String[] args) throws Exception {
    Vector<DeviceFilter> filters = null; 
    int min = -1;
    int max = -1;

    if (args.length > 0)
    {
      min = Integer.parseInt(args[0]); 
      max = min;
    }

    if (args.length > 1)
    {
      max = Integer.parseInt(args[1]);
    }

    if (args.length > 2)
    {
      String filterfile = readFile(args[2], Charset.forName("US-ASCII"));
      com.google.gson.Gson gson = new com.google.gson.Gson();

      java.lang.reflect.Type vectortype 
        = new com.google.gson.reflect.TypeToken<Vector<DeviceFilter>>() {}.getType();
      filters = gson.fromJson(filterfile, vectortype);
    } else {
      /*
      java.io.FileOutputStream jsonfile = null;
      java.io.PrintWriter jsonw = null;
      try {
        jsonfile = new java.io.FileOutputStream("ExampleFilter.json");
      } catch (Exception e) {
      }

      if (jsonfile != null)
      {
        jsonw = new java.io.PrintWriter(jsonfile, true);
        com.google.gson.Gson gson = new com.google.gson.Gson();
        Vector<DeviceFilter> examplefilters = new Vector<DeviceFilter>();
        DeviceFilter f = new DeviceFilter();
        f.instanceNumber = ".*";
        f.networkNumber = ".*";
        f.macAddress = ".*";
        f.networkAddress = ".*";

        OIDFilter of = new OIDFilter();
        of.objectType = "Binary .*";
        of.instanceNumber = "1.*";
        f.oidFilters = new Vector<OIDFilter>();
        f.oidFilters.add(of);
        examplefilters.add(f);
        jsonw.println(gson.toJson(examplefilters));
      }
      */
    }



    Scan s = new Scan(min, max, filters);
    s.run();
  }

  static class TrendLogData
  {
    com.serotonin.bacnet4j.type.primitive.Date date;
    com.serotonin.bacnet4j.type.primitive.Time time;
    String value;
  };

  static class OID
  {
    OID(ObjectIdentifier t_oid, String t_objectName, String t_presentValue, String t_units)
    {
      oid = t_oid;
      objectName = t_objectName;
      presentValue = t_presentValue;
      units = t_units;
    
      children = new Vector<OID>();  
      trendLog = new Vector<TrendLogData>();
    }

    ObjectIdentifier oid;
    String objectName;
    String presentValue;
    String units;

    Vector<TrendLogData> trendLog;

    Vector<OID> children;
  }

  private void printObject(OID t_parent, OID t_oid,
      java.io.PrintWriter writer)
  {
    writer.println(String.format("%s, %s, %s, %s, %s, %s, %s", 
          t_parent.oid,
          t_parent.objectName,
          t_oid.oid.getObjectType(),
          t_oid.oid.getInstanceNumber(),
          t_oid.objectName,
          t_oid.presentValue,
          t_oid.units));

    for (TrendLogData tld : t_oid.trendLog)
    {
      writer.println(String.format("%s, %s, %s, %s, %s, %s, %s, %s, %s", 
            t_parent.oid,
            t_parent.objectName,
            t_oid.oid.getObjectType(),
            t_oid.oid.getInstanceNumber(),
            t_oid.objectName,
            tld.value,
            t_oid.units,
            tld.date,
            tld.time));
    }

    for (OID child : t_oid.children)
    {
      printObject(t_oid, child, writer);
    }


  }

  private String logRecordToString(LogRecord lr)
  {
    try {
      m_logger.fine("trendLogRecord logstatus Data: " + lr.getLogStatus().toString());
      return lr.getLogStatus().toString();
    } catch (Exception ex) {}
    try {
      m_logger.fine("trendLogRecord booleanean Data: " + lr.getBoolean().toString());
      return lr.getBoolean().toString();
    } catch (Exception ex) {}
    try {
      m_logger.fine("trendLogRecord real Data: " + lr.getReal().toString());
      return lr.getReal().toString();
    } catch (Exception ex) {}
    try {
      m_logger.fine("trendLogRecord timechange Data: " + lr.getTimeChange().toString()); 
      return lr.getTimeChange().toString();
    } catch (Exception ex) {}
    try {
      m_logger.fine("trendLogRecord enumerated Data: " + lr.getEnumerated().toString()); 
      return lr.getEnumerated().toString();
    } catch (Exception ex) {}
    try {
      m_logger.fine("trendLogRecord unsigned integer Data: " + lr.getUnsignedInteger().toString()); 
      return lr.getUnsignedInteger().toString();
    } catch (Exception ex) {}
    try {
      return lr.getSignedInteger().toString();
    } catch (Exception ex) {}
    try {
      m_logger.fine("trendLogRecord bitstring Data: " + lr.getBitString().toString());
      return lr.getBitString().toString();
    } catch (Exception ex) {}
    try {
      m_logger.fine("trendLogRecord null Data: " + lr.getNull().toString());
      return lr.getNull().toString();
    } catch (Exception ex) {}
    try {
      m_logger.fine("trendLogRecord bacneterror Data: " + lr.getBACnetError().toString()); 
      return lr.getBACnetError().toString();
    } catch (Exception ex) {}
    try {
      m_logger.fine("trendLogRecord basetype Data: " + lr.getBaseType().toString());
      return lr.getBaseType().toString();
    } catch (Exception ex) {}
    try {
      m_logger.fine("trendLogRecord statusflags Data: " + lr.getStatusFlags().toString());
      return lr.getStatusFlags().toString();
    } catch (Exception ex) {}

    return "UNKNOWN";
  }

  private OID buildObject(LocalDevice ld, RemoteDevice rd, ObjectIdentifier oid, PropertyValues pvs) throws Exception
  {
    Encodable objectName = pvs.get(oid, PropertyIdentifier.objectName);

    Encodable presentValue = null;
    Encodable units = null;

    Vector<TrendLogData> trendlogdata = new Vector<TrendLogData>();

    try {
      presentValue = pvs.get(oid, PropertyIdentifier.presentValue);
      units = pvs.get(oid, PropertyIdentifier.units);
    } catch (Exception e) {
      m_logger.log(Level.FINEST, "Exception getting propertyvalu: " + oid.toString(), e);
    }

    // if this object is a trend log, then let's get the trend log data
    // adapted from: http://mango.serotoninsoftware.com/forum/posts/list/666.page
    if (oid.getObjectType().equals(ObjectType.trendLog))
    {
      try {
        UnsignedInteger totalRecordCount = (UnsignedInteger) pvs.get(oid, PropertyIdentifier.totalRecordCount);   
        UnsignedInteger ui = (UnsignedInteger) pvs.get(oid, PropertyIdentifier.recordCount);  
        SignedInteger count = new SignedInteger(ui.bigIntegerValue());

        UnsignedInteger referenceIndex = new UnsignedInteger(totalRecordCount.longValue() - count.longValue() - 1);   
        ReadRangeRequest rrr = new ReadRangeRequest(oid, PropertyIdentifier.logBuffer, null, 
            new BySequenceNumber(referenceIndex, count));   
        ReadRangeAck rra = (ReadRangeAck)ld.send(rd, rrr);   
        m_logger.fine("trendLog itemCount: " + rra.getItemCount());
        m_logger.fine("trendLog firstSequenceNumber: " + rra.getFirstSequenceNumber());
        //        m_logger.fine("trendLog itemData: " + rra.getItemData());
        Iterator<?> it = (Iterator<?>)rra.getItemData().iterator();   

        while (it.hasNext())   
        {   
          TrendLogData tld = new TrendLogData();
          Encodable e = (Encodable)it.next();   
          if (e instanceof LogRecord)   
          {   
            LogRecord lr = (LogRecord)e;   
            m_logger.fine("trendLogRecord TimeStamp: " + lr.getTimestamp().toString());

            DateTime ts = lr.getTimestamp();

            m_logger.fine("trendLogRecord date: " + ts.getDate().toString());
            m_logger.fine("trendLogRecord time: " + ts.getTime().toString());

            tld.date = ts.getDate();
            tld.time = ts.getTime();
            tld.value = logRecordToString(lr);

            trendlogdata.add(tld);
          } else {
            m_logger.warning("Trend log data of unknown type: " + e.toString());
          }
        }  
      } catch (Exception e) {
        m_logger.log(Level.SEVERE, "Error with reading trend log from device", e);
      }
    }


    for (ObjectPropertyReference opr : pvs) {
      if (oid.equals(opr.getObjectIdentifier())) {
        m_logger.finer(String.format("OID: %s: %s = %s", 
              oid,
              opr.getPropertyIdentifier().toString(), 
              pvs.getNoErrorCheck(opr)));
      }
    }

    OID newoid = new OID(oid, 
        (objectName == null?null:objectName.toString()), 
        (presentValue == null?null:presentValue.toString()), 
        (units == null?null:units.toString())
       );
    newoid.trendLog = trendlogdata;
    return newoid;
  }

  private boolean deviceMatches(RemoteDevice t_rd, Vector<DeviceFilter> t_filters)
  {
    return oidMatches(t_rd, null, t_filters);
  }

  private boolean oidMatches(RemoteDevice t_rd, ObjectIdentifier t_id, Vector<DeviceFilter> t_filters)
  {
    if (t_filters == null)
    {
      return true;
    }

    for (DeviceFilter filter : t_filters) {
      if (filter.matches(t_rd))
      {
        if (t_id == null)
        {
          return true;
        } else {
          if (filter.oidFilters.isEmpty())
          {
            // if there's no oidfilters, accept all of them for this device
            return true;
          } else {
            for (OIDFilter oidFilter : filter.oidFilters)
            {
              if (oidFilter.matches(t_id))
              {
                return true;
              }
            }
          }
        }
      }
    }

    return false;
  }

  private void broadcastWhois(LocalDevice localDevice, int low, int high)
  {
    byte[] ip = new byte[4];
    ip[0] = (byte) 192; // A
    ip[1] = (byte) 168; // B
    ip[2] = (byte) 2; // C
    ip[3] = (byte) 0; // D

    WhoIsRequest whois;
    if (low == -1 && high == -1)
    {
      whois = new WhoIsRequest();
    } else {
      if (low < m_min)
      {
        low = m_min;
      } 

      if (high > m_max)
      {
        high = m_max;
      }

      if (low < 0) low = 0;
      if (high < 0) high = 0;

      m_logger.info("Scanning device ids: " + low + " to " + high);

      whois = new WhoIsRequest(new UnsignedInteger(low), new UnsignedInteger(high));
    }

    while (true) {
      String ipstr = IpAddressUtils.toIpString(ip);
      localDevice.setBroadcastAddress(ipstr);
      m_logger.fine("Scanning IP: " + ipstr);
      try {
        localDevice.sendBroadcast(whois);
      }
      catch (BACnetException e) {
        m_logger.log(Level.WARNING, "Exception occured with ip " + ipstr, e);
      }

      try {
        increment(ip);
      }
      catch (Exception e) {
        m_logger.info("Done pinging devices");
        break;
      }
    }

  }

  private void run() {
    try {
      FileHandler fh = new FileHandler("LogFile.log", true);
      m_logger.addHandler(fh);
      SimpleFormatter formatter = new SimpleFormatter();
      fh.setFormatter(formatter);
    } catch (Exception e) {
      m_logger.log(Level.SEVERE, "Unable to create log file", e);
    }

    m_logger.setLevel(Level.ALL);

    m_logger.info("Scanning device ids: " + m_min + " to " + m_max);

    LocalDevice localDevice = new LocalDevice(1234, "192.168.2.255");
    localDevice.setPort(LocalDevice.DEFAULT_PORT);
    localDevice.setTimeout(localDevice.getTimeout() * 3);
    localDevice.setSegTimeout(localDevice.getSegTimeout() * 3);
    Handler h = new Handler();
    localDevice.getEventHandler().addListener(h);
    localDevice.setExceptionListener(new MyExceptionListener());
    try {
      localDevice.initialize();
    }
    catch (IOException e) {
      e.printStackTrace();
      return;
    }

    /// let's act as a slave device while we are up and running
    SlaveDevice sd = new SlaveDevice(localDevice);

    broadcastWhois(localDevice, m_min, m_max);

    BlockingQueue<RemoteDevice> remoteDevices = h.remoteDevices();


    java.io.FileOutputStream file = null;
    java.io.PrintWriter w = null;
    try {
      file = new java.io.FileOutputStream("output.csv");
    } catch (Exception e) {
      m_logger.log(Level.SEVERE, "Error opening output file", e);
    }

    if (file != null)
    {
      w = new java.io.PrintWriter(file, true);
    }

    java.io.FileOutputStream jsonfile = null;
    java.io.PrintWriter jsonw = null;
    try {
      jsonfile = new java.io.FileOutputStream("output.json");
    } catch (Exception e) {
      m_logger.log(Level.SEVERE, "Error opening output file", e);
    }

    if (jsonfile != null)
    {
      jsonw = new java.io.PrintWriter(jsonfile, true);
    }


    w.println("Device OID, Device Name, Object Type, Object Instance Number, Object Name, Object Value, Trend Log Date, Trend Log Time");

    while (true)
    {
      try {
        RemoteDevice rd = remoteDevices.poll(10, TimeUnit.SECONDS);

        int min = rd.getInstanceNumber() - 500;
        int max = rd.getInstanceNumber() + 500;

        broadcastWhois(localDevice, min, max);

        if (rd == null)
        {
          break;
        }

        if (deviceMatches(rd, m_filters))
        {
          m_logger.info("Getting device info: " + rd);
          localDevice.getExtendedDeviceInformation(rd);

          @SuppressWarnings("unchecked")
            List<ObjectIdentifier> oids = ((SequenceOf<ObjectIdentifier>) localDevice.sendReadPropertyAllowNull(rd, rd.getObjectIdentifier(), PropertyIdentifier.objectList)).getValues();

          m_logger.info("Getting device info: " + rd);
          m_logger.fine("Oids found: " + Integer.toString(oids.size()));
          PropertyReferences refs = new PropertyReferences();
          // add the property references of the "device object" to the list
          refs.add(rd.getObjectIdentifier(), PropertyIdentifier.objectName);

          // and now from all objects under the device object >> ai0, ai1,bi0,bi1...
          for (ObjectIdentifier oid : oids) {
            if (oidMatches(rd, oid, m_filters))
            {
              refs.add(oid, PropertyIdentifier.objectName);
              refs.add(oid, PropertyIdentifier.presentValue);
              refs.add(oid, PropertyIdentifier.units);

              m_logger.finer("OID Object: " + oid.toString() + " objectype: " + oid.getObjectType().toString());
              if (oid.getObjectType().equals(ObjectType.trendLog))
              {
                m_logger.finer("Adding totalRecordCount and recordCount for oid: " + oid.toString());
                refs.add(oid, PropertyIdentifier.totalRecordCount);
                refs.add(oid, PropertyIdentifier.recordCount);
              }
            } else {
              m_logger.finest("Skipping OID, no filter matched: " + oid.toString());
            }
          }

          m_logger.fine("Start read properties");
          final long start = System.currentTimeMillis();

          m_logger.fine(String.format("Trying to read %d properties", refs.size()));


          PropertyValues pvs; 
          try {
            pvs = localDevice.readProperties(rd, refs);
            m_logger.fine(String.format("Properties read done in %d ms", System.currentTimeMillis() - start));
          } catch (BACnetException e) {
            // let's try breaking it up ourselves if it fails
            List<PropertyReferences> lprs = refs.getPropertiesPartitioned(1);

            pvs = new PropertyValues();
            for (PropertyReferences prs: lprs)
            {
              PropertyValues lpvs = localDevice.readProperties(rd, prs);

              for (ObjectPropertyReference opr : lpvs)
              {
                pvs.add(opr.getObjectIdentifier(), opr.getPropertyIdentifier(), opr.getPropertyArrayIndex(), 
                    lpvs.getNoErrorCheck(opr));
              }
            }
          }

          try {
            OID parent = buildObject(localDevice, rd, rd.getObjectIdentifier(), pvs);
            for (ObjectIdentifier oid : oids) {
              try {
                OID child = buildObject(localDevice, rd, oid, pvs);
                parent.children.add(child);
              } catch (Exception e) {
                m_logger.log(Level.SEVERE, "Error creating child object", e);
              }
            }

            printObject(parent, parent, w) ;
            com.google.gson.Gson gson = new com.google.gson.Gson();
            jsonw.println(gson.toJson(parent));
         

          } catch (Exception e) {
            m_logger.log(Level.SEVERE, "Error creating parent object", e);
          }
        } else {
          m_logger.finest("Skipping device, no filter matches: " + rd.toString());
        }

      } catch (BACnetException e) {
        m_logger.log(Level.SEVERE, "BACnetException", e);
      } catch (java.lang.InterruptedException e) {
        m_logger.info("Done Scanning Devices");
        break;
      }
    }

    localDevice.terminate();
  }

  private static void increment(byte[] ip) throws Exception {
    int value = (ip[3] & 0xff) + 1;
    if (value < 256)
      ip[3] = (byte) value;
    else {
      throw new Exception("done");
      /*
      ip[3] = 0;
      value = (ip[2] & 0xff) + 1;
      if (value < 256) {
        ip[2] = (byte) value;
        // System.out.println("C="+ value);
      }
      else {
        ip[2] = 0;
        value = (ip[1] & 0xff) + 1;
        if (value < 256) {
          ip[1] = (byte) value;
         System.out.println("B=" + value);
        }
        else {
          ip[1] = 0;
          value = (ip[0] & 0xff) + 1;
          if (value < 256) {
            ip[0] = (byte) value;
            System.out.println("A=" + value);
          }
          else
             throw new Exception("done");
        }
      }
      */
    }
  }

}
