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

package gov.nrel.bacnet.consumer.beans;

import gov.nrel.bacnet.consumer.PropertiesReader;
import gov.nrel.bacnet.consumer.DatabusDataWriter;
import java.util.logging.Logger;
import java.util.ArrayList;
import java.util.List;

import com.serotonin.bacnet4j.RemoteDevice;
import com.serotonin.bacnet4j.type.primitive.ObjectIdentifier;

public class JsonAllFilters {

	private static final Logger log = Logger.getLogger(JsonAllFilters.class.getName());
	
	private List<JsonObjectData> filters = new ArrayList<JsonObjectData>();

	public List<JsonObjectData> getFilters() {
		return filters;
	}

	public void setFilters(List<JsonObjectData> allDevices) {
		this.filters = allDevices;
	}

	public int getPollingInterval(RemoteDevice remoteDevice, ObjectIdentifier oid) {
		Integer interval = null;
		for(JsonObjectData filter : filters) {
			if(filter.match(remoteDevice, oid))
				interval = filter.getInterval();
		}
		
		if(interval == null)
			interval = 120*60; //120 minutes * 60 seconds/minute
		
		log.info("For device number="+remoteDevice.getInstanceNumber()+" we are using an interval="+interval);
		return interval;
	}
	
}
