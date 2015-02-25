/* *********************************************************************** *
 * project: org.matsim.*
 * DgCb2Ks2010
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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
package playground.dgrether.koehlerstrehlersignal.run;

import playground.dgrether.DgPaths;
import playground.dgrether.koehlerstrehlersignal.conversion.TtMatsim2KS2015;

/**
 * @author dgrether
 * @author tthunig
 * 
 */
public class Cottbus2KS2010 {

	public static void main(String[] args) throws Exception {
		// input files
		String signalSystemsFilename = DgPaths.REPOS
				+ "shared-svn/projects/cottbus/cottbus_scenario/signal_systems_no_13.xml";
		String signalGroupsFilename = DgPaths.REPOS
				+ "shared-svn/projects/cottbus/cottbus_scenario/signal_groups_no_13.xml";
		String signalControlFilename = DgPaths.REPOS
				+ "shared-svn/projects/cottbus/cottbus_scenario/signal_control_no_13.xml";
		String networkFilename = DgPaths.REPOS
				+ "shared-svn/projects/cottbus/cottbus_scenario/network_wgs84_utm33n.xml.gz";
		String lanesFilename = DgPaths.REPOS
				+ "shared-svn/projects/cottbus/cottbus_scenario/lanes.xml";
		// change run number here to use another base case
		String populationFilename = DgPaths.REPOS
				+ "runs-svn/cottbus/before2015/run1728/1728.output_plans.xml.gz";

		// output files
		String outputDirectory = DgPaths.REPOS
				+ "shared-svn/projects/cottbus/cb2ks2010/";
		String dateFormat = "2015-02-25";

		/* parameters for the time interval */
		double startTime = 5.5 * 3600.0;
		double endTime = 9.5 * 3600.0;
		// double startTime = 13.5 * 3600.0;
		// double endTime = 18.5 * 3600.0;
		/* parameters for the network area */
		double signalsBoundingBoxOffset = 500.0;
		// Okt'14: sBB 500 instead of 50 to avoid effect that travelers drive
		// from the ring around cottbus outside and inside again to jump in time
		double cuttingBoundingBoxOffset = 50.0; // an offset >= 31000.0 results
												// in a bounding box that
												// contains the hole network
		/* parameters for the interior link filter */
		double freeSpeedFilter = 15.0;
		boolean useFreeSpeedTravelTime = true;
		 double maximalLinkLength = Double.MAX_VALUE; // = default value
		/* parameters for the demand filter */
		 double matsimPopSampleSize = 1.0; // = default value
		 double ksModelCommoditySampleSize = 1.0; // = default value
		double minCommodityFlow = 50.0;
		int cellsX = 5; // = default value
		int cellsY = 5; // = default value
		/* other parameters */
		String scenarioDescription = "run run1728 output plans between 05:30 and 09:30";
		// String scenarioDescription =
		// "run run1728 output plans between 13:30 and 18:30";

		TtMatsim2KS2015 converter = new TtMatsim2KS2015(signalSystemsFilename,
				signalGroupsFilename, signalControlFilename, networkFilename,
				lanesFilename, populationFilename, startTime, endTime,
				signalsBoundingBoxOffset, cuttingBoundingBoxOffset,
				freeSpeedFilter, useFreeSpeedTravelTime, maximalLinkLength,
				matsimPopSampleSize, ksModelCommoditySampleSize,
				minCommodityFlow, cellsX, cellsY, scenarioDescription,
				dateFormat, outputDirectory);

		converter.convertMatsim2KS();

	}

}
