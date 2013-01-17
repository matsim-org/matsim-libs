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

package playground.wrashid.parkingSearch.planLevel.scoring;

import playground.wrashid.lib.obj.plan.PersonGroups;

public class PersonGroupParkingScoreGraphicGenerator {
	
	public static final String iterationScoreSum="scoreSum-iteration-";
	
	public static void generateGraphic(PersonGroups personGroups,String fileName){
		
		String xLabel = "Iteration";
		String yLabel = "utility score";
		String title="Average Person Group Parking Utilities";
		
		personGroups.generateIterationAverageGraph(xLabel, yLabel, title, iterationScoreSum, fileName);
		
	}
	
}
