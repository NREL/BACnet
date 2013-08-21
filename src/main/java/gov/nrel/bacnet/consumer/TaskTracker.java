package gov.nrel.bacnet.consumer;

import java.util.List;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.ScheduledFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

public class TaskTracker
{
	private static final Logger log = Logger.getLogger(TaskTracker.class.getName());

	private List<TrackableTask> tasks = new ArrayList<TrackableTask>();
	private int id = 0;

	public TaskTracker()
	{
		
	}

	public synchronized void add(TrackableTask t) 
	{
		cull();
		t.setId(id++);
		tasks.add(t);
		log.info("Added TrackableTask: " + t.getDescription());
	}

	public synchronized void cull()
	{
		Collection<TrackableTask> toCull = new ArrayList<TrackableTask>();

		for (TrackableTask task : tasks)
		{
			ScheduledFuture<?> future = task.getFuture();
			if (future != null)
			{
				if (future.isDone())
				{
					toCull.add(task);	
					log.info("Culling TrackableTask: " + task.getDescription());
				}
			}
		}

		tasks.removeAll(toCull);
	}

	public synchronized Collection<TrackableTask> getTasks()
	{
		cull();
		return tasks;	
	}

}
