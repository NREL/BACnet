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
	
	private OurExecutor exec;
	
	public static void main(String[] args) throws SecurityException, IOException {
		
		logger.info("starting.  Parsing command line options");


		try {
			Config config = BACnet.parseOptions(args);
			BACnet bacnet = new BACnet(config);
		} catch(Throwable e) {
			logger.log(Level.WARNING, "exception starting", e);
		}
	}	

}

