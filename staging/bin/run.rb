require 'java'
Dir["../lib/\*.jar"].each { |jar| require jar }


require 'sinatra/base'

def gov
  Java::Gov
end


class Writer < gov.nrel.bacnet.consumer.BACnetDataWriter

  def oidsDiscoveredImpl(data)
    data.each { |item| 
      puts "Ruby BACnetDataWriter oid: " + item.oid.to_s + " " + item.value.to_s + " " + item.instanceNumber.to_s + " " + item.curTime.to_s
    }
  end

  def deviceDiscoveredImpl(device)
    puts "Ruby BACnetDataWriter device: " + device
  end

end

class Logger < gov.nrel.bacnet.consumer.LogHandler
  def initialize
    @records = []
  end

  def publishRecordImpl(record)
    @records << record
  end

  def getRecordsImpl
    return @records.to_java(java.util.logging.LogRecord)
  end
end


begin
  $config = gov.nrel.bacnet.consumer.BACnet.parseOptions(ARGV)
  $bacnet = gov.nrel.bacnet.consumer.BACnet.new($config)
  $bacnet.initializeDefaultScanner


  $bacnetWriters = {}
  $bacnetWriters["databus"] = $bacnet.getDatabusDataWriter
  $bacnetWriters["stdout"] = Writer.new

  $bacnet.setLogger(Logger.new)
   

rescue java.lang.Throwable => e
  puts "Error in starting up: #{e.message}"
  exit!
end


class SinatraApp < Sinatra::Base


  get '/' do
    "BACnet Scanner Service"
  end

  get '/logmessages' do
    messages = $bacnet.getLogger.getRecords

    formatter = java.util.logging.SimpleFormatter.new

    body = ""

    messages.each { |message| 
      body += formatter.format(message) + "<br/>"
    }

    return body;

  end

  post '/scan/:minId/:maxId/:writer' do
    minId = params[:minId].to_i
    maxId = params[:maxId].to_i
    writer = params[:writer]

    # schedule a single device scan. However, the device OID's are re-polled at the interval(s) specified in the filters 
    # bacnet.scheduleScan(1234, bacnet.getDefaultFilters(), [w].to_java(gov.nrel.consumer.BACnetDataWriter));
    $bacnet.scheduleScan(minId, maxId, $bacnet.getDefaultFilters, 
			[$bacnetWriters[writer]].to_java(gov.nrel.bacnet.consumer.BACnetDataWriter))

    "New Scanner Scheduled: #{minId} #{maxId} #{writer}"

  end
end

SinatraApp.run!


