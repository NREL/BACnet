package gov.nrel.bacnet.consumer;

import gov.nrel.bacnet.consumer.beans.Counter;
import gov.nrel.bacnet.consumer.beans.Delay;

import java.util.List;
import java.util.Collection;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

class TaskERunTaskENow implements Runnable, Callable<Object> {

	private static final Logger log = Logger.getLogger(TaskERunTaskENow.class.getName());
	private CountDownLatch latch;
	private List<TaskFPollDeviceTask> devices;
	private OurExecutor exec;
	
	public TaskERunTaskENow(OurExecutor exec, List<TaskFPollDeviceTask> devices, CountDownLatch latch) {
		this.exec = exec;
		this.devices = devices;
		this.latch = latch;
	}
	@Override
	public void run() {
		log.info("throwing task to start all devices polling into threadpool");
		exec.execute(this);
	}
	
	@Override
	public Object call() throws Exception {
		log.info("This thread is STUCK until ALL the TaskC have been read");
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e1) {
			log.log(Level.WARNING, "what, who interrupted us", e1);
		}
		
		log.info("now waiting on countdown latch");
		try {
			latch.await();
		} catch (InterruptedException e) {
			log.log(Level.WARNING, "what, who interrupted us", e);
		}
		
		log.info("Starting scheduled tasks now IF they are not scheduled already...numDevices="+devices.size());
		Counter latch = new Counter();
		for(TaskFPollDeviceTask st : devices) {
			st.setCounter(latch);
			schedule(st);
		}

		log.info("All devices have been scheduled="+devices.size());
		return null;
	}

	private void schedule(TaskFPollDeviceTask st) {
		try {
			scheduleImpl(st);
		} catch(Exception e) {
			log.log(Level.WARNING, "Device not scheduled="+st.getRemoteDevice()+" due to exception", e);
		}
	}
	
	private void scheduleImpl(TaskFPollDeviceTask st) {
		if(st.getFuture() == null) {
			if(st.getCachedOids().size() == 0) {
				log.info("Skipping scheduling of device="+st.getRemoteDevice()+" since it has no properties we want due to filtering");
				return;
			}
			
			Delay delay = st.initialize();
			
			//To uniformly distribute times across the same interval, we randomize the delay between 0 and interval...

			log.info("scheduling device="+st.getRemoteDevice()+" delay="+delay.getDelay()+" interval="+delay.getInterval());
			//since currently devices are sending broadcasts out, let's wait on sending anything...
			ScheduledExecutorService svc = exec.getScheduledSvc();
			Runnable task = st;
			//ScheduledFuture<?> future = svc.schedule(task, 0, TimeUnit.SECONDS);
			ScheduledFuture<?> future = svc.scheduleWithFixedDelay(task, delay.getDelay(), delay.getInterval(), TimeUnit.SECONDS);
			st.setFuture(future);
		}
	}

}
