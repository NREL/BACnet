package gov.nrel.consumer;

import java.util.List;

abstract class BACnetDataWriter {
	public synchronized void write(List<BACnetData> data) throws Exception
	{
	  writeImpl(data);
	}

	protected abstract void writeImpl(List<BACnetData> data) throws Exception;
}
