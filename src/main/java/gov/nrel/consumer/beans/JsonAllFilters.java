package gov.nrel.consumer.beans;

import java.util.ArrayList;
import java.util.List;

import com.serotonin.bacnet4j.RemoteDevice;
import com.serotonin.bacnet4j.type.primitive.ObjectIdentifier;

public class JsonAllFilters {

	private List<JsonObjectData> filters = new ArrayList<JsonObjectData>();

	public List<JsonObjectData> getFilters() {
		return filters;
	}

	public void setFilters(List<JsonObjectData> allDevices) {
		this.filters = allDevices;
	}

	public int getPollingInterval(RemoteDevice remoteDevice, ObjectIdentifier oid) {
		for(JsonObjectData filter : filters) {
			if(filter.match(remoteDevice, oid))
				return filter.getInterval();
		}
		
		return 120*60; //120 minutes * 60 seconds/minute
	}
	
}
