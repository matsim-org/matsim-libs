/* *********************************************************************** *
 * project: org.matsim.*
 * FacilityMatcher.java                                                                        *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
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
/**
 * 
 */
package playground.nmviljoen.grain.facilities;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.Collection;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.ActivityFacility;
import org.matsim.facilities.MatsimFacilitiesReader;

import playground.southafrica.utilities.FileUtils;
import playground.southafrica.utilities.Header;

/**
 * Class to read in facilities (producers and processors) and match them to the
 * closest node(s) in the path-dependent complex network.
 * 
 * @author jwjoubert
 */
public class FacilityMatcher {
	final private static Logger log = Logger.getLogger(FacilityMatcher.class);

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Header.printHeader(FacilityMatcher.class.toString(), args);
		String facilitiesFile = args[0];
		String producerFile = args[1];
		String processorFile = args[2];
		String output = args[3];
		
		/* Clear the output file. */
		File file = new File(output);
		if(file.exists()){
			log.warn("Output file " + output + " will be overwritten!");
			FileUtils.delete(file);
		}
		BufferedWriter bw = IOUtils.getAppendingBufferedWriter(output);
		try{
			/* Write the output file's header. */
			bw.write("Input,Id,lon,lat,within50,within100,within250,within500,within1000");
			bw.newLine();
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException("Cannot write to " + output);
		} finally{
			try {
				bw.close();
			} catch (IOException e) {
				e.printStackTrace();
				throw new RuntimeException("Cannot close " + output);
			}
		}
		
		FacilityMatcher fm = new FacilityMatcher();
		QuadTree<ActivityFacility> qtFacilities = fm.buildFacilitiesQT(facilitiesFile);
		fm.matchRange(producerFile, 6, 5, 2, 1, qtFacilities, output);
		fm.matchRange(processorFile, 5, 4, 1, 2, qtFacilities, output);
		
		Header.printFooter();
	}
	
	public FacilityMatcher() {

	}
	
	
	/**
	 * Reads an {@link ActivityFacilities} file, and building a {@link QuadTree}
	 * from them.
	 * @param filename
	 * @return
	 */
	public QuadTree<ActivityFacility> buildFacilitiesQT(String filename){
		log.info("Populating the QuadTree of facilities.");
		/* Determine the extent of the facilities file. */
		double minX = Double.POSITIVE_INFINITY;
		double minY = Double.POSITIVE_INFINITY;
		double maxX = Double.NEGATIVE_INFINITY;
		double maxY = Double.NEGATIVE_INFINITY;
		
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new MatsimFacilitiesReader(scenario ).parse(filename);
		ActivityFacilities facilities = scenario.getActivityFacilities();
		for(Id<ActivityFacility> id : facilities.getFacilities().keySet()){
			ActivityFacility facility = facilities.getFacilities().get(id);
			Coord c = facility.getCoord();
			minX = Math.min(minX, c.getX());
			minY = Math.min(minY, c.getY());
			maxX = Math.max(maxX, c.getX());
			maxY = Math.max(maxY, c.getY());
		}
		
		/* Build the QuadTree. */
		QuadTree<ActivityFacility> qt = new QuadTree<ActivityFacility>(minX, minY, maxX, maxY);
		for(Id<ActivityFacility> id : facilities.getFacilities().keySet()){
			ActivityFacility facility = facilities.getFacilities().get(id);
			Coord c = facility.getCoord();
			qt.put(c.getX(), c.getY(), facility);
		}
		
		log.info("Done populating the Quadtree of facilities (" + qt.size() + " found)");
		return qt;
	}
	
	public void matchRange(String filename, int xField, int yField, int idField, 
			int fileId, QuadTree<ActivityFacility> qt, String output){
		log.info("Evaluating the number of facilities within range from " + filename);
		
		/* Set up some counters. */
		int numberWithin0050 = 0;
		int numberWithin0100 = 0;
		int numberWithin0250 = 0;
		int numberWithin0500 = 0;
		int numberWithin1000 = 0;
		int producers = 0;
		
		
		CoordinateTransformation ct = TransformationFactory.getCoordinateTransformation("WGS84", "WGS84_SA_Albers");
		
		BufferedReader br = IOUtils.getBufferedReader(filename);
		BufferedWriter bw = IOUtils.getAppendingBufferedWriter(output);
		try{
			
			String line = br.readLine(); // Header.
			while((line = br.readLine()) != null){
				String[] sa = line.split(",");
				Coord cWgs = null;
				try{
					cWgs = new CoordImpl(Double.parseDouble(sa[xField]), Double.parseDouble(sa[yField]));
				} catch(NumberFormatException ee){
					log.debug("Ooops!!");
				} catch(ArrayIndexOutOfBoundsException eee){
					log.debug("Ooops!!");
				}
				Coord cAlbers = ct.transform(cWgs);
				
				Collection<ActivityFacility> within0050 = qt.get(cAlbers.getX(), cAlbers.getY(), 50.0);
				Collection<ActivityFacility> within0100 = qt.get(cAlbers.getX(), cAlbers.getY(), 100.0);
				Collection<ActivityFacility> within0250 = qt.get(cAlbers.getX(), cAlbers.getY(), 250.0);
				Collection<ActivityFacility> within0500 = qt.get(cAlbers.getX(), cAlbers.getY(), 500.0);
				Collection<ActivityFacility> within1000 = qt.get(cAlbers.getX(), cAlbers.getY(), 1000.0);
				
				/* Update statistics. */
				numberWithin0050 += Math.min(1, within0050.size());
				numberWithin0100 += Math.min(1, within0100.size());
				numberWithin0250 += Math.min(1, within0250.size());
				numberWithin0500 += Math.min(1, within0500.size());
				numberWithin1000 += Math.min(1, within1000.size());
				
				/* Write the output. */
				String s = String.format("%d,%s,%.6f,%.6f,%d,%d,%d,%d,%d\n", 
						fileId,
						sa[idField],
						Double.parseDouble(sa[xField]),
						Double.parseDouble(sa[yField]),
						within0050.size(),
						within0100.size(),
						within0250.size(),
						within0500.size(),
						within1000.size() );
				bw.write(s);
				
				producers++;
			}
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException("Cannot read from " + filename);
		} finally{
			try {
				br.close();
				bw.close();
			} catch (IOException e) {
				e.printStackTrace();
				throw new RuntimeException("Cannot close " + filename);
			}
		}
		
		/* Report ranges. */
		log.info("Total number of input locations: " + producers);
		log.info("   Facilities within...");
		log.info("        50m: " + numberWithin0050);
		log.info("       100m: " + (numberWithin0100 - numberWithin0050));
		log.info("       250m: " + (numberWithin0250 - numberWithin0100));
		log.info("       500m: " + (numberWithin0500 - numberWithin0250));
		log.info("      1000m: " + (numberWithin1000 - numberWithin0500));
		log.info("     >1000m: " + (producers - numberWithin1000));
	}

}
