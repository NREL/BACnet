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

import javax.xml.bind.annotation.XmlElement;

import org.codehaus.jackson.annotate.JsonProperty;

class RegisterResponseMessage {

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
