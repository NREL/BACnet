package gov.nrel.consumer;

import gov.nrel.consumer.beans.Device;
import gov.nrel.consumer.beans.JsonAllFilters;
import gov.nrel.consumer.beans.Stream;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.gson.Gson;
import com.serotonin.bacnet4j.LocalDevice;
import com.serotonin.bacnet4j.RemoteDevice;
import com.serotonin.bacnet4j.exception.BACnetException;
import com.serotonin.bacnet4j.type.constructed.SequenceOf;
import com.serotonin.bacnet4j.type.enumerated.PropertyIdentifier;
import com.serotonin.bacnet4j.type.primitive.ObjectIdentifier;
import com.serotonin.bacnet4j.util.PropertyReferences;

public class TaskCReadBasicProps implements Runnable, Callable<Object> {

	private static final Logger log = Logger.getLogger(TaskCReadBasicProps.class.getName());
	
	private List<TaskFPollDeviceTask> devices;
	private JsonAllFilters deviceConfig;
	private Gson gson = new Gson();
	private String fileName;
	private LocalDevice m_localDevice;
	private CountDownLatch latch;
	private OurExecutor exec;
	private DataPointWriter writer;

	private PropertiesReader propReader;
	private String id;
	
	public TaskCReadBasicProps(int counter, LocalDevice local, OurExecutor exec, List<TaskFPollDeviceTask> devices,
			JsonAllFilters deviceConfig2, CountDownLatch latch, DataPointWriter dataPointWriter) {
		this.id = "task"+counter;
		this.m_localDevice = local;
		this.propReader = new PropertiesReader(local);
		this.exec = exec;
		this.devices = devices;
		this.deviceConfig = deviceConfig2;
		this.latch = latch;
		this.writer = dataPointWriter;
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
			log.info(id+"SCANNING ALL DEVICES FOR OIDS IN THREAD devices size="+devices.size());
			int counter = 0;
			for(TaskFPollDeviceTask st : devices) {
				counter++;
				processDevice(st, counter);
			}
			long total = System.currentTimeMillis() - start;
			log.info(id+"SCANNING FINISHED, took time="+total+"ms  dataobjects size="+devices.size());
	
			return null;
		} finally {
			latch.countDown();
			log.info(id+"latch countdown.  count="+latch.getCount());
		}
	}

	private void processDevice(TaskFPollDeviceTask st, int counter2) {
		long time = System.currentTimeMillis();
		try {
			processDeviceImpl(st, counter2);
		} catch(Exception e) {
			long end = System.currentTimeMillis() - time;
			log.log(Level.WARNING, "Exception on device="+st.getRemoteDevice()+".  method took time="+end+"ms", e);
		}
	}

	private void processDeviceImpl(TaskFPollDeviceTask st, int counter2) throws BACnetException {
//		if(rd.getNetwork() != null) {
//			d.setNetworkAddress(rd.getNetwork().getNetworkAddressDottedString());
//			d.setNetworkNumber(""+rd.getNetwork().getNetworkNumber());
//		}
//		if(rd.getAddress() != null) {
//			d.setMacAddress(""+rd.getAddress().getMacAddress());
//			d.setMacNetworkNumber(""+rd.getAddress().getNetworkNumber());
//		}
		List<Stream> streams = new ArrayList<Stream>();
		Device dev = new Device();
		List<ObjectIdentifier> oids = readInDeviceOidsAndBaseProperties(st, streams, dev, id, counter2);

		log.info(id+"posting streams="+streams.size()+" c="+counter2);
		DatabusSender sender = writer.getSender();
		for(Stream str : streams) {
			sender.postNewStream(str, dev, "bacnet", id);
		}

		st.setCachedOids(oids);
	}

	private void setDeviceProps(RemoteDevice rd, Device dev) {
		String deviceDescription = rd.getName();
		int spaceIndex = deviceDescription.indexOf(" ");
		int uscoreIndex = deviceDescription.indexOf("_");
		String site = deviceDescription.startsWith("NWTC") ? "NWTC" : "STM";
		String bldg = deviceDescription.startsWith("CP")
				|| deviceDescription.startsWith("FTU")
				|| deviceDescription.startsWith("1ST") ? "RSF"
				: (deviceDescription.startsWith("Garage") ? "Garage"
						: (spaceIndex < uscoreIndex
								&& spaceIndex != -1
								|| uscoreIndex == -1 ? deviceDescription
								.split(" ")[0]
								: deviceDescription.split("_")[0]));
		
		String endUse = "unknown";
		String protocol = "BACNet";

		String deviceId = TaskGRecordTask.BACNET_PREFIX+rd.getInstanceNumber();
		
		log.info("setting up deviceobject="+deviceId);
		
		dev.setDeviceId(deviceId);
		dev.setDeviceDescription(deviceDescription);
		dev.setOwner("NREL");
		dev.setSite(site);
		dev.setBldg(bldg);
		dev.setEndUse(endUse);
		dev.setProtocol(protocol);
		if(rd.getAddress() != null)
			dev.setAddress(rd.getAddress().toIpPortString());
	}

	@SuppressWarnings("unchecked")
	private List<ObjectIdentifier> readInDeviceOidsAndBaseProperties(TaskFPollDeviceTask task, List<Stream> streams, Device dev, String id2, int counter2)
			throws BACnetException {
		RemoteDevice d = task.getRemoteDevice();
		long startExt = System.currentTimeMillis();
		//unfortunately, we need some of the info that is filled into the remote device for later to see
		//if reading multiple properties is supported.
		m_localDevice.getExtendedDeviceInformation(d);
		long total = System.currentTimeMillis()-startExt;

		setDeviceProps(d, dev);

		long startOid = System.currentTimeMillis();
		List<ObjectIdentifier> allOids = ((SequenceOf<ObjectIdentifier>) m_localDevice
					.sendReadPropertyAllowNull(d,
							d.getObjectIdentifier(),
							PropertyIdentifier.objectList)).getValues();
		long totalOid = System.currentTimeMillis()-startOid;

		PropertyReferences refs = new PropertyReferences();
		List<ObjectIdentifier> oidsToPoll = setupRefs(task, allOids, refs);
		
		long startProps = System.currentTimeMillis();
		boolean singleReadSuccess = true;
		if(refs.size() > 0)
			singleReadSuccess = propReader.readAllProperties(task, refs, oidsToPoll, dev, streams, id);
		long totalProps = System.currentTimeMillis()-startProps;
		
		log.info(id+"device count="+counter2+" device="+d.getInstanceNumber()+"refreshing oid information ext dev info="+total+"ms getting oids="+totalOid+"ms  props="+totalProps+"ms singleReadSuccess="+singleReadSuccess);
		return oidsToPoll;
	}

	private List<ObjectIdentifier> setupRefs(TaskFPollDeviceTask task, List<ObjectIdentifier> cachedOids, PropertyReferences refs) {
		List<ObjectIdentifier> oidsToPoll = new ArrayList<ObjectIdentifier>();
		for(ObjectIdentifier oid : cachedOids) {
			int pollInSeconds = deviceConfig.getPollingInterval(task.getRemoteDevice(), oid);
			if(pollInSeconds > 0) {
				task.addInterval(oid, pollInSeconds);
				refs.add(oid, PropertyIdentifier.units);
				refs.add(oid, PropertyIdentifier.objectName);
				oidsToPoll.add(oid);
			}
		}
		return oidsToPoll;
	}

}
