require 'java'
Dir["../build/bacnet/lib/\*.jar"].each { |jar| require jar }

def gov
  Java::Gov
end

begin
  config = gov.nrel.consumer.BACnet.parseOptions(ARGV)
  bacnet = gov.nrel.consumer.BACnet.new(config)
#  bacnet.initializeDefaultScanner()
  bacnet.scheduleScan(1234, 1234, bacnet.getDefaultFilters(), bacnet.getDefaultDataWriters());

rescue java.lang.Throwable => e
  puts "Error in starting up: #{e.message}"
end

puts "We've reached the end of 'main' and now the threads are keeping us alive"

