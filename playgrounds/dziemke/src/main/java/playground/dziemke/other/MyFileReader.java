/* *********************************************************************** *
 * project: org.matsim.*
 * UCSBStopsParser.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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

package playground.dziemke.other;

import java.io.BufferedReader;
import java.io.IOException;

import org.apache.log4j.Logger;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.utils.io.IOUtils;

/**
 * @author dziemke
 * ... parsing and checking something
 */
public class MyFileReader {
	
	private final static Logger log = Logger.getLogger(MyFileReader.class);
	
	public static void main(String[] args) throws IOException {
		String inputFile = "/Users/dominik/Workspace/shared-svn/projects/feathers/feathers0/prdToAscii.csv";
	
		int lineCount = 0;
		
		try {
			BufferedReader bufferedReader = IOUtils.getBufferedReader(inputFile);

			// header
			String currentLine = bufferedReader.readLine();
			lineCount++;
//			String[] heads = currentLine.split(";", -1);
//			Map<String,Integer> columnNumbers = new LinkedHashMap<String,Integer>(heads.length);
//			for (int i = 0; i < heads.length; i++) {
//				columnNumbers.put(heads[i],i);
//				//System.out.println("Column No.=" + i + " has an entry with title=" + heads[i]);
//			}
			
			// read data and write relevant information to a new Trip object
			while ((currentLine = bufferedReader.readLine()) != null) {
				//String[] entries = curr_line.split("\t", -1);
				String[] entries = currentLine.split(";", -1);
				lineCount++;
		
				// test file (it.150 of run.104) has 111229 lines altogether
				// line count is correct since starting with "0" take in account the fact that
				// the first line is the header, which may not be counted
				
				if (lineCount % 100000 == 0) {
					log.info(lineCount+ " lines read in so far.");
					Gbl.printMemoryUsage();
				}
				
//				System.out.println(entries[13]);
				
				if ( ! entries[13].equals("1")) {
					System.out.println("Not 1!");
				}
			}
		} catch (IOException e) {
			log.error(new Exception(e));
		}
	}
	
	
}
