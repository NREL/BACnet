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

import gov.nrel.consumer.beans.Config;
import gov.nrel.consumer.BACnet;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.BufferedInputStream;
import java.io.InputStream;

import java.nio.charset.Charset;

import java.util.logging.*;
import java.util.Vector;

import java.lang.Integer;

import com.serotonin.bacnet4j.LocalDevice;


import com.serotonin.bacnet4j.obj.BACnetObject;

import com.serotonin.bacnet4j.type.enumerated.BinaryPV;
import com.serotonin.bacnet4j.type.enumerated.EngineeringUnits;
import com.serotonin.bacnet4j.type.enumerated.ObjectType;
import com.serotonin.bacnet4j.type.enumerated.PropertyIdentifier;
import com.serotonin.bacnet4j.type.enumerated.Segmentation;
import com.serotonin.bacnet4j.type.primitive.CharacterString;



public class SlaveDevice extends java.util.TimerTask {
	private static final Logger logger = Logger.getLogger(SlaveDevice.class.getName());
	
	
	private static class OIDs {
		Vector<OIDValue> data;
	}

	private static class OIDValue {
		String uuid; // I'm not sure how these are intended to be used
		String source;

		String objectName; // name of bacnet object
		String objectType; // type of bacnet object
		String objectSource; // system call to execute to get value of object
	}

	private static class OIDValueResponse {
		String value;
		int timeStamp; // which bacnet property do we actually want to set with this?
		String units;
	}

	private class ObjectSource {
		public BACnetObject object;
		public String source;
		public String name;
		public String type;

		public String toString()
		{
		  	return "(" + object.toString() + ": " + source.toString() + ")";
		}

	}

	private Vector<ObjectSource> m_objects;
	private static Config m_config;
	private static LocalDevice m_ld;

	private EngineeringUnits stringToUnits(String value) throws java.text.ParseException 
	{
		for (EngineeringUnits units : EngineeringUnits.ALL) {
			if (units.toString().equalsIgnoreCase(value)) {
				return units;
			}
		}

		throw new java.text.ParseException("Unknown Units Type: " + value,
				0);
	
	}

	private ObjectType stringToType(String value) throws java.text.ParseException {
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
	
	private void updateObjects() {
		OIDs values = null;

		if (m_objects == null)
		{
			m_objects = new Vector<ObjectSource>();
		}
		
		try {
			logger.log(Level.INFO, "Reading oid file " + m_config.getSlaveDeviceConfigFile());
			String oidData = gov.nrel.consumer.BACnet.readFile(m_config.getSlaveDeviceConfigFile(),
					Charset.forName("US-ASCII"));

			com.google.gson.Gson gson = new com.google.gson.Gson();

			java.lang.reflect.Type vectortype = new com.google.gson.reflect.TypeToken<OIDs>() { }.getType();
	
			values = gson.fromJson(oidData, vectortype);
		} catch (java.lang.Exception e) {
			logger.log(Level.SEVERE, "Error loading slave device configuration, but allowing to continue: ", e);
		}

		Vector<ObjectSource> newobjects = new Vector<ObjectSource>();

		try {
			m_ld.getConfiguration().setProperty(
					PropertyIdentifier.objectName,
					new CharacterString("Building Agent Slave Device"));
			// .getConfiguration().setProperty(PropertyIdentifier.vendorIdentifier,
			// new Unsigned16(513));
			m_ld.getConfiguration().setProperty(
					PropertyIdentifier.segmentationSupported,
					Segmentation.segmentedBoth);

			if (values != null) {
				for (OIDValue value : values.data) {
					try {
						boolean needstobeadded = true;

						for (ObjectSource existingObject : m_objects) {
							if (existingObject.name.equals(value.objectName)) {
								// this is an object we already have
								if (existingObject.type.equals(value.objectType)) {
									needstobeadded = false;
									// and the types match
									if (existingObject.source.equals(value.objectSource)) {
										// and the source matches too, nothing to do
										newobjects.add(existingObject);
									} else {
										logger.log(Level.INFO, "Upating object: " + existingObject.name 
										    + " source from: " + existingObject.source + " to " + value.objectSource);
										existingObject.source = value.objectSource;
										newobjects.add(existingObject);
									}
								} else {
									logger.log(Level.INFO, "Object exists, but with a different type, recreating: " 
									    + existingObject.name);
								}

								break;
							}
						}

						if (needstobeadded)
						{
							ObjectType ot = stringToType(value.objectType);
	
							BACnetObject o = new BACnetObject(m_ld,
									m_ld.getNextInstanceObjectIdentifier(ot));
							o.setProperty(PropertyIdentifier.objectName,
									new CharacterString(value.objectName));
							ObjectSource os = new ObjectSource();
							os.name = value.objectName;
							os.type = value.objectType;
							os.object = o;
							os.source = value.objectSource;
							newobjects.add(os);
							logger.info("Created new bacnet object: "
									+ value.objectName);
							m_ld.addObject(o);
						}
					} catch (java.text.ParseException e) {
						logger.log(Level.SEVERE,
							"Error with creating new bacnet object", e);
					}
				}
			}


			for (ObjectSource os : m_objects) {
				boolean found = false;

				for (ObjectSource newos : newobjects) {
					if (os.name.equals(newos.name)
					    && os.type.equals(newos.type)
					    && os.source.equals(newos.source))
					{
						found = true;
						break;
					}
				}

				if (!found)
				{
					logger.log(Level.INFO, "Removing no longer used object: " + os.name + " " + os.type);
					m_ld.removeObject(os.object.getId());
				}
			}


			m_objects.clear();
			m_objects.addAll(newobjects);

		} catch (java.lang.Exception e) {
			logger.log(Level.SEVERE,
				"Error creating new bacnet object", e);
		}	
	}

	private String executeCommand(String cmd) {
		Runtime r = Runtime.getRuntime();

		String returnval = "";

		logger.fine("Executing command: " + cmd);
		Process p = null;
		InputStream in = null;
		BufferedInputStream buf = null;
		InputStreamReader inread = null;
		BufferedReader bufferedreader = null;

		try {
			p = r.exec(cmd);
			in = p.getInputStream();
			buf = new BufferedInputStream(in);
			inread = new InputStreamReader(buf);
			bufferedreader = new BufferedReader(inread);

			// Read the output
			String line;
			while ((line = bufferedreader.readLine()) != null) {
				returnval += line + "\n";
			}
			if (p.waitFor() != 0) {
				logger.severe("Error executing process: "
						+ p.exitValue());
			}
		} catch (InterruptedException e) {
			logger.log(Level.SEVERE, "Error executing process: ", e);
		} catch (IOException e) {
			logger.log(Level.SEVERE, "Error creating process: ", e);
		} finally {
			// Close the InputStream
			if (bufferedreader != null)
			{
				try {
					bufferedreader.close();
				} catch (Exception e) {
					logger.log(Level.SEVERE, "Error closing bufferedreader: ", e);
				}
				bufferedreader = null;
			}
			if (inread != null)
			{
				try {
					inread.close();
				} catch (Exception e) {
					logger.log(Level.SEVERE, "Error closing inread: ", e);
				}
				inread = null;
			}

			if (buf != null)
			{
				try {
					buf.close();
				} catch (Exception e) {
					logger.log(Level.SEVERE, "Error closing buf: ", e);
				}
				buf = null;
			}

			if (in != null)
			{
				try {
					in.close();
				} catch (Exception e) {
					logger.log(Level.SEVERE, "Error closing in: ", e);
				}
				in = null;
			}

			if (p != null)
			{
				p.destroy();
				p = null;
			}
		}

		return returnval;
	}

	public void run() {
		updateObjects();
		updateValues();
	}

	private void updateValues() {
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


