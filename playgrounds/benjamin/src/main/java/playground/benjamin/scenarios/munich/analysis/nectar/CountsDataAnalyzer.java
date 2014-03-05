/* *********************************************************************** *
 * project: org.matsim.*
 * CountsDataAnalyzer.java
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
package playground.benjamin.scenarios.munich.analysis.nectar;


/**
 * @author benjamin
 *
 */
public class CountsDataAnalyzer {
	
	static String runDir = "../../runs-svn/run981/";
	static String iterDir = runDir + "ITERS/it.1500/";
	
	static String networkFilename = runDir + "981.output_network.xml.gz";
	static String countsFilename = runDir + "input/counts-2008-01-10_correctedSums_manuallyChanged_strongLinkMerge.xml";
	static String linkStatsFilename = iterDir + "981.1500.linkstats.txt.gz";
	static double scaleFactor = 10.0;
	static String coordinateSystem = "GK4";
	
	static private String outputFile = iterDir + "981.1500.countscompare";
	static String outputFormat = "txt";

	public static void main(String[] args) {
//		CountsAnalyser ca = new CountsAnalyser();
//		ca.setCoordinateSystem(coordinateSystem);
//		ca.setCountsFilename(countsFilename);
//		ca.setLinkStatsFilename(linkStatsFilename);
//		ca.setNetworkFilename(networkFilename);
//		ca.setScaleFactor(scaleFactor);
//		ca.loadData();
//		ca.writeCountsComparisonList(outputFile, outputFormat)
		throw new RuntimeException("commented code because playground.dgrether is no longer available");
	}
}
