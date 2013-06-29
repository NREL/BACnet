package gov.nrel.bacnet.consumer;

import java.util.List;
import com.serotonin.bacnet4j.RemoteDevice;

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



	protected abstract void deviceDiscoveredImpl(RemoteDevice data) throws Exception;

	protected abstract void oidsDiscoveredImpl(List<BACnetData> data) throws Exception;

}
