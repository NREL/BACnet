package gov.nrel.consumer;

import gov.nrel.bacnet.DatabusSender;
import gov.nrel.bacnet.Numbers;
import gov.nrel.consumer.beans.JsonObjectData;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class TaskGRecordTask implements Runnable {

	private static final Logger log = Logger.getLogger(TaskGRecordTask.class.getName());
	
	private final static String BACNET_PREFIX = "bacnet";
	private List<JsonObjectData> data;
	private DataPointWriter writer;
	private static DatabusSender sender;
	private static int counter = 0;
	
	public TaskGRecordTask(List<JsonObjectData> data, DataPointWriter writer2) {
		this.data = data;
		this.writer = writer2;

		synchronized(TaskGRecordTask.class) {
			if(sender == null)
				sender = new DatabusSender();
		}
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

	private void runImpl() {
		for(JsonObjectData d : data) {
			writeIt(d);
		}
	}

	private void writeIt(JsonObjectData d) {
		try {
			
			sendToDatabus(d);
			
//			com.google.gson.Gson gson = new com.google.gson.Gson();
//			String json = gson.toJson(d);
//			writer.addDataPoint(json);
		} catch(Exception e) {
			log.log(Level.WARNING, "Exception recording", e);
		}
	}

	private void sendToDatabus(JsonObjectData d) {
		String tableName = BACNET_PREFIX+d.getDeviceId()+d.getObjectType()+d.getObjectId();
		String device = BACNET_PREFIX + d.getDeviceId();
		String address = d.getAddress();
		Numbers n = new Numbers();
		sender.sendData(tableName, d.getTime(), d.getValue(), 
						d.getUnits(), device, d.getObjectName(), 
						d.getDeviceName(), address, n);
	}
}
