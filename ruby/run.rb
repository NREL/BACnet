require 'java'
Dir["../build/bacnet/lib/\*.jar"].each { |jar| require jar }

def gov
  Java::Gov
end


class Writer < gov.nrel.consumer.BACnetDataWriter

  def writeImpl(data)
    data.each { |item| 
      puts item.oid.to_s + " " + item.value.to_s + " " + item.instanceNumber.to_s + " " + item.curTime.to_s
    }
  end

end


begin
  config = gov.nrel.consumer.BACnet.parseOptions(ARGV)
  bacnet = gov.nrel.consumer.BACnet.new(config)
#  bacnet.initializeDefaultScanner()

  w = Writer.new();
 
  # schedule a single device scan. However, the device OID's are re-polled at the interval(s) specified in the filters 
  bacnet.scheduleScan(1234, bacnet.getDefaultFilters(), [w].to_java(gov.nrel.consumer.BACnetDataWriter));

rescue java.lang.Throwable => e
  puts "Error in starting up: #{e.message}"
end

puts "We've reached the end of 'main' and now the threads are keeping us alive"

