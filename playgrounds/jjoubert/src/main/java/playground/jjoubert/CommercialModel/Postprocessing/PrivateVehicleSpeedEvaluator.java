/* *********************************************************************** *
 * project: org.matsim.*
 * PrivateVehicleSpeedEvaluator.java
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

package playground.jjoubert.CommercialModel.Postprocessing;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.ConfigUtils;

import playground.jjoubert.CommercialModel.Listeners.MyPrivateVehicleSpeedAnalyser;
import playground.jjoubert.CommercialTraffic.SAZone;
import playground.jjoubert.Utilities.DateString;
import playground.jjoubert.Utilities.MyGapReader;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineSegment;
import com.vividsolutions.jts.geom.Point;

public class PrivateVehicleSpeedEvaluator {

	private static Logger log = Logger.getLogger(PrivateVehicleSpeedEvaluator.class);
	private static GeometryFactory gf = new GeometryFactory();

	/*
	 * Things that must be SET
	 */
	private static String[] runs = {"01", "02", "03", "04", "05", "06"};
	//	private static String[] runs = {"01"};
	private static String root = "/Users/johanwjoubert/MATSim/workspace/MATSimData/";
	private static String province = "Gauteng";
	private static double distanceThreshold = 10000;
	private static int lowerId = 0;
	private static int upperId = 99999;
	private static String delimiter = ",";
	private static int numberOfHourBins = 24;
	private static boolean simulated = true;


	/**
	 * This file determines the average speed travelled across all links in a given zone
	 * over multiple runs.
	 * @param args
	 */
	public static void main(String[] args) {
		log.info("=========================================================================");
		log.info("Calculating the average speeds for zones in " + province);
		log.info("=========================================================================");

		/*
		 * Determine which link Ids are associated with the given GAP mesozone. That means
		 * that GAP file must be read first.
		 */
		String shapefile = root + "ShapeFiles/" + province + "/" + province + "GAP_UTM35S.shp";
		MyGapReader mgr = new MyGapReader(province, shapefile);

		String networkFile = root + "Commercial/Input/network" + province + ".xml";
		ScenarioImpl scenario = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		NetworkImpl nl = scenario.getNetwork();
		MatsimNetworkReader nr = new MatsimNetworkReader(scenario);
		nr.readFile(networkFile);

		log.info("Building a (Hash)Map of the mesozones associated with each link, this may take a while.");
		int linkCounter = 0;
		int linkMultiplier = 1;
		int linksFound = 0;
		int linksNotFound = 0;

		Map<Id, SAZone> zoneTree = new HashMap<Id, SAZone>();

		/*
		 *  TODO Check!! I've changed from LinkImpl to Link since it gave an error.
		 *  I'm not sure if this will give the right output now.
		 */
		
		Map<Id, Link> map = nl.getLinks();
		for (Id key : map.keySet()) {
			Link link = map.get(key);

			Coordinate fromPoint = new Coordinate(link.getFromNode().getCoord().getX(), link.getFromNode().getCoord().getY());
			Coordinate toPoint = new Coordinate(link.getToNode().getCoord().getX(), link.getToNode().getCoord().getY());
			LineSegment ls = new LineSegment(fromPoint, toPoint);
			Coordinate c = ls.pointAlong(0.5);
			Point midPoint = gf.createPoint(c);

			Collection<SAZone> zoneList = mgr.getGapQuadTree().get(midPoint.getX(), midPoint.getY(), distanceThreshold);
			boolean foundZone = false;
			for (SAZone zone : zoneList) {
				if(zone.contains(midPoint)){
					foundZone = true;
					zoneTree.put(key, zone);
				}
			}
			if(foundZone){
				linksFound++;
			} else{
				//				log.warn("   Link " + key.toString() + " could not be allocated a mesozone!");
				linksNotFound++;
			}

			linkCounter++;
			if(linkCounter == linkMultiplier){
				log.info("   Links completed... " + linkCounter);
				linkMultiplier *= 2;
			}

		}
		log.info("   Links completed... " + linkCounter + " (resolved: " + linksFound + "; unresolved: " + linksNotFound + ").");


		/*
		 * Create an empty Map:
		 * 		Key: an integer representing the GAP_ID.
		 *  	Value: an array of doubles, each accounting for the average speed in a unique hour.
		 */
		Map<Integer, Double[]> statsMap = new HashMap<Integer, Double[]>();
		for (SAZone zone : mgr.getAllZones()) {
			Double [] speedList = new Double[zone.getTimeBins()];
			for(int i = 0; i < zone.getTimeBins(); i++){
				speedList[i] = 0.0;
			}
			statsMap.put(Integer.parseInt(zone.getName()), speedList);
		}
		if(simulated){
			/*
			 * Repeat for each run.
			 */
			for (String run : runs) {
				log.info("==============================  Start processing Run" + run + "  ==============================");
				String folderName = root + "Commercial/Output/Run" + run;
				File theFolder = new File(folderName);
				File[] files = theFolder.listFiles();
				File inputFolder = null;
				for (File file : files) {
					if(file.getName().startsWith("it.100")){
						inputFolder = file;
					}
				}
				if(inputFolder == null){
					log.warn("Could not find the events file for Run" + run + "!");
				} else{
					EventsManagerImpl events = new EventsManagerImpl();
					MyPrivateVehicleSpeedAnalyser handler = new MyPrivateVehicleSpeedAnalyser(zoneTree, nl, lowerId, upperId, numberOfHourBins);
					events.addHandler(handler);

					/*
					 * Read the events file.
					 */
					MatsimEventsReader mer = new MatsimEventsReader(events);
					log.info("Reading events, this may take a while.");
					mer.readFile(inputFolder.getAbsolutePath() + "/100.events.txt.gz");
					handler.doAnalysis();

					/*
					 * TODO Add the results to some aggregated list.
					 */
					for (SAZone zone : mgr.getAllZones()) {
						zone.calculateAverageSpeed();

						Integer statsKey = Integer.parseInt(zone.getName());
						Double[] statsList = statsMap.get(statsKey);
						for(int i = 0; i < statsList.length; i++){
							statsList[i] += zone.getSpeedDetail()[i];
						}
						/*
						 * Clear the SAZone's speed details, otherwise it just keeps adding up across the multiple runs.
						 */
						zone.clearSAZone();
					}
				}
			}
		} else{
			// TODO Process Pieter's original Private Vehicle only events file
			log.info("==============================  Processing private-only events file  ==============================");

			String fileName = root + "Commercial/Input/PrivateOnlyEvents.txt.gz";
			EventsManagerImpl events = new EventsManagerImpl();
			MyPrivateVehicleSpeedAnalyser handler = new MyPrivateVehicleSpeedAnalyser(zoneTree, nl, lowerId, upperId, numberOfHourBins);
			events.addHandler(handler);

			/*
			 * Read the events file.
			 */
			MatsimEventsReader mer = new MatsimEventsReader(events);
			log.info("Reading events, this may take a while.");
			mer.readFile(fileName);
			handler.doAnalysis();

			/*
			 * TODO Add the results to some aggregated list.
			 */
			for (SAZone zone : mgr.getAllZones()) {
				zone.calculateAverageSpeed();

				Integer statsKey = Integer.parseInt(zone.getName());
				Double[] statsList = statsMap.get(statsKey);
				for(int i = 0; i < statsList.length; i++){
					statsList[i] += zone.getSpeedDetail()[i];
				}
				/*
				 * Clear the SAZone's speed details, otherwise it just keeps adding up across the multiple runs.
				 */
				zone.clearSAZone();
			}


		}

		/*
		 * Write the analyses to file. To make sure the mesozones appear in the same order as
		 * in the original list, I use the original list as source, find the associated key
		 * from the GAP_ID name, and retrieve the statistics from the map.
		 */
		log.info("Writing the statistics to file.");
		DateString ds = new DateString();
		double divider;
		String outputFile;
		if(simulated){
			divider = runs.length;
			outputFile = root + "Commercial/PostProcess/AveragePrivateVehicleSpeed-" + ds.toString() + ".txt";
		} else{
			divider = 1;
			outputFile = root + "Commercial/PostProcess/OriginalPrivateVehicleSpeed-" + ds.toString() + ".txt";
		}
		try {
			BufferedWriter output = new BufferedWriter(new FileWriter(new File(outputFile)));
			try{
				// Write header.
				output.write("Name");
				output.write(delimiter);
				for(int i = 0; i < numberOfHourBins-1; i++){
					output.write("H");
					output.write(String.valueOf(i));
					output.write(delimiter);
				}
				output.write("H");
				output.write(String.valueOf(numberOfHourBins-1));
				output.newLine();

				// Write the stats.
				for (SAZone zone : mgr.getAllZones()) {
					Integer key = Integer.parseInt(zone.getName());
					output.write(String.valueOf(key));
					output.write(delimiter);
					Double[] stats = statsMap.get(key);
					for(int i = 0; i < stats.length-1; i++){
						output.write(String.valueOf(stats[i] / divider));
						output.write(delimiter);
					}
					output.write(String.valueOf(stats[stats.length-1] / divider));
					output.newLine();
				}

			} finally{
				output.close();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		log.info("Process completed!");
	}

}
