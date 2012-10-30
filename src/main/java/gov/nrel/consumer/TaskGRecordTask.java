package gov.nrel.consumer;

import gov.nrel.consumer.beans.DatabusBean;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.codehaus.jackson.map.ObjectMapper;

public class TaskGRecordTask implements Runnable {

	private static final Logger log = Logger.getLogger(TaskGRecordTask.class.getName());
	
	public final static String BACNET_PREFIX = "bacnet";
	private List<DatabusBean> data;
	private DataPointWriter writer;
	private DatabusSender sender;
	private static int counter = 0;
	private ObjectMapper mapper = new ObjectMapper();
	
	public TaskGRecordTask(List<DatabusBean> data, DataPointWriter writer2) {
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

	private void runImpl() throws Exception {
		try {
			sender.postData(data);
		} catch(Exception e) {
			log.log(Level.WARNING, "logging to file since to databus failed", e);
			logToFile();
		}
	}

	private void logToFile() throws Exception{
		for(DatabusBean b : data) {
			String json = mapper.writeValueAsString(b);
			writer.addDataPoint(json);
		}
	}
}
