

import javax.xml.bind.annotation.XmlElement;

import org.codehaus.jackson.annotate.JsonProperty;

public class RegisterResponseMessage {

	@JsonProperty("modelName")
	@XmlElement(name="modelName")
	public String modelName;
	
	@JsonProperty("postKey")
	@XmlElement(name="postKey")
	public String postKey;

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
