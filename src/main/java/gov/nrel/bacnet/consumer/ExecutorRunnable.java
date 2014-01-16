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

import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;

class ExecutorRunnable implements Runnable {

	private static final Logger log = Logger.getLogger(ExecutorRunnable.class.getName());
	private Callable<Object> callable;

	public ExecutorRunnable(Callable<Object> callable) {
		this.callable = callable;
	}

	@Override
	public void run() {
		try {
			callable.call();
		} catch (Exception e) {
			log.log(Level.WARNING, "Exception", e);
		}
	}
	
}
