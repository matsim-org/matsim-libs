/* *********************************************************************** *
 * project: org.matsim.*
 * Adress2Coord.java
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
package playground.johannes.socialnetworks.survey.ivt2009;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;

/**
 * @author illenberger
 *
 */
public class Address2Coord {
	
	private static final Logger logger = Logger.getLogger(Address2Coord.class);

	private static final String TAB = "\t";
	
	private static final String ADDRESSROW = "adresszeile";
	/**
	 * @param args
	 * @throws InterruptedException 
	 */
	public static void main(String[] args) throws IOException, InterruptedException {
		
		BufferedReader reader = new BufferedReader(new FileReader(args[0]));
		BufferedWriter writer = new BufferedWriter(new FileWriter(args[1]));
		String line;
		/*
		 * read header and get address columns
		 */
		line = reader.readLine();
		String tokens[] = line.split(TAB);
		int addressIdx1 = getIndex(ADDRESSROW + "3", tokens);
		int addressIdx2 = getIndex(ADDRESSROW + "4", tokens);
		int addressIdx3 = getIndex(ADDRESSROW + "5", tokens);
		/*
		 * check if all columns are found
		 */
		if(addressIdx1 < 0 || addressIdx2 < 0 || addressIdx3 < 0)
			throw new IllegalArgumentException("A header column has not been found!");
		/*
		 * add two new columns
		 */
		String[] header = new String[tokens.length + 2];
		System.arraycopy(tokens, 0, header, 0, tokens.length);
		header[tokens.length] = "long";
		header[tokens.length + 1] = "lat";
		/*
		 * write header
		 */
		for(int i = 0; i < header.length - 1; i++) {
			writer.write(header[i]);
			writer.write(TAB);
		}
		writer.write(header[header.length - 1]);
		writer.write(TAB);
		writer.newLine();
		/*
		 * read in one line, get coordinates and write out
		 */
		logger.info("Starting address lookup...");
		int counter = 0;
		int notfound = 0;
		GoogleLocationLookup lookup = new GoogleLocationLookup();
		int lookupInterval = 100;
		while((line = reader.readLine()) != null) {
			Thread.currentThread().sleep(lookupInterval);
			writer.write(line);
			
			tokens = line.split(TAB);
			
			String addressRow1 = tokens[addressIdx1];
			String addressRow2 = tokens[addressIdx2];
			String addressRow3 = tokens[addressIdx3];
			
			if(!addressRow1.equalsIgnoreCase("NA") ||
					!addressRow2.equalsIgnoreCase("NA") ||
					!addressRow3.equalsIgnoreCase("NA")) {
			StringBuilder builder = new StringBuilder();
			builder.append(addressRow1);
			builder.append(", ");
			builder.append(addressRow2);
			builder.append(", ");
			builder.append(addressRow3);
			
			Coord coord = lookup.locationToCoord(builder.toString());
			if(lookup.getLastErrorCode() == 620) {
				while(lookup.getLastErrorCode() == 620) {
					if(lookupInterval > 5000) {
						logger.warn("Lookup interval is 5 secs. Seems we reached the request limit!");
						System.exit(-1);
					}
					logger.warn(String.format("Increasing lookup interval, now %1$s msecs.", lookupInterval));
					lookupInterval += 100;
					coord = lookup.locationToCoord(builder.toString());
				}
			}
			if(coord != null) {
				writer.write(TAB);
				writer.write(String.valueOf(coord.getX()));
				writer.write(TAB);
				writer.write(String.valueOf(coord.getY()));
			} else {
				notfound++;
			}
			}
			writer.newLine();
			
			counter++;
			if(counter % 10 == 0)
				logger.info(String.format("Processed %1$s addresses...", counter));
		}
		writer.close();
		logger.info(String.format("Address lookup done. Processed %1$s addresses, %2$s addresses unkown.", counter, notfound));
	}

	private static int getIndex(String header, String[] tokens) {
		for(int i = 0; i < tokens.length; i++) {
			if(tokens[i].equalsIgnoreCase(header))
				return i;
		}
		
		return -1;
	}
}
