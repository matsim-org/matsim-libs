/* *********************************************************************** *
 * project: org.matsim.*
 * RunMyCommercialDemandGenerator.java
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

package playground.jjoubert.CommercialDemandGenerator;

public class RunMyCommercialDemandGenerator {

	/**
	 * This is the old approach (first run at ETH). It will be replaced to run separate 
	 * demand generators for 'within' and 'through' traffic.
	 * @param args
	 */
	public static void main(String[] args) {
		/*===========================================================
		 * Variables that must be set:
		 */
		String root = "/Users/johanwjoubert/MATSim/workspace/MATSimData/";
		String studyArea = "Gauteng";
		String version = "20091202131951";
		String threshold = "0300";
		String sample = "Sample01";
		double withinThreshold = 0.6;
		MyCommercialDemandGeneratorStringBuilder sb = new MyCommercialDemandGeneratorStringBuilder(root, studyArea);

		// Generate input matrices for conditional probabilities.
		MyCommercialChainAnalyser mcca = new MyCommercialChainAnalyser(withinThreshold, 
				sb.getVehicleStatsFilename(version, threshold, sample));
		mcca.analyse(sb.getXmlSourceFolderName(version, threshold, sample), 20, 48);
		mcca.writeMatrixFiles(sb.getMatrixFileLocation(version, threshold, sample), studyArea);	
		

		/*
		 * This is where multiple samples must be 'driven'.
		 */
		
		
//		MyCommercialDemandGenerator01 mcdg = new MyCommercialDemandGenerator01(root, studyArea, 0.9);
//		mcdg.buildVehicleLists();
//		
//		
//		mcdg.createPlans();

	}
	
	

}
