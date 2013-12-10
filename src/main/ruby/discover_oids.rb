require 'java'
Dir["../lib/\*.jar"].each { |jar| require jar }
require '../../../src/main/ruby/known_device.rb'
require '../../../src/main/ruby/new_device_handler.rb'
require '../../../src/main/ruby/discoverer.rb'

def gov
  Java::Gov
end
# TODO move into discoverer class
config = gov.nrel.bacnet.consumer.BACnet.parseOptions(ARGV)
bacnet = gov.nrel.bacnet.consumer.BACnet.new(config)
local_device = bacnet.getLocalDevice
KnownDevice.all.each do |kd|
  kd.discover_oids local_device
end
