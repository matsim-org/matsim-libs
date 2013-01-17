/* *********************************************************************** *
 * project: org.matsim.*
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

package playground.wrashid.parkingSearch.planLevel.occupancy;

import playground.wrashid.lib.obj.plan.PersonGroups;

public class PersonGroupWalkingDistanceGraphGenerator {

	public static final String iterationWalkingDistanceSum = "walkingDistanceSum-iteration-";

	public static void generateGraphic(PersonGroups personGroups,
			String fileName) {

		String xLabel = "Iteration";
		String yLabel = "walking distance [m]";
		String title = "Average Person Group Walking Distance";
		personGroups.generateIterationAverageGraph(xLabel, yLabel, title, iterationWalkingDistanceSum, fileName);
	}
}
