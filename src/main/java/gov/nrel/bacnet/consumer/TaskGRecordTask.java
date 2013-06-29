package gov.nrel.bacnet.consumer;

import gov.nrel.bacnet.consumer.beans.DatabusBean;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.codehaus.jackson.map.ObjectMapper;



public class TaskGRecordTask implements Runnable {

	private static final Logger log = Logger.getLogger(TaskGRecordTask.class.getName());
	
	private List<BACnetData> data;
	private List<BACnetDataWriter> writers;

	private static int counter = 0;
	
	public TaskGRecordTask(List<BACnetData> data, List<BACnetDataWriter> writers) {
		this.writers = writers;
		this.data = data;
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
		for (BACnetDataWriter writer : writers) {
			writer.oidsDiscovered(data);
		}
	}

}
