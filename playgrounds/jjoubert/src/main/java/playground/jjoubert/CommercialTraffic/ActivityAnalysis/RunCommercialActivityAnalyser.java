/* *********************************************************************** *
 * project: org.matsim.*
 * AnalyseActivityDuration.java
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

package playground.jjoubert.CommercialTraffic.ActivityAnalysis;

import org.apache.log4j.Logger;

import playground.jjoubert.Utilities.DateString;

public class RunCommercialActivityAnalyser {
	private final static Logger log = Logger.getLogger(RunCommercialActivityAnalyser.class);

	 /*=============================================================================
	 * String value indicating where the root where job is executed. 				|
	 * 		- Mac																	|
	 * 		- IVT-Sim0																|
	 * 		- Satawal																|
	 * 		- IE-Calvin														  		|
	 *=============================================================================*/
//	private static String root = "/Users/johanwjoubert/MATSim/workspace/MATSimData/"; 	// Mac
//	private static String root = "/home/jjoubert/";										// IVT-Sim0
//	private static String root = "/home/jjoubert/data/";								// Satawal
	private static String root = "/home/jwjoubert/MATSim/MATSimData/";					// IE-Calvin

	 /*=============================================================================
	 * String value that must be set. Allowed study areas are:						|																		|
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
	 * Cluster parameters that must be set. These include: 							|
	 * 		- clusterRadius: the radius around an activity within which other 		|
	 * 		  activities will be searched.											|
	 * 		- clusterCount: the minimum number of activities required within the	|
	 * 		  radius before a new cluster will be created.							|
	 * 		- numberOfSamples: the number of samples to run.						|
	 * 		- sampleSize: the number of vehicles sampled within each run.			| 
	 *=============================================================================*/
	private static float clusterRadius = 15;
	private static int clusterCount = 30;
	private static int numberOfSamples = 1;
	private static int sampleSize = 100;

	 /*=============================================================================
	 * Double array with all the minor/major thresholds that should be considered. 	|
	 * This threshold indicates the duration (in minutes) that distinguishes 		|
	 * between activity types. Activities with durations less than the threshold	|
	 * are considered `minor'. Those with durations exceeding the threshold are 	|
	 * considered `major' activities.												|
	 *=============================================================================*/
//	private static double[] majorThresholds = {179.6, 339.4, 628.8, 931.5, 2062.5};
	private static double[] majorThresholds = {300};

	 /*=============================================================================
	 * The threshold value (expressed as a fraction) that distinguishes between		| 
	 * vehicle types. Vehicles that spend more than the threshold of their 			|
	 * activities in the study area are considered `within' vehicles. Those with 	|
	 * fewer of their activities in the study area are considered to be `through'	|
	 * traffic vehicles.															|
	 *=============================================================================*/	
	private static double withinThreshold = 0.60;
	
	 /*=============================================================================
	 * Boolean variables to indicate what type of run should be done. The 			|
	 * following are current options:												|
	 * 		- analyseForR: Only write the activity durations to file. No chains are	|
	 * 		  extracted since the objective is to plot merely the activity 			|
	 * 		  of all activities, both minor and major.								|
	 * 		- extractChains: As the name implies, extract activities, and then 		|
	 * 		  also extract the activity chains.										|
	 *=============================================================================*/
	private static boolean analyseForR = false;
	private static boolean extractChains = true;
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		if(analyseForR == extractChains || (!analyseForR && !extractChains)){
			log.error("The activity choice `analyseForR' and `extractChains' are awkward!");
			throw new RuntimeException("Check process settings.");
		}
		DateString ds = new DateString();
			
		// Sample for different threshold values.
		for (Double threshold : majorThresholds) {
			String theThreshold = String.format("%04d", (int)Math.round(threshold));
			// Analyse as many samples as you want.
			for(int sample = 1; sample <= numberOfSamples; sample++){
				
				String theSample = String.format("%02d", sample);
				MyActivityAnalysisStringBuilder sb = new MyActivityAnalysisStringBuilder(root, ds.toString(), theThreshold, theSample, studyAreaName, year);
				CommercialActivityAnalyser caa = new CommercialActivityAnalyser(sb, sb.getWgs84(), sb.getUtm35S(), studyAreaName, ds);
				if(extractChains){
					caa.extractChains(sample, sampleSize, threshold, withinThreshold, clusterRadius, clusterCount, true);
				} 
				if(analyseForR){
					caa.analyseSampleDurationsForR(sample, sampleSize);
				}
			}			
		}
		
		log.info("=======================================================");
		log.info("                  PROCESS COMPLETED                    ");
		log.info("=======================================================");
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
