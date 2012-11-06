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
public class RunLegModeDistanceDistributionComparison {
	private final static Logger logger = Logger.getLogger(RunLegModeDistanceDistributionComparison.class);
	
	static String baseFolder1 = "../../runs-svn/detEval/mobilTUM/run20/";
	static String configFile1 = baseFolder1 + "output_config.xml.gz";
	static String iteration1 = "0";
	
	// ===
	static String baseFolder2 = "../../runs-svn/detEval/mobilTUM/run20/";
	static String configFile2 = baseFolder2 + "output_config.xml.gz";
	static String iteration2 = "200";
	
	static UserGroup userGroup = null;
	
	public static void main(String[] args) {
		Gbl.startMeasurement();
		RunLegModeDistanceDistribution rlmdd1 = new RunLegModeDistanceDistribution(baseFolder1, configFile1, iteration1, userGroup);
		rlmdd1.run();
		
		RunLegModeDistanceDistribution rlmdd2 = new RunLegModeDistanceDistribution(baseFolder2, configFile2, iteration2, userGroup);
		rlmdd2.run();
		
		// TODO: comparison!
	}
}
