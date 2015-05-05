/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,     *
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
package playground.southafrica.projects.digicore;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;

import org.apache.log4j.Logger;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.Counter;

import playground.southafrica.utilities.Header;

public class SingleDayExtractor {
	final private static Logger LOG = Logger.getLogger(SingleDayExtractor.class);

	public static void main(String[] args) {
		Header.printHeader(SingleDayExtractor.class.toString(), args);
		
		String file = args[0];
		String day = args[1];
		String dayFile = args[2];
		
		LOG.info("Extracting data for " + day);
		Counter counter = new Counter("   lines # ");
		int total = 0;
		
		BufferedReader br = IOUtils.getBufferedReader(file);
		BufferedWriter bw = IOUtils.getBufferedWriter(dayFile);
		try{
			String line = null;
			while((line = br.readLine()) != null){
				String[] sa1 = line.split(",");
				long date1996 = Long.parseLong(sa1[2]);
				String date = DigicoreUtils.getDateSince1996(date1996);
				String[] sa2 = date.split(" ");
				String[] sa3 = sa2[0].split("/");
				String yearMonthDay = sa3[0] + sa3[1] + sa3[2];
				if(yearMonthDay.equalsIgnoreCase(day)){
					bw.write(line);
					bw.newLine();
					total++;
				}
				counter.incCounter();
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally{
			try {
				br.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
			try {
				bw.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		counter.printCounter();
		LOG.info("Total number of records for " + day + ": " + total);
		Header.printFooter(); 
	}

}
