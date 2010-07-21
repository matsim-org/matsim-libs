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

import gnu.trove.TObjectIntHashMap;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import org.matsim.api.core.v01.Coord;

import playground.johannes.socialnetworks.survey.ivt2009.util.GoogleLocationLookup;

/**
 * @author illenberger
 *
 */
public class GeoCoder {

	private static GoogleLocationLookup googleLookup = new GoogleLocationLookup();
	
	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		BufferedReader reader = new BufferedReader(new FileReader(args[0]));
		/*
		 * read header
		 */
		String line = reader.readLine();
		TObjectIntHashMap<String> colIndices = new TObjectIntHashMap<String>();
		int idx = 0;
		for(String token : line.split("\t")) {
			colIndices.put(token, idx);
			idx++;
		}
		/*
		 * create new header
		 */
		BufferedWriter writer = new BufferedWriter(new FileWriter(args[1]));
		
		StringBuilder builder = new StringBuilder(line.length() + 50);
		builder.append(line);
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
		while((line = reader.readLine()) != null) {
			String[] tokens = line.split("\t");
			/*
			 * get coordinates
			 */
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
				for(int i = 0; i < 4; i++)
					builder.append("\t");
			}
			
			
			if(dest != null) {
				builder.append(String.valueOf(dest[0]));
				builder.append("\t");
				builder.append(String.valueOf(dest[1]));
			} else {
				for(int i = 0; i < 3; i++)
					builder.append("\t");
			}
			
			writer.write(builder.toString());
			writer.newLine();
		}
		writer.close();
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
		
		Coord c = googleLookup.requestCoordinates(builder.toString());
		if(c == null)
			return null;
		
		return new double[]{c.getX(), c.getY()};
	}
}
