package gov.nrel.consumer;

import gov.nrel.consumer.beans.Config;
import gov.nrel.consumer.beans.JsonAllFilters;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.serotonin.bacnet4j.LocalDevice;
import com.serotonin.bacnet4j.Network;
import com.serotonin.bacnet4j.RemoteDevice;
import com.serotonin.bacnet4j.event.DefaultDeviceEventListener;
import com.serotonin.bacnet4j.exception.BACnetException;
import com.serotonin.bacnet4j.service.unconfirmed.WhoIsRequest;
import com.serotonin.bacnet4j.type.constructed.Address;
import com.serotonin.bacnet4j.type.primitive.UnsignedInteger;

public class TaskBDiscoverer implements Runnable, Callable<Object> {

	private static final Logger log = Logger.getLogger(TaskBDiscoverer.class.getName());
	
	private LocalDevice m_localDevice;
	private NewDeviceHandler newDeviceListener;
	private int counter = 0;
	private ScheduledFuture<?> future;
	private int range;
	private JsonAllFilters deviceConfig;
	private Config config;
	private DataPointWriter dataPointWriter;
	private OurExecutor exec;
	
	public TaskBDiscoverer(LocalDevice localDevice, OurExecutor exec, Config config, JsonAllFilters deviceConfig2, DataPointWriter writer) {
		this.m_localDevice = localDevice;
		this.deviceConfig = deviceConfig2;
		this.config = config;
		this.range= config.getRange();
		this.dataPointWriter = writer;
		this.exec = exec;
		
		newDeviceListener = new NewDeviceHandler();
		m_localDevice.getEventHandler().addListener(newDeviceListener);
	}
	
	public void run() {
		log.info("putting scan for devices on into threadpool");
		exec.execute(this);
	}

	@Override
	public Object call() throws Exception {
		try {
			log.info("kicking off scan for devices");
			scanImpl();
			return null;
		} finally {
			counter+=range;
		}
	}
	
	public List<TaskFPollDeviceTask> getLatestDiscoveredDevices() {
		if(newDeviceListener == null)
			throw new IllegalStateException("You need to call scan method first");
		return newDeviceListener.getLatestDiscoveredDevices();
	}
	
	private void scanImpl() throws BACnetException {
		//At 25000, increase the range to 1000000 as there are no devices in that range...
		// NL: updated to at least 65000 to allow for more devices to be detected
		if(counter > 65000)
			range = 1000000;

		if(counter > 4000000) {
			log.info("future="+future);
			future.cancel(true);
			
			nowGetAllProperties();
			return;
		}
		
		int min = counter;
		int max = counter+range-1;
		broadcastWhois(m_localDevice, min, max);
		log.info("sent broadcast request, will be receiving responses for who knows how long");
	}

	private void nowGetAllProperties() {
		List<TaskFPollDeviceTask> devices = this.newDeviceListener.getLatestDiscoveredDevices();
		
		List<List<TaskFPollDeviceTask>> partitions = create(devices, config.getNumThreads());
		
		SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd-hh-mm-ss.SSS");
		String time = fmt.format(new Date());
		String fileName = "../logs/jsonInMemory-"+time+".json";
		
		int counter = 0;
		ScheduledExecutorService svc = exec.getScheduledSvc();
		log.info("partitions size="+partitions.size()+" numThreads="+config.getNumThreads()+" numDevices="+devices.size());
		CountDownLatch latch = new CountDownLatch(partitions.size()); //+1 is for the failures
		for(List<TaskFPollDeviceTask> partition : partitions) {
			log.info("Scheduling partition to run immediately.  size="+partition.size());
			Runnable taskC = new TaskCReadBasicProps(counter, m_localDevice, exec, partition, deviceConfig, latch, dataPointWriter);
			svc.schedule(taskC, 0, TimeUnit.SECONDS);
			counter++;
		}

//		//NOTE: We KNOW the thread count so this runs 
//		//we should process the failures now to see if we can get them to succeed now
//		svc.schedule(new TaskCRerun(m_localDevice, failures, null, deviceConfig, fileName, latch), 0, TimeUnit.SECONDS);
//		
		//schedule a task that will wait on all good runs+ 1 failures run...
		Runnable task = new TaskERunTaskENow(exec, devices, latch);
		svc.schedule(task, 10, TimeUnit.SECONDS);
	}
	
	private List<List<TaskFPollDeviceTask>> create(List<TaskFPollDeviceTask> devices,
			int numThreads) {
		int numInPartition = (devices.size() / numThreads)+1; //914 devices / 10 threads+1 = 92 in each partition(last will have leftovers)
		List<List<TaskFPollDeviceTask>> partitions = new ArrayList<List<TaskFPollDeviceTask>>();
		List<TaskFPollDeviceTask> partition = new ArrayList<TaskFPollDeviceTask>();
		List<Integer> sizes = new ArrayList<Integer>();
		for(int i = 0; i < devices.size(); i++) {
			TaskFPollDeviceTask task = devices.get(i);
			partition.add(task);
			
			//Now if partition is full OR if this is the last task, add the partition to the list...
			if(partition.size() >= numInPartition ||
					i == devices.size()-1) {
				sizes.add(partition.size());
				partitions.add(partition);
				partition = new ArrayList<TaskFPollDeviceTask>();
			}
		}
		
		log.info(" devices count="+devices.size()+" threadcnt="+numThreads+" numInPartition="+numInPartition+" numParts="+partitions.size()+" partSizes="+sizes);
		return partitions;
	}

	private void broadcastWhois(LocalDevice t_localDevice, int low, int high) throws BACnetException {
		WhoIsRequest whois;

		if (low == -1 && high == -1) {
			whois = new WhoIsRequest();
		} else if(low < 0) {
			throw new IllegalArgumentException("low end cannot be less than 0. low="+low);
		} else if(high <= low) {
			throw new IllegalArgumentException("high must be greater than low.  high="+high+" low="+low);
		} else {
			log.info("Scanning device ids: " + low + " to " + high);

			whois = new WhoIsRequest(new UnsignedInteger(low),
					new UnsignedInteger(high));
		}

		t_localDevice.sendBroadcast(whois);
	}
	
	class NewDeviceHandler extends DefaultDeviceEventListener {
		private HashSet<TaskFPollDeviceTask> deviceStates = new HashSet<TaskFPollDeviceTask>();
		private HashSet<RemoteDevice> devices = new HashSet<RemoteDevice>();
		
		public synchronized List<TaskFPollDeviceTask> getLatestDiscoveredDevices() {
			//deep clone here so it does not screw with any iterators(or foreach loops)
			
			List<TaskFPollDeviceTask> devices = new ArrayList<TaskFPollDeviceTask>();
			for(TaskFPollDeviceTask d : deviceStates) {
				devices.add(d);
			}
			return devices;
		}

		/**
		 * This is called when a new device sends us a message that he exists in response to a broadcast request...
		 */
		@Override
		public void iAmReceived(RemoteDevice d) {
			try {
				iAmReceivedImpl(d);
			} catch (Exception e) {
				log.log(Level.WARNING, "exception", e);
			}
		}
		
		private synchronized void iAmReceivedImpl(RemoteDevice d) throws BACnetException {
			if(!devices.add(d)) {
				log.info("Device already queued for scanning: " + d);
			} else {
				int numDevices = devices.size();
				log.info("Device found: "+d+" total devices="+numDevices);
				
				TaskFPollDeviceTask st = new TaskFPollDeviceTask(d, m_localDevice, exec, dataPointWriter);
				deviceStates.add(st);
			}
		}

		@Override
		public void whoIsRequest(Address from, Network network,	UnsignedInteger min, UnsignedInteger max) {
			log.log(Level.INFO, "Someone sent who is requests="+from.toIpPortString()+" network="+network+"  min="+min+" max="+max);
		}
	}

	public void setFuture(ScheduledFuture<?> future) {
		log.info("set future="+future);
		this.future = future;
	}

}
