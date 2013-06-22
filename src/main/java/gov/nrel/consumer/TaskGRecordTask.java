package gov.nrel.consumer;

import gov.nrel.consumer.beans.DatabusBean;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.codehaus.jackson.map.ObjectMapper;

public class TaskGRecordTask implements Runnable {

	private static final Logger log = Logger.getLogger(TaskGRecordTask.class.getName());
	
	public final static String BACNET_PREFIX = "bacnet";
	private List<BACnetData> data;
	private DataPointWriter writer;
	private DatabusSender sender;
	private static int counter = 0;
	private ObjectMapper mapper = new ObjectMapper();
	
	public TaskGRecordTask(List<BACnetData> data, DataPointWriter writer2) {
		this.data = data;
		this.writer = writer2;
		this.sender = writer2.getSender();
	}

	private synchronized static int getNextCount() {
		return counter++;
	}
	
	@Override
	public void run() {
		long start = System.currentTimeMillis();
		int count = getNextCount();
		try {
			runImpl();
		} catch(Exception e) {
			log.log(Level.WARNING, "Exception", e);
		}
		long total = System.currentTimeMillis()-start;
		log.info("wrote to databus. counter="+count+" total time="+total);
	}

	private String toString(Object val) {
		if(val == null)
			return null;
		return ""+val;
	}

	private void runImpl() throws Exception {
		List<DatabusBean> postdata = new ArrayList<DatabusBean>();
		for(BACnetData d : data) {
			String deviceId = TaskGRecordTask.BACNET_PREFIX+d.instanceNumber;
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
}
