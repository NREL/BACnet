require 'mongoid'
require 'java'
require 'yaml'
require '../../../src/main/ruby/oid.rb'

Mongoid.load!("../../../src/main/ruby/mongoid.yml", :development)

class KnownDevice 
  include Mongoid::Document
  include Mongoid::Timestamps
 
  field :instance_number, type: Integer
  field :discovered_heartbeat, type: DateTime
  field :poll_heartbeat, type: DateTime
  field :port, type: Integer
  field :ip_base64, type: String #byte array ?!
  field :network_number, type: Integer
  field :network_address, type: String
  field :max_apdu_length_accepted, type: Integer
  field :segmentation_value, type: Integer 
  field :vendor_id, type: Integer

  index({ :instance_number => 1 }, :unique => true)

  @remote_device = nil

  has_many :oids

  # create or update Mongo 
  def self.discovered rd
    kd = KnownDevice.where(:instance_number => rd.getInstanceNumber).first
    # TODO if the device is already known, do we want to look for any changes?
    if kd.nil?
      kd = KnownDevice.new(:instance_number => rd.getInstanceNumber)
      kd.set_fields(rd)
    end 
    kd.discovered_heartbeat = Time.now
    kd.save
  end

  def discover_oids local_device
    p = gov.nrel.bacnet.consumer.PropertyLoader.new(local_device)
    oids = p.getOids(self.get_remote_device)
    oids.each do |oid|
      Oid.discover(self, oid)
    end
  end

  # init the remote device if it isn't already
  def get_remote_device
    if @remote_device.nil?
      init_remote_device
    end
    @remote_device
  end

  # def load_properties local_device
  #   puts "loading properties for #{instance_number}"
  #   read_in_device_oids_and_base_properties local_device
  # end
  

  # called by static discover method if mongo doesn't already know this device
  def set_fields rd 
    require 'base64'
    # assigning these props without "self." prefix doesn't work
    address = rd.getAddress
    if address.present?
      self.port = address.getPort 
      self.ip_base64 = Base64.encode64(String.from_java_bytes(address.getIpBytes))
    end
    network = rd.getNetwork 
    if network.present?
      self.network_number = network.getNetworkNumber
      self.network_address = network.getNetworkAddressDottedString
    end
    self.max_apdu_length_accepted = rd.getMaxAPDULengthAccepted
    self.vendor_id = rd.getVendorId
    self.segmentation_value = rd.getSegmentationSupported.intValue
  end

private
  # initialize remote device and related java objects
  def init_remote_device 
    require 'base64'
    LoggerSingleton.logger.debug("initializing remote device with id #{instance_number}")
    ip = Base64.decode64(ip_base64).to_java_bytes
    address = com.serotonin.bacnet4j.type.constructed.Address.new(ip, port)
    network = (network_number.present?) ? com.serotonin.bacnet4j.Network.new(network_number, network_address) : nil
    @remote_device = com.serotonin.bacnet4j.RemoteDevice.new(instance_number, address, network)
    @remote_device.setMaxAPDULengthAccepted(max_apdu_length_accepted)
    @remote_device.setVendorId(vendor_id)
    seg = com.serotonin.bacnet4j.type.enumerated.Segmentation.new(segmentation_value)
    @remote_device.setSegmentationSupported(seg)
  end

  # returns oids available to poll for remote_device
  # def read_in_device_oids_and_base_properties local_device
    
  #   # oids = p.getOids(self.get_remote_device)
  #   # l = Java::JavaUtil::ArrayList.new
  #   # oids.each do |oid|
  #   #   l.add(com.serotonin.bacnet4j.type.primitive.ObjectIdentifier.new(oid.getObjectType, oid.getInstanceNumber))
  #   #   # l.add(com.serotonin.bacnet4j.type.primitive.ObjectIdentifier.new(oid.getObjectType, oid.getInstanceNumber)
  #   #   # l.add(com.serotonin.bacnet4j.type.primitive.ObjectIdentifier.new(oid.getObjectType, oid.getInstanceNumber)
  #   # end
  #   # test = com.serotonin.bacnet4j.type.primitive.ObjectIdentifier.new(oids.first.getObjectType,oids.first.getInstanceNumber)


  #   # oids is a List<ObjectIdentifier> that must be passed to getProperties
  #   props = p.getProperties(get_remote_device,oids)

  # end

end
