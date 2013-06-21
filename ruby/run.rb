require 'java'
Dir["../build/bacnet/lib/\*.jar"].each { |jar| require jar }

#public class Main {
#
#	private static final Logger logger = Logger.getLogger(Main.class.getName());
#	
#	
#	public static void main(String[] args) throws SecurityException, IOException {
#		
#		logger.info("starting.  Parsing command line options");
#
#
#		try {
#			Config config = BACnet.parseOptions(args);
#			BACnet bacnet = new BACnet(config);
#		} catch(Throwable e) {
#			logger.log(Level.WARNING, "exception starting", e);
#		}
#	}	
#
#}

def gov
  Java::Gov
end

begin
  config = gov.nrel.consumer.BACnet.parseOptions(ARGV)
  bacnet = gov.nrel.consumer.BACnet.new(config)
rescue java.lang.Throwable => e
  puts "Error in starting up: #{e.message}"
end

puts "We've reached the end of 'main' and now the threads are keeping us alive"

