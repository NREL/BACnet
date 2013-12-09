require 'java'
require '../../../src/main/ruby/logger_singleton.rb'
class NewDeviceHandler < com.serotonin.bacnet4j.event.DefaultDeviceEventListener
  # A remote device sends this message in response to broadcast
  # @Override
  def iAmReceived(remote_device)
    begin
      KnownDevice.discovered(remote_device)
    rescue Exception => e
      LoggerSingleton.logger.error "\n\nerror processing iamrecieved: #{e.to_s}: #{e.backtrace.join("\n")}"
    end
  end  
end