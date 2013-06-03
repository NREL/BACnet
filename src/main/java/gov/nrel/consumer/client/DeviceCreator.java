package gov.nrel.consumer.client;

import java.io.IOException;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.util.List;
import java.util.logging.Logger;

import com.serotonin.bacnet4j.LocalDevice;

public class DeviceCreator {

	private static final Logger logger = Logger.getLogger(DeviceCreator.class.getName());
	
	public static LocalDevice createDevice(String networkCardName, int device_id) {
		NetworkInterface networkinterface = null;

		try {
			networkinterface = java.net.NetworkInterface.getByName(networkCardName);
		} catch (Exception ex) {
			ex.printStackTrace();
			System.out.println("Unable to open device: " + networkCardName);
			System.exit(-1);
		}

		if (networkinterface == null) {
			System.out.println("Unable to open device(device is null): " + networkCardName);
			System.exit(-1);
		}

		List<InterfaceAddress> addresses = networkinterface.getInterfaceAddresses();

		String sbroadcast = null;
		String saddress = null;
		//InterfaceAddress ifaceaddr = null;

		for (InterfaceAddress address : addresses) {
			logger.fine("Evaluating address: " + address.toString());
			if (address.getAddress().getAddress().length == 4) {
				logger.info("Address is ipv4, selecting: " + address.toString());
				sbroadcast = address.getBroadcast().toString().substring(1);
				saddress = address.getAddress().toString().substring(1);
				//ifaceaddr = address;
				break;
			} else {
				logger.info("Address is not ipv4, not selecting: "
						+ address.toString());
			}
		}

		logger.info("Binding to: " + saddress + " " + sbroadcast);

		LocalDevice localDevice = new LocalDevice(device_id, sbroadcast);
		localDevice.setPort(LocalDevice.DEFAULT_PORT);
		localDevice.setTimeout(localDevice.getTimeout() * 3);
		localDevice.setSegTimeout(localDevice.getSegTimeout() * 3);
		try {
			localDevice.initialize();
			localDevice.setRetries(0); //don't retry as it seems to really be a waste.
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		
		return localDevice;
	}
}
