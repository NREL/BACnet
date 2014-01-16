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

package gov.nrel.bacnet.consumer;

public class BACnetData
{
  public BACnetData(com.serotonin.bacnet4j.type.primitive.ObjectIdentifier oid, com.serotonin.bacnet4j.type.Encodable value, int instanceNumber,
      long curTime)
  {
    this.oid = oid;
    this.value = value;
    this.instanceNumber = instanceNumber;
    this.curTime = curTime;
  }


  public int instanceNumber;
  public com.serotonin.bacnet4j.type.primitive.ObjectIdentifier oid;
  public com.serotonin.bacnet4j.type.Encodable value;
  public long curTime;

}

