require 'java'
require '../../../src/main/ruby/logger_singleton.rb'
# extends a seratonin class
class NewDeviceHandler < com.serotonin.bacnet4j.event.DefaultDeviceEventListener
  # could choose to create a threadpool to handle the iam responses if the processing/io there gets more complicated

  # A remote device sends this message in response to broadcast
  # @Override
  def iAmReceived(remote_device)
    begin
      record = KnownDevice.discovered(remote_device)
    rescue Exception => e
      LoggerSingleton.logger.error "error processing iamrecieved: #{e.to_s}"
    end
  end  
end