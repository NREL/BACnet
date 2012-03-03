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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.HashSet;
import java.util.logging.*;
import com.serotonin.bacnet4j.service.confirmed.ReinitializeDeviceRequest.ReinitializedStateOfDevice;
import com.serotonin.bacnet4j.LocalDevice;
import com.serotonin.bacnet4j.RemoteDevice;
import com.serotonin.bacnet4j.RemoteObject;
import com.serotonin.bacnet4j.event.DeviceEventListener;
import com.serotonin.bacnet4j.exception.BACnetException;
import com.serotonin.bacnet4j.obj.BACnetObject;
import com.serotonin.bacnet4j.type.Encodable;
import com.serotonin.bacnet4j.event.DeviceEventListener;
import com.serotonin.bacnet4j.event.DefaultDeviceEventListener;
import com.serotonin.bacnet4j.event.DefaultExceptionListener;
import com.serotonin.bacnet4j.exception.BACnetException;
import com.serotonin.bacnet4j.service.unconfirmed.WhoIsRequest;
import com.serotonin.util.IpAddressUtils;
import com.serotonin.util.queue.*;
import com.serotonin.bacnet4j.type.constructed.Choice;
import com.serotonin.bacnet4j.type.constructed.DateTime;
import com.serotonin.bacnet4j.type.constructed.ObjectPropertyReference;
import com.serotonin.bacnet4j.type.constructed.PropertyValue;
import com.serotonin.bacnet4j.type.constructed.SequenceOf;
import com.serotonin.bacnet4j.type.constructed.TimeStamp;
import com.serotonin.bacnet4j.type.enumerated.EventState;
import com.serotonin.bacnet4j.type.enumerated.EventType;
import com.serotonin.bacnet4j.type.enumerated.MessagePriority;
import com.serotonin.bacnet4j.type.enumerated.NotifyType;
import com.serotonin.bacnet4j.type.enumerated.PropertyIdentifier;
import com.serotonin.bacnet4j.type.enumerated.Segmentation;
import com.serotonin.bacnet4j.type.notificationParameters.NotificationParameters;
import com.serotonin.bacnet4j.type.primitive.Boolean;
import com.serotonin.bacnet4j.type.primitive.CharacterString;
import com.serotonin.bacnet4j.type.primitive.ObjectIdentifier;
import com.serotonin.bacnet4j.type.primitive.UnsignedInteger;
import com.serotonin.bacnet4j.type.primitive.Unsigned16;
import com.serotonin.bacnet4j.util.PropertyReferences;
import com.serotonin.bacnet4j.util.PropertyValues;


public class Scan {
  class MyExceptionListener extends DefaultExceptionListener {  
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

  static class Handler extends DefaultDeviceEventListener {
    private BlockingQueue<RemoteDevice> devices = new LinkedBlockingQueue<RemoteDevice>();
    private HashSet<RemoteDevice> founddevices = new HashSet<RemoteDevice>();

    @Override
      public void iAmReceived(RemoteDevice d) {
        System.out.println("RemoteDevice found: " + d);

        try {
          if (!founddevices.contains(d))
          {
            founddevices.add(d);
            devices.put(d);
          } else {
            System.out.println("Device already queued for scanning: " + d);
          }
        } catch (java.lang.InterruptedException e) {
        }
      }

    public BlockingQueue<RemoteDevice> remoteDevices()
    {
      return devices;
    }
  }


  private Logger logger;

  public static void main(String[] args) throws Exception {
    Scan s = new Scan();
    s.run();
  }

  private void printObject(ObjectIdentifier parentoid, ObjectIdentifier oid, PropertyValues pvs,
      java.io.PrintWriter writer) {

    try {
      Encodable parentObjectName = pvs.get(parentoid, PropertyIdentifier.objectName);
      Encodable objectName = pvs.get(oid, PropertyIdentifier.objectName);

      Encodable presentValue = null;
      Encodable units = null;

      try {
        presentValue = pvs.get(oid, PropertyIdentifier.presentValue);
        units = pvs.get(oid, PropertyIdentifier.units);
      } catch (Exception e) {
      }


      writer.println(String.format("%s, %s, %s, %s, %s, %s, %s", 
            parentoid,
            parentObjectName,
            oid.getObjectType(),
            oid.getInstanceNumber(),
            objectName,
            presentValue,
            units));
    } catch (Exception e) {
    }


    System.out.println(String.format("\t%s", oid));
    for (ObjectPropertyReference opr : pvs) {
      if (oid.equals(opr.getObjectIdentifier())) {
        System.out.println(String.format("\t\t%s = %s", opr.getPropertyIdentifier().toString(), 
            pvs.getNoErrorCheck(opr)));
      }

    }
  }


  private void run() {
    logger = Logger.getLogger("BACnetScanner");

    try {
      FileHandler fh = new FileHandler("LogFile.log", true);
      logger.addHandler(fh);
      SimpleFormatter formatter = new SimpleFormatter();
      fh.setFormatter(formatter);
    } catch (Exception e) {
      logger.log(Level.SEVERE, "Unable to create log file", e);
    }

    logger.setLevel(Level.ALL);

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


    byte[] ip = new byte[4];
    ip[0] = (byte) 192; // A
    ip[1] = (byte) 168; // B
    ip[2] = (byte) 2; // C
    ip[3] = (byte) 0; // D

    WhoIsRequest whois = new WhoIsRequest();
    while (true) {
      String ipstr = IpAddressUtils.toIpString(ip);
      localDevice.setBroadcastAddress(ipstr);
      logger.fine("Scanning IP: " + ipstr);
      try {
        localDevice.sendBroadcast(whois);
      }
      catch (BACnetException e) {
        logger.log(Level.WARNING, "Exception occured with ip " + ipstr, e);
      }

      try {
        increment(ip);
      }
      catch (Exception e) {
        logger.info("Done pinging devices");
        break;
      }
    }

    BlockingQueue<RemoteDevice> remoteDevices = h.remoteDevices();


    java.io.FileOutputStream file = null;
    java.io.PrintWriter w = null;
    try {
      file = new java.io.FileOutputStream("output.csv");
    } catch (Exception e) {
      logger.log(Level.SEVERE, "Error opening output file", e);
    }

    if (file != null)
    {
      w = new java.io.PrintWriter(file, true);
    }

    w.println("Device OID, Device Name, Object Type, Object Instance Number, Object Name, Object Value");

    while (true)
    {
      try {
        RemoteDevice rd = remoteDevices.poll(300, TimeUnit.SECONDS);
        if (rd == null)
        {
          break;
        }
        logger.info("Getting device info: " + rd);
        localDevice.getExtendedDeviceInformation(rd);

        @SuppressWarnings("unchecked")
        List<ObjectIdentifier> oids = ((SequenceOf<ObjectIdentifier>) localDevice.sendReadPropertyAllowNull(rd, rd.getObjectIdentifier(), PropertyIdentifier.objectList)).getValues();

        logger.info("Getting device info: " + rd);
        logger.fine("Oids found: " + Integer.toString(oids.size()));
        PropertyReferences refs = new PropertyReferences();
        // add the property references of the "device object" to the list
        refs.add(rd.getObjectIdentifier(), PropertyIdentifier.objectName);

        // and now from all objects under the device object >> ai0, ai1,bi0,bi1...
        for (ObjectIdentifier oid : oids) {
          refs.add(oid, PropertyIdentifier.objectName);
          refs.add(oid, PropertyIdentifier.presentValue);
          refs.add(oid, PropertyIdentifier.units);
//          refs.add(oid, PropertyIdentifier.maximumValue);
//          refs.add(oid, PropertyIdentifier.minimumValue);
//          refs.add(oid, PropertyIdentifier.averageValue);
//          refs.add(oid, PropertyIdentifier.maximumValueTimestamp);
//          refs.add(oid, PropertyIdentifier.minimumValueTimestamp);
        }

        logger.fine("Start read properties");
        final long start = System.currentTimeMillis();

        logger.fine(String.format("Trying to read %d properties", refs.size()));

      
        PropertyValues pvs; 
        try {
          pvs = localDevice.readProperties(rd, refs);
          logger.fine(String.format("Properties read done in %d ms", System.currentTimeMillis() - start));
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

        printObject(rd.getObjectIdentifier(), rd.getObjectIdentifier(), pvs, w);
        for (ObjectIdentifier oid : oids) {
          printObject(rd.getObjectIdentifier(), oid, pvs, w);
        }

      } catch (BACnetException e) {
        logger.log(Level.SEVERE, "BACnetException", e);
      } catch (java.lang.InterruptedException e) {
        logger.info("Done Scanning Devices");
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
