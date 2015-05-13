/* *********************************************************************** *
 * project: org.matsim.*
 * FilterHistoricSpeedRecords.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

/**
 * 
 */
package playground.southafrica.projects.digicore;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;

import org.apache.log4j.Logger;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.Counter;

import playground.southafrica.utilities.Header;

/**
 * Class to extract only those records that do have historic road speed values.
 * 
 * @author jwjoubert
 */
public class FilterHistoricSpeedRecords {
	final private static Logger LOG = Logger.getLogger(FilterHistoricSpeedRecords.class);

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Header.printHeader(FilterHistoricSpeedRecords.class.toString(), args);
		String input = args[0];
		String output = args[1];
		
		int present = 0;
		int missing = 0;
		BufferedReader br = IOUtils.getBufferedReader(input);
		BufferedWriter bw = IOUtils.getBufferedWriter(output);
		Counter counter = new Counter("   line # ");
		try{
			String line = null;
			while((line=br.readLine()) != null){
				String[] sa = line.split(",");
				double historic = Double.parseDouble(sa[10]);
				if(historic > 0){
					present++;
				} else{
					missing++;
				}
				counter.incCounter();
			}
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException("Could not read from " + input + 
					" or write to " + output);
		} finally{
			try {
				br.close();
				bw.close();
			} catch (IOException e) {
				e.printStackTrace();
				throw new RuntimeException("Could not close " + input + " or " + output);
			}
		}
		counter.printCounter();
		double percentage = ((double) present) / ((double)present + (double)missing) * 100.0;
		LOG.info("Present: " + present + " (" + String.format("%.2f%%)", percentage));
		LOG.info("Missing: " + missing);
		Header.printFooter();
	}

}
