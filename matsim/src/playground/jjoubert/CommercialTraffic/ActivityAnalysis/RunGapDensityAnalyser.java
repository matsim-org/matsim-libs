/* *********************************************************************** *
 * project: org.matsim.*
 * RunGapDensityAnalyser.java
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

public class RunGapDensityAnalyser {

	/**
	 * @param args
	 */
	public static void main(String[] args) {

		String root = "/Users/johanwjoubert/MATSim/workspace/MATSimData";
		String studyArea = "Gauteng";
		String version = "20090922165959";
		String threshold = "0300";
		String sample = "01";
		
		GapDensityAnalyser gda = new GapDensityAnalyser(studyArea, version, threshold, sample, root);
		gda.analyseGapDensity();
	}

}
