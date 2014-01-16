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

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

class DataPointWriter {

	private static final Logger log = Logger.getLogger(DataPointWriter.class.getName());
	
	private DatabusSender sender;
	private FileWriter jsonw;
	private int counter = 0;
	
	public DataPointWriter(DatabusSender sender) {
		this.sender = sender;
	}
	
	public DatabusSender getSender() {
		return sender;
	}
	
	public synchronized void addDataPoint(String json) {
		try {
			if(jsonw == null || counter > 5000) {
				newFile();
			}
			jsonw.write(json+"\n");
			counter++;
		} catch (Exception e) {
			log.log(Level.WARNING, "Error opening output file", e);
		}
	}

	private void newFile() throws IOException {
		if(jsonw != null) {
			jsonw.close();
		}
		
		SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd-hh-mm-ss.SSS");
		String time = fmt.format(new Date());
		
		File f = new File("logs/dataPoints"+time+".json");
		f.createNewFile();
		jsonw = new FileWriter(f, true);
		counter = 0;
	}
}
