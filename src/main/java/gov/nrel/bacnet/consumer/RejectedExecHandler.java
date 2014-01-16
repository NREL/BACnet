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
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.logging.Level;
import java.util.logging.Logger;

class RejectedExecHandler implements RejectedExecutionHandler {

	private static final Logger log = Logger.getLogger(RejectedExecHandler.class.getName());
	
	@Override
	public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
		BlockingQueue<Runnable> queue = executor.getQueue();
		
		long time = System.currentTimeMillis();
		try {
			queue.put(r);
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
		long total = System.currentTimeMillis() - time;
		
		log.log(Level.WARNING, "Downstream not keeping up.  We waited to add to queue time="+total, new RuntimeException("time="+total));
	}

}
