/* *********************************************************************** *
 * project: org.matsim.*
 * AnalysisSelectedPlansGeneral.java
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

package playground.mfeil.analysis;



import org.apache.log4j.Logger;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.core.facilities.MatsimFacilitiesReader;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.MatsimPopulationReader;


/**
 * This is a class that facilitates calling various other analysis classes. 
 * It summarizes the analysis functionalities and offers simple access.
 *
 * @author mfeil
 */
public class ASPGeneral {
	
	private static final Logger log = Logger.getLogger(ASPActivityChains.class);
	
	public static void main(final String [] args) {
		// Scenario files
		final String facilitiesFilename = "/home/baug/mfeil/data/Zurich10/facilities.xml";
		final String networkFilename = "/home/baug/mfeil/data/Zurich10/network.xml";
		
		// Special MZ file so that weights of MZ persons can be read
		final String attributesInputFile = "/home/baug/mfeil/data/mz/attributes_MZ2005.txt";
		
		// Population files
		final String populationFilenameMATSim = "/home/baug/mfeil/data/runs/run0922_initialdemand_20/output_plans.xml";
		final String populationFilenameMZ = "/home/baug/mfeil/data/mz/plans_Zurich10.xml";
		
		// Output file
		final String outputDir = "/home/baug/mfeil/data/runs/run0922_initialdemand_20";	
		
		// Settings
		final String compareWithMZ = "no"; 
		
	
		
		// Start calculations
		ScenarioImpl scenarioMATSim = new ScenarioImpl();
		new MatsimNetworkReader(scenarioMATSim).readFile(networkFilename);
		new MatsimFacilitiesReader(scenarioMATSim).readFile(facilitiesFilename);
		new MatsimPopulationReader(scenarioMATSim).readFile(populationFilenameMATSim);
		
		if (compareWithMZ.equals("yes")){
			ScenarioImpl scenarioMZ = new ScenarioImpl();
			scenarioMZ.setNetwork(scenarioMATSim.getNetwork());
			new MatsimFacilitiesReader(scenarioMZ).readFile(facilitiesFilename);
			new MatsimPopulationReader(scenarioMZ).readFile(populationFilenameMZ);
		}

		
		ASPActivityChains sp = new ASPActivityChains(scenarioMATSim.getPopulation(), scenarioMATSim.getKnowledges(), outputDir);
		sp.run();
		
		sp.analyze();
		sp.checkCorrectness();
		
		log.info("Analysis of plan finished.");
	}

}

