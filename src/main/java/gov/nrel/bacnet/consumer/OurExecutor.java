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

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.logging.Level;
import java.util.logging.Logger;

class OurExecutor {

	private static final Logger log = Logger.getLogger(OurExecutor.class.getName());
	private ThreadPoolExecutor svc;
	private ScheduledExecutorService scheduledSvc;
	private ThreadPoolExecutor recorderSvc;
	
	public OurExecutor(ScheduledExecutorService scheduledSvc, ExecutorService svc, ExecutorService recorderSvc) {
		this.scheduledSvc = scheduledSvc;
		this.svc = (ThreadPoolExecutor) svc;
		this.recorderSvc = (ThreadPoolExecutor)recorderSvc;
	}
	
	public void execute(Runnable runnable) {
		log.info("runnable: "+runnable);
		try {
			BlockingQueue<Runnable> queue = svc.getQueue();
			int size = queue.size();
			log.log(Level.FINE, "executor queue current size="+size+". Attempting to add task="+runnable);
			if(size > 1000) {
				// log.log(Level.WARNING, "Dropping task as queue is too full right now size="+size+" task="+runnable, new RuntimeException("queue too full, system backing up"));
				log.log(Level.WARNING, "Dropping task as queue is too full right now size="+size+" task="+runnable);
				return;
			}
			svc.execute(runnable);
		} catch(Exception e) {
			log.log(Level.WARNING, "Exception moving task into thread pool", e);
		}
	}

	public ScheduledExecutorService getScheduledSvc() {
		return scheduledSvc;
	}

	public ThreadPoolExecutor getRecorderSvc() {
		return recorderSvc;
	}

	public int getQueueCount() {
		BlockingQueue<Runnable> queue = svc.getQueue();
		return queue.size();
	}

	public int getActiveCount() {
		return svc.getActiveCount();
	}
	
}
