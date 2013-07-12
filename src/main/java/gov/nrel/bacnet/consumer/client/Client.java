package gov.nrel.bacnet.consumer.client;

import gov.nrel.bacnet.consumer.beans.ObjKey;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.serotonin.bacnet4j.LocalDevice;
import com.serotonin.bacnet4j.RemoteDevice;
import com.serotonin.bacnet4j.exception.BACnetException;
import com.serotonin.bacnet4j.service.unconfirmed.WhoIsRequest;
import com.serotonin.bacnet4j.type.Encodable;
import com.serotonin.bacnet4j.type.constructed.ObjectPropertyReference;
import com.serotonin.bacnet4j.type.constructed.SequenceOf;
import com.serotonin.bacnet4j.type.enumerated.ObjectType;
import com.serotonin.bacnet4j.type.enumerated.PropertyIdentifier;
import com.serotonin.bacnet4j.type.primitive.ObjectIdentifier;
import com.serotonin.bacnet4j.type.primitive.UnsignedInteger;
import com.serotonin.bacnet4j.util.PropertyReferences;
import com.serotonin.bacnet4j.util.PropertyValues;

public class Client {

	private static final Logger log = Logger.getLogger(Client.class.getName());
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		new Client().start();
	}

	private void start() {
		String devname = "eth5";
		int ourDeviceId = 11235;
		LocalDevice ourDevice = DeviceCreator.createDevice(devname, ourDeviceId);
		
		NewDeviceListener newDeviceListener = new NewDeviceListener();
		ourDevice.getEventHandler().addListener(newDeviceListener);
		
		while(true) {
			System.out.println("Please enter a device id");
			Scanner s = new Scanner(System.in);
			String nextLine = s.nextLine();
			if("exit".equals(nextLine))
				break;
			
			int deviceId = Integer.parseInt(nextLine.trim());
			newDeviceListener.setDeviceId(deviceId);
			broadcastWhois(ourDevice, deviceId, deviceId);
			RemoteDevice device = newDeviceListener.getDevice();
			
			getExtendedInfo(ourDevice, device);
			
			System.out.println("instanceNumber="+device.getInstanceNumber());
			System.out.println("name="+device.getName());
			System.out.println("vendorID="+device.getVendorId());
			System.out.println("maxAPDU="+device.getMaxAPDULengthAccepted());
			System.out.println("address.port="+device.getAddress().getPort());
			try {
				System.out.println("address.inetAddr="+device.getAddress().getInetAddress());
			} catch (UnknownHostException e) {
				System.out.println("address.inetAddr=(unknown host exception)");
			}
			System.out.println("address"+device.getAddress());
			System.out.println("network.networkNum"+device.getNetwork().getNetworkNumber());
			System.out.println("network.networkAddressDottedStr="+device.getNetwork().getNetworkAddressDottedString());
			System.out.println("objectIdentifier.instanceNumer="+device.getObjectIdentifier().getInstanceNumber());
			System.out.println("objectIdentifier.objectType"+device.getObjectIdentifier().getObjectType());
			System.out.println("protocolVersion"+device.getProtocolVersion());
			System.out.println("protocolRevision"+device.getProtocolRevision());
			System.out.println("segmentation.receive="+device.getSegmentationSupported().hasReceiveSegmentation());
			System.out.println("segmentation.send="+device.getSegmentationSupported().hasTransmitSegmentation());
			System.out.println("services.isAckAlarm="+device.getServicesSupported().isAcknowledgeAlarm());
			System.out.println("services.isAddListElem"+device.getServicesSupported().isAddListElement());
			System.out.println("services.isAtomicRead"+device.getServicesSupported().isAtomicReadFile());
			System.out.println("services.isAtomicWrite"+device.getServicesSupported().isAtomicWriteFile());
			System.out.println("services.isAuth"+device.getServicesSupported().isAuthenticate());
			System.out.println("services.isConfirmCovNotif"+device.getServicesSupported().isConfirmedCovNotification());
			System.out.println("services.isConfirmEvtNotif"+device.getServicesSupported().isConfirmedEventNotification());
			System.out.println("services.isConfirmPrivateTxfr"+device.getServicesSupported().isConfirmedPrivateTransfer());
			System.out.println("services.isConfirmTxtMsg"+device.getServicesSupported().isConfirmedTextMessage());
			System.out.println("services.isCreateObj"+device.getServicesSupported().isCreateObject());
			System.out.println("services.isDeleteObj"+device.getServicesSupported().isDeleteObject());
			System.out.println("services.isDeviceCommCtrl"+device.getServicesSupported().isDeviceCommunicationControl());
			System.out.println("services.isGetAlrmSummary"+device.getServicesSupported().isGetAlarmSummary());
			System.out.println("services.isGetEnrollment"+device.getServicesSupported().isGetEnrollmentSummary());
			System.out.println("services.isGetEvtInfo"+device.getServicesSupported().isGetEventInformation());
			System.out.println("services.isIAm"+device.getServicesSupported().isIAm());
			System.out.println("services.isIHave"+device.getServicesSupported().isIHave());
			System.out.println("services.isLifeSafety"+device.getServicesSupported().isLifeSafetyOperation());
			System.out.println("services.isReadProp"+device.getServicesSupported().isReadProperty());
			System.out.println("services.isReadPropConditional"+device.getServicesSupported().isReadPropertyConditional());
			System.out.println("services.isReadPropMultiple"+device.getServicesSupported().isReadPropertyMultiple());
			System.out.println("services.isReadRange"+device.getServicesSupported().isReadRange());
			System.out.println("services.isReinit"+device.getServicesSupported().isReinitializeDevice());
			System.out.println("services.isRemoveListElem"+device.getServicesSupported().isRemoveListElement());
			System.out.println("services.isRequestKey"+device.getServicesSupported().isRequestKey());
			System.out.println("services.isSubscribeCov"+device.getServicesSupported().isSubscribeCov());
			System.out.println("services.isSubscribeCofProp"+device.getServicesSupported().isSubscribeCovProperty());
			System.out.println("services.isTimeSynch"+device.getServicesSupported().isTimeSynchronization());
			System.out.println("services.isUnconfirmCovNotif"+device.getServicesSupported().isUnconfirmedCovNotification());
			System.out.println("services.isUnconfirmEvtNotif"+device.getServicesSupported().isUnconfirmedEventNotification());
			System.out.println("services.isUnconfirmPrivateTxfr"+device.getServicesSupported().isUnconfirmedPrivateTransfer());
			System.out.println("services.isUnconfirmTxtMsg"+device.getServicesSupported().isUnconfirmedTextMessage());
			System.out.println("services.isUtcTimeSynch"+device.getServicesSupported().isUtcTimeSynchronization());
			System.out.println("services.isVtClose"+device.getServicesSupported().isVtClose());
			System.out.println("services.isVtData"+device.getServicesSupported().isVtData());
			System.out.println("services.isVtOpen"+device.getServicesSupported().isVtOpen());
			System.out.println("services.isWhoHas"+device.getServicesSupported().isWhoHas());
			System.out.println("services.isWhoIs"+device.getServicesSupported().isWhoIs());
			System.out.println("services.isWriteProp"+device.getServicesSupported().isWriteProperty());
			System.out.println("services.isWritePropMultiple"+device.getServicesSupported().isWritePropertyMultiple());
			
			SequenceOf<ObjectIdentifier> seq = readOids(ourDevice, device);
			List<ObjectIdentifier> oids = seq.getValues();
			
			PropertyReferences refs = new PropertyReferences();
			List<ObjectIdentifier> oidsToPoll = new ArrayList<ObjectIdentifier>();
			for(ObjectIdentifier oid : oids) {
				refs.add(oid, PropertyIdentifier.units);
				refs.add(oid, PropertyIdentifier.objectName);
				if (!oid.getObjectType().equals(ObjectType.trendLog))
					refs.add(oid, PropertyIdentifier.presentValue);
				oidsToPoll.add(oid);
			}
			
			Map<ObjKey, Encodable> properties = new HashMap<ObjKey, Encodable>();
			readAndFillInProps(ourDevice, device, refs, properties);
			
			log.info("logging list of properties="+oidsToPoll.size());
			for(ObjectIdentifier oid : oidsToPoll) {
				logProperties(properties, oid);
			}
		}
	}

	private void logProperties(Map<ObjKey, Encodable> properties,
			ObjectIdentifier oid) {
		Encodable objectName = properties.get(new ObjKey(oid, PropertyIdentifier.objectName));
		Encodable units = properties.get(new ObjKey(oid, PropertyIdentifier.units));
		Encodable val = properties.get(new ObjKey(oid, PropertyIdentifier.presentValue));

		System.out.println(objectName+"("+units+"/"+oid.getObjectType()+"/"+oid.getInstanceNumber()+")="+val);
	}

	private void readAndFillInProps(LocalDevice ourDevice, RemoteDevice remoteDevice, PropertyReferences theRefs, Map<ObjKey, Encodable> properties) {
		PropertyValues propVals = readProperties(ourDevice, remoteDevice, theRefs);

		//Here we have an iterator of units and objectNames....
		Iterator<ObjectPropertyReference> iterator = propVals.iterator();
		while(iterator.hasNext()) {
			ObjectPropertyReference ref = iterator.next();
			ObjectIdentifier oid = ref.getObjectIdentifier();
			PropertyIdentifier id = ref.getPropertyIdentifier();

			try {
				Encodable value = propVals.get(ref);
				ObjKey k = new ObjKey(oid, id);
				properties.put(k, value);
			} catch(Exception e) {
				//Tons of stuff has no units and some stuff has no objectNames
				if(log.isLoggable(Level.FINE))
					log.log(Level.FINE, "Exception reading prop oid="+oid+" from id="+id+" device="+remoteDevice, e);
			}
		}
	}

	private PropertyValues readProperties(LocalDevice ourDevice,
			RemoteDevice remoteDevice, PropertyReferences theRefs) {
		try {
			return ourDevice.readProperties(remoteDevice, theRefs);
		} catch (BACnetException e) {
			throw new RuntimeException(e);
		}
	}
	
	@SuppressWarnings("unchecked")
	private SequenceOf<ObjectIdentifier> readOids(LocalDevice ourDevice, RemoteDevice device) {
		try {
			return (SequenceOf<ObjectIdentifier>) ourDevice.sendReadPropertyAllowNull(device, device.getObjectIdentifier(), PropertyIdentifier.objectList);
		} catch (BACnetException e) {
			throw new RuntimeException(e);
		}
	}

	private void getExtendedInfo(LocalDevice ourDevice, RemoteDevice device) {
		try {
			ourDevice.getExtendedDeviceInformation(device);
		} catch (BACnetException e) {
			throw new RuntimeException(e);
		}
	}

	private void broadcastWhois(LocalDevice t_localDevice, int low, int high) {
		WhoIsRequest whois;

		if (low == -1 && high == -1) {
			whois = new WhoIsRequest();
		} else if(low < 0) {
			throw new IllegalArgumentException("low end cannot be less than 0. low="+low);
		} else if(high <= low) {
			throw new IllegalArgumentException("high must be greater than low.  high="+high+" low="+low);
		} else {
			log.info("Scanning device ids: " + low + " to " + high);

			whois = new WhoIsRequest(new UnsignedInteger(low),
					new UnsignedInteger(high));
		}

		try {
			t_localDevice.sendBroadcast(whois);
		} catch (BACnetException e) {
			throw new RuntimeException(e);
		}
	}
}
