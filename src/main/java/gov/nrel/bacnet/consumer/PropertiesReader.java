/*
 * Copyright (C) 2013, Alliance for Sustainable Energy
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package gov.nrel.bacnet.consumer;

import gov.nrel.bacnet.consumer.beans.Device;
import gov.nrel.bacnet.consumer.beans.ObjKey;
import gov.nrel.bacnet.consumer.beans.Stream;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.serotonin.bacnet4j.LocalDevice;
import com.serotonin.bacnet4j.exception.BACnetException;
import com.serotonin.bacnet4j.type.Encodable;
import com.serotonin.bacnet4j.type.constructed.ObjectPropertyReference;
import com.serotonin.bacnet4j.type.enumerated.PropertyIdentifier;
import com.serotonin.bacnet4j.type.primitive.ObjectIdentifier;
import com.serotonin.bacnet4j.util.PropertyReferences;
import com.serotonin.bacnet4j.util.PropertyValues;

public class PropertiesReader {

	private static final Logger log = Logger.getLogger(PropertiesReader.class.getName());
	private LocalDevice m_localDevice;
	
	public PropertiesReader(LocalDevice local) {
		this.m_localDevice = local;
	}
	
	private void createStream(ObjectIdentifier oid,
			Map<ObjKey, Encodable> properties, Device dev, List<Stream> streams) {
		Encodable objectName = properties.get(new ObjKey(oid, PropertyIdentifier.objectName));
		Encodable units = properties.get(new ObjKey(oid, PropertyIdentifier.units));

		String deviceId = dev.getDeviceId();
		String tableName = formTableName(deviceId, oid);
		
		Stream str = new Stream();
		str.setTableName(tableName);
		str.setStreamDescription(""+objectName);
		str.setUnits(""+units);
		str.setDevice(deviceId);
		str.setStreamType(oid.getObjectType()+"");
		str.setStreamId(oid.getInstanceNumber()+"");
		
		streams.add(str);
	}
	
	public static String formTableName(String deviceId, ObjectIdentifier oid) {
		String objectType = oid.getObjectType()+"";
		objectType = objectType.replace(" ","");
		objectType = objectType.replace("-","");
		String objectId = ""+oid.getInstanceNumber();
		String tableName = deviceId+objectType+objectId;
		return tableName;
	}
}
