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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.Reader;
import java.io.BufferedInputStream;
import java.io.InputStream;

import java.net.NetworkInterface;
import java.net.InterfaceAddress;

import java.nio.charset.Charset;

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
import java.util.Scanner;

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
import com.serotonin.bacnet4j.type.primitive.OctetString;   

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
import java.sql.Timestamp;
import java.util.Date;

import org.apache.commons.cli.*;

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
    class ObjectSource
    {
      public BACnetObject object;
      public String source;
    }

    private Vector<ObjectSource> m_objects;
    Logger m_logger;

    EngineeringUnits stringToUnits(String value) throws java.text.ParseException
    {
      for (EngineeringUnits units : EngineeringUnits.ALL)
      {
        if (units.toString().equalsIgnoreCase(value))
        {
          return units;
        }
      }

      throw new java.text.ParseException("Unknown Units Type: " + value, 0);
    }

    ObjectType stringToType(String value) throws java.text.ParseException
    {
      for (ObjectType type : ObjectType.ALL)
      {
        if (type.toString().equalsIgnoreCase(value))
        {
          return type;
        }
      }

      throw new java.text.ParseException("Unknown Object Type: " + value, 0);
    }

    public SlaveDevice(LocalDevice ld, Vector<OIDValue> values)
    {
      m_logger = Logger.getLogger("BACnetScanner");
      m_objects = new Vector<ObjectSource>();

      try {
        ld.getConfiguration().setProperty(PropertyIdentifier.objectName,
            new CharacterString("BuildingAgent Slave Device"));
//        ld.getConfiguration().setProperty(PropertyIdentifier.vendorIdentifier,
//            new Unsigned16(513));
        ld.getConfiguration().setProperty(PropertyIdentifier.segmentationSupported,
            Segmentation.segmentedBoth);

        for (OIDValue value : values)
        {
          try {
            ObjectType ot = stringToType(value.objectType);

            BACnetObject o = new BACnetObject(ld, ld.getNextInstanceObjectIdentifier(ot));
            o.setProperty(PropertyIdentifier.objectName, new CharacterString(value.objectName));
            ld.addObject(o);
            ObjectSource os = new ObjectSource();
            os.object = o;
            os.source = value.objectSource;
            m_objects.add(os);
            m_logger.info("Created new bacnet object: " + value.objectName);
          } catch (java.text.ParseException e) {
            m_logger.log(Level.SEVERE, "Error with creating new bacnet object", e);
          }
        }
      } catch (java.lang.Exception e) {
      }
    } 

    String executeCommand(String cmd)
    {
      Runtime r = Runtime.getRuntime();

      String returnval = "";

      try {
        m_logger.fine("Executing command: " + cmd);
        Process p = r.exec(cmd);
        InputStream in = p.getInputStream();
        BufferedInputStream buf = new BufferedInputStream(in);
        InputStreamReader inread = new InputStreamReader(buf);
        BufferedReader bufferedreader = new BufferedReader(inread);

        // Read the ls output
        String line;
        while ((line = bufferedreader.readLine()) != null) {
          returnval += line + "\n";
        }

        try {
          if (p.waitFor() != 0) {
            m_logger.severe("Error executing process: " + p.exitValue());
          }
        } catch (InterruptedException e) {
          m_logger.log(Level.SEVERE, "Error executing process: ", e);
        } finally {
          // Close the InputStream
          bufferedreader.close();
          inread.close();
          buf.close();
          in.close();
        }
      } catch (IOException e) {
        m_logger.log(Level.SEVERE, "Error executing process: ", e);
      }

      return returnval;
    }

    public void updateValues()
    {
      for (ObjectSource os : m_objects)
      {
        try {
          String output = executeCommand(os.source);
          m_logger.info("Parsing result value: " + output);

          com.google.gson.Gson gson = new com.google.gson.Gson();

          java.lang.reflect.Type valuetype 
            = new com.google.gson.reflect.TypeToken<OIDValueResponse>() {}.getType();
          OIDValueResponse response = gson.fromJson(output, valuetype);

          if (response.units != null && !response.units.equals(""))
          {
            os.object.setProperty(PropertyIdentifier.units, stringToUnits(response.units));
          }

          boolean somethingstuck = false;

          // try until we find something that sticks
          try {
            int v = Integer.parseInt(response.value);
            os.object.setProperty(PropertyIdentifier.presentValue, new com.serotonin.bacnet4j.type.primitive.UnsignedInteger(v));
            somethingstuck = true;
          } catch (Exception e) { }

          try {
            int v = Integer.parseInt(response.value);
            os.object.setProperty(PropertyIdentifier.presentValue, new com.serotonin.bacnet4j.type.primitive.SignedInteger(v));
            somethingstuck = true;
          } catch (Exception e) { }

          try {
            boolean v = java.lang.Boolean.getBoolean(response.value);
            os.object.setProperty(PropertyIdentifier.presentValue, new com.serotonin.bacnet4j.type.primitive.Boolean(v));
            somethingstuck = true;
          } catch (Exception e) { }

          try {
            boolean v = java.lang.Boolean.getBoolean(response.value);
            os.object.setProperty(PropertyIdentifier.presentValue, v ? BinaryPV.active : BinaryPV.inactive);
            somethingstuck = true;
          } catch (Exception e) { }



          try {
            float v = Float.parseFloat(response.value);
            os.object.setProperty(PropertyIdentifier.presentValue, new com.serotonin.bacnet4j.type.primitive.Real(v));
            somethingstuck = true;
          } catch (Exception e) { }

          try {
            double v = Double.parseDouble(response.value);
            os.object.setProperty(PropertyIdentifier.presentValue, new com.serotonin.bacnet4j.type.primitive.Double(v));
            somethingstuck = true;
          } catch (Exception e) { }

          try {
            os.object.setProperty(PropertyIdentifier.presentValue, new com.serotonin.bacnet4j.type.primitive.CharacterString(response.value));
            somethingstuck = true;
          } catch (Exception e) { }

          if (!somethingstuck)
          {
            throw new Exception("Unknown / unexpected type of data");
          }
        } catch (Exception e) {
          m_logger.log(Level.SEVERE, "Error updating value", e);
        }
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
      m_logger = Logger.getLogger("BACnetScanner");
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
  private LocalDevice m_localDevice;
  private java.util.Map<RemoteOID, java.util.Date> m_oidScanTimes;
  private int m_defaultTimeBetweenScans;

  public Scan(LocalDevice ld, int min, int max, Vector<DeviceFilter> filters, int defaultTime)
  {
    m_localDevice = ld;
    m_logger = Logger.getLogger("BACnetScanner");
    m_filters = filters;
    m_min = min;
    m_max = max;
    m_oidScanTimes = new java.util.HashMap<RemoteOID, java.util.Date>();
    m_defaultTimeBetweenScans = defaultTime;
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

  static class RemoteOID {
    int instanceNumber;
    UnsignedInteger networkNumber;
    OctetString macAddress;
    byte[] networkAddress;

    ObjectType objectType;
    int objectNumber;


    @Override public int hashCode()
    {
      return instanceNumber 
        ^ (networkNumber == null?0:networkNumber.hashCode())
        ^ (macAddress == null?0:macAddress.hashCode())
        ^ (networkAddress == null?0:java.util.Arrays.hashCode(networkAddress))
        ^ (objectType == null?0:objectType.hashCode())
        ^ objectNumber;
    }

    @Override public boolean equals(Object o)
    {
      if (this == o) { return true; }

      if (!(o instanceof RemoteOID)) { return false; }

      RemoteOID that = (RemoteOID)o;

      return instanceNumber == that.instanceNumber
        && m_equals(networkNumber, that.networkNumber)
        && m_equals(macAddress, that.macAddress)
        && m_equals(networkAddress, that.networkAddress)
        && m_equals(objectType, that.objectType)
        && objectNumber == that.objectNumber;
    }

    private static <T> boolean m_equals(T t_lhs, T t_rhs)
    {
      if (t_lhs == null && t_rhs == null) { return true; }

      if (t_lhs == null || t_rhs == null) { return false; }

      return t_lhs.equals(t_rhs);
    }

    public RemoteOID(RemoteDevice t_rd, ObjectIdentifier t_oid)
    {
      instanceNumber = t_rd.getInstanceNumber();

      networkNumber = null;
      macAddress = null;
      networkAddress = null;

      Address a = t_rd.getAddress();

      if (a != null)
      {
        networkNumber = a.getNetworkNumber();
        macAddress = a.getMacAddress();
      }

      Network n = t_rd.getNetwork();

      if (n != null)
      {
        networkAddress = n.getNetworkAddress();
      }

      if (t_oid != null)
      {
        objectType = t_oid.getObjectType();
        objectNumber = t_oid.getInstanceNumber();
      } else {
        objectType = null;
        objectNumber = 0;
      }
    }
  }

  static class OIDFilter {
    String objectType;
    String instanceNumber;
    int timeBetweenScans;

    public boolean matches(ObjectIdentifier t_oid)
    {
      String strObjectType = t_oid.getObjectType().toString();
      String strInstanceNumber = Integer.toString(t_oid.getInstanceNumber());

      return Pattern.matches(objectType, strObjectType)
        && Pattern.matches(instanceNumber, strInstanceNumber);
    }
  }

  static class OIDValue {
    String objectName;
    String objectType;
    String objectSource;
  }

  static class OIDValueResponse {
    String value;
    String timeStamp;
    String units;
  }

  static class DeviceFilter {
    String instanceNumber;
    String networkNumber;
    String macAddress;
    String networkAddress;

    int timeBetweenScans;

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

    CommandLineParser parser = new PosixParser();
    Options options = new Options();
    options.addOption("m", "min-device-id", true, "Minimum device ID to scan for, default is -1, or 0 if only a max is specified");
    options.addOption("M", "max-device-id", true, "Maximum device ID to scan for, default is -1");
    options.addOption("i", "id", true, "Device ID of this software, default is 1234");
    options.addOption("D", "device-id", true, "device ID to scan, exclusive of min-device-id and max-device-id");
    options.addOption("f", "filter-file", true, "JSON filter file to use during scanning");
    options.addOption("d", "dev", true, "Network device to use for broadcasts, default is eth0");
    options.addOption("e", "example-filter-file", true, "Write an example JSON filter file out and exit, with the given filename");
    options.addOption("s", "scan", false, "Enable scanning feature");
    options.addOption("S", "slave-device", false, "Enable slave device feature");
    options.addOption("n", "num-scans", true, "Number of scans to perform, default is 1, -1 scans indefinitely");
    options.addOption("t", "time-between-updates", true, "Amount of time (in ms) to wait between finishing one scan and starting another. This time is also used for the update interval for the slave device values. Default is 10000ms");
    options.addOption("F", "oid-file", true, "JSON oid file to use for the slave device configuration");
    options.addOption("E", "example-oid-file", true, "Write an example JSON oid inset file out and exit, with the given filename");
    options.addOption("v", "verbose", false, "Verbose logging (Info Level). Default is warning and error logging.");
    options.addOption("vv", "very-verbose", false, "Very verbose logging (All Levels). Default is warning and error logging.");

    int min = -1;
    int max = -1;
    int time_between_updates = 10000;
    int num_scans = 1;
    int device_id = 1234;

    String devname = null;
    Vector<DeviceFilter> filters = null; 
    Vector<OIDValue> oidvalues = null; 

    Level log_level = Level.WARNING;

    boolean scan = false;
    boolean slave_device = false;

    try {
      CommandLine line = parser.parse(options, args);

      if (line.hasOption("e"))
      {
        System.out.println("Writing example JSON filter file to: " + line.getOptionValue("e"));

        java.io.FileOutputStream jsonfile = null;
        java.io.PrintWriter jsonw = null;

        try {
          jsonfile = new java.io.FileOutputStream(line.getOptionValue("e"));
        } catch (Exception e) {
          System.out.println("Error writing example JSON file");
          System.exit(-1);
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
          f.timeBetweenScans = 30000;

          OIDFilter of = new OIDFilter();
          of.objectType = "Binary .*";
          of.instanceNumber = "1.*";
          of.timeBetweenScans = 60000;
          f.oidFilters = new Vector<OIDFilter>();
          f.oidFilters.add(of);
          examplefilters.add(f);
          jsonw.println(gson.toJson(examplefilters));
        }
      }


      if (line.hasOption("E"))
      {
        System.out.println("Writing example JSON oid file to: " + line.getOptionValue("E"));

        java.io.FileOutputStream jsonfile = null;
        java.io.PrintWriter jsonw = null;

        try {
          jsonfile = new java.io.FileOutputStream(line.getOptionValue("E"));
        } catch (Exception e) {
          System.out.println("Error writing example JSON oid file");
          System.exit(-1);
        }

        if (jsonfile != null)
        {
          jsonw = new java.io.PrintWriter(jsonfile, true);
          com.google.gson.Gson gson = new com.google.gson.Gson();
          Vector<OIDValue> exampleoids = new Vector<OIDValue>();
          OIDValue i = new OIDValue();
          i.objectName = "some_object";
          i.objectType = "analog inset";
          i.objectSource = "echo {value:\"72.5\", timestamp:\"2012-02-01 12:00:00\", units:\"degrees fahrenheit\"}";
          exampleoids.add(i);

          OIDValue i2 = new OIDValue();
          i2.objectName = "some_object 2";
          i2.objectType = "binary inset";
          i2.objectSource = "echo {value:\"true\", timestamp:\"2012-02-01 12:00:00\", units:\"\"}";


          exampleoids.add(i2);
          jsonw.println(gson.toJson(exampleoids));
        }

      }

      if (line.hasOption("e") || line.hasOption("E"))
      {
        System.exit(0);
      }

      time_between_updates = Integer.parseInt(line.getOptionValue("t", "10000"));
      num_scans = Integer.parseInt(line.getOptionValue("n", "1"));
      device_id = Integer.parseInt(line.getOptionValue("i", "1234"));
      scan = line.hasOption("s");
      slave_device = line.hasOption("S");

      min = Integer.parseInt(line.getOptionValue("m", "-1")); 
      max = min;

      max = Integer.parseInt(line.getOptionValue("M", "-1"));

      if (min == -1 && max > -1)
      {
        min = 0;
      }

      if (line.hasOption("m") && !line.hasOption("M"))
      {
        HelpFormatter hp = new HelpFormatter();
        hp.printHelp("Syntax:", "Error: a max-device-id must be specified if a min-device-id is specified", options, "", true);
        System.exit(-1);
      }

      if (max < min)
      {
        HelpFormatter hp = new HelpFormatter();
        hp.printHelp("Syntax:", "Error: max-device-id cannot be less than min-device-id", options, "", true);
        System.exit(-1);
      }

      if (line.hasOption("D") && (line.hasOption("m") || line.hasOption("M")))
      {
        HelpFormatter hp = new HelpFormatter();
        hp.printHelp("Syntax:", "Error: you cannot specify both a specific device-id and a min-device-id or max-device-id", options, "", true);
        System.exit(-1);
      }

      if (line.hasOption("D"))
      {
        min = max = Integer.parseInt(line.getOptionValue("D"));
      }

      if (line.hasOption("f"))
      {
        System.out.println("Loading JSON filter file: " + line.getOptionValue("f"));
        String filterfile = readFile(line.getOptionValue("f"), Charset.forName("US-ASCII"));
        com.google.gson.Gson gson = new com.google.gson.Gson();

        java.lang.reflect.Type vectortype 
          = new com.google.gson.reflect.TypeToken<Vector<DeviceFilter>>() {}.getType();
        filters = gson.fromJson(filterfile, vectortype);
      }

      if (line.hasOption("F"))
      {
        System.out.println("Loading JSON oid file: " + line.getOptionValue("F"));
        String filterfile = readFile(line.getOptionValue("F"), Charset.forName("US-ASCII"));
        com.google.gson.Gson gson = new com.google.gson.Gson();

        java.lang.reflect.Type vectortype 
          = new com.google.gson.reflect.TypeToken<Vector<OIDValue>>() {}.getType();
        oidvalues = gson.fromJson(filterfile, vectortype);
      }

      devname = line.getOptionValue("dev", "eth0");

      if (line.hasOption("v"))
      {
        log_level = Level.INFO;
      }

      if (line.hasOption("vv"))
      {
        log_level = Level.ALL;
      }

    } catch (Exception e) {
      HelpFormatter hp = new HelpFormatter();
      hp.printHelp("Syntax:", options, true);
      System.exit(-1);
    }


    Logger logger = Logger.getLogger("BACnetScanner");

    try {
      FileHandler fh = new FileHandler("LogFile.log", true);
      logger.addHandler(fh);
      SimpleFormatter formatter = new SimpleFormatter();
      fh.setFormatter(formatter);
    } catch (Exception e) {
      logger.log(Level.SEVERE, "Unable to create log file", e);
    }


    logger.setLevel(log_level);


    NetworkInterface networkinterface = null;
  
    try {
      networkinterface = java.net.NetworkInterface.getByName(devname);
    } catch (Exception ex) {
      System.out.println("Unable to open device: " + devname);
      System.exit(-1);
    }


    List<InterfaceAddress> addresses = networkinterface.getInterfaceAddresses();

    String sbroadcast = null;
    String saddress = null;
    InterfaceAddress ifaceaddr = null;

    for (InterfaceAddress address: addresses) {
      logger.fine("Evaluating address: " + address.toString());
      if (address.getAddress().getAddress().length == 4)
      {
        logger.info("Address is ipv4, selecting: " + address.toString());
        sbroadcast = address.getBroadcast().toString().substring(1);
        saddress = address.getAddress().toString().substring(1);
        ifaceaddr = address;
        break;
      } else {
        logger.info("Address is not ipv4, not selecting: " + address.toString());
      }
    }

    logger.info("Vendor ID is hardcoded by bacnet4j to serotonin's (236) it should be possible to change it if necessary");
    logger.info("Binding to: " + saddress + " " + sbroadcast);

    logger.severe("We cannot bind to the specific interface, bacnet4j doesn't work when we do");
 
      
    LocalDevice localDevice = new LocalDevice(device_id, sbroadcast);
    localDevice.setPort(LocalDevice.DEFAULT_PORT);
    localDevice.setTimeout(localDevice.getTimeout() * 3);
    localDevice.setSegTimeout(localDevice.getSegTimeout() * 3);
    try {
      localDevice.initialize();
    }
    catch (IOException e) {
      e.printStackTrace();
      return;
    }    
 
    Scan s = null;
     
    if (scan)
    {
      logger.info("Creating scanner object");
      s = new Scan(localDevice, min, max, filters, time_between_updates);
    }

    SlaveDevice sd = null;

    if (slave_device)
    {
      logger.info("Creating slave device object");
      sd = new SlaveDevice(localDevice, oidvalues);
    }


    java.util.Date lastSlaveUpdate = null;

    for (int i=0; s!=null && (i < num_scans || num_scans == -1); ++i)
    {
      logger.info("Beginning scan " + (i+1) + " of " + num_scans);

      if (slave_device)
      {
        boolean doupdate = false;
        java.util.Date now = new java.util.Date();
        if (lastSlaveUpdate == null)
        {
          doupdate = true;
        } else {
          if (now.getTime() - lastSlaveUpdate.getTime() >= time_between_updates)
          {
            doupdate = true;
          }
        }
          

        if (doupdate)
        {
          logger.info("Updating slave device");
          sd.updateValues();
          lastSlaveUpdate = now;
        }
      }

      s.run();

      Thread.sleep(time_between_updates);
    }

    logger.info("Scanning complete");

    // keep running if we have a slave_device
    while (slave_device)
    {
      boolean doupdate = false;
      java.util.Date now = new java.util.Date();
      if (lastSlaveUpdate == null)
      {
        doupdate = true;
      } else {
        if (now.getTime() - lastSlaveUpdate.getTime() >= time_between_updates)
        {
          doupdate = true;
        }
      }

      if (doupdate)
      {
        logger.info("Updating slave device");
        sd.updateValues();
        lastSlaveUpdate = now;
      }

      Thread.sleep(1000);
    }

    logger.info("Shutting down");
    localDevice.terminate();

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
      m_logger.fine("trendLogRecord encodable Data: " + lr.getEncodable().toString());
      return lr.getEncodable().toString();
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
        m_logger.log(Level.INFO, "Error with reading trend log from device", e);
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

  private int deviceMatches(RemoteDevice t_rd, Vector<DeviceFilter> t_filters)
  {
    return oidMatches(t_rd, null, t_filters);
  }

  private int oidMatches(RemoteDevice t_rd, ObjectIdentifier t_id, Vector<DeviceFilter> t_filters)
  {
    if (t_filters == null)
    {
      return 0;
    }

    for (DeviceFilter filter : t_filters) {
      if (filter.matches(t_rd))
      {
        if (t_id == null)
        {
          return filter.timeBetweenScans;
        } else {
          if (filter.oidFilters.isEmpty())
          {
            // if there's no oidfilters, accept all of them for this device
            return filter.timeBetweenScans;
          } else {
            for (OIDFilter oidFilter : filter.oidFilters)
            {
              if (oidFilter.matches(t_id))
              {
                return oidFilter.timeBetweenScans;
              }
            }
          }
        }
      }
    }

    return -1;
  }

  private void broadcastWhois(LocalDevice t_localDevice, int low, int high)
  {
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

    try {
      t_localDevice.sendBroadcast(whois);
    } catch (BACnetException e) {
      m_logger.log(Level.WARNING, "Exception occured with broadcast", e);
    }

    /*
    InetAddress curaddr = startAddress(m_ifaceAddr);
    InetAddress end = endAddress(m_ifaceAddr);
    while (lessThan(curaddr, end)) {
      String ipstr = curaddr.getHostAddress();
      t_localDevice.setBroadcastAddress(ipstr);
      m_logger.fine("Scanning IP: " + ipstr);
      try {
        t_localDevice.sendBroadcast(whois);
      }
      catch (BACnetException e) {
        m_logger.log(Level.WARNING, "Exception occured with ip " + ipstr, e);
      }

      try {
        increment(curaddr);
      }
      catch (Exception e) {
        m_logger.info("Done pinging devices");
        break;
      }
    }
*/
    

  }

  private void run() {
    m_logger.info("Scanning device ids: " + m_min + " to " + m_max);

  
    Handler h = new Handler();
    m_localDevice.getEventHandler().addListener(h);
    m_localDevice.setExceptionListener(new MyExceptionListener());


    broadcastWhois(m_localDevice, m_min, m_max);

    BlockingQueue<RemoteDevice> remoteDevices = h.remoteDevices();

    java.util.Date date = new java.util.Date();
    String csv_outputname = "output-" + new Timestamp(date.getTime()).toString() + ".csv";
    String json_outputname = "output-" + new Timestamp(date.getTime()).toString() + ".json";

    java.io.FileOutputStream file = null;
    java.io.PrintWriter w = null;
    try {
      file = new java.io.FileOutputStream(csv_outputname);
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
      jsonfile = new java.io.FileOutputStream(json_outputname);
    } catch (Exception e) {
      m_logger.log(Level.SEVERE, "Error opening output file", e);
    }

    if (jsonfile != null)
    {
      jsonw = new java.io.PrintWriter(jsonfile, true);
    }

    w.println(new Timestamp(date.getTime()));
    w.println("Device OID, Device Name, Object Type, Object Instance Number, Object Name, Object Value, Trend Log Date, Trend Log Time");

    while (true)
    {
      try {
        RemoteDevice rd = remoteDevices.poll(10, TimeUnit.SECONDS);

        if (rd == null)
        {
          break;
        }

        int min = rd.getInstanceNumber() - 500;
        int max = rd.getInstanceNumber() + 500;

        broadcastWhois(m_localDevice, min, max);

        boolean doscandevice = false;
        int deviceTimeBetweenScans = deviceMatches(rd, m_filters);

        if (deviceTimeBetweenScans >= 0)
        {
          if (deviceTimeBetweenScans == 0)
          {
            deviceTimeBetweenScans = m_defaultTimeBetweenScans;
          }

          RemoteOID ro = new RemoteOID(rd, null);

          java.util.Date lastscan = m_oidScanTimes.get(ro);
          java.util.Date thisscan = new java.util.Date();

          if (lastscan != null)
          {
            long ms = thisscan.getTime() - lastscan.getTime();
            if (ms >= deviceTimeBetweenScans)
            {
              doscandevice = true;
            } else {
              m_logger.finest("Skipping device, it's only been " + ms + " required: " + deviceTimeBetweenScans);
            }
          } else {
            doscandevice = true;
            m_logger.finest("Scanning device, it has never been scanned.");
          }
        }

        if (doscandevice)
        {
          m_oidScanTimes.put(new RemoteOID(rd, null), new java.util.Date());

          m_logger.info("Getting device info: " + rd);
          m_localDevice.getExtendedDeviceInformation(rd);

          @SuppressWarnings("unchecked")
            List<ObjectIdentifier> oids = ((SequenceOf<ObjectIdentifier>) m_localDevice.sendReadPropertyAllowNull(rd, rd.getObjectIdentifier(), PropertyIdentifier.objectList)).getValues();

          m_logger.info("Getting device info: " + rd);
          m_logger.fine("Oids found: " + Integer.toString(oids.size()));
          PropertyReferences refs = new PropertyReferences();
          // add the property references of the "device object" to the list
          refs.add(rd.getObjectIdentifier(), PropertyIdentifier.objectName);

          // and now from all objects under the device object >> ai0, ai1,bi0,bi1...
          for (ObjectIdentifier oid : oids) {
            int timeBetweenScans = oidMatches(rd, oid, m_filters);

            if (timeBetweenScans >= 0)
            {
              if (timeBetweenScans == 0)
              {
                timeBetweenScans = m_defaultTimeBetweenScans;
              }

              RemoteOID ro = new RemoteOID(rd, oid);

              boolean doscan = false;

              java.util.Date lastscan = m_oidScanTimes.get(ro);
              java.util.Date thisscan = new java.util.Date();

              if (lastscan != null)
              {
                long ms = thisscan.getTime() - lastscan.getTime();
                if (ms >= timeBetweenScans)
                {
                  doscan = true;
                } else {
                  m_logger.finest("Skipping OID, it's only been " + ms + " required: " + timeBetweenScans);
                }
              } else {
                m_logger.finest("Scanning OID, it has never been scanned.");
                doscan = true;
              }

              if (doscan)
              {
                m_oidScanTimes.put(ro, thisscan);
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
            pvs = m_localDevice.readProperties(rd, refs);
            m_logger.fine(String.format("Properties read done in %d ms", System.currentTimeMillis() - start));
          } catch (BACnetException e) {
            // let's try breaking it up ourselves if it fails
            List<PropertyReferences> lprs = refs.getPropertiesPartitioned(1);

            pvs = new PropertyValues();
            for (PropertyReferences prs: lprs)
            {
              PropertyValues lpvs = m_localDevice.readProperties(rd, prs);

              for (ObjectPropertyReference opr : lpvs)
              {
                pvs.add(opr.getObjectIdentifier(), opr.getPropertyIdentifier(), opr.getPropertyArrayIndex(), 
                    lpvs.getNoErrorCheck(opr));
              }
            }
          }

          try {
            OID parent = buildObject(m_localDevice, rd, rd.getObjectIdentifier(), pvs);
            for (ObjectIdentifier oid : oids) {
              try {
                OID child = buildObject(m_localDevice, rd, oid, pvs);
                parent.children.add(child);
              } catch (Exception e) {
                m_logger.log(Level.FINE, "Error creating child object", e);
              }
            }

            printObject(parent, parent, w) ;
            com.google.gson.Gson gson = new com.google.gson.Gson();
            jsonw.println(gson.toJson(parent));
         

          } catch (Exception e) {
            m_logger.log(Level.FINE, "Error creating parent objectWARNING", e);
          }
        } else {
          m_logger.finest("Skipping device, no filter matches: " + rd.toString());
        }

      } catch (BACnetException e) {
        m_logger.log(Level.WARNING, "BACnetException", e);
      } catch (java.lang.InterruptedException e) {
        m_logger.info("Done Scanning Devices");
        break;
      }
    }

    // Send CSV files to building agent
    BASender ba_sender = new BASender();
    com.google.gson.JsonObject jObj = new com.google.gson.JsonObject();
    System.out.println("attempting to send data to boa");
    System.out.println("CSV File is " + csv_outputname);
    try {
      String output = new Scanner( new File("./" + csv_outputname) ).useDelimiter("\\Z").next();
      ba_sender.sendData(output);
    } catch (Exception e) {
      System.out.println("could not open file to send: " + e.getMessage());
    }

  }


}
