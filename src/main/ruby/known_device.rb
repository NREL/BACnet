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
  field :serialized_remote_device, type: String #this is the remote device object from an iAm response, serialized

  # from http://www.bacnet.org/Bibliography/ES-7-96/ES-7-96.htm :
  # Unlike other Objects, the Device Object's Instance number must be unique across the entire BACnet internetwork because it is used to uniquely identify the BACnet devices.
  index({ :instance_number => 1 }, :unique => true)

  @remote_device = nil

  # create or update Mongo 
  def self.discovered rd
    kd = KnownDevice.where(:instance_number => rd.getInstanceNumber).first
    if kd.present?
      # TODO compare remote_device to kd.remote_device (saved version of device)
      kd.discovered_heartbeat = Time.now
      kd.save
    else
          # //unfortunately, we need some of the info that is filled into the remote device for later to see
    # //if reading multiple properties is supported.
    # m_localDevice.getExtendedDeviceInformation(d);
      kd = KnownDevice.create(:instance_number => rd.getInstanceNumber, :serialized_remote_device => YAML::dump(rd), :discovered_heartbeat => Time.now)
    end 
  end

  def get_remote_device
    if @remote_device.nil?
      @remote_device = YAML::load(serialized_remote_device)
    end
    @remote_device
  end

  def load_properties local_device
    LoggerSingleton.logger.info "loading properties for #{instance_number}"
    read_in_device_oids_and_base_properties local_device
  end
  
  private 

  # returns oids available to poll for remote_device
  def read_in_device_oids_and_base_properties local_device
    #   private List<ObjectIdentifier> readInDeviceOidsAndBaseProperties(TaskFPollDeviceTask task, List<Stream> streams, Device dev, String id2, int counter2)

    # //unfortunately, we need some of the info that is filled into the remote device for later to see
    # //if reading multiple properties is supported.
    # TODO this method updates properties on remote_device, so we should really run this before we ever serialize it
    local_device.getExtendedDeviceInformation(get_remote_device)

# copy from remote_device to new Device dev (not sure why)
    # setDeviceProps(d, dev);

    allOids = local_device.sendReadPropertyAllowNull(get_remote_device, get_remote_device.getObjectIdentifier, PropertyIdentifier.objectList).getValues

    allOids.each do |oid|
      puts "oid: #{oid.inspect}"
    end

    # refs = PropertyReferences.new

    # ANYA - think this is not necessary now that we've separated polling from oid detection
    # oidsToPoll = setupRefs(task, allOids, refs);

    # boolean singleReadSuccess = true;
    # if(refs.size() > 0)
    #   singleReadSuccess = propReader.readAllProperties(task, refs, oidsToPoll, dev, streams, id);
    # long totalProps = System.currentTimeMillis()-startProps;
    
    # log.info(id+"device count="+counter2+" device="+d.getInstanceNumber()+"refreshing oid information ext dev info="+total+"ms getting oids="+totalOid+"ms  props="+totalProps+"ms singleReadSuccess="+singleReadSuccess);
    # return oidsToPoll;
  end

  def update_discovered_heartbeat
    iam_heartbeat = Time.now
    save
  end

  def update_poll_heartbeat
    poll_heartbeat = Time.now
    save
  end

end
