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
