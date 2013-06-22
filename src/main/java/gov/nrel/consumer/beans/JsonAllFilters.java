package gov.nrel.consumer.beans;

import gov.nrel.consumer.PropertiesReader;
import gov.nrel.consumer.DatabusDataWriter;
import java.util.logging.Logger;
import java.util.ArrayList;
import java.util.List;

import com.serotonin.bacnet4j.RemoteDevice;
import com.serotonin.bacnet4j.type.primitive.ObjectIdentifier;

public class JsonAllFilters {

	private static final Logger log = Logger.getLogger(JsonAllFilters.class.getName());
	
	private List<JsonObjectData> filters = new ArrayList<JsonObjectData>();

	public List<JsonObjectData> getFilters() {
		return filters;
	}

	public void setFilters(List<JsonObjectData> allDevices) {
		this.filters = allDevices;
	}

	public int getPollingInterval(RemoteDevice remoteDevice, ObjectIdentifier oid) {
		Integer interval = null;
		for(JsonObjectData filter : filters) {
			if(filter.match(remoteDevice, oid))
				interval = filter.getInterval();
		}
		
		if(interval == null)
			interval = 120*60; //120 minutes * 60 seconds/minute
		
		String deviceId = DatabusDataWriter.BACNET_PREFIX+remoteDevice.getInstanceNumber();
		String tableName = PropertiesReader.formTableName(deviceId, oid);
		log.info("For table="+tableName+" we are using an interval="+interval);
		return interval;
	}
	
}
