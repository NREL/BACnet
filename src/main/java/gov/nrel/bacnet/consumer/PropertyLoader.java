package gov.nrel.bacnet.consumer;

import gov.nrel.bacnet.consumer.beans.Device;
// import gov.nrel.bacnet.consumer.beans.JsonAllFilters;
import gov.nrel.bacnet.consumer.beans.Stream;
import gov.nrel.bacnet.consumer.beans.ObjKey;


import java.util.ArrayList;
import java.util.List;
import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;


import com.google.gson.Gson;
import com.serotonin.bacnet4j.LocalDevice;
import com.serotonin.bacnet4j.RemoteDevice;
import com.serotonin.bacnet4j.exception.BACnetException;
import com.serotonin.bacnet4j.type.constructed.SequenceOf;
import com.serotonin.bacnet4j.type.enumerated.PropertyIdentifier;
import com.serotonin.bacnet4j.type.primitive.ObjectIdentifier;
import com.serotonin.bacnet4j.util.PropertyReferences;
import com.serotonin.bacnet4j.type.Encodable;
import com.serotonin.bacnet4j.type.constructed.ObjectPropertyReference;
import com.serotonin.bacnet4j.util.PropertyValues;

class PropertyLoader  {

	private static final Logger log = Logger.getLogger(TaskCReadBasicProps.class.getName());
	
	// private JsonAllFilters deviceConfig;
	// private Gson gson = new Gson();
	// private String fileName;
	private LocalDevice localDevice;
	// private Collection<BACnetDataWriter> writers;

	// private PropertiesReader propReader;
	// private String id;
	
	public PropertyLoader(LocalDevice ld) {
		localDevice = ld;
	}

	public void requestProperties(RemoteDevice rd) throws BACnetException{
		// List<Stream> streams = new ArrayList<Stream>();
		// Device dev = new Device();
		Map<ObjKey, Encodable> properties = new HashMap<ObjKey, Encodable>();

		long startExt = System.currentTimeMillis();
		//unfortunately, we need some of the info that is filled into the remote device for later to see
		//if reading multiple properties is supported.
		System.out.println("getting extended device info");
		localDevice.getExtendedDeviceInformation(rd);
		long total = System.currentTimeMillis()-startExt;
		System.out.println("set extended device into in "+total);
		// copying old code - think this is only used for writing
		// setDeviceProps(rd, dev);
		System.out.println("set props");
		List<ObjectIdentifier> allOids = ((SequenceOf<ObjectIdentifier>) localDevice
					.sendReadPropertyAllowNull(rd,
							rd.getObjectIdentifier(),
							PropertyIdentifier.objectList)).getValues();
		System.out.println("returned from sendreadprop");
		// for(ObjectIdentifier oid : allOids) {
		// 	System.out.println("oid: "+ oid.toString());
		// }
		// copying from task c
		PropertyReferences refs = new PropertyReferences();
		List<ObjectIdentifier> oidsToPoll = setupRefs(allOids, refs);
		System.out.println("refs size = "+ refs.size());
		if(refs.size() > 0)
			readAllProperties(rd, refs, properties);

	}

	private void readAllProperties(RemoteDevice rd,
			PropertyReferences refs, Map<ObjKey, Encodable> properties) throws BACnetException {
		PropertyValues propVals = localDevice.readProperties(rd, refs);
		System.out.println("properties read");
		// //Here we have an iterator of units and objectNames....
		Iterator<ObjectPropertyReference> iterator = propVals.iterator();
		while(iterator.hasNext()) {
			ObjectPropertyReference ref = iterator.next();
			ObjectIdentifier oid = ref.getObjectIdentifier();
			PropertyIdentifier id = ref.getPropertyIdentifier();
			
			try {
				Encodable value = propVals.get(ref);
				ObjKey k = new ObjKey(oid, id);
				properties.put(k, value);
				System.out.println("id "+id+" objkey="+k+" value = "+value);
			} catch(Exception e) {
				System.out.println("Exception reading prop oid="+oid+" from id="+id+" device="+rd);
				//Tons of stuff has no units and some stuff has no objectNames
				// if(log.isLoggable(Level.FINE))
					// log.log(Level.FINE, "Exception reading prop oid="+oid+" from id="+id+" device="+task.getRemoteDevice(), e);
			}
		}
	}
	// filter was applied here in original code
	private List<ObjectIdentifier> setupRefs(List<ObjectIdentifier> cachedOids, PropertyReferences refs) {
		List<ObjectIdentifier> oidsToPoll = new ArrayList<ObjectIdentifier>();
		for(ObjectIdentifier oid : cachedOids) {
				refs.add(oid, PropertyIdentifier.units);
				refs.add(oid, PropertyIdentifier.objectName);
				oidsToPoll.add(oid);
		}
		// this should return refs instead.  oidstopoll aren't needed now
		return oidsToPoll;
	}
	// private void setDeviceProps(RemoteDevice rd, Device dev) {
	// 	String deviceDescription = rd.getName();
	// 	int spaceIndex = deviceDescription.indexOf(" ");
	// 	int uscoreIndex = deviceDescription.indexOf("_");
	// 	String site = deviceDescription.startsWith("NWTC") ? "NWTC" : "STM";
	// 	String bldg = deviceDescription.startsWith("CP")
	// 			|| deviceDescription.startsWith("FTU")
	// 			|| deviceDescription.startsWith("1ST") ? "RSF"
	// 			: (deviceDescription.startsWith("Garage") ? "Garage"
	// 					: (spaceIndex < uscoreIndex
	// 							&& spaceIndex != -1
	// 							|| uscoreIndex == -1 ? deviceDescription
	// 							.split(" ")[0]
	// 							: deviceDescription.split("_")[0]));
		
	// 	String endUse = "unknown";
	// 	String protocol = "BACNet";

	// 	String deviceId = DatabusDataWriter.BACNET_PREFIX+rd.getInstanceNumber();
		
	// 	log.info("setting up deviceobject="+deviceId);
		
	// 	dev.setDeviceId(deviceId);
	// 	dev.setDeviceDescription(deviceDescription);
	// 	dev.setOwner("NREL");
	// 	dev.setSite(site);
	// 	dev.setBldg(bldg);
	// 	dev.setEndUse(endUse);
	// 	dev.setProtocol(protocol);
	// 	if(rd.getAddress() != null)
	// 		dev.setAddress(rd.getAddress().toIpPortString());
	// }



}
