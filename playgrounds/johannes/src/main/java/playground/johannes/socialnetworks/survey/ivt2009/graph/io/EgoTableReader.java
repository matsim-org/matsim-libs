/* *********************************************************************** *
 * project: org.matsim.*
 * EgoTableReader.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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
package playground.johannes.socialnetworks.survey.ivt2009.graph.io;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;

import playground.johannes.socialnetworks.survey.ivt2009.util.GoogleLocationLookup;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;

/**
 * @author illenberger
 *
 */
public class EgoTableReader {
	
	private static final Logger logger = Logger.getLogger(EgoTableReader.class);
	
	private static final String TAB = "\t";
	
	private static final String ADDRESSROW = "adresszeile";
	
	private Map<String, Record> egos;
	
	private GoogleLocationLookup lookup = new GoogleLocationLookup();
	
	private GeometryFactory geoFactory = new GeometryFactory();
	
	private long lastLookup;
	
	private long interval = 100;

	private class Record {
		
		private String egoId;
		
		private String location1;
		
		private String location2;
		
		private String location3;
		
	}
	
	public EgoTableReader(List<String> files) throws IOException {
		egos = new HashMap<String, Record>();
		for(String file : files) {
			BufferedReader reader = new BufferedReader(new FileReader(file));
			String line;
			/*
			 * read header and get address columns
			 */
			line = reader.readLine();
			String tokens[] = line.split(TAB);
			int idIdx = getIndex("Laufnr.", tokens);
			int addressIdx1 = getIndex(ADDRESSROW + "3", tokens);
			int addressIdx2 = getIndex(ADDRESSROW + "4", tokens);
			int addressIdx3 = getIndex(ADDRESSROW + "5", tokens);
			/*
			 * check if all columns are found
			 */
			if(addressIdx1 < 0 || addressIdx2 < 0 || addressIdx3 < 0)
				throw new IllegalArgumentException("A header column has not been found!");
			/*
			 * read lines
			 */
			while((line = reader.readLine()) != null) {
				tokens = line.split(TAB);
				if(tokens.length > addressIdx3) {
					Record record = new Record();
					record.egoId = tokens[idIdx].substring(tokens[idIdx].lastIndexOf(" ") + 1);
					record.location1 = tokens[addressIdx1];
					record.location2 = tokens[addressIdx2];
					record.location3 = tokens[addressIdx3];

					if (egos.put(record.egoId, record) != null)
						logger.warn(String.format("Overwriting values for id %1$s.", record.egoId));
				}
			}
		}
	}
	
	private static int getIndex(String header, String[] tokens) {
		for(int i = 0; i < tokens.length; i++) {
			if(tokens[i].equalsIgnoreCase(header))
				return i;
		}
		
		return -1;
	}
	
	public Point getEgoLocation(String egoId) {
		/*
		 * get record
		 */
		Record record = egos.get(egoId);
		if(record == null)
			return null;
//		/*
//		 * check for valid addresses
//		 */
//		if (record.location1.equalsIgnoreCase("NA")
//				|| record.location2.equalsIgnoreCase("NA")
//				|| record.location3.equalsIgnoreCase("NA")) {
//			return null;
//		}
		/*
		 * build query string
		 */
		StringBuilder builder = new StringBuilder();
		if(!record.location1.equalsIgnoreCase("NA")) {
			builder.append(record.location1);
			builder.append(", ");
		}
		if(!record.location2.equalsIgnoreCase("NA")) {
			builder.append(record.location2);
			builder.append(", ");
		}
		if(!record.location3.equalsIgnoreCase("NA")) {
			builder.append(record.location3);
		}
		/*
		 * sleep if necessary
		 */
		try {
		long now = System.currentTimeMillis();
		if((now - lastLookup) < interval) {
			
				Thread.sleep((now - lastLookup));
			
		}
		/*
		 * try obtaining coordinates
		 */
		Coord c = lookup.locationToCoord(builder.toString());
		if (lookup.getLastErrorCode() == 620) {
			while (lookup.getLastErrorCode() == 620) {
				if (interval > 5000) {
					logger.warn("Lookup interval is 5 secs. Seems we reached the request limit!");
					System.exit(-1);
				}
				logger.warn(String.format("Increasing lookup interval, now %1$s msecs.", interval));
				interval += 100;
				Thread.sleep(interval);
				c = lookup.locationToCoord(builder.toString());
			}
		}
		
		if(c != null) {
//			return geoFactory.createPoint(new Coordinate(0,0));
			return geoFactory.createPoint(new Coordinate(c.getX(), c.getY()));
		} else {
			return null;
		}
		} catch (InterruptedException e) {
			e.printStackTrace();
			return null;
		}
	}
}
