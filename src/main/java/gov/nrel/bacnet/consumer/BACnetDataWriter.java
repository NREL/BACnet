package gov.nrel.bacnet.consumer;

import java.util.List;

public abstract class BACnetDataWriter {
	public BACnetDataWriter() {
	}

	public synchronized void write(List<BACnetData> data) throws Exception
	{
	  writeImpl(data);
	}

	protected abstract void writeImpl(List<BACnetData> data) throws Exception;
}
