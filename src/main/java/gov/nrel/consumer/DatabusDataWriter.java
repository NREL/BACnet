package gov.nrel.consumer;

import gov.nrel.consumer.beans.DatabusBean;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.codehaus.jackson.map.ObjectMapper;



public class DatabusDataWriter extends BACnetDataWriter {
	private static final Logger log = Logger.getLogger(DatabusDataWriter.class.getName());

	private DataPointWriter writer;
	private DatabusSender sender;
	public final static String BACNET_PREFIX = "bacnet";
	private ObjectMapper mapper = new ObjectMapper();

	public DatabusDataWriter(DataPointWriter writer)
	{
		this.writer = writer;
		this.sender = writer.getSender();
	}

	public DatabusSender getSender()
	{
		return sender;
	}

	protected void writeImpl(List<BACnetData> data) throws Exception
	{
		List<DatabusBean> postdata = new ArrayList<DatabusBean>();
		for(BACnetData d : data) {
			String deviceId = BACNET_PREFIX+d.instanceNumber;
			String tableName = PropertiesReader.formTableName(deviceId, d.oid);

			String valStr = toString(d.value);
			Double dVal = null;
			if(valStr != null)
				dVal = new Double(valStr);
		
			DatabusBean bean = new DatabusBean();
			bean.setTableName(tableName);
			bean.setValue(dVal);
			bean.setTime(d.curTime);
			postdata.add(bean);
		}

		try {
			if (sender != null)
			{
				sender.postData(postdata);
			}
		} catch(Exception e) {
			log.log(Level.WARNING, "logging to file since to databus failed", e);
			logToFile(postdata);
		}
	}

	private void logToFile(List<DatabusBean> beans) throws Exception{
		for (DatabusBean b : beans) {	
			String json = mapper.writeValueAsString(b);
			writer.addDataPoint(json);
		}
	}

	private String toString(Object val) {
		if(val == null)
			return null;
		return ""+val;
	}
}




