package gov.nrel.bacnet.consumer;

import gov.nrel.bacnet.Config;
import gov.nrel.bacnet.consumer.beans.JsonAllFilters;

import java.util.concurrent.Callable;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import java.util.List;

import com.serotonin.bacnet4j.LocalDevice;

class TaskADiscoverAll implements Runnable, Callable<Object> {

	private static final Logger log = Logger.getLogger(TaskADiscoverAll.class.getName());
	private LocalDevice localDevice;
	private Config config;
	private JsonAllFilters deviceConfig;
	private List<BACnetDataWriter> writers;
	private OurExecutor exec;
	
	public TaskADiscoverAll(LocalDevice localDevice, 
			OurExecutor exec, Config config, JsonAllFilters filters, List<BACnetDataWriter> writers) {
		this.localDevice = localDevice;
		this.exec = exec;
		this.config = config;
		this.deviceConfig = filters;
		this.writers = writers;
	}

	@Override
	public void run() {
		log.info("putting into the threadpool");
		exec.execute(this);
	}

	@Override
	public Object call() throws Exception {
		log.info("Staring discovery to find new devices");
		int broadcastInterval = config.getBroadcastInterval();
		ScheduledExecutorService svc = exec.getScheduledSvc();
		TaskBDiscoverer scan = new TaskBDiscoverer(localDevice, exec, config, deviceConfig, writers);
		ScheduledFuture<?> future = svc.scheduleAtFixedRate(scan, 0, broadcastInterval, TimeUnit.SECONDS);
		scan.setFuture(future);
		return null;
	}

}