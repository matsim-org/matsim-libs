/* *********************************************************************** *
 * project: matsim
 * PrepareBackgroundPopulation.java
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

package playground.christoph.ped2012;

import java.io.IOException;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.population.PopulationReader;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.facilities.MatsimFacilitiesReader;

import playground.christoph.evacuation.population.RemoveUnselectedPlans;

public class PrepareBackgroundPopulation {

private static final Logger log = Logger.getLogger(PrepareBackgroundPopulation.class);
	
	private String populationFile = "D:/Users/Christoph/workspace/matsim/mysimulations/ped2012/input_zh/it.50/1.50.plans.xml.gz";
	private String networkFile = "D:/Users/Christoph/workspace/matsim/mysimulations/ped2012/input_zh/network_ivtch.xml.gz";
	private String facilitiesFile = "D:/Users/Christoph/workspace/matsim/mysimulations/ped2012/input_zh/facilities.xml.gz";
	private String outFile = "D:/Users/Christoph/workspace/matsim/mysimulations/ped2012/input_zh/plans_25pct.xml.gz";
		
	public static void main(String[] args) {
		try {
			new PrepareBackgroundPopulation(((ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig())));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public PrepareBackgroundPopulation(Scenario scenario) throws IOException {
		log.info("Read Network File...");
		new MatsimNetworkReader(scenario).readFile(networkFile);
		log.info("done.");
		
		log.info("Reading facilities file...");
		new MatsimFacilitiesReader((ScenarioImpl)scenario).readFile(facilitiesFile);
		log.info("done.");
		
		log.info("Setting up plans objects...");
		PopulationImpl plans = (PopulationImpl) scenario.getPopulation();
		plans.setIsStreaming(true);
		double sample = 0.25;
		PopulationWriter plansWriter = new PopulationWriter(plans, scenario.getNetwork(), sample);
		plansWriter.startStreaming(outFile);
		PopulationReader plansReader = new MatsimPopulationReader(scenario);
		log.info("done.");
		
		log.info("Adding unselected plans remover...");
		((PopulationImpl)scenario.getPopulation()).addAlgorithm(new RemoveUnselectedPlans());
		log.info("done.");
				
		log.info("Reading, processing, writing plans...");
		plans.addAlgorithm(plansWriter);
		plansReader.readFile(populationFile);
		plans.printPlansCount();
		plansWriter.closeStreaming();
		log.info("done.");
	}
}