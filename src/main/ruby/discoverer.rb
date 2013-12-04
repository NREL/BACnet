# require 'rubygems'
# require 'bundler/setup'
# require 'java'
# Dir["../lib/\*.jar"].each { |jar| require jar }

# todo distribute java code with gem?

# to execute discovery

  # $config = gov.nrel.bacnet.consumer.BACnet.parseOptions(ARGV)
  # $bacnet = gov.nrel.bacnet.consumer.BACnet.new($config)
  # configure database connection to persist state...
  # d = Discoverer.new(low, high, $bacnet)
  # d.broadcastWhoIs
  # keep alive??

class Discoverer 
  # initially we will just do this single-threaded.  it probably isn't that slow.  
  # do we want to just leave listener running?
  # If performance dictates, we may split the range over threads, each with it's own event listener

  # other todos: add discoveryManager to control access.

  # localdevice is initialized and configured in the bacnet#initialize method.  let's leave that class alone for now.
  @broadcast_timer = nil

  def initialize(min_id, max_id, local_device)
    @min = min_id
    @max = max_id
    @local_device = local_device
  end

  def valid?
    begin
      raise InvalidArgumentError.new("Max cannot be less than min.") if @min > @max 
      # ... more checks
      true
    rescue Exception => e
      # todo use a logger..
      puts "argument error: #{e.message}"
      false
    end 
  end

  # todo control access to trigger broadcast 
  def broadcastWhoIs step = 100, interval = 1

    # Executors.newScheduledThreadPool(1).scheduleAtFixedRate(self.broadcastOnInterval, 0, 1, TimeUnit::SECONDS)
    # create whois ( see taskb#broadcastwhois)
    # set eventhandler on localdevice to manage sensors reporting in
    @local_device.getEventHandler().addListener(NewDeviceHandler.new);

    # for now, broadcast over entire range 
    whois = com.serotonin.bacnet4j.service.unconfirmed.WhoIsRequest.new(com.serotonin.bacnet4j.type.primitive.UnsignedInteger.new(@min), com.serotonin.bacnet4j.type.primitive.UnsignedInteger.new(@max))

    @local_device.sendBroadcast(whois);
    # schedule in steps of 100 
    # broadcast on localdevice with min and max
  end

end
