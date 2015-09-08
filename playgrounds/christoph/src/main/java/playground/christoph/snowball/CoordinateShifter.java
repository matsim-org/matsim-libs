/* *********************************************************************** *
 * project: org.matsim.*
 * CoordinateShifter.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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

package playground.christoph.snowball;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.apache.log4j.Logger;
import org.geotools.referencing.GeodeticCalculator;
import org.matsim.api.core.v01.Coord;
import org.matsim.core.utils.geometry.transformations.CH1903LV03toWGS84;
import org.matsim.core.utils.geometry.transformations.WGS84toCH1903LV03;
import org.matsim.core.utils.io.IOUtils;

public class CoordinateShifter {

	private static final Logger log = Logger.getLogger(CoordinateShifter.class);
	
	private static String inFile = "../../matsim/mysimulations/Datenverfremdung/001_Data_Ego.csv";
	private static String outFile = "../../matsim/mysimulations/Datenverfremdung/001_Data_Ego_shifted.csv";
	private static int latColumn = 54;
	private static int lonColumn = 55;

//	private static String inFile = "../../matsim/mysimulations/Datenverfremdung/002_Data_Ego_Ausbildung.csv";
//	private static String outFile = "../../matsim/mysimulations/Datenverfremdung/002_Data_Ego_Ausbildung_shifted.csv";
//	private static int latColumn = 1;
//	private static int lonColumn = 2;
	
//	private static String inFile = "../../matsim/mysimulations/Datenverfremdung/003_Data_Ego_Erstwohn.csv";
//	private static String outFile = "../../matsim/mysimulations/Datenverfremdung/003_Data_Ego_Erstwohn_shifted.csv";
//	private static int latColumn = 1;
//	private static int lonColumn = 2;
	
//	private static String inFile = "../../matsim/mysimulations/Datenverfremdung/010_Data_Alter.csv";
//	private static String outFile = "../../matsim/mysimulations/Datenverfremdung/010_Data_Alter_A_shifted.csv";
//	private static int latColumn = 20;
//	private static int lonColumn = 21;
	
//	private static String inFile = "../../matsim/mysimulations/Datenverfremdung/010_Data_Alter.csv";
//	private static String outFile = "../../matsim/mysimulations/Datenverfremdung/010_Data_Alter_A_1M_shifted.csv";
//	private static int latColumn = 23;
//	private static int lonColumn = 24;
	
	private static String delimiter = ",";
	private static boolean hasHeader = true;
	
	private static double maxShift = 50.0; // shifted +/-, therefore the range is 2x maxShift!
	
	public static void main(String[] args) throws Exception {
		
		Random random = new Random(47110815);
		
		GeodeticCalculator geodeticCalculator = new GeodeticCalculator();
		
		List<String> lines = parseFile();
		
		BufferedWriter writer = IOUtils.getBufferedWriter(outFile); 
		
		if (hasHeader) {
			String line = lines.remove(0);
			
			writer.write(line);
			writer.write(delimiter);
			writer.write("\"lat_random_shifted\"");
			writer.write(delimiter);
			writer.write("\"lon_random_shifted\"");
			writer.write(delimiter);
			writer.write("\"distance_random_shifted\"");
			writer.write(delimiter);
			writer.write("\"lat_hectare_shifted\"");
			writer.write(delimiter);
			writer.write("\"lon_hectare_shifted\"");
			writer.write(delimiter);
			writer.write("\"distance_hectare_shifted\"");
			writer.newLine();
		}
		
		for (String line : lines) {
			String[] columns = line.split(delimiter);

			try {
				double lat = Double.valueOf(columns[latColumn]);
				double lon = Double.valueOf(columns[lonColumn]);
				
				double dx = calcDX(lat, lon);
				double dy = calcDY(lat, lon);
				
				double dlat = 0.0;
				double dlon = 0.0;

				// shift by dx/2 .. dx: dx * (1 .. 2) / 2 = dx * (0.5 .. 1)
				dlon = dx * (1 + random.nextDouble()) / 2;
				if (!random.nextBoolean()) dlon = - dlon;

				// shift by dy/2 .. dy: dy * (1 .. 2) / 2 = dy * (0.5 .. 1)
				dlat = dy * (1 + random.nextDouble()) / 2;
				if (!random.nextBoolean()) dlat = - dlat;

				geodeticCalculator.setStartingGeographicPoint(lon, lat);
				geodeticCalculator.setDestinationGeographicPoint(lon + dlon, lat + dlat);
				
				if (geodeticCalculator.getOrthodromicDistance() < (maxShift/2) * Math.sqrt(2)) {
					throw new RuntimeException("Shifted distance was shorter than expected: " + geodeticCalculator.getOrthodromicDistance());
				}
				
				writer.write(line);
				writer.write(delimiter);
				writer.write(String.valueOf(lat + dlat));
				writer.write(delimiter);
				writer.write(String.valueOf(lon + dlon));
				writer.write(delimiter);
				writer.write(String.valueOf(geodeticCalculator.getOrthodromicDistance()));

				// round coordinate to hectare level
				Coord swissCoord = new WGS84toCH1903LV03().transform(new Coord(lon, lat));
				swissCoord.setX(Math.round(swissCoord.getX()/100.0)*100);
				swissCoord.setY(Math.round(swissCoord.getY()/100.0)*100);
				
				/*
				 * Converting the shifted coordinates back could lead to major error for location
				 * far away from Switzerland. As a result, GeodeticCalculator could crash.
				 */
				try {
					Coord wgs84Coord = new CH1903LV03toWGS84().transform(swissCoord);
					geodeticCalculator.setDestinationGeographicPoint(wgs84Coord.getX(), wgs84Coord.getY());
					
					writer.write(delimiter);
					writer.write(String.valueOf(wgs84Coord.getY()));
					writer.write(delimiter);
					writer.write(String.valueOf(wgs84Coord.getX()));
					writer.write(delimiter);
					writer.write(String.valueOf(geodeticCalculator.getOrthodromicDistance()));
				} catch (IllegalArgumentException iae) {
					log.info("\t" + "lat:" + "\t" + columns[latColumn] + "\t" + "lon:" +"\t" + columns[lonColumn]);
					log.warn(iae.getMessage());
					writer.write(delimiter);
					writer.write("NA");
					writer.write(delimiter);
					writer.write("NA");
					writer.write(delimiter);
					writer.write("NA");
				}
				
				writer.newLine();
				
				log.info("\t" + "lat:" + "\t" + lat + "\t" + "lon:" +"\t" + lon + 
						"\t" + "shifted lat:" + "\t" + (lat + dlat) + "\t" + "shifted lon:" +"\t" + (lon + dlon));
			
			} catch (NumberFormatException nfe) {
				log.info("\t" + "lat:" + "\t" + columns[latColumn] + "\t" + "lon:" +"\t" + columns[lonColumn]);
				
				writer.write(line);
				writer.write(delimiter);
				writer.write(columns[latColumn]);
				writer.write(delimiter);
				writer.write(columns[lonColumn]);
				writer.write(delimiter);
				writer.write(columns[latColumn]);
				writer.write(delimiter);
				writer.write(columns[lonColumn]);
				writer.write(delimiter);
				writer.write(columns[latColumn]);
				writer.write(delimiter);
				writer.write(columns[lonColumn]);
				writer.newLine();
			}
		}
		
		writer.flush();
		writer.close();
	}
	
	private static double calcDX(double lat, double lon) {
		
		double dx = 1.0;
		double distance = Double.MAX_VALUE;
		
		while (true) {
			
			double lastDx = dx;
			double lastDistance = distance;
			
			dx = dx * 0.99;	// decrease by 1%
			GeodeticCalculator geodeticCalculator = new GeodeticCalculator();
			geodeticCalculator.setStartingGeographicPoint(lon, lat);
			geodeticCalculator.setDestinationGeographicPoint(lon, lat + dx);
			
			distance = geodeticCalculator.getOrthodromicDistance();
			
			if (distance < maxShift) {
				dx = lastDx;
				distance = lastDistance;
				break;
			}
		}
//		log.info("dx: " + dx);
//		log.info("distance: " + distance);
		
		return dx;
	}
	
	private static double calcDY(double lat, double lon) {
		
		double dy = 1.0;
		double distance = Double.MAX_VALUE;
		
		while (true) {
			
			double lastDy = dy;
			double lastDistance = distance;
			
			dy = dy * 0.99;	// decrease by 1%
			GeodeticCalculator geodeticCalculator = new GeodeticCalculator();
			geodeticCalculator.setStartingGeographicPoint(lon, lat);
			geodeticCalculator.setDestinationGeographicPoint(lon + dy, lat);
			
			distance = geodeticCalculator.getOrthodromicDistance();
			
			if (distance < maxShift) {
				dy = lastDy;
				distance = lastDistance;
				break;
			}
		}
//		log.info("dy: " + dy);
//		log.info("distance: " + distance);
		
		return dy;
	}
	
	private static List<String> parseFile() throws Exception {
		
		List<String> lines = new ArrayList<String>();
		
		BufferedReader reader = IOUtils.getBufferedReader(inFile);
		
		String line;
		while ((line = reader.readLine()) != null) lines.add(line);
		
		return lines;
	}
}
