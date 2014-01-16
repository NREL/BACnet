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

import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.lang.SecurityException;

public abstract class LogHandler extends java.util.logging.Handler
{
  public void close() throws java.lang.SecurityException
  {
  }

  public void flush() 
  {
  }

  public synchronized void publish(java.util.logging.LogRecord r)
  {
    publishRecordImpl(r);
  }

  public synchronized java.util.logging.LogRecord[] getRecords()
  {
    return getRecordsImpl();
  }

  protected abstract void publishRecordImpl(java.util.logging.LogRecord r);
  protected abstract java.util.logging.LogRecord[] getRecordsImpl();

}


