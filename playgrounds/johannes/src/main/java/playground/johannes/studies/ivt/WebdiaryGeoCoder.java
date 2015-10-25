/* *********************************************************************** *
 * project: org.matsim.*
 * WebdiaryGeoCoder.java
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
package playground.johannes.studies.ivt;

import com.google.code.geocoder.model.LatLng;
import playground.johannes.studies.ivt2009.util.GoogleGeoCoder;

import java.io.*;

/**
 * @author illenberger
 *
 */
public class WebdiaryGeoCoder {

	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		GoogleGeoCoder geoCoder = new GoogleGeoCoder();
		BufferedReader reader = new BufferedReader(new FileReader("/Users/jillenberger/Work/socialnets/data/ivt2009/webdiary/webdiary_analysis_activitytype.csv"));
		BufferedWriter writer = new BufferedWriter(new FileWriter("/Users/jillenberger/Work/socialnets/data/ivt2009/webdiary/webdiary.xy.csv"));
		
		String line = reader.readLine();
		
		writer.write(line);
		writer.write(";\"lat\";\"long\"");
		writer.newLine();
		
		String[] tokens = line.split(";");
		
		int idx1 = getIndex("\"Adresse_Strasse\"", tokens);
		int idx2 = getIndex("\"Adresse_Hausnummer\"", tokens);
		int idx3 = getIndex("\"Adresse_PLZ\"", tokens);
		int idx4 = getIndex("\"Adresse_Ort\"", tokens);

		int cnt = 0;
		System.out.println("Starting parsing...");
		
		while((line = reader.readLine()) != null) {
			try {
			tokens = line.split(";");
			
			if(tokens.length > idx4) {
			StringBuilder builder = new StringBuilder(200);
			builder.append(tokens[idx1]);
			builder.append(" ");
			builder.append(tokens[idx2]);
			builder.append(" ");
			builder.append(tokens[idx3]);
			builder.append(" ");
			builder.append(tokens[idx4]);
			
			String query = builder.toString();
			query = query.replace("\"", "");
			
			LatLng coord = geoCoder.requestCoordinate(query);
			if(coord != null) {
				writer.write(line);
				writer.write(";");
				writer.write(Double.toString(coord.getLat().doubleValue()));
				writer.write(";");
				writer.write(Double.toString(coord.getLng().doubleValue()));
				writer.newLine();
			}
			
			cnt++;
			if(cnt % 100 == 0)
				System.out.println("Parsed 100 addresses...");
			} else {
				System.err.println("Invalid line...");
			}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		System.out.println("Done.");
		
		reader.close();
		writer.close();
	}

	private static int getIndex(String name, String tokens[]) {
		for(int i = 0; i < tokens.length; i++) {
			if(name.equals(tokens[i]))
				return i;
		}
		
		return -1;
	}
}
