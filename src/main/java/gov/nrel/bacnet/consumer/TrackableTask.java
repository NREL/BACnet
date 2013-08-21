package gov.nrel.bacnet.consumer;

import java.util.concurrent.ScheduledFuture;


public interface TrackableTask
{
	public ScheduledFuture<?> getFuture();

	public int getId();
	public void setId(int id);

	public String getDescription();
	public void cancelTask();
}

