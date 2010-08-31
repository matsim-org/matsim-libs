/* *********************************************************************** *
 * project: org.matsim.*
 * GeoCoder.java
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
package playground.johannes.socialnetworks.survey.mz2005;

import geo.google.datamodel.GeoCoordinate;
import gnu.trove.TObjectIntHashMap;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.core.utils.geometry.CoordImpl;

import playground.johannes.socialnetworks.survey.ivt2009.util.GoogleGeoCoder;
import playground.johannes.socialnetworks.survey.ivt2009.util.GoogleLocationLookup.RequestLimitException;

/**
 * @author illenberger
 *
 */
public class GeoCoder {

	private static final Logger logger = Logger.getLogger(GeoCoder.class);
	
//	private static final GoogleLocationLookup googleLookup = new GoogleLocationLookup();
	
	private static final GoogleGeoCoder googleLookup = new GoogleGeoCoder();
	
	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		BufferedReader reader = new BufferedReader(new FileReader(args[0]));
		/*
		 * read header
		 */
		String header = reader.readLine();
		TObjectIntHashMap<String> colIndices = new TObjectIntHashMap<String>();
		int idx = 0;
		for(String token : header.split("\t")) {
			colIndices.put(token, idx);
			idx++;
		}
		/*
		 * create new header
		 */
		BufferedWriter writer = new BufferedWriter(new FileWriter(args[1]));
		
		StringBuilder builder = new StringBuilder(header.length() + 50);
		builder.append(header);
		builder.append("\t");
		builder.append("x_start");
		builder.append("\t");
		builder.append("y_start");
		builder.append("\t");
		builder.append("x_dest");
		builder.append("\t");
		builder.append("y_dest");
		
		writer.write(builder.toString());
		writer.newLine();
		/*
		 * parse file
		 */
		logger.info("Starting geo coding...");
		int lineCount = 0;
		int invalid = 0;
		String line;
		while((line = reader.readLine()) != null) {
			String[] tokens = line.split("\t");
			/*
			 * get coordinates
			 */
			try {
			double[] start = coordinates(tokens, colIndices, "S");
			double[] dest = coordinates(tokens, colIndices, "Z");
			
			/*
			 * write new line
			 */
			builder = new StringBuilder(line.length() + 50);
			builder.append(line);
			builder.append("\t");

			if(start != null) {
				builder.append(String.valueOf(start[0]));
				builder.append("\t");
				builder.append(String.valueOf(start[1]));
				builder.append("\t");

			} else {
				builder.append("\t");
				builder.append("\t");
			}
			
			
			if(dest != null) {
				builder.append(String.valueOf(dest[0]));
				builder.append("\t");
				builder.append(String.valueOf(dest[1]));
			} else {
				builder.append("\t");
			}
			
			writer.write(builder.toString());
			writer.newLine();
			writer.flush();
			
			lineCount++;
			if(start == null || dest == null)
				invalid++;
			
			if(lineCount % 20 == 0)
				logger.info(String.format("Parsed %1$s lines. %2$s addresses not found.", lineCount, invalid));
			} catch (RequestLimitException e) {
				e.printStackTrace();
				
				writer.close();
				
				BufferedWriter remainingWriter = new BufferedWriter(new FileWriter(args[1] + ".remaining"));
				remainingWriter.write(header);
				remainingWriter.newLine();
				
				remainingWriter.write(line);
				remainingWriter.newLine();
				while((line = reader.readLine()) != null) {
					remainingWriter.write(line);
					remainingWriter.newLine();
				}
				logger.info("Writing remaining file done.");
				System.exit(0);
			}
		}
		writer.close();
		
		logger.info("Done.");
	}

	private static double[] coordinates(String tokens[], TObjectIntHashMap<String> colNames, String prefix) {
		StringBuilder builder = new StringBuilder();
		
		builder.append(tokens[colNames.get(prefix + "_STRA")]);
		builder.append(" ");
		builder.append(tokens[colNames.get(prefix + "_HNR")]);
		builder.append(" ");
		
		String plz = tokens[colNames.get(prefix + "_PLZ")];
		if(!plz.equalsIgnoreCase("-97")) {
			builder.append(plz);
			builder.append(" ");
		}
		
		builder.append(tokens[colNames.get(prefix + "_ORT")]);
		builder.append(" ");
		builder.append(tokens[colNames.get(prefix + "_LAND")]);
		builder.append(" ");
		
		String query = builder.toString().trim();
		Coord c = null;
		
		if(query.isEmpty())
			c = null;
		else {
			GeoCoordinate coord = googleLookup.requestCoordinate(builder.toString());
			if(coord != null)
				c = new CoordImpl(coord.getLongitude(), coord.getLatitude());
		}
		
		if(c == null) {
			logger.warn(String.format("No results for query \"%1$s\".", query));
			return null;
		} else {
			return new double[]{c.getX(), c.getY()};
		}
	}
}
