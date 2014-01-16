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

package gov.nrel.bacnet.consumer.beans;

public class Numbers {

	private long postTime;
	private long fullRegTime;
	private long reregisterTime;
	private long registerTime;
	private long postNewStreamTime;
	private long postDeviceTime;

	public void setPostTime(long total) {
		this.postTime= total;
	}

	public void setFullRegTime(long total) {
		this.fullRegTime = total;
	}

	public void setReregisterTime(long total) {
		this.reregisterTime = total;
	}

	public void setRegisterTime(long total) {
		this.registerTime = total;
	}

	public void setPostNewStreamTime(long total) {
		this.postNewStreamTime = total;
	}

	public void setPostDeviceTime(long total) {
		this.postDeviceTime = total;
	}

	public long getPostTime() {
		return postTime;
	}

	public long getFullRegTime() {
		return fullRegTime;
	}

	public long getReregisterTime() {
		return reregisterTime;
	}

	public long getRegisterTime() {
		return registerTime;
	}

	public long getPostNewStreamTime() {
		return postNewStreamTime;
	}

	public long getPostDeviceTime() {
		return postDeviceTime;
	}

}
