package gov.nrel.bacnet;

import javax.xml.bind.annotation.XmlElement;

import org.codehaus.jackson.annotate.JsonProperty;

class GroupKey {

	@JsonProperty("modelName")
	@XmlElement(name="modelName")
	public String groupName;
	
	@JsonProperty("keyForGet")
	@XmlElement(name="keyForGet")
	public String keyForGet;

	public GroupKey() {}
	public GroupKey(String name, String apiKeyForGet) {
		this.groupName = name;
		this.keyForGet = apiKeyForGet;
	}

	public String getGroupName() {
		return groupName;
	}

	public void setGroupName(String groupName) {
		this.groupName = groupName;
	}

	public String getKeyForGet() {
		return keyForGet;
	}

	public void setKeyForGet(String keyForGet) {
		this.keyForGet = keyForGet;
	}
}
