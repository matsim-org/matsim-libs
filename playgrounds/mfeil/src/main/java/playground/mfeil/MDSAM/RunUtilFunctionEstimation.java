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
import java.util.Map;
import org.matsim.api.core.v01.Id;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.core.facilities.MatsimFacilitiesReader;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.MatsimPopulationReader;

public class RunUtilFunctionEstimation {
	
	private static final Logger log = Logger.getLogger(RunUtilFunctionEstimation.class);

	/**
	 * Class to run PlansConstructor, MDSAM, and/or ModFileMaker
	 * @param args
	 */
	public static void main(String[] args) {
		log.info("Process started...");
		
		final String version = "160";
		final String facilitiesFilename = "/home/baug/mfeil/data/Zurich10/facilities.xml";
		final String networkFilename = "/home/baug/mfeil/data/Zurich10/network.xml";
		final String populationFilename = "/home/baug/mfeil/data/choiceSet/it0/output_plans_mzAS0997b.xml";
		final String outputFileBiogeme = "/home/baug/mfeil/data/choiceSet/it0/output_plans"+version+".dat";
		final String attributesInputFile = "/home/baug/mfeil/data/mz/attributes_MZ2005.txt";
		final String outputFileMod = "/home/baug/mfeil/data/choiceSet/it0/model"+version+".mod";
		final String outputFileSimsOverview = "/home/baug/mfeil/data/choiceSet/it0/simsOverview"+version+".xls";
		final String outputFileSimsDetailLog = "/home/baug/mfeil/data/choiceSet/it0/simsDetails"+version+".xls";

		
		String beta				= "yes";
		String gamma			= "yes";
		String similarity 		= "yes";
		String incomeConstant 	= "no";
		String incomeDivided	= "no";
		String incomeDividedLN	= "no";
		String incomeBoxCox		= "no";
		String gender 			= "no";
		String age 				= "no";
		String income	 		= "no";
		String license 			= "no";
		String carAvail 		= "no";
		String seasonTicket 	= "no";
		String travelDistance	= "no"; 
		String travelCost		= "yes"; 
		String travelConstant 	= "yes";
		String beta_travel		= "no";
		String bikeIn			= "yes";
		String munType			= "no";
		String innerHome		= "yes";
		
		// if incomeBoxCox is set to yes, travelCost must be yes, too.
		if (incomeBoxCox.equals("yes")) travelCost = "yes";

		ScenarioImpl scenario = new ScenarioImpl();
		new MatsimNetworkReader(scenario).readFile(networkFilename);
		new MatsimFacilitiesReader(scenario).readFile(facilitiesFilename);
		new MatsimPopulationReader(scenario).readFile(populationFilename);
		
		PlansConstructor pc = new PlansConstructor(scenario.getPopulation(), outputFileSimsOverview, outputFileSimsDetailLog);
		pc.keepPersons();
		//pc.writePlansForBiogemeWithRandomSelection(outputFileBiogeme, attributesInputFile, 
		//		similarity, incomeConstant, incomeDivided, incomeDividedLN, incomeBoxCox, age, gender, employed, license, carAvail, seasonTicket, travelDistance, travelCost, travelConstant, bikeIn);
		pc.writePlansForBiogemeWithRandomSelectionAccumulated(outputFileBiogeme, attributesInputFile, 
				beta, gamma, similarity, incomeConstant, incomeDivided, incomeDividedLN, incomeBoxCox, 
				age, gender, income, license, carAvail, seasonTicket, travelDistance, travelCost, travelConstant, 
				beta_travel, bikeIn, munType, innerHome);
		pc.writeModFileWithRandomSelection(outputFileMod);
		log.info("Process finished.");
	}
}


