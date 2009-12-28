/* *********************************************************************** *
 * project: org.matsim.*
 * Run.java
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

package playground.mfeil.MDSAM;

import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.core.facilities.MatsimFacilitiesReader;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.MatsimPopulationReader;

public class Run {
	
	private static final Logger log = Logger.getLogger(Run.class);

	/**
	 * Class to run PlansConstructor, MDSAM, and/or ModFileMaker
	 * @param args
	 */
	public static void main(String[] args) {
		log.info("Process started...");
		
		final String facilitiesFilename = "/home/baug/mfeil/data/Zurich10/facilities.xml";
		final String networkFilename = "/home/baug/mfeil/data/Zurich10/network.xml";
		final String populationFilename = "/home/baug/mfeil/data/choiceSet/it0/output_plans_mz05.xml";
		final String outputFileBiogeme = "/home/baug/mfeil/data/choiceSet/it0/output_plans0930.dat";
		final String attributesInputFile = "/home/baug/mfeil/data/mz/attributes_MZ2005.txt";
		final String outputFileMod = "/home/baug/mfeil/data/choiceSet/it0/model0930.mod";
		
/*		final String populationFilename = "./plans/output_plans.xml";
		final String networkFilename = "./plans/network.xml";
		final String facilitiesFilename = "./plans/facilities.xml";
		final String outputFileSims = "/home/baug/mfeil/data/largeSet/it0/sims062.xls";
		final String outputFile = "./plans/output_plans.dat";
		*/
		
		String beta				= "yes";
		String gamma			= "no";
		String similarity 		= "no";
		String incomeConstant 	= "no";
		String incomeDivided	= "yes";
		String incomeDividedLN	= "no";
		String incomeBoxCox		= "no";
		String gender 			= "yes";
		String age 				= "no";
		String employed 		= "no";
		String license 			= "no";
		String carAvail 		= "no";
		String seasonTicket 	= "no";
		String travelDistance	= "no"; 
		String travelCost		= "yes"; 
		String travelConstant 	= "yes";
		String beta_travel		= "no";
		String bikeIn			= "yes";

		ScenarioImpl scenario = new ScenarioImpl();
		new MatsimNetworkReader(scenario.getNetwork()).readFile(networkFilename);
		new MatsimFacilitiesReader(scenario.getActivityFacilities()).readFile(facilitiesFilename);
		new MatsimPopulationReader(scenario).readFile(populationFilename);
		
//		MDSAM mdsam = new MDSAM(scenario.getPopulation());				
//		List<List<Double>> sims = mdsam.runPopulation();
//		log.info("Size of sime is "+sims.size()+" times "+sims.get(0).size());
		
		List<List<Double>> sims = null;

		PlansConstructor pc = new PlansConstructor(scenario.getPopulation(), sims);
		pc.keepPersons();
		//pc.writePlansForBiogemeWithRandomSelection(outputFileBiogeme, attributesInputFile, 
		//		similarity, incomeConstant, incomeDivided, incomeDividedLN, incomeBoxCox, age, gender, employed, license, carAvail, seasonTicket, travelDistance, travelCost, travelConstant, bikeIn);
		pc.writePlansForBiogemeWithRandomSelectionAccumulated(outputFileBiogeme, attributesInputFile, 
				beta, gamma, similarity, incomeConstant, incomeDivided, incomeDividedLN, incomeBoxCox, age, gender, employed, license, carAvail, seasonTicket, travelDistance, travelCost, travelConstant, beta_travel, bikeIn);
		pc.writeModFileWithRandomSelection(outputFileMod);
		log.info("Process finished.");
	}
}


