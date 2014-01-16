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
