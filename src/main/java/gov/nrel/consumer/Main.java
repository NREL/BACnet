package gov.nrel.consumer;

import gov.nrel.consumer.beans.Config;
import gov.nrel.consumer.beans.JsonAllFilters;
import gov.nrel.consumer.beans.JsonObjectData;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.nio.charset.Charset;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import com.serotonin.bacnet4j.LocalDevice;
import com.serotonin.bacnet4j.event.DefaultExceptionListener;
import com.serotonin.bacnet4j.type.primitive.UnsignedInteger;
import com.serotonin.util.queue.ByteQueue;

public class Main {

	private static final Logger logger = Logger.getLogger(Main.class.getName());
	
	private ScheduledThreadPoolExecutor svc;
	private ExecutorService execSvc;
	private ExecutorService recorderSvc;
	
	public static void main(String[] args) throws SecurityException, IOException {
		logger.info("starting.  Loading logging.properties first to log to a file in logs directory");
		
		FileInputStream configFile = new FileInputStream("../conf/logging.properties");
		LogManager.getLogManager().readConfiguration(configFile);
		
		logger.info("Starting now that logger properties are loaded");

		String network = "eth4";
		int scanInterval = 168;
		int broadcastInterval = 1;
		int range = 100;
		int numThreads = 10;
		Config config = new Config(scanInterval, broadcastInterval, range, numThreads, network);
		try {
			LocalDevice.setExceptionListener(new MyExceptionListener());
			new Main().start(config);
		} catch(Throwable e) {
			logger.log(Level.WARNING, "exception starting", e);
		}
	}

	private void start(Config config) throws IOException {
		LinkedBlockingQueue<Runnable> queue = new LinkedBlockingQueue<Runnable>(1000);
		RejectedExecutionHandler rejectedExec = new RejectedExecHandler();
		recorderSvc = new ThreadPoolExecutor(20, 20, 120, TimeUnit.SECONDS, queue, rejectedExec );
		
		svc = (ScheduledThreadPoolExecutor) Executors.newScheduledThreadPool(1);
		execSvc = Executors.newFixedThreadPool(config.getNumThreads());
		OurExecutor exec = new OurExecutor(svc, execSvc, recorderSvc);
		String devname = config.getNetwork();
		//no need to put this in property I think...
		int device_id = 11234;
		NetworkInterface networkinterface = null;

		try {
			networkinterface = java.net.NetworkInterface.getByName(devname);
		} catch (Exception ex) {
			System.out.println("Unable to open device: " + devname);
			System.exit(-1);
		}

		if (networkinterface == null) {
			System.out.println("Unable to open device: " + devname);
			System.exit(-1);
		}

		List<InterfaceAddress> addresses = networkinterface
				.getInterfaceAddresses();

		String sbroadcast = null;
		String saddress = null;
		//InterfaceAddress ifaceaddr = null;

		for (InterfaceAddress address : addresses) {
			logger.fine("Evaluating address: " + address.toString());
			if (address.getAddress().getAddress().length == 4) {
				logger.info("Address is ipv4, selecting: " + address.toString());
				sbroadcast = address.getBroadcast().toString().substring(1);
				saddress = address.getAddress().toString().substring(1);
				//ifaceaddr = address;
				break;
			} else {
				logger.info("Address is not ipv4, not selecting: "
						+ address.toString());
			}
		}

		logger.info("Vendor ID is hardcoded by bacnet4j to serotonin's (236) it should be possible to change it if necessary");
		logger.info("Binding to: " + saddress + " " + sbroadcast);

		logger.severe("We cannot bind to the specific interface, bacnet4j doesn't work when we do");

		LocalDevice localDevice = new LocalDevice(device_id, sbroadcast);
		localDevice.setPort(LocalDevice.DEFAULT_PORT);
		localDevice.setTimeout(localDevice.getTimeout() * 3);
		localDevice.setSegTimeout(localDevice.getSegTimeout() * 3);
		try {
			localDevice.initialize();
			localDevice.setRetries(0); //don't retry as it seems to really be a waste.
		} catch (IOException e) {
			e.printStackTrace();
			return;
		}

		String file = "../conf/filter.json";
		String filterfile = readFile(file, Charset.forName("US-ASCII"));
		com.google.gson.Gson gson = new com.google.gson.Gson();

		JsonAllFilters filters = gson.fromJson(filterfile, JsonAllFilters.class);

		int counter = 0;
		for(JsonObjectData d : filters.getFilters()) {
			if(d.getInterval() == 0)
				throw new IllegalArgumentException("Filter cannot have interval value of 0 or null.  It must be less than 0 to disable and greater than 0 to enable in seconds");
			else if(d.getInterval() > 0 && d.getInterval() < 15)
				throw new IllegalArgumentException("oh come on, give the system a break...under 15 seconds is really pushing it or change this exception I guess");
			logger.info("filter"+counter+"="+d.getInterval()+" devId="+d.getDeviceId()+" objId="+d.getObjectId()+" objType="+d.getObjectType());
			
			//initialize regular expression matchers...
			d.init();
		}
		
		String streamTable = "bacnetstreamMeta";
		String deviceTable = "bacnetdeviceMeta";

		String username = "robot-bacnet";
		String key = "941RCGC.B2.1WWXM5WZVA5YL";
		
		logger.info("user="+username+" key="+key);
		DatabusSender sender = new DatabusSender(username, key, deviceTable, streamTable, recorderSvc);
		DataPointWriter writer = new DataPointWriter(sender);
		
		logger.info("Kicking off scanner object to run every "+config.getScanInterval()+" hours with broadcasts every "+config.getBroadcastInterval()+" seconds");
		TaskADiscoverAll all = new TaskADiscoverAll(localDevice, exec, config, filters, writer);
		svc.scheduleAtFixedRate(all, 0, config.getScanInterval(), TimeUnit.HOURS);
	}
	
	public static String readFile(String file, Charset cs) throws IOException {
		// No real need to close the BufferedReader/InputStreamReader
		// as they're only wrapping the stream
		FileInputStream stream = new FileInputStream(file);
		try {
			Reader reader = new BufferedReader(
					new InputStreamReader(stream, cs));
			StringBuilder builder = new StringBuilder();
			char[] buffer = new char[8192];
			int read;
			while ((read = reader.read(buffer, 0, buffer.length)) > 0) {
				builder.append(buffer, 0, read);
			}
			return builder.toString();
		} finally {
			// Potential issue here: if this throws an IOException,
			// it will mask any others. Normally I'd use a utility
			// method which would log exceptions and swallow them
			stream.close();
		}
	}
	
	static class MyExceptionListener extends DefaultExceptionListener {
		
		@Override
		public void unimplementedVendorService(UnsignedInteger vendorId,
				UnsignedInteger serviceNumber, ByteQueue queue) {
			if (vendorId.intValue() == 8 && serviceNumber.intValue() == 1) {
				// do nothing
				logger.info("do nothing...unimplemented vendor service="+vendorId+" service="+serviceNumber);
			} else {
		        logger.info("Received unimplemented vendor service: vendor id=" + vendorId + ", service number="
		                + serviceNumber + ", bytes (with context id)=" + queue);
			}
		}
		
	    public void receivedException(Exception e) {
	        logger.log(Level.WARNING, "Exception", e);
	    }

	    public void receivedThrowable(Throwable t) {
	    	logger.log(Level.WARNING, "Exc", t);
	    }
	}
}
