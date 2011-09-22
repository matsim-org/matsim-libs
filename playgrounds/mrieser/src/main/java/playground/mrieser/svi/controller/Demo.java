/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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

package playground.mrieser.svi.controller;


public class Demo {
	
	public static void main(String[] args) {
		
		String networkFile = "/Volumes/Data/projects/sviDosierungsanlagen/scenarios/kreuzlingen/data1/matsim/network.cleaned.xml";
		String singlePlanPopulationFile = "/Volumes/Data/projects/sviDosierungsanlagen/scenarios/kreuzlingen/data1/matsim/population.xml";
		String populationFile = "/Volumes/Data/projects/sviDosierungsanlagen/scenarios/kreuzlingen/data1/matsim/populationNew.xml";

		PlansAlternativeGenerator.main(new String[] {
				networkFile,
				singlePlanPopulationFile,
				populationFile
		});
		
		
	}

}
