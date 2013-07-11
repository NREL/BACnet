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


