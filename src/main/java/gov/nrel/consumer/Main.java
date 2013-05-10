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
import java.util.Timer;

import com.serotonin.bacnet4j.LocalDevice;
import com.serotonin.bacnet4j.event.DefaultExceptionListener;
import com.serotonin.bacnet4j.type.primitive.UnsignedInteger;
import com.serotonin.util.queue.ByteQueue;

import org.apache.commons.cli.*;

public class Main {

	private static final Logger logger = Logger.getLogger(Main.class.getName());
	
	private ScheduledThreadPoolExecutor svc;
	private ExecutorService execSvc;
	private ExecutorService recorderSvc;
	private Timer slaveDeviceTimer;
	
	public static void main(String[] args) throws SecurityException, IOException {
		
		logger.info("starting.  Parsing command line options");


		try {
			Config config = parseOptions(args);

			logger.info("starting.  Loading logging.properties first to log to a file in logs directory");
			FileInputStream configFile = new FileInputStream(config.getLoggingPropertiesFileName());
			LogManager.getLogManager().readConfiguration(configFile);

			if (config.getVerboseLogging())
			{
				logger.setLevel(Level.INFO);
			}

			if (config.getVeryVerboseLogging())
			{
				logger.setLevel(Level.ALL);
			}
		
			logger.info("Starting now that logger properties are loaded");
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
		String devname = config.getNetworkDevice();
		int device_id = config.getDeviceId();
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

		List<InterfaceAddress> addresses = networkinterface.getInterfaceAddresses();

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

		logger.info("Binding to: " + saddress + " " + sbroadcast);

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

		if (config.getSlaveDeviceEnabled())
		{
			slaveDeviceTimer = new Timer();
			slaveDeviceTimer.schedule(new gov.nrel.bacnet.SlaveDevice(localDevice, config), 1000, config.getSlaveDeviceUpdateInterval() * 1000);
		}

		String file = config.getFilterFileName();
		String filterfile = readFile(file, Charset.forName("US-ASCII"));
		com.google.gson.Gson gson = new com.google.gson.Gson();

		JsonAllFilters filters = gson.fromJson(filterfile, JsonAllFilters.class);

		int counter = 0;
		for(JsonObjectData d : filters.getFilters()) {
			if(d.getInterval() == 0) {
				throw new IllegalArgumentException("Filter cannot have interval value of 0 or null.  It must be less than 0 to disable and greater than 0 to enable in seconds");
		        } else if(d.getInterval() > 0 && d.getInterval() < 15) {
				throw new IllegalArgumentException("Scan interval cannot be less than 15 seconds");
			}

			logger.info("filter"+counter+"="+d.getInterval()+" devId="+d.getDeviceId()+" objId="+d.getObjectId()+" objType="+d.getObjectType());
			
			//initialize regular expression matchers...
			d.init();
		}
		
		String streamTable = config.getDatabusStreamTable();
		String deviceTable = config.getDatabusDeviceTable();

		String username = config.getDatabusUserName();
		String key = config.getDatabusKey();
		
		logger.info("user="+username+" key="+key);

		DatabusSender sender = null;

		if (config.getDatabusEnabled())
		{
			sender = new DatabusSender(username, key, deviceTable, streamTable, recorderSvc, config.getDatabusUrl(), config.getDatabusPort(), true);
		}

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

	public static Config parseOptions(String[] args) throws Exception {

		int min_id = -1;
		int max_id = -1;
		int device_id = 11234;

		int scanInterval = 168;
		int broadcastInterval = 1;
		int range = 100;
		int numThreads = 10;

		String databusStreamTable = "bacnetstreamMeta";
		String databusDeviceTable = "bacnetdeviceMeta";

		String databusUserName = "robot-bacnet";
		String databusKey = "941RCGC.B2.1WWXM5WZVA5YL";
		
		String databusURL = "databus.nrel.gov";
		int databusPort = 5502;


		String devname = "eth0";


                String filterFile = "../conf/filter.json";
		String slaveDeviceConfigFile = "../conf/example_oid.json";

		int slaveDeviceUpdateInterval = 10;



		boolean scan = true;
		boolean slaveDeviceEnabled = false;
                String loggingPropertiesFile = "../conf/logging.properties";

		boolean verboseLogging = false;
		boolean veryVerboseLogging = false;

		boolean databusEnabled = true;


		CommandLineParser parser = new PosixParser();
		Options options = new Options();
//		options.addOption("m", "min-device-id", true,
//				"Minimum device ID to scan for, default: " + min_id);
//		options.addOption("M", "max-device-id", true,
//				"Maximum device ID to scan for, default: " + max_id);
		options.addOption("i", "id", true,
				"Device ID of this software, default: " + device_id);
//		options.addOption("D", "device-id", true,
//				"device ID to scan, exclusive of min-device-id and max-device-id");
		options.addOption("f", "filter-file", true,
				"JSON filter file to use during scanning, default: " + filterFile);
		options.addOption("d", "dev", true,
				"Network device to use for broadcasts, default: " + devname);
//		options.addOption("e", "example-filter-file", true,
//				"Write an example JSON filter file out and exit, with the given filename");
		options.addOption("s", "scan", false, "Enable scanning feature, default: " + scan);
		options.addOption("S", "slave-device", false,
				"Enable slave device feature, default: " + slaveDeviceEnabled);
		options.addOption("T", "slave-device-interval", true,
				"Number of seconds between updates to slave device values, default: " + slaveDeviceUpdateInterval);
		options.addOption(
				"t",
				"scan-interval",
				true,
				"Amount of time (in ms) to wait between finishing one scan and starting another. default: " + scanInterval);
		options.addOption("F", "oid-file", true,
				"JSON oid file to use for the slave device configuration, default: " + slaveDeviceConfigFile);

//		options.addOption("E", "example-oid-file", true,
//				"Write an example JSON oid input file out and exit, with the given filename");
		options.addOption("v", "verbose", false,
				"Verbose logging (Info Level). Default is warning and error logging. default: " + verboseLogging);
		options.addOption("vv", "very-verbose", false,
				"Very verbose logging (All Levels). Default is warning and error logging. default: " + veryVerboseLogging);
		options.addOption("u", "databus-url", true,
				"Databus URL to send data to, default: " + databusURL);
		options.addOption("k", "databus-key", true,
				"Key for sending to Databus, default: " + databusKey);
		options.addOption("U", "databus-user", true,
				"Databus username for sending to Database, default: " + databusUserName);
		options.addOption("p", "databus-port", true,
				"Databus port for sending to Database, default: " + databusPort);
		options.addOption("l", "logging-properties-file", true,
				"File for loading logger configuration, default: " + loggingPropertiesFile);

		options.addOption("databus", "databus-enabled", true, "Enable writing to databus. default: " + databusEnabled);

		try {
			CommandLine line = parser.parse(options, args);
/*
			if (line.hasOption("e")) {
			        
				System.out.println("Writing example JSON filter file to: "
						+ line.getOptionValue("e"));

				java.io.FileOutputStream jsonfile = null;
				java.io.PrintWriter jsonw = null;

				try {
					jsonfile = new java.io.FileOutputStream(
							line.getOptionValue("e"));
				} catch (Exception e) {
					System.out.println("Error writing example JSON file");
					System.exit(-1);
				}

				if (jsonfile != null) {
					jsonw = new java.io.PrintWriter(jsonfile, true);
					com.google.gson.Gson gson = new com.google.gson.Gson();
					Vector<DeviceFilter> examplefilters = new Vector<DeviceFilter>();
					DeviceFilter f = new DeviceFilter();
					f.instanceNumber = ".*";
					f.networkNumber = ".*";
					f.macAddress = ".*";
					f.networkAddress = ".*";
					f.timeBetweenScans = 30000;

					OIDFilter of = new OIDFilter();
					of.objectType = "Binary .*";
					of.instanceNumber = "1.*";
					of.timeBetweenScans = 60000;
					f.oidFilters = new Vector<OIDFilter>();
					f.oidFilters.add(of);
					examplefilters.add(f);
					jsonw.println(gson.toJson(examplefilters));
				}
			}

			if (line.hasOption("E")) {
				System.out.println("Writing example JSON oid file to: "
						+ line.getOptionValue("E"));

				java.io.FileOutputStream jsonfile = null;
				java.io.PrintWriter jsonw = null;

				try {
					jsonfile = new java.io.FileOutputStream(
							line.getOptionValue("E"));
				} catch (Exception e) {
					System.out.println("Error writing example JSON oid file");
					System.exit(-1);
				}

				if (jsonfile != null) {
					jsonw = new java.io.PrintWriter(jsonfile, true);
					com.google.gson.Gson gson = new com.google.gson.Gson();
					Vector<OIDValue> exampleoids = new Vector<OIDValue>();
					OIDValue i = new OIDValue();
					i.objectName = "some_object";
					i.objectType = "analog input";
					i.objectSource = "echo {value:\"72.5\", timestamp:\"2012-02-01 12:00:00\", units:\"degrees fahrenheit\"}";
					exampleoids.add(i);

					OIDValue i2 = new OIDValue();
					i2.objectName = "some_object 2";
					i2.objectType = "binary input";
					i2.objectSource = "echo {value:\"true\", timestamp:\"2012-02-01 12:00:00\", units:\"\"}";

					exampleoids.add(i2);
					jsonw.println(gson.toJson(exampleoids));
				}

			}

			if (line.hasOption("e") || line.hasOption("E")) {
				System.exit(0);
			}
*/

			scanInterval = Integer.parseInt(line.getOptionValue("t",
					"168"));
			// time_to_scan = Integer.parseInt(line.getOptionValue("T", "-1"));
			device_id = Integer.parseInt(line.getOptionValue("i", "1234"));
			scan = line.hasOption("s");

			slaveDeviceEnabled = line.hasOption("S");

			/*
			min_id = Integer.parseInt(line.getOptionValue("m", "-1"));
			max_id = min_id;

			max_id = Integer.parseInt(line.getOptionValue("M", "-1"));

			if (min_id == -1 && max_id > -1) {
				min_id = 0;
			}

			if (line.hasOption("m") && !line.hasOption("M")) {
				HelpFormatter hp = new HelpFormatter();
				hp.printHelp(
						"Syntax:",
						"Error: a max-device-id must be specified if a min-device-id is specified",
						options, "", true);
				System.exit(-1);
			}

			if (max_id < min_id) {
				HelpFormatter hp = new HelpFormatter();
				hp.printHelp(
						"Syntax:",
						"Error: max-device-id cannot be less than min-device-id",
						options, "", true);
				System.exit(-1);
			}
			*/

			if (line.hasOption("D")
					&& (line.hasOption("m") || line.hasOption("M"))) {
				HelpFormatter hp = new HelpFormatter();
				hp.printHelp(
						"Syntax:",
						"Error: you cannot specify both a specific device-id and a min-device-id or max-device-id",
						options, "", true);
				System.exit(-1);
			}

			if (line.hasOption("D")) {
				min_id = max_id = Integer.parseInt(line.getOptionValue("D"));
			}

			if (line.hasOption("f")) {
				filterFile = line.getOptionValue("f");
			}

			if (line.hasOption("F")) {
				slaveDeviceConfigFile = line.getOptionValue("F");
			}

			devname = line.getOptionValue("dev", "eth0");

			if (line.hasOption("v")) {
				verboseLogging = true;
			}

			if (line.hasOption("vv")) {
				veryVerboseLogging = true;
			}

			if (line.hasOption("u")) {
				databusURL = line.getOptionValue("u",
						"databus.nrel.gov");
			}

			if (line.hasOption("k")) {
				databusKey = line.getOptionValue("k");
			}

			if (line.hasOption("U")) {
				databusUserName = line.getOptionValue("U");
			}
			
			if (line.hasOption("p")) {
				databusPort = Integer.parseInt(line.getOptionValue("p"));
			}

			if (line.hasOption("l")) {
				loggingPropertiesFile = line.getOptionValue("l");
			}

			if (line.hasOption("databus-enabled")) {
				databusEnabled = Boolean.parseBoolean(line.getOptionValue("databus-enabled"));
			}

		} catch (Exception e) {
			HelpFormatter hp = new HelpFormatter();
			logger.log(Level.SEVERE, "Commmand Line Parsing Error: ", e);
			hp.printHelp("Syntax:", options, true);
			System.exit(-1);
		}

		Config config = new Config(scanInterval, broadcastInterval, range, numThreads, devname, verboseLogging,
		    veryVerboseLogging, device_id, filterFile, loggingPropertiesFile, databusEnabled,
		    databusDeviceTable, databusStreamTable, databusUserName, databusKey,
		    databusURL, databusPort, slaveDeviceEnabled, slaveDeviceConfigFile, slaveDeviceUpdateInterval);

		return config;
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





/*
		Logger logger = Logger.getLogger("BACnetScanner");

		try {
			FileHandler fh = new FileHandler("LogFile.log", true);
			logger.addHandler(fh);
			SimpleFormatter formatter = new SimpleFormatter();
			fh.setFormatter(formatter);
		} catch (Exception e) {
			logger.log(Level.SEVERE, "Unable to create log file", e);
		}

		logger.setLevel(log_level);

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
		InterfaceAddress ifaceaddr = null;

		for (InterfaceAddress address : addresses) {
			logger.fine("Evaluating address: " + address.toString());
			if (address.getAddress().getAddress().length == 4) {
				logger.info("Address is ipv4, selecting: " + address.toString());
				sbroadcast = address.getBroadcast().toString().substring(1);
				saddress = address.getAddress().toString().substring(1);
				ifaceaddr = address;
				break;
			} else {
				logger.info("Address is not ipv4, not selecting: "
						+ address.toString());
			}
		}

		logger.info("Binding to: " + saddress + " " + sbroadcast);

		LocalDevice localDevice = new LocalDevice(device_id, sbroadcast);
		localDevice.setPort(LocalDevice.DEFAULT_PORT);
		localDevice.setTimeout(localDevice.getTimeout() * 3);
		localDevice.setSegTimeout(localDevice.getSegTimeout() * 3);
		try {
			localDevice.initialize();
		} catch (IOException e) {
			e.printStackTrace();
			return;
		}

		Scan s = null;

		if (scan) {
			logger.info("Creating scanner object");
			s = new Scan(localDevice, min, max, filters, time_between_updates);
		}

		SlaveDevice sd = null;

		if (slaveDeviceEnabled) {
			logger.info("Creating slave device object");
			sd = new SlaveDevice(localDevice, oidvalues);
		}

		if ((slaveDeviceEnabled == false && scan == false)
				|| (slaveDeviceEnabled == false && scan != false && time_to_scan == 0)) {
			logger.severe("Nothing to do, no slaveDeviceEnabled enabled and no scan enabled, or scan enabled and numscans set to 0");
		}

		java.util.Date lastSlaveUpdate = null;
                java.util.Date startTime = new java.util.Date();

		for (int i = 0; 
                     s != null && (time_to_scan == -1 || (new java.util.Date()).getTime() - startTime.getTime() < time_to_scan ); 
                     ++i) 
                {

			if (slaveDeviceEnabled) {
				boolean doupdate = false;
				java.util.Date now = new java.util.Date();
				if (lastSlaveUpdate == null) {
					doupdate = true;
				} else {
					if (now.getTime() - lastSlaveUpdate.getTime() >= time_between_updates) {
						doupdate = true;
					}
				}

				if (doupdate) {
					logger.info("Updating slave device");
					sd.updateValues();
					lastSlaveUpdate = now;
				}
			}

			s.run();

			// Thread.sleep(time_between_updates);
		}

                logger.info("Scanning complete");

                // keep running if we have a slaveDeviceEnabled

                while (slaveDeviceEnabled) { 
                  boolean doupdate = false; 
                  java.util.Date now = new java.util.Date(); 
                  
                  if (lastSlaveUpdate == null) { 
                    doupdate = true;
                  } else { 
                    if (now.getTime() - lastSlaveUpdate.getTime() >= time_between_updates) 
                    { 
                      doupdate = true; 
                    } 
                  }

                  if (doupdate) { 
                    logger.info("Updating slave device");
                    sd.updateValues(); lastSlaveUpdate = now; 
                  }

                  Thread.sleep(1000); 
                }

                logger.info("Shutting down"); localDevice.terminate();
	}
*/
