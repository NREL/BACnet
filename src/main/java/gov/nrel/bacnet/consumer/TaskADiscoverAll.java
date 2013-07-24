package gov.nrel.bacnet.consumer;

import gov.nrel.bacnet.Config;
import gov.nrel.bacnet.consumer.beans.JsonAllFilters;

import java.util.concurrent.Callable;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import java.util.List;
import java.util.Collection;

import com.serotonin.bacnet4j.LocalDevice;

class TaskADiscoverAll implements Runnable, Callable<Object>, TrackableTask {

	private static final Logger log = Logger.getLogger(TaskADiscoverAll.class.getName());
	private LocalDevice localDevice;
	private Config config;
	private JsonAllFilters deviceConfig;
	private Collection<BACnetDataWriter> writers;
	private OurExecutor exec;
	private TaskTracker tracker;
        private ScheduledFuture<?> future;
	private int trackableTaskId;
	private TaskBDiscoverer scan;

	public TaskADiscoverAll(LocalDevice localDevice, 
			OurExecutor exec, Config config, JsonAllFilters filters, Collection<BACnetDataWriter> writers,
			TaskTracker tracker) {
		this.localDevice = localDevice;
		this.exec = exec;
		this.config = config;
		this.deviceConfig = filters;
		this.writers = writers;
		this.tracker = tracker;
	}

	@Override
	public void run() {
		log.info("putting into the threadpool");
		exec.execute(this);
	}

	public String getDescription()
	{
		return "DiscoverAllTask: " + config.getMinId() + " to " + config.getMaxId();
	}

	public int getId()
	{
		return trackableTaskId;
	}

	public void setId(int id)
	{
		trackableTaskId = id;
	}

	@Override
	public Object call() throws Exception {
		log.info("Staring discovery to find new devices");
		int broadcastInterval = config.getBroadcastInterval();
		ScheduledExecutorService svc = exec.getScheduledSvc();
		scan = new TaskBDiscoverer(localDevice, exec, config, deviceConfig, writers, tracker);
		ScheduledFuture<?> future = svc.scheduleAtFixedRate(scan, 0, broadcastInterval, TimeUnit.SECONDS);
		scan.setFuture(future);
		return null;
	}

	public void cancelTask()
	{
		future.cancel(true);
		scan.getFuture().cancel(true);
	}

	public ScheduledFuture<?> getFuture()
	{
		return future;
	}

	public void setFuture(ScheduledFuture<?> t_future)
	{
		this.future = t_future;
	}

}
