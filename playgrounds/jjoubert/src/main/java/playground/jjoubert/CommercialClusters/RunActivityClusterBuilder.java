/* *********************************************************************** *
 * project: org.matsim.*
 * ClusterActivities.java
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

package playground.jjoubert.CommercialClusters;

import java.io.IOException;
import java.util.List;

import org.apache.log4j.Logger;

import playground.jjoubert.Utilities.MyXmlConverter;

public class RunActivityClusterBuilder {
	 /*=============================================================================
	 * Different scenarios to run. See method `loadScenario' for a more detailed	|
	 * description of each of the scenarios.										|
	 * 																				|
	 * 		1 - TODO City Deep sample (with output)									|
	 * 		2 - Study area (with output)											|
	 * 		3 - TODO City Deep sample (no output; single vehicle SNA)				|
	 * 		4 - Study area (no output; single vehicle SNA)							|
	 * 		5 - Study area (no output; all vehicles SNA)							|
	 * 		6 - Study area (output and SNA)											|
	 *=============================================================================*/
	private static int scenario = 5	;
	
	 /*=============================================================================
	 * String value that must be set. Allowed study areas are:						|
	 * 																				|
	 * 		- SouthAfrica															|
	 * 		- Gauteng																|
	 * 		- KZN																	|
	 * 		- WesternCape															|
	 * 		- TODO CityDeep															|
	 *=============================================================================*/
	private static String studyAreaName = "Gauteng";

	/*=============================================================================
	 * The year for which the DigiCore analysis is being done. Available years are:	|
	 * 		- 2008																	|														|
	 *=============================================================================*/
	private static int year = 2008;

	 /*=============================================================================
	 * String value that must be set. Allowed activity types are:					|
	 * 		- Minor																	|
	 * 		- Major																	|
	 * 		- null (this ensures that BOTH minor and major activities are 			|
	 * 		  considered.															|
	 *=============================================================================*/
	private static String activityType = null;

	 /*=============================================================================
	 * String value indicating where the root where job is executed. 				|
	 * 		- Mac																	|
	 * 		- IVT-Sim0																|
	 * 		- Satawal																|
	 *=============================================================================*/
//	private static String root = "/Users/johanwjoubert/MATSim/workspace/MATSimData/"; 	// Mac
//	private static String root = "/home/jjoubert/";										// IVT-Sim0
	private static String root = "/home/jjoubert/data/";								// Satawal
	
	 /*=============================================================================
	 * String value indicating the version to run									|
	 *=============================================================================*/
	private static String version = "20091202131951";
	
	 /*=============================================================================
	 * String array with all the minor/major thresholds that should be considered. 	|
	 *=============================================================================*/
	private static String[] thresholdArray = {"0300"};

	 /*=============================================================================
	 * String array with all the sample numbers that should be considered. 			|
	 *=============================================================================*/	
	private static String[] sampleArray = {"01"};
//	private static String[] sampleArray = {"01", "02", "03", "04", "05", "06", "07", "08", "09", "10"};
	
	 /*=============================================================================
	 * Cluster parameters that must be set. These include: 							|
	 * 		- radius: the radius around an activity for which other activities will |
	 * 		  be searched.															|
	 * 		- minimumPoints: the minimum number of activities required within the	|
	 * 		  radius before a new cluster will be created.							| 
	 *=============================================================================*/
//	private static int radius = 20;
	private static int[] radiusArray = {30};
//	private static int minimumPoints = 30;
	private static int[] minimumPointsArray = {15};
	
	 /*=============================================================================
	 * Other utility parameters. Need not be set/changed							|
	 *=============================================================================*/
	private static Logger log = Logger.getLogger(RunActivityClusterBuilder.class);
	private static String vehicleFilename = null;
	private static boolean visualizeClusters;
	private static boolean writeClusters;
	private static boolean writeClusterListToXml;
	private static boolean performSNA;
	private static boolean snaSilence;

	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		
		loadScenario();
		
		for (String threshold : thresholdArray) {
			for (String sample : sampleArray) {
				for (int radius : radiusArray) {
					for (int minimumPoints : minimumPointsArray) {
						log.info("==================================================================");
						log.info(" Clustering activities for " + studyAreaName);
						log.info("------------------------------------------------------------------");
						log.info(" Parameter values:");
						log.info("");
						log.info("                   Version: " + version);
						log.info("                Thresholds: " + thresholdsToString(thresholdArray));
						log.info("                   Samples: " + samplesToString(sampleArray));
						log.info(" Cluster search radius (m): " + radius);
						log.info(" Minimum points in cluster: " + minimumPoints);
						log.info("==================================================================");
						log.info("");
						
						MyCommercialClusterStringBuilder sb = new MyCommercialClusterStringBuilder(root, version, threshold, 
								sample, studyAreaName, year, minimumPoints, radius, activityType);
						
						ActivityClusterBuilder acb = new ActivityClusterBuilder(sb);
						acb.clusterActivities(radius, minimumPoints, activityType);
						
						if(visualizeClusters){
							List<String> outputList = sb.getClusterVisualisationFilenameList();
							acb.getDjc().visualizeClusters(outputList.get(0), outputList.get(1), outputList.get(2), outputList.get(3));
						}
						
						if(writeClusters){
							acb.getDjc().writeClustersToFile(sb.getClusterOutputFilename());
						}
						
						if(writeClusterListToXml){
							MyXmlConverter mxc = new MyXmlConverter();
							mxc.writeObjectToFile(acb.getDjc().getClusterList(), sb.getClusterXmlFilename());
						}
						
						if(performSNA){
							acb.executeSna(sb.getVehicleFoldername(), null, snaSilence);
						}						
					}
				}
			}
		}
						
		log.info("Process completed.");
	}

	private static void loadScenario() {
		visualizeClusters = false;
		writeClusters = false;
		writeClusterListToXml = false;
		performSNA = false;
		snaSilence = false;
		switch (scenario) {
		case 1: // City Deep (with output)
			visualizeClusters = true;
			break;
			
		case 2: // Given study area (with output)
			visualizeClusters = true;
			writeClusters = true;
			break;
			
		case 3: // City Deep (no output; with SNA; single vehicle)
			vehicleFilename = root + "Gauteng/XML/93580.xml"; // has 2
			vehicleFilename = root + "Gauteng/XML/93579.xml"; // has 2
			performSNA = true; 
			break;
			
		case 4: // Study area (no output; with single vehicle SNA)
			vehicleFilename = root + "Gauteng/XML/93579.xml";
			performSNA = true;
			break;

		case 5: // Study area (no output; with ALL vehicles SNA)
			performSNA = true;
			snaSilence = true;
			break;
			
		case 6: // Study area (output and ALL vehicle SNA)
			visualizeClusters = true;
			writeClusters = true;
			performSNA = true;
			snaSilence = true;
			break;

		default:
			log.info("An invalid scenario " + scenario + " has been selected");
			break;
		}
		
		/*
		 * Perform a couple of basic checks.
		 */
		if(performSNA && activityType != null){
			throw new RuntimeException("When doing SNA, activityType paramater must be set to null!");
		}
	}

	/**
	 * Converts a given array of strings into a single string with the array elements 
	 * separated by a semi-colon. 
	 * @param thresholdArray
	 * @return single <code>String</code>
	 */
	private static String thresholdsToString(String[] thresholdArray){
		String result = null;
		if(thresholdArray.length > 0){
			result = thresholdArray[0];
			for(int i = 1; i < thresholdArray.length; i++){
				result += "; ";
				result += thresholdArray[i];
			}
		}		
		return result;
	}
	
	/**
	 * Converts a given array of strings into a single string with the array elements 
	 * separated by a semi-colon. 
	 * @param sampleArray
	 * @return single <code>String</code>
	 */
	private static String samplesToString(String[] sampleArray){
		String result = null;
		if(sampleArray.length > 0){
			result = sampleArray[0];
			for(int i = 1; i < sampleArray.length; i++){
				result += "; ";
				result += sampleArray[i];
			}
		}		
		return result;
	}

}