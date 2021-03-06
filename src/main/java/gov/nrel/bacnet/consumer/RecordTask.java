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

import gov.nrel.bacnet.consumer.beans.Counter;
import gov.nrel.bacnet.consumer.beans.DatabusBean;
import gov.nrel.bacnet.consumer.beans.Delay;
import gov.nrel.bacnet.consumer.beans.MultiplyConfig;
import gov.nrel.bacnet.consumer.beans.Stream;
import gov.nrel.bacnet.consumer.beans.Times;

import java.math.BigInteger;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

// import com.serotonin.bacnet4j.LocalDevice;
// import com.serotonin.bacnet4j.RemoteDevice;
// import com.serotonin.bacnet4j.exception.BACnetException;
// import com.serotonin.bacnet4j.exception.BACnetTimeoutException;
// import com.serotonin.bacnet4j.exception.PropertyValueException;
// import com.serotonin.bacnet4j.type.Encodable;
// import com.serotonin.bacnet4j.type.constructed.ObjectPropertyReference;
// import com.serotonin.bacnet4j.type.enumerated.ObjectType;
// import com.serotonin.bacnet4j.type.enumerated.PropertyIdentifier;
// import com.serotonin.bacnet4j.type.primitive.ObjectIdentifier;
// import com.serotonin.bacnet4j.util.PropertyReferences;
// import com.serotonin.bacnet4j.util.PropertyValues;

class RecordTask implements Runnable {

  private static final Logger log = Logger.getLogger(PollDeviceTask.class.getName());
  private Collection<BACnetDataWriter> writers;
  private List<BACnetData> data;

  
  public RecordTask(List<BACnetData> data, Collection<BACnetDataWriter> writers) {
    this.data = data;
    this.writers = writers;
  }

  @Override
  public void run() {
    log.info("running record task");
    try {
      for (BACnetData datum : data) {
        log.info("data: time: "+datum.curTime + " value "+datum.value);
      }
      log.info("launching databus writer.");
      for (BACnetDataWriter writer : writers) {
        writer.oidsDiscovered(data);
      }
    } catch(Exception e) {
      log.log(Level.WARNING, "Exception recording data",e);
    } 
  }

}
