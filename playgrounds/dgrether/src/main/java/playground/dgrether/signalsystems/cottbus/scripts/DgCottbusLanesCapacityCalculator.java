/* *********************************************************************** *
 * project: org.matsim.*
 * DgCottbusLanesCapacityCalculator
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
package playground.dgrether.signalsystems.cottbus.scripts;

import org.matsim.lanes.LanesUtils;

import playground.dgrether.DgPaths;


/**
 * @author dgrether
 *
 */
public class DgCottbusLanesCapacityCalculator {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		String inputDir = DgPaths.STUDIESDG + "cottbus/cottbus_feb_fix/";
		String networkInputFilename = inputDir + "network_wgs84_utm33n.xml.gz";
		String lanesInputFilename = inputDir + "lanes_without_capacities.xml";
		String lanesOutputFilename = inputDir + "lanes.xml";
		LanesUtils.calculateMissingCapacitiesForLanes20(networkInputFilename,
				lanesInputFilename, lanesOutputFilename);
	}

}
