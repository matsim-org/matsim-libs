/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,     *
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

package playground.jjoubert.DigiCore;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.io.IOUtils;

import com.vividsolutions.jts.geom.Coordinate;

import playground.southafrica.utilities.Header;

public class VehicleFilesToSpaceTimeData {
	private final static Logger LOG = Logger.getLogger(VehicleFilesToSpaceTimeData.class);
	private final static Double DISTANCE_FACTOR = 1.35;
	private final static String[] files = {
		"100002", "100003", "100004", "100005", "100008", "100010", "100012",
		"100013", "100015", "100016", "100018", "100019", "100020", "100021",
		"100023", "100024"
		};
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Header.printHeader(VehicleFilesToSpaceTimeData.class.toString(), args);

		CoordinateTransformation ct = TransformationFactory.getCoordinateTransformation("WGS84", "WGS84_SA_Albers");
		int droppedPoints = 0;
		
		for(String s : files){
			/* Get baseline date/time */
			BufferedReader br = IOUtils.getBufferedReader(args[0] + s + ".txt.gz");
			BufferedWriter bw = IOUtils.getBufferedWriter(args[0] + s + "_SpaceTime.csv");
			int i = 1;
			Coordinate previousCoord = null;
			double cumulativeDistance = 0;
			
			/* Base date */
			Calendar base = new GregorianCalendar(TimeZone.getTimeZone("GMT+2"), new Locale("en"));
			base.set(2009, 0, 1, 0, 0, 0);
			
			try{
				String line = br.readLine();
				while((line = br.readLine()) != null){
					String[] sa = line.split(",");
					Calendar calendar = new GregorianCalendar(TimeZone.getTimeZone("GMT+2"), new Locale("en"));
					calendar.setTimeInMillis(Long.parseLong(sa[1])*1000);
					Coord coord = new Coord(Double.parseDouble(sa[2]), Double.parseDouble(sa[3]));
					Coord newCoord = ct.transform(coord);
					Coordinate newCoordinate = new Coordinate(newCoord.getX(), newCoord.getY());				
					
					if(i++ == 1){
						previousCoord = newCoordinate;
					}
					
					/* TODO Major assumption: what is an `outlier' point?
					 * Right now (Jun '13) I have very large (10000km) inter-
					 * gps point distances. This clearly reflects an outlier.
					 * I'm going to put in an arbitrary threshold to remove, or 
					 * at least ignore, GPS records where the distance between
					 * consecutive points exceed 2500km. Why 2500km? You can 
					 * transport a vehicle on an auto carrier from Cape Town to
					 * the North of South Africa.   
					 */
					double distance = newCoordinate.distance(previousCoord)*DISTANCE_FACTOR;
					if(distance < 2500000){
						cumulativeDistance += distance; 
						
						/* Write to file. */
						bw.write( String.format("%d,%.4f,%.4f,%.4f\n", calendar.getTimeInMillis()/1000, newCoord.getX(), newCoord.getY(), cumulativeDistance) );
						
						/* Update `previous coordinate' */
						previousCoord = newCoordinate;						
					}else{
						/* Basically ignore the current GPS record in the data set. */
						droppedPoints++;
					}
				}
			} catch (IOException e) {
				throw new RuntimeException("Cannot read/write");
			} finally{
				try {
					br.close();
					bw.close();
				} catch (IOException e) {
					throw new RuntimeException("Cannot close");
				}
			}
		}
		
		/* For the purpose of R, indicate the months for x-axis. */
		LOG.info("-------------------------------------");
		LOG.info(" Monthly cutoff times.");
		for(int j = 0; j < 7; j++){
			Calendar calendar = new GregorianCalendar(2009, j, 1, 0, 0, 0);
			LOG.info("   Month " + (j+1) + ": " + calendar.getTimeInMillis()/1000);
		}
		LOG.info("-------------------------------------");
		LOG.info("Dropped points: " + droppedPoints);
		LOG.info("-------------------------------------");
		
		Header.printFooter();
	}
	
	
	

}
