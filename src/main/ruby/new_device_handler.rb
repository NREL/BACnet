require 'java'
# extends a seratonin class
class NewDeviceHandler < com.serotonin.bacnet4j.event.DefaultDeviceEventListener
  # A remote device sends this message in response to broadcast
  # @Override
  def iAmReceived(remote_device) 
    begin 
      record = KnownDevice.discovered(remote_device)
    rescue Exception => e 
      # log error
    end
  end
  
end