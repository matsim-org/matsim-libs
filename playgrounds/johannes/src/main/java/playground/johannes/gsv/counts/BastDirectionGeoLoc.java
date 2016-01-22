/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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

package playground.johannes.gsv.counts;

import com.google.code.geocoder.model.LatLng;
import org.apache.log4j.Logger;
import playground.johannes.studies.sbsurvey.util.GoogleGeoCoder;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

/**
 * @author johannes
 * 
 */
public class BastDirectionGeoLoc {

	private static final Logger logger = Logger.getLogger(BastDirectionGeoLoc.class);

	private static GoogleGeoCoder googleLookup;

	/**
	 * @param args
	 * @throws IOException
	 */
	public static void main(String[] args) throws IOException {
		String bastFile = "/home/johannes/gsv/matrix2014/counts/counts2014.csv";
		String outFile = "/home/johannes/gsv/matrix2014/counts/counts2014-directions.csv";

		String separator = ";";
		String destinationLongPrefix = "fernziel_ri";
		String destinationShortPrefix = "nahziel_ri";

		googleLookup = new GoogleGeoCoder("localhost", 3128, 100);

		logger.info("Loading bast counts...");
		BufferedReader reader = new BufferedReader(new FileReader(bastFile));
		BufferedWriter writer = new BufferedWriter(new FileWriter(outFile));

		String line = reader.readLine();
		writer.write(line);
		writer.write(separator + "Fernziel_Ri1_long");
		writer.write(separator + "Fernziel_Ri1_lat");
		writer.write(separator + "Fernziel_Ri2_long");
		writer.write(separator + "Fernziel_Ri2_lat");
		writer.newLine();

		String[] header = line.split(";");
		Map<String, Integer> colIdices = new HashMap<String, Integer>();
		for (int i = 0; i < header.length; i++) {
			colIdices.put(header[i].toLowerCase(), i);
		}

		int dest1Idx = colIdices.get(destinationLongPrefix + "1");
		int dest2Idx = colIdices.get(destinationLongPrefix + "2");

		int dest1LocalIdx = colIdices.get(destinationShortPrefix + "1");
		int dest2LocalIdx = colIdices.get(destinationShortPrefix + "2");

		int cnt = 0;
		while ((line = reader.readLine()) != null) {
			String tokens[] = line.split(separator);

			if (tokens.length > dest2Idx) {
				double[] coord1 = coordinates(tokens[dest1Idx]);
				if(coord1 == null && tokens.length > dest1LocalIdx) {
					coord1 = coordinates(tokens[dest1LocalIdx]);
				}
				double[] coord2 = coordinates(tokens[dest2Idx]);
				if(coord2 == null && tokens.length > dest2LocalIdx) {
					coord2 = coordinates(tokens[dest2LocalIdx]);
				}
				
				writer.write(line);
				writer.write(separator);
				if (coord1 != null) {
					writer.write(String.valueOf(coord1[0]));
				}
				writer.write(separator);
				if (coord1 != null) {
					writer.write(String.valueOf(coord1[1]));
				}

				writer.write(separator);
				if (coord2 != null) {
					writer.write(String.valueOf(coord2[0]));
				}
				writer.write(separator);
				if (coord2 != null) {
					writer.write(String.valueOf(coord2[1]));
				}
				writer.newLine();
				writer.flush();
			}
			cnt++;
			if (cnt % 20 == 0) {
				logger.info(String.format("Parsed %s lines...", cnt));
			}
		}

		reader.close();
		writer.close();

		logger.info("Done.");
	}

	private static double[] coordinates(String destination) {
		LatLng coord = googleLookup.requestCoordinate(destination);

		if (coord != null) {
			return new double[] { coord.getLng().doubleValue(), coord.getLat().doubleValue() };
		} else {
			return null;
		}
	}

}
