/* *********************************************************************** *
 * project: org.matsim.*
 * RunLegModeDistanceDistribution.java
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
package playground.benjamin.scenarios.munich.analysis.modular.legModeDistanceDistribution;

import org.apache.log4j.Logger;
import org.matsim.core.gbl.Gbl;

import playground.benjamin.scenarios.munich.analysis.filter.UserGroup;

/**
 * @author benjamin
 *
 */
public class LegModeDistanceDistributionMunichSingle {
	private final static Logger logger = Logger.getLogger(LegModeDistanceDistributionMunichSingle.class);
	
//	static String baseFolder = "../../runs-svn/detEval/mobilTUM/run20/";
	static String baseFolder = "../../runs-svn/internalizationCar/output/baseCase2a/";
	static String configFile = baseFolder + "output_config.xml.gz";
	static String iteration = "0";
	
	static UserGroup userGroup = null;
//	static UserGroup userGroup = UserGroup.URBAN;

	public static void main(String[] args) {
		Gbl.startMeasurement();
		RunLegModeDistanceDistribution rlmdd = new RunLegModeDistanceDistribution(baseFolder, configFile, iteration, userGroup);
		rlmdd.run();
	}
}