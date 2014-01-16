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

import java.util.List;
import com.serotonin.bacnet4j.RemoteDevice;
import java.util.Arrays;

public abstract class BACnetDataWriter {
	public BACnetDataWriter() {
	}

	public synchronized void deviceDiscovered(RemoteDevice data) throws Exception
	{
		deviceDiscoveredImpl(data);
	}

	public synchronized void oidsDiscovered(List<BACnetData> data) throws Exception
	{
		oidsDiscoveredImpl(data);
	}


	public synchronized void write(RemoteDevice device, List<BACnetData> oids) throws Exception
	{
		writeImpl(device, oids);
	}

	public synchronized void writeWithParams(RemoteDevice device, List<BACnetData> oids, java.util.HashMap params) throws Exception
	{
		writeWithParamsImpl(device, oids, params);
	}

	public synchronized void write(RemoteDevice device, BACnetData []oids) throws Exception
	{
		writeImpl(device, Arrays.asList(oids));
	}

	public synchronized void writeWithParams(RemoteDevice device, BACnetData []oids, java.util.HashMap params) throws Exception
	{
		writeWithParamsImpl(device, Arrays.asList(oids), params);
	}

	protected abstract void deviceDiscoveredImpl(RemoteDevice data) throws Exception;

	protected abstract void oidsDiscoveredImpl(List<BACnetData> data) throws Exception;

	protected abstract void writeImpl(RemoteDevice device, List<BACnetData> oids) throws Exception;

	protected abstract void writeWithParamsImpl(RemoteDevice device, List<BACnetData> oids, java.util.HashMap params) throws Exception;
}
