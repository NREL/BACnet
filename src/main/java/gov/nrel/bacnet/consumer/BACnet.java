package gov.nrel.bacnet.consumer;

import gov.nrel.bacnet.Config;
import gov.nrel.bacnet.consumer.beans.JsonAllFilters;
import gov.nrel.bacnet.consumer.beans.JsonObjectData;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;
import java.util.Collection;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
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

public class BACnet {

	private static final Logger logger = Logger.getLogger(Main.class.getName());
	
	private ScheduledThreadPoolExecutor svc;
	private ExecutorService execSvc;
	private ExecutorService recorderSvc;
	private Timer slaveDeviceTimer;
	private OurExecutor exec;
	private DatabusDataWriter writer;
	private ScheduledFuture<?> backgroundDiscoverer;
	private Config config;
	private LocalDevice localDevice;
	private JsonAllFilters filters;
	private LogHandler logHandler;
	private BACnetDatabase database;
	private TaskTracker tracker;

        public BACnet(Config t_config)
	{
		config = t_config;
		tracker = new TaskTracker();
		localDevice = null;
		filters = null;
		logHandler = null;

		try {
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
			initialize(config);

		} catch(Throwable e) {
			logger.log(Level.WARNING, "exception starting", e);
		}
	}

	public void setDatabase(BACnetDatabase d)
	{
		logger.info("Setting new database");
		database = d;
	}

	public BACnetDatabase getDatabase()
	{
		return database;
	}

	public TaskTracker getTaskTracker()
	{
		return tracker;
	}

	public void setLogger(LogHandler h)
	{
		logger.info("Setting new loghandler");

		if (logHandler != null)
		{
			Logger.getLogger("gov.nrel.bacnet").removeHandler(logHandler);
		}

		logHandler = h;

		Logger.getLogger("gov.nrel.bacnet").addHandler(logHandler);
	}

	public LogHandler getLogger()
	{
		return logHandler;
	}

	public LocalDevice getLocalDevice()
	{
		return localDevice;
	}


	public JsonAllFilters getFilters()
	{
		return filters;
	}

	public JsonAllFilters parseFilters(String filters)
	{
		com.google.gson.Gson gson = new com.google.gson.Gson();
		return gson.fromJson(filters, JsonAllFilters.class);
	}

	private Collection<BACnetDataWriter> getWriters(BACnetDataWriter[] writers)
	{
		java.util.List<BACnetDataWriter> writerlist = new java.util.ArrayList(Arrays.asList(writers));

		if (database != null)
		{
			writerlist.add(database);
		}

		return writerlist;
	}

	private void initialize(Config config) throws IOException {
		LinkedBlockingQueue<Runnable> queue = new LinkedBlockingQueue<Runnable>(1000);
		RejectedExecutionHandler rejectedExec = new RejectedExecHandler();
		recorderSvc = new ThreadPoolExecutor(20, 20, 120, TimeUnit.SECONDS, queue, rejectedExec );
		
		svc = (ScheduledThreadPoolExecutor) Executors.newScheduledThreadPool(1);
		execSvc = Executors.newFixedThreadPool(config.getNumThreads());
		exec = new OurExecutor(svc, execSvc, recorderSvc);
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

		localDevice = new LocalDevice(device_id, sbroadcast);
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

		logger.info("using filter file:" + file);
		filters = parseFilters(filterfile);

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

		writer = new DatabusDataWriter(new DataPointWriter(sender));
		
		logger.info("Kicking off scanner object to run every "+config.getScanInterval()+" hours with broadcasts every "+config.getBroadcastInterval()+" seconds");
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

	public BACnetDataWriter getDatabusDataWriter()
	{
		return writer;
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

		String databusUserName = "";
		String databusKey = "";
		String databusURL = "";
		int databusPort = 1;


		String devname = "eth0";


		String filterFile = "conf/example_filter.json";
		String slaveDeviceConfigFile = "conf/example_oid.json";

		int slaveDeviceUpdateInterval = 10;



		boolean scan = true;
		boolean slaveDeviceEnabled = false;
		String loggingPropertiesFile = "../conf/logging.properties";

		boolean verboseLogging = false;
		boolean veryVerboseLogging = false;

		boolean databusEnabled = true;


		CommandLineParser parser = new PosixParser();
		Options options = new Options();
		options.addOption("m", "min-device-id", true,
				"Minimum device ID to scan for, default: " + min_id);
		options.addOption("M", "max-device-id", true,
				"Maximum device ID to scan for, default: " + max_id);
		options.addOption("i", "id", true,
				"Device ID of this software, default: " + device_id);
		options.addOption("D", "device-id", true,
				"device ID to scan, exclusive of min-device-id and max-device-id");
		options.addOption("f", "filter-file", true,
				"JSON filter file to use during scanning, default: " + filterFile);
		options.addOption("d", "dev", true,
				"Network device to use for broadcasts, default: " + devname);
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
			scanInterval = Integer.parseInt(line.getOptionValue("t",
					"168"));
			device_id = Integer.parseInt(line.getOptionValue("i", "1234"));
			scan = line.hasOption("s");
			slaveDeviceEnabled = line.hasOption("S");
			min_id = Integer.parseInt(line.getOptionValue("m", "-1"));
			max_id = min_id;
			max_id = Integer.parseInt(line.getOptionValue("M", "-1"));

			if (min_id == -1) {
				min_id = 0;
			}

			if (max_id == -1)
			{
				max_id = 4000000;
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
				databusURL = line.getOptionValue("u");
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
		    databusURL, databusPort, slaveDeviceEnabled, slaveDeviceConfigFile, slaveDeviceUpdateInterval,
		    min_id, max_id);

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