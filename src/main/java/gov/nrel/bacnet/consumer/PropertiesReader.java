package gov.nrel.bacnet.consumer;

import gov.nrel.bacnet.consumer.beans.Device;
import gov.nrel.bacnet.consumer.beans.ObjKey;
import gov.nrel.bacnet.consumer.beans.Stream;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.serotonin.bacnet4j.LocalDevice;
import com.serotonin.bacnet4j.exception.BACnetException;
import com.serotonin.bacnet4j.type.Encodable;
import com.serotonin.bacnet4j.type.constructed.ObjectPropertyReference;
import com.serotonin.bacnet4j.type.enumerated.PropertyIdentifier;
import com.serotonin.bacnet4j.type.primitive.ObjectIdentifier;
import com.serotonin.bacnet4j.util.PropertyReferences;
import com.serotonin.bacnet4j.util.PropertyValues;

public class PropertiesReader {

	private static final Logger log = Logger.getLogger(PropertiesReader.class.getName());
	private LocalDevice m_localDevice;
	
	
	
	public PropertiesReader(LocalDevice local) {
		this.m_localDevice = local;
	}

	boolean readAllProperties(TaskFPollDeviceTask task, PropertyReferences refs, List<ObjectIdentifier> oidsToPoll, Device dev, List<Stream> streams, String id) {
		Map<ObjKey, Encodable> properties = new HashMap<ObjKey, Encodable>();
		boolean readSuccess = readAllPropertiesImpl(task, refs, oidsToPoll, properties);
		
		log.info(id+"creating streams="+oidsToPoll.size());
		for(ObjectIdentifier oid : oidsToPoll) {
			createStream(oid, properties, dev, streams);
		}
		return readSuccess;
	}
	
	private boolean readAllPropertiesImpl(TaskFPollDeviceTask task, PropertyReferences refs, List<ObjectIdentifier> oidsToPoll, Map<ObjKey, Encodable> properties) {
		try {
			readAndFillInProps(task, refs, properties);
			return true;
		} catch(BACnetException e) {
			log.info("Trying to recover from read failure by breaking up reads");
			//A frequent failure is reading tooooo many properties so now let's read 10 at a time from the device instead..
			readProperties(task, refs, properties);
			return false;
		}
	}
	
	private void readProperties(TaskFPollDeviceTask task, PropertyReferences refs, Map<ObjKey, Encodable> properties) {
		//TODO: NEED to play with this partition size here..... from 1 to 10 to 50 maybe....
		List<PropertyReferences> props = refs.getPropertiesPartitioned(1);
		PropertyReferences theRefs = null;
		try {
			for(PropertyReferences propRefs : props) {
				theRefs = propRefs;
				readAndFillInProps(task, theRefs, properties);
			}
		} catch(Exception e) {
			log.log(Level.WARNING, "Exception reading props size="+theRefs.size()+" props="+theRefs.getProperties().keySet()+" for device="+task.getRemoteDevice(), e);
		}
	}
	
	private void readAndFillInProps(TaskFPollDeviceTask task,
			PropertyReferences theRefs, Map<ObjKey, Encodable> properties) throws BACnetException {
		PropertyValues propVals = m_localDevice.readProperties(task.getRemoteDevice(), theRefs);
		
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
					log.log(Level.FINE, "Exception reading prop oid="+oid+" from id="+id+" device="+task.getRemoteDevice(), e);
			}
		}
	}
	
	private void createStream(ObjectIdentifier oid,
			Map<ObjKey, Encodable> properties, Device dev, List<Stream> streams) {
		Encodable objectName = properties.get(new ObjKey(oid, PropertyIdentifier.objectName));
		Encodable units = properties.get(new ObjKey(oid, PropertyIdentifier.units));

		String deviceId = dev.getDeviceId();
		String tableName = formTableName(deviceId, oid);
		
		Stream str = new Stream();
		str.setTableName(tableName);
		str.setStreamDescription(""+objectName);
		str.setUnits(""+units);
		str.setDevice(deviceId);
		str.setStreamType(oid.getObjectType()+"");
		str.setStreamId(oid.getInstanceNumber()+"");
		
		streams.add(str);
	}
	
	public static String formTableName(String deviceId, ObjectIdentifier oid) {
		String type = oid.getObjectType()+"";
		String objectType = type.replaceAll("\\s","");
		String objectId = ""+oid.getInstanceNumber();
		String tableName = deviceId+objectType+objectId;
		return tableName;
	}
}
