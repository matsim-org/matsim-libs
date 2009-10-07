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
import playground.jjoubert.Utilities.MyStringBuilder;

public class RunCommercialActivityAnalyser {
	private final static Logger log = Logger.getLogger(RunCommercialActivityAnalyser.class);

	//====================================================
	// Variables that must be set
	//----------------------------------------------------
	private static String studyAreaName = "Gauteng";
	
	//====================================================
	// Parameters that must be set
	//----------------------------------------------------		
	private static int numberOfSamples = 10;
	private static int sampleSize = 3000;
	private static float clusterRadius = 10;
	private static int clusterCount = 20;
//	private static double[] majorThresholds = {179.6, 339.4, 628.8, 931.5, 2062.5};
	private static double[] majorThresholds = {300};
	private static double withinThreshold = 0.60;
	//====================================================
	// Processes that must be run
	//----------------------------------------------------			
	private static boolean analyseForR = false;
	private static boolean extractChains = true;
	//====================================================
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		if(analyseForR == extractChains || (!analyseForR && !extractChains)){
			log.error("The activity choice `analyseForR' and `extractChains' are awkward!");
			throw new RuntimeException("Check process settings.");
		}
		DateString ds = new DateString();
			
		// Create an instance of the analyser.
//		MyStringBuilder sb = new MyStringBuilder("/Users/johanwjoubert/MATSim/workspace/MATSimData/");		// Mac
		MyStringBuilder sb = new MyStringBuilder("/home/jjoubert/data/");									// Satawal
		CommercialActivityAnalyser caa = new CommercialActivityAnalyser(sb, sb.getWgs84(), sb.getUtm35S(), studyAreaName, ds);

		// Sample for different threshold values.
		for (Double threshold : majorThresholds) {
			// Analyse as many samples as you want.
			for(int sample = 1; sample <= numberOfSamples; sample++){
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
	

}
