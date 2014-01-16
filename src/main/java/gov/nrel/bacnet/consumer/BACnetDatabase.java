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

import com.serotonin.bacnet4j.type.primitive.ObjectIdentifier;
import com.serotonin.bacnet4j.RemoteDevice;

abstract public class BACnetDatabase extends BACnetDataWriter
{
	public synchronized RemoteDevice getDevice(int deviceId)
	{
		return getDeviceImpl(deviceId);
	}

	public synchronized RemoteDevice[] getDevices()
	{
		return getDevicesImpl();
	}

	public synchronized BACnetData getOID(int deviceId, com.serotonin.bacnet4j.type.primitive.ObjectIdentifier oid)
	{
		return getOIDImpl(deviceId, oid);
	}

	public synchronized BACnetData[] getOIDs(int deviceId)
	{
		return getOIDsImpl(deviceId);
	}

	abstract protected RemoteDevice getDeviceImpl(int deviceId);

	abstract protected RemoteDevice[] getDevicesImpl();

	abstract protected BACnetData getOIDImpl(int deviceId, com.serotonin.bacnet4j.type.primitive.ObjectIdentifier oid);

	abstract protected BACnetData[] getOIDsImpl(int deviceId);
}
