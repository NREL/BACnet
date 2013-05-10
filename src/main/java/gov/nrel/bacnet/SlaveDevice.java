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

import gov.nrel.consumer.beans.Numbers;
import gov.nrel.consumer.beans.Config;

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
import java.util.Calendar;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.TimeZone;
import java.util.Vector;
import java.util.Scanner;

import java.lang.Integer;

import com.serotonin.util.IpAddressUtils;
import com.serotonin.util.queue.*;

import com.serotonin.bacnet4j.LocalDevice;
import com.serotonin.bacnet4j.RemoteDevice;
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
import com.serotonin.bacnet4j.type.primitive.Time;
import com.serotonin.bacnet4j.type.primitive.Date;
import com.serotonin.bacnet4j.type.primitive.Unsigned16;
import com.serotonin.bacnet4j.type.primitive.UnsignedInteger;

import com.serotonin.bacnet4j.exception.BACnetException;
import java.sql.Timestamp;

import org.apache.commons.cli.*;

public class SlaveDevice extends java.util.TimerTask {
	private static final Logger logger = Logger.getLogger(SlaveDevice.class.getName());
	
	
	static class OIDs {
		Vector<OIDValue> data;
	}

	static class OIDValue {
		String uuid; // I'm not sure how these are intended to be used
		String source;

		String objectName; // name of bacnet object
		String objectType; // type of bacnet object
		String objectSource; // system call to execute to get value of object
	}

	static class OIDValueResponse {
		String value;
		int timeStamp; // which bacnet property do we actually want to set with this?
		String units;
	}

	class ObjectSource {
		public BACnetObject object;
		public String source;
	}

	private Vector<ObjectSource> m_objects;
	private static Config m_config;
	private static LocalDevice m_ld;

	EngineeringUnits stringToUnits(String value) throws java.text.ParseException 
	{
		for (EngineeringUnits units : EngineeringUnits.ALL) {
			if (units.toString().equalsIgnoreCase(value)) {
				return units;
			}
		}

		throw new java.text.ParseException("Unknown Units Type: " + value,
				0);
	
	}

	ObjectType stringToType(String value) throws java.text.ParseException {
		for (ObjectType type : ObjectType.ALL) {
			if (type.toString().equalsIgnoreCase(value)) {
				return type;
			}
		}

		throw new java.text.ParseException("Unknown Object Type: " + value,
				0);
	}

	public SlaveDevice(LocalDevice ld, Config config) {
		m_config = config;
		m_ld = ld;
		updateObjects();
		
	}
	
	public void updateObjects() {
		OIDs values = null;
		
		try {
			logger.log(Level.INFO, "Reading oid file " + m_config.getSlaveDeviceConfigFile());
			String oidData = gov.nrel.consumer.Main.readFile(m_config.getSlaveDeviceConfigFile(),
					Charset.forName("US-ASCII"));

			com.google.gson.Gson gson = new com.google.gson.Gson();

			java.lang.reflect.Type vectortype = new com.google.gson.reflect.TypeToken<OIDs>() { }.getType();
	
			values = gson.fromJson(oidData, vectortype);
		} catch (java.lang.Exception e) {
			logger.log(Level.SEVERE, "Error loading slave device configuration, but allowing to continue: ", e);
		}

		m_objects = new Vector<ObjectSource>();

		try {
			m_ld.getConfiguration().setProperty(
					PropertyIdentifier.objectName,
					new CharacterString("BuildingAgent Slave Device"));
			// .getConfiguration().setProperty(PropertyIdentifier.vendorIdentifier,
			// new Unsigned16(513));
			m_ld.getConfiguration().setProperty(
					PropertyIdentifier.segmentationSupported,
					Segmentation.segmentedBoth);

			if (values != null) {
				for (OIDValue value : values.data) {
					try {
						ObjectType ot = stringToType(value.objectType);
	
						BACnetObject o = new BACnetObject(m_ld,
								m_ld.getNextInstanceObjectIdentifier(ot));
						o.setProperty(PropertyIdentifier.objectName,
								new CharacterString(value.objectName));
						m_ld.addObject(o);
						ObjectSource os = new ObjectSource();
						os.object = o;
						os.source = value.objectSource;
						m_objects.add(os);
						logger.info("Created new bacnet object: "
								+ value.objectName);
					} catch (java.text.ParseException e) {
						logger.log(Level.SEVERE,
							"Error with creating new bacnet object", e);
					}
				}
			}
		} catch (java.lang.Exception e) {
		}	
	}

	String executeCommand(String cmd) {
		Runtime r = Runtime.getRuntime();

		String returnval = "";

		try {
			logger.fine("Executing command: " + cmd);
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
					logger.severe("Error executing process: "
							+ p.exitValue());
				}
			} catch (InterruptedException e) {
				logger.log(Level.SEVERE, "Error executing process: ", e);
			} finally {
				// Close the InputStream
				bufferedreader.close();
				inread.close();
				buf.close();
				in.close();
			}
		} catch (IOException e) {
			logger.log(Level.SEVERE, "Error executing process: ", e);
		}

		return returnval;
	}

	public void run() {
		updateObjects();
		updateValues();
	}

	public void updateValues() {
		for (ObjectSource os : m_objects) {
			try {
								
				String output = executeCommand(os.source);
				logger.info("Parsing result value: " + output);

				com.google.gson.Gson gson = new com.google.gson.Gson();

				java.lang.reflect.Type valuetype = new com.google.gson.reflect.TypeToken<OIDValueResponse>() { }.getType();

				OIDValueResponse response = gson.fromJson(output, valuetype);

				if (response.units != null && !response.units.equals("")) {
					os.object.setProperty(PropertyIdentifier.units,
							stringToUnits(response.units));
				}

				boolean somethingstuck = false;

				// try until we find something that sticks
				try {
					int v = Integer.parseInt(response.value);
					os.object.setProperty(
						PropertyIdentifier.presentValue,
						new com.serotonin.bacnet4j.type.primitive.UnsignedInteger(v));
					somethingstuck = true;
				} catch (Exception e) {
				}

				try {
					int v = Integer.parseInt(response.value);
					os.object.setProperty(
						PropertyIdentifier.presentValue,
						new com.serotonin.bacnet4j.type.primitive.SignedInteger(v));
					somethingstuck = true;
				} catch (Exception e) {
				}

				try {
					boolean v = java.lang.Boolean.getBoolean(response.value);
					os.object.setProperty(
						PropertyIdentifier.presentValue,
						new com.serotonin.bacnet4j.type.primitive.Boolean(v));
					somethingstuck = true;
				} catch (Exception e) {
				}

				try {
					boolean v = java.lang.Boolean.getBoolean(response.value);
					os.object.setProperty(PropertyIdentifier.presentValue,
						v ? BinaryPV.active : BinaryPV.inactive);
					somethingstuck = true;
				} catch (Exception e) {
				}

				try {
					float v = Float.parseFloat(response.value);
					os.object.setProperty(PropertyIdentifier.presentValue,
						new com.serotonin.bacnet4j.type.primitive.Real(v));
					somethingstuck = true;
				} catch (Exception e) {
				}

				try {
					double v = Double.parseDouble(response.value);
					os.object.setProperty(
						PropertyIdentifier.presentValue,
						new com.serotonin.bacnet4j.type.primitive.Double(v));
					somethingstuck = true;
				} catch (Exception e) {
				}

				try {
					os.object.setProperty(
						PropertyIdentifier.presentValue,
						new com.serotonin.bacnet4j.type.primitive.CharacterString(response.value));
					somethingstuck = true;
				} catch (Exception e) {
				}

				if (!somethingstuck) {
					os.object.setProperty(
						PropertyIdentifier.outOfService,
						new com.serotonin.bacnet4j.type.primitive.Boolean(true));
					throw new Exception("Unknown / unexpected type of data");
				}
				os.object.setProperty(PropertyIdentifier.outOfService,
						new com.serotonin.bacnet4j.type.primitive.Boolean(
								false));
			} catch (Exception e) {
				logger.log(Level.SEVERE, "Error updating value", e);
			}
		}
	}
}


