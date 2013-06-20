package gov.nrel.bacnet;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import org.codehaus.jackson.annotate.JsonProperty;

@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD) class DatasetColumnSemanticModel {

	@JsonProperty("property")
    @XmlElement(name="property")
    public String property;
	
	@JsonProperty("value")
    @XmlElement(name="value")
    public String value;

	public String getProperty() {
		return property;
	}

	public void setProperty(String property) {
		this.property = property;
	}

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}
	
} // DatasetColumnSemanticModel
