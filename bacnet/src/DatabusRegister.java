import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;
import java.util.Vector;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.PosixParser;

import gov.nrel.bacnet.Scan.DeviceFilter;


public class DatabusRegister {
	
	public static void main(String[] args) throws Exception {
		CommandLineParser parser = new PosixParser();
		Options options = new Options();
		options.addOption("f", "filter-file", true, "JSON filter file to use during scanning");
		options.addOption("u", "databus-url", true, "Databus URL to register toward");
		options.addOption("k", "register-key", true, "Register key for user from Databus");
		options.addOption("p", "table-prefix", true, "System prefix for tables registered");
	

		CommandLine line = parser.parse(options, args);
		String filterfile = readFile(line.getOptionValue("f"), Charset.forName("US-ASCII"));
		String databusUrl = readFile(line.getOptionValue("u","databus.nrel.gov"), Charset.forName("US-ASCII"));
		String registerKey = readFile(line.getOptionValue("k","nokey"), Charset.forName("US-ASCII"));
		String tablePrefix = readFile(line.getOptionValue("p","bacnet"), Charset.forName("US-ASCII"));
		com.google.gson.Gson gson = new com.google.gson.Gson();

		java.lang.reflect.Type vectortype 
		= new com.google.gson.reflect.TypeToken<Vector<DeviceFilter>>() {}.getType();
		Vector<DeviceFilter> dFilters = gson.fromJson(filterfile, vectortype);
		
		DatabusSender sender = new DatabusSender(databusUrl);
		
		for(DeviceFilter dFilter : dFilters) {
			Vector<OIDFilter> oFilters = dFilter.oidFilters;
			for(OIDFilter oFilter : oFilters) {
				if(oFilter.apiKey == null) {
					oFilter.apiKey = sender.registerStream(tablePrefix + "-"
							+ dFilter.instanceNumber + "-"
							+ oFilter.instanceNumber, registerKey);
					System.out.println(oFilter.instanceNumber + ": " + oFilter.apiKey);
				}
			}
		}
		
		new FileWriter(filterfile + ".registered").write(gson.toJson(dFilters));	
	}

	public static String readFile(String file, Charset cs)
			throws IOException {
		// No real need to close the BufferedReader/InputStreamReader
		// as they're only wrapping the stream
		FileInputStream stream = new FileInputStream(file);
		try {
			Reader reader = new BufferedReader(new InputStreamReader(stream, cs));
			StringBuilder builder = new StringBuilder();
			char[] buffer = new char[8192];
			int read;
			while ((read = reader.read(buffer, 0, buffer.length)) > 0) {
				builder.append(buffer, 0, read);
			}
			return builder.toString();
		} finally {
			// Potential issue here: if this throws an IOException,
			// it will mask any others. Normally I'd use a utility
			// method which would log exceptions and swallow them
			stream.close();
		}        
	}
	

}
