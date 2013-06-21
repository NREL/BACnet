package gov.nrel.consumer.client;

import java.util.logging.Level;
import java.util.logging.Logger;

import com.serotonin.bacnet4j.Network;
import com.serotonin.bacnet4j.RemoteDevice;
import com.serotonin.bacnet4j.event.DefaultDeviceEventListener;
import com.serotonin.bacnet4j.type.constructed.Address;
import com.serotonin.bacnet4j.type.primitive.UnsignedInteger;

class NewDeviceListener extends DefaultDeviceEventListener {

	private static final Logger log = Logger.getLogger(NewDeviceListener.class.getName());
	private RemoteDevice device;
	private int deviceId;

	@Override
	public void iAmReceived(RemoteDevice d) {
		if(d.getInstanceNumber() == deviceId) {
			device = d;
			
			synchronized (this) {
				this.notifyAll();
			}
		}
	}

	@Override
	public void whoIsRequest(Address from, Network network,	UnsignedInteger min, UnsignedInteger max) {
		log.log(Level.INFO, "Someone sent who is requests="+from.toIpPortString()+" network="+network+"  min="+min+" max="+max);
	}

	public void setDeviceId(int deviceId) {
		this.deviceId = deviceId;
	}
	
	public RemoteDevice getDevice() {
		long start = System.currentTimeMillis();
		
		while(device == null) {
			long timeWaited = System.currentTimeMillis()-start;
			if(timeWaited > 15000)
				throw new RuntimeException("timed out waiting for broadcast response");

			try {
				this.wait(5000);
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			}
		}

		RemoteDevice temp = device;
		device = null;
		return temp;
	}
}
