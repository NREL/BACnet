class PropertyLoader

  def self.refresh_all local_device
    KnownDevice.all.each do |d|
      d.load_properties local_device
    end
  end

end