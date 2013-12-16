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
// TODO fix logger
  private static final Logger log = Logger.getLogger(PropertiesReader.class.getName());
  
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

  public List<ObjectIdentifier> getOids(RemoteDevice rd) throws BACnetException{
    // List<Stream> streams = new ArrayList<Stream>();
    // Device dev = new Device();
    Map<ObjKey, Encodable> properties;

    // copying old code - think this is only used for writing
    // setDeviceProps(rd, dev);
    List<ObjectIdentifier> allOids = ((SequenceOf<ObjectIdentifier>) localDevice
          .sendReadPropertyAllowNull(rd,
              rd.getObjectIdentifier(),
              PropertyIdentifier.objectList)).getValues();

    return allOids;
  }

// possible that this must be run after getOids as that runs "getExtendedInformation"
  public Map<ObjKey, Encodable> getProperties(RemoteDevice rd, List<ObjectIdentifier> oids) throws BACnetException {
    Map<ObjKey, Encodable> properties = new HashMap<ObjKey, Encodable>();
    PropertyReferences refs = setupRefs(oids);

    System.out.println("oids size = "+oids.size()+" refs size = "+ refs.size());
    PropertyValues propVals = localDevice.readProperties(rd, refs);
    System.out.println("properties read");
    // //Here we have an iterator of units and objectNames....
    Iterator<ObjectPropertyReference> iterator = propVals.iterator();
    while(iterator.hasNext()) {
      ObjectPropertyReference ref = iterator.next();
      ObjectIdentifier oid = ref.getObjectIdentifier();
      PropertyIdentifier id = ref.getPropertyIdentifier();
      
      try {
        // get encodable from propertyvalues by objectpropertyreference
        Encodable value = propVals.get(ref);
        ObjKey k = new ObjKey(oid, id);
        properties.put(k, value);
        System.out.println("type = "+oid.getObjectType()+" objkey="+k+" value = "+value);
      } catch(Exception e) {
        System.out.println("Exception reading prop oid="+oid+" from id="+id+" device="+rd);
        //Tons of stuff has no units and some stuff has no objectNames
        // if(log.isLoggable(Level.FINE))
          // log.log(Level.FINE, "Exception reading prop oid="+oid+" from id="+id+" device="+task.getRemoteDevice(), e);
      }
    }
    return properties;
  }
  // filter was applied here in original code
  // basically, we are requesting 2 prop values from each oid on the device:
  // units and objectName
  private PropertyReferences setupRefs(List<ObjectIdentifier> cachedOids) {
    PropertyReferences refs = new PropertyReferences();
    for(ObjectIdentifier oid : cachedOids) {
        refs.add(oid, PropertyIdentifier.units);
        refs.add(oid, PropertyIdentifier.objectName);
        // refs.add(oid, PropertyIdentifier.presentValue);
    }
    // this should return refs instead.  oidstopoll aren't needed now
    return refs;
  }




}
