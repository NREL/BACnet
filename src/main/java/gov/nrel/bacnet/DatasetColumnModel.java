/*
 * Copyright (C) 2013, Alliance for Sustainable Energy
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package gov.nrel.bacnet;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import org.codehaus.jackson.annotate.JsonProperty;

@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class DatasetColumnModel {
	
	@JsonProperty("name")
    @XmlElement(name="name")
    public String name;
	
	@JsonProperty("dataType")
    @XmlElement(name="dataType")
    public String dataType;
	
	@JsonProperty("semanticType")
    @XmlElement(name="semanticType")
    public String semanticType;
	
	@JsonProperty("isIndex")
    @XmlElement(name="isIndex")
    public boolean isIndex;
	
	@JsonProperty("isPrimaryKey")
    @XmlElement(name="isPrimaryKey")
    public boolean isPrimaryKey;

	@JsonProperty("fkTableName")
    @XmlElement(name="fkTableName")
    public String foreignKeyTablename;
	
	@JsonProperty("semantics")
    @XmlElement(name="semantics")
	public List<DatasetColumnSemanticModel> semantics = new ArrayList<DatasetColumnSemanticModel>();
	
	
	// Default constructor stuff
	{
		isIndex = false;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getForeignKeyTablename() {
		return foreignKeyTablename;
	}

	public void setForeignKeyTablename(String foreignKeyTablename) {
		this.foreignKeyTablename = foreignKeyTablename;
	}

	public String getDataType() {
		return dataType;
	}

	public void setDataType(String dataType) {
		this.dataType = dataType;
	}

	public String getSemanticType() {
		return semanticType;
	}

	public void setSemanticType(String semanticType) {
		this.semanticType = semanticType;
	}

	public boolean getIsIndex() {
		return isIndex;
	}

	public void setIsIndex(boolean isIndex) {
		this.isIndex = isIndex;
	}

	public List<DatasetColumnSemanticModel> getSemantics() {
		return semantics;
	}

	public void setSemantics(List<DatasetColumnSemanticModel> semantics) {
		this.semantics = semantics;
	}

	public boolean getIsPrimaryKey() {
		return isPrimaryKey;
	}

	public void setIsPrimaryKey(boolean isPrimaryKey) {
		this.isPrimaryKey = isPrimaryKey;
	}
	
} // DatasetColumnModel

