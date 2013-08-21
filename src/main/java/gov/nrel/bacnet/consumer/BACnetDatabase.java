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
