/* *********************************************************************** *
 * project: org.matsim.*
 * EventByMonthSplitter.java                                                                        *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
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
package playground.southafrica.projects.digicore.automation;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.Counter;

import playground.southafrica.projects.digicore.DigicoreUtils;
import playground.southafrica.utilities.FileUtils;
import playground.southafrica.utilities.Header;

public class EventByMonthSplitter {
	final private static Logger LOG = Logger.getLogger(EventByMonthSplitter.class);

	public static void main(String[] args) {
		Header.printHeader(EventByMonthSplitter.class.toString(), args);
		
		String filename = args[0];
		String folder = args[1];
		processEventsFile(filename, folder);
		
		Header.printFooter();
	}
	
	private EventByMonthSplitter() {
		/* Hide the constructor. */
	}
	
	public static void processEventsFile(String filename, String folder){
		LOG.info("Processing the events file " + filename);
		
		List<String> filenames = new ArrayList<String>();
		List<String> dateList = new ArrayList<String>();
		String thisFilename = null;
		String previousFilename = null;
		
		BufferedReader br = IOUtils.getBufferedReader(filename);
		BufferedWriter bw = null;
		
		Counter counter = new Counter("   lines # ");
		try{
			String line = null;
			while((line=br.readLine()) != null){
				String[] sa1 = line.split(",");
				long date1996 = Long.parseLong(sa1[1]);
				String date = DigicoreUtils.getDateSince1996(date1996);
				String[] sa2 = date.split(" ");
				String[] sa3 = sa2[0].split("/");
				String yearMonth = sa3[0] + sa3[1];
				String yearMonthDay = sa3[0] + sa3[1] + sa3[2];

				thisFilename = "events_" + yearMonth + ".csv";
				if(!filenames.contains(thisFilename)){
					filenames.add(thisFilename);
				}

				if(!dateList.contains(yearMonthDay)){
					LOG.info("---------------------> new date: " + yearMonthDay);
					dateList.add(yearMonthDay);
				}

				if(!thisFilename.equalsIgnoreCase(previousFilename)) {
					if(bw != null){
						bw.close();
					}
					bw = IOUtils.getAppendingBufferedWriter(folder + (folder.endsWith("/") ? "" : "/") + thisFilename);
				}

				bw.write(line);
				bw.newLine();
				previousFilename = thisFilename;

				counter.incCounter();
			}
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException("Cannot read/write events splitting.");
		} finally{
			try {
				br.close();
			} catch (IOException e) {
				e.printStackTrace();
				throw new RuntimeException("Cannot close reader/writer.");
			}
		}
		
		LOG.info("Done processing events file.");
		
		/* Compress the monthly events files. */
		LOG.info("Compressing each monthly events file...");
		for(String s : filenames){
			String fullname = folder + (folder.endsWith("/") ? "" : "/") + s;
			BufferedReader br2 = IOUtils.getBufferedReader(fullname);
			BufferedWriter bw2 = IOUtils.getBufferedWriter(fullname + ".gz");
			try{
				String line = null;
				while((line = br2.readLine()) != null){
					bw2.write(line);
					bw2.newLine();
				}
			} catch (IOException e) {
				e.printStackTrace();
				throw new RuntimeException("Cannot read/write from compressors.");
			} finally{
				try {
					br2.close();
					bw2.close();
				} catch (IOException e) {
					e.printStackTrace();
					throw new RuntimeException("Cannot close compressing reader/writer.");
				}
			}
			
			/* Clean up. */
			FileUtils.delete(new File(fullname));
		}
		LOG.info("Done compressing monthly events files.");
	}

}
