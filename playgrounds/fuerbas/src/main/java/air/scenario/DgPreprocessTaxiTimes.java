/* *********************************************************************** *
 * project: org.matsim.*
 * DgPreprocessTaxiTimes
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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
package air.scenario;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;

import org.apache.log4j.Logger;
import org.matsim.core.utils.io.IOUtils;


/**
 * @author dgrether
 *
 */
public class DgPreprocessTaxiTimes {
	
	private static final Logger log = Logger.getLogger(DgPreprocessTaxiTimes.class);
	
	private static void convertFile(String inputFile, String outputFile) throws IOException{
		BufferedReader reader = IOUtils.getBufferedReader(inputFile);
		BufferedWriter writer = IOUtils.getBufferedWriter(outputFile);
		String line = reader.readLine();
		int lineNumber = 1;
		int noColumns = 7;
		while (line != null){
			if (lineNumber <= 3) {
				writer.write(line);
				writer.newLine();
				lineNumber++;
			}
			else {
				String[] s = line.split(" ");
				writer.write(s[0]);
				writer.write("\t");
				writer.write(s[1]);
				writer.write("\t");
				int airportNameColumns = s.length - noColumns;
				String airportName = s[2];
				for (int i =  1; i < airportNameColumns; i++) {
					airportName = airportName.concat(" ");
					airportName = airportName.concat(s[2 + i]);
				}
				log.debug("Airport Name : " + airportName);
				writer.write(airportName);
				writer.write("\t");
				
				for (int i = (2 + airportNameColumns); i < s.length; i++){
					writer.write(s[i]);
					writer.write("\t");
				}
				writer.newLine();
			}
			line = reader.readLine();
		}
		reader.close();
		writer.close();
	}
	
	
	public static void main(String[] args) throws IOException {
		String directory = "/media/data/work/repos/shared-svn/studies/sfuerbas/CODA/";
		String fileName = directory + "coda_taxi_in_summer2010";
		convertFile(fileName + ".txt", fileName + "_tabseparated.txt");
		fileName = directory + "coda_taxi_out_summer2010";
		convertFile(fileName + ".txt", fileName + "_tabseparated.txt");
		log.info("done");
	}

}
