package gov.nrel.consumer;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import gov.nrel.consumer.beans.Config;
import org.apache.commons.cli.*;

public class Main {

	private static final Logger logger = Logger.getLogger(Main.class.getName());
	
	
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

