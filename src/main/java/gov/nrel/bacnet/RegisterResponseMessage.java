package gov.nrel.bacnet;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlElement;

import org.codehaus.jackson.annotate.JsonProperty;

public class RegisterResponseMessage {

	@JsonProperty("modelName")
	@XmlElement(name="modelName")
	public String modelName;
	
	@JsonProperty("postKey")
	@XmlElement(name="postKey")
	public String postKey;

	@JsonProperty("groups")
    @XmlElement(name="groups")
	public List<GroupKey> groupKeys = new ArrayList<GroupKey>();
	
	public List<GroupKey> getGroupKeys() {
		return groupKeys;
	}

	public void setGroupKeys(List<GroupKey> semantics) {
		this.groupKeys = semantics;
	}

	public String getModelName() {
		return modelName;
	}

	public void setModelName(String modelName) {
		this.modelName = modelName;
	}

	public String getPostKey() {
		return postKey;
	}

	public void setPostKey(String postKey) {
		this.postKey = postKey;
	}
	
} // RegisterResponseMessage
