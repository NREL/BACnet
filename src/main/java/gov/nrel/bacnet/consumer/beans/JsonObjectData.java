package gov.nrel.bacnet.consumer.beans;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.serotonin.bacnet4j.RemoteDevice;
import com.serotonin.bacnet4j.type.primitive.ObjectIdentifier;

public class JsonObjectData {

	private Boolean justInstance = false;
	private String tableName;
	private String deviceId;
	private String deviceName;
	private String address;
	private String network;
	private String objectType;
	private String objectId;
	private Double value;
	private String objectName;
	private String units;
	private int interval;
	private String error;
	private long time;
	
	private transient Pattern deviceIdPattern;
	private transient Pattern objectTypePattern;
	private transient Pattern objectIdPattern;
	
	public Boolean isJustInstance() {
		return justInstance;
	}
	public void setJustInstance(Boolean justInstance) {
		this.justInstance = justInstance;
	}
	public String getDeviceId() {
		return deviceId;
	}
	public void setDeviceId(String instanceId) {
		this.deviceId = instanceId;
	}
	public String getDeviceName() {
		return deviceName;
	}
	public void setDeviceName(String instanceName) {
		this.deviceName = instanceName;
	}
	public String getAddress() {
		return address;
	}
	public void setAddress(String address) {
		this.address = address;
	}
	public String getNetwork() {
		return network;
	}
	public void setNetwork(String network) {
		this.network = network;
	}
	public String getObjectType() {
		return objectType;
	}
	public void setObjectType(String objectType) {
		this.objectType = objectType;
	}
	public String getObjectId() {
		return objectId;
	}
	public void setObjectId(String objectId) {
		this.objectId = objectId;
	}
	public Double getValue() {
		return value;
	}
	public void setValue(Double val) {
		this.value = val;
	}
	public String getObjectName() {
		return objectName;
	}
	public void setObjectName(String objName) {
		this.objectName = objName;
	}
	public String getUnits() {
		return units;
	}
	public void setUnits(String units) {
		this.units = units;
	}
	public int getInterval() {
		return interval;
	}
	public void setInterval(int time) {
		this.interval = time;
	}
	public String getError() {
		return error;
	}
	public void setError(String error) {
		this.error = error;
	}
	public boolean match(RemoteDevice remoteDevice, ObjectIdentifier oid) {
		String deviceId = ""+remoteDevice.getInstanceNumber();
		//String deviceType = ""+remoteDevice.getObjectIdentifier().getObjectType();
		//String deviceName = ""+remoteDevice.getName();
		String objectType = ""+oid.getObjectType();
		String objectId = ""+oid.getInstanceNumber();
		
		Matcher deviceMatcher = deviceIdPattern.matcher(deviceId);
		Matcher objectTypeMatcher = objectTypePattern.matcher(objectType);
		Matcher objectIdMatcher = objectIdPattern.matcher(objectId);
		
		return deviceMatcher.matches() && objectTypeMatcher.matches() && objectIdMatcher.matches();
	}
	
	public void init() {
		deviceIdPattern = compile(deviceId);
		objectTypePattern = compile(objectType);
		objectIdPattern = compile(objectId);
	}
	private Pattern compile(String pattern) {
		try {
			return Pattern.compile(pattern);
		} catch(Exception e) {
			throw new RuntimeException("Could not compile pattern="+pattern+" PLEASE fix your configuration file filter.json", e);
		}
	}
	public void setTime(long cur) {
		this.time = cur;
	}
	public long getTime() {
		return time;
	}
	
	public String getTableName() {
		return tableName;
	}
	public void setTableName(String tableName) {
		this.tableName = tableName;
	}

}
