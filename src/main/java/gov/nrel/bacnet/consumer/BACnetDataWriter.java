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
