package gov.nrel.consumer;

import gov.nrel.consumer.beans.JsonAllFilters;
import gov.nrel.consumer.beans.JsonObjectData;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.gson.Gson;
import com.serotonin.bacnet4j.LocalDevice;
import com.serotonin.bacnet4j.RemoteDevice;
import com.serotonin.bacnet4j.exception.BACnetException;
import com.serotonin.bacnet4j.type.Encodable;
import com.serotonin.bacnet4j.type.constructed.ObjectPropertyReference;
import com.serotonin.bacnet4j.type.constructed.SequenceOf;
import com.serotonin.bacnet4j.type.enumerated.PropertyIdentifier;
import com.serotonin.bacnet4j.type.primitive.ObjectIdentifier;
import com.serotonin.bacnet4j.util.PropertyReferences;
import com.serotonin.bacnet4j.util.PropertyValues;

public class TaskCReadBasicProps implements Runnable, Callable<Object> {

	private static final Logger log = Logger.getLogger(TaskCReadBasicProps.class.getName());
	
	private List<TaskFPollDeviceTask> devices;
	private JsonAllFilters deviceConfig;
	private Gson gson = new Gson();
	private String fileName;
	private LocalDevice m_localDevice;
	private CountDownLatch latch;
	private List<TaskFPollDeviceTask> failures;

	private OurExecutor exec;
	
	public TaskCReadBasicProps(LocalDevice local, OurExecutor exec, List<TaskFPollDeviceTask> devices, List<TaskFPollDeviceTask> failures,
			JsonAllFilters deviceConfig2, String fileName, CountDownLatch latch) {
		this.m_localDevice = local;
		this.exec = exec;
		this.devices = devices;
		this.deviceConfig = deviceConfig2;
		this.fileName = fileName;
		this.latch = latch;
		this.failures = failures;
	}

	@Override
	public void run() {
		log.info("throwing task to get device properties into threadpool");
		exec.execute(this);
	}
	
	@Override
	public Object call() throws Exception {
		try {
			long start = System.currentTimeMillis();
			log.info("SCANNING ALL DEVICES FOR OIDS IN THREAD devices size="+devices.size());
			List<JsonObjectData> data = new ArrayList<JsonObjectData>();
			int counter = 0;
			for(TaskFPollDeviceTask st : devices) {
				counter++;
				processDevice(st, data, counter);
			}
			long total = System.currentTimeMillis() - start;
			log.info("SCANNING FINISHED, took time="+total+"ms  dataobjects size="+data.size());
	
			writeToFile(data, gson, fileName);
			
			return null;
		} finally {
			latch.countDown();
			log.info("latch countdown.  count="+latch.getCount());
		}
	}

	private static synchronized void writeToFile(List<JsonObjectData> data, Gson gson, String fileName) {
		FileWriter out = null;
		try {
			File f = new File(fileName);
			if(!f.exists())
				f.createNewFile();
			out = new FileWriter(f, true);
			
			for(JsonObjectData d : data) {
				String json = gson.toJson(d);
				out.write(json+"\n");
			}
		} catch(Exception e) {
			log.log(Level.WARNING, "could not log devices we have in memory", e);
		} finally {
			try {
				if(out != null)
					out.close();
			} catch (IOException e) {
				log.log(Level.WARNING, "could not close file", e);
			}
		}
	}

	private void processDevice(TaskFPollDeviceTask st, List<JsonObjectData> data, int counter2) {
		long time = System.currentTimeMillis();
		JsonObjectData d = new JsonObjectData();
		d.setJustInstance(true);
		d.setInterval(-1); //we never query presentValue of the device(does it have one??...should we do a match with it???)
		data.add(d);
		try {
			processDeviceImpl(st, d, data, counter2);
		} catch(Exception e) {
			long end = System.currentTimeMillis() - time;
			d.setError(e.getClass().getSimpleName());
			log.log(Level.WARNING, "Exception on device="+st.getRemoteDevice()+".  method took time="+end+"ms", e);
		}
	}

	private void processDeviceImpl(TaskFPollDeviceTask st, JsonObjectData d, List<JsonObjectData> data, int counter2) throws BACnetException {
		setDeviceProps(st, d);
//		if(rd.getNetwork() != null) {
//			d.setNetworkAddress(rd.getNetwork().getNetworkAddressDottedString());
//			d.setNetworkNumber(""+rd.getNetwork().getNetworkNumber());
//		}
//		if(rd.getAddress() != null) {
//			d.setMacAddress(""+rd.getAddress().getMacAddress());
//			d.setMacNetworkNumber(""+rd.getAddress().getNetworkNumber());
//		}
		
		List<ObjectIdentifier> oids = readInDeviceOidsAndBaseProperties(st, d, data, counter2);
		st.setCachedOids(oids);
	}

	private void setDeviceProps(TaskFPollDeviceTask st, JsonObjectData d) {
		RemoteDevice rd = st.getRemoteDevice();
		d.setDeviceId(""+rd.getInstanceNumber());
		d.setNetwork(""+rd.getNetwork());
		d.setAddress(""+rd.getAddress());
		d.setDeviceName(rd.getName());
	}

	@SuppressWarnings("unchecked")
	private List<ObjectIdentifier> readInDeviceOidsAndBaseProperties(TaskFPollDeviceTask task, JsonObjectData devData, List<JsonObjectData> data, int counter2)
			throws BACnetException {
		RemoteDevice d = task.getRemoteDevice();
		long startExt = System.currentTimeMillis();
		//unfortunately, we need some of the info that is filled into the remote device for later to see
		//if reading multiple properties is supported.
		m_localDevice.getExtendedDeviceInformation(d);
		long total = System.currentTimeMillis()-startExt;

		devData.setDeviceName(d.getName());
		
		long startOid = System.currentTimeMillis();
		List<ObjectIdentifier> cachedOids = ((SequenceOf<ObjectIdentifier>) m_localDevice
					.sendReadPropertyAllowNull(d,
							d.getObjectIdentifier(),
							PropertyIdentifier.objectList)).getValues();
		long totalOid = System.currentTimeMillis()-startOid;
		
		List<ObjectIdentifier> oidsToPoll = new ArrayList<ObjectIdentifier>();
		PropertyReferences refs = new PropertyReferences();
		for(ObjectIdentifier oid : cachedOids) {
			JsonObjectData objD = new JsonObjectData();
			data.add(objD);
			setDeviceProps(task, objD);
			
			objD.setObjectId(""+oid.getInstanceNumber());
			objD.setObjectType(""+oid.getObjectType());
			
			int pollInSeconds = deviceConfig.getPollingInterval(task.getRemoteDevice(), oid);
			objD.setInterval(pollInSeconds);
			
			if(pollInSeconds > 0) {
				task.addInterval(oid, pollInSeconds);
				refs.add(oid, PropertyIdentifier.units);
				refs.add(oid, PropertyIdentifier.objectName);
				oidsToPoll.add(oid);
			}
		}
		
		long startProps = System.currentTimeMillis();
		
		boolean singleReadSuccess = true;
		if(refs.size() > 0)
			singleReadSuccess = readAllProperties(task, refs);
		
		long totalProps = System.currentTimeMillis()-startProps;
		
		log.info("device count="+counter2+" refreshing oid information ext dev info="+total+"ms getting oids="+totalOid+"ms  props="+totalProps+"ms singleReadSuccess="+singleReadSuccess);
		return oidsToPoll;
	}
	
	private boolean readAllProperties(TaskFPollDeviceTask task, PropertyReferences refs) {
		try {
			readAndFillInProps(task, refs);
			return true;
		} catch(BACnetException e) {
			log.fine("Trying to recover from read failure by breaking up reads");
			//A frequent failure is reading tooooo many properties so now let's read 10 at a time from the device instead..
			readProperties(task, refs);
			return false;
		}
	}

	private void readProperties(TaskFPollDeviceTask task, PropertyReferences refs) {
		//TODO: NEED to play with this partition size here..... from 1 to 10 to 50 maybe....
		List<PropertyReferences> props = refs.getPropertiesPartitioned(1);
		PropertyReferences theRefs = null;
		try {
			for(PropertyReferences propRefs : props) {
				theRefs = propRefs;
				readAndFillInProps(task, theRefs);
			}
		} catch(Exception e) {
			log.log(Level.WARNING, "Exception reading props size="+theRefs.size()+" props="+theRefs.getProperties().keySet()+" for device="+task.getRemoteDevice(), e);
		}
	}

	private void readAndFillInProps(TaskFPollDeviceTask task,
			PropertyReferences theRefs) throws BACnetException {
		PropertyValues propVals = m_localDevice.readProperties(task.getRemoteDevice(), theRefs);
		
		//Here we have an iterator of units and objectNames....
		Iterator<ObjectPropertyReference> iterator = propVals.iterator();
		while(iterator.hasNext()) {
			ObjectPropertyReference ref = iterator.next();
			ObjectIdentifier oid = ref.getObjectIdentifier();
			PropertyIdentifier id = ref.getPropertyIdentifier();
			
			try {
				Encodable value = propVals.get(ref);
				task.addProperty(oid, id, value);
			} catch(Exception e) {
				//Tons of stuff has no units and some stuff has no objectNames
				if(log.isLoggable(Level.FINE))
					log.log(Level.FINE, "Exception reading prop oid="+oid+" from id="+id+" device="+task.getRemoteDevice(), e);
			}
		}
	}
}
