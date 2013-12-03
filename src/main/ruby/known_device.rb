require 'mongoid'
require 'java'
require 'yaml'

Mongoid.load!("../../../src/main/ruby/mongoid.yml", :development)

class KnownDevice 
  include Mongoid::Document
  include Mongoid::Timestamps
 
  field :instance_number, type: Integer
  field :discovered_heartbeat, type: DateTime
  field :poll_heartbeat, type: DateTime
  field :remote_device, type: String #this is the remote device object from an iAm response, serialized

  # from http://www.bacnet.org/Bibliography/ES-7-96/ES-7-96.htm :
  # Unlike other Objects, the Device Object's Instance number must be unique across the entire BACnet internetwork because it is used to uniquely identify the BACnet devices.
  index({ :instance_number => 1 }, :unique => true)

  # create or update Mongo 
  def self.discovered rd
    kd = KnownDevice.where(:instance_number => rd.getInstanceNumber).first
    if kd.present?
      # TODO compare remote_device to kd.remote_device (saved version of device)
      kd.discovered_heartbeat = Time.now
      kd.save
    else
      kd = KnownDevice.create(:instance_number => rd.getInstanceNumber, :remote_device => YAML::dump(rd), :discovered_heartbeat => Time.now)
    end 
  end

  def get_remote_device
    YAML::load(remote_device)
  end
  
  private 

  def update_discovered_heartbeat
    iam_heartbeat = Time.now
    save
  end

  def update_poll_heartbeat
    poll_heartbeat = Time.now
    save
  end

end
