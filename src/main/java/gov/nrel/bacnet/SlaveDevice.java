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


public class SlaveDevice {

  static class SlaveDeviceImpl {
    private BACnetObject ai0;
    private BACnetObject ai1;
    private BACnetObject bi0;
    private BACnetObject bi1;

    private BACnetObject mso0;
    private BACnetObject ao0;
    private BACnetObject av0;
    private FileObject file0;
    private BACnetObject bv1;

    public SlaveDeviceImpl(LocalDevice ld)
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

  private Logger m_logger;
  private java.net.NetworkInterface m_networkInterface;

  public SlaveDevice(java.net.NetworkInterface ni)
  {
    m_logger = Logger.getLogger("BACnetSlaveDevicener");
    m_networkInterface = ni;
  }


  public static void main(String[] args) throws Exception {
    if (args.length < 1)
    {
      System.out.println("Usage: <ethernetdevice>\n");
      System.exit(-1);
    }

    NetworkInterface networkinterface = null;
   
    try {
      networkinterface = java.net.NetworkInterface.getByName(args[0]);
    } catch (Exception ex) {
      System.out.println("Unable to open device: " + args[0]);
      System.exit(-1);
    }


    SlaveDevice s = new SlaveDevice(networkinterface);
    s.run();
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

    List<InterfaceAddress> addresses = m_networkInterface.getInterfaceAddresses();

    String sbroadcast = null;
    String saddress = null;

    for (InterfaceAddress address: addresses) {
      m_logger.fine("Evaluating address: " + address.toString());
      if (address.getAddress().getAddress().length == 4)
      {
        m_logger.info("Address is ipv4, selecting: " + address.toString());
        sbroadcast = address.getBroadcast().toString().substring(1);
        saddress = address.getAddress().toString().substring(1);
        break;
      } else {
        m_logger.info("Address is not ipv4, not selecting: " + address.toString());
      }
    }

    m_logger.info("Binding to: " + saddress + " " + sbroadcast);

    m_logger.severe("We cannot bind to the specific interface, bacnet4j doesn't work when we do");
    LocalDevice localDevice = new LocalDevice(1234, sbroadcast);
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

    SlaveDeviceImpl sd = new SlaveDeviceImpl(localDevice);

    while (true)
    {
      try {
        Thread.sleep(10000); // update values and such here
        sd.updateValues();
      } catch (Exception e) {
        m_logger.log(Level.SEVERE, "Exception while updating values", e);
      }

   }

    //    localDevice.terminate();
  }


}
