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

	public synchronized void writeWithParam(RemoteDevice device, List<BACnetData> oids, String param) throws Exception
	{
		writeWithParamImpl(device, oids, param);
	}

	public synchronized void write(RemoteDevice device, BACnetData []oids) throws Exception
	{
		writeImpl(device, Arrays.asList(oids));
	}

	public synchronized void writeWithParam(RemoteDevice device, BACnetData []oids, String param) throws Exception
	{
		writeWithParamImpl(device, Arrays.asList(oids), param);
	}

	protected abstract void deviceDiscoveredImpl(RemoteDevice data) throws Exception;

	protected abstract void oidsDiscoveredImpl(List<BACnetData> data) throws Exception;

	protected abstract void writeImpl(RemoteDevice device, List<BACnetData> oids) throws Exception;

	protected abstract void writeWithParamImpl(RemoteDevice device, List<BACnetData> oids, String param) throws Exception;
}
