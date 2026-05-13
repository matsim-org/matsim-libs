package org.matsim.codeexamples.population.demandGenerationWithFacilities;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.facilities.MatsimFacilitiesReader;

public class RunCreatePopulationAndDemand {
	
	private final static Logger log = LogManager.getLogger(RunCreatePopulationAndDemand.class);
	private Scenario scenario;
	
	private static final String facilitiesFile = "output/facilities.xml";

	// --------------------------------------------------------------------------
	public static void main(String[] args) {
		RunCreatePopulationAndDemand creator = new RunCreatePopulationAndDemand();
		creator.run();		
	}
	
	private void run() {
		this.init();
		CreatePopulation populationCreator = new CreatePopulation();
		populationCreator.run(this.scenario);
		CreateDemand demandCreator = new CreateDemand();
		demandCreator.run(this.scenario, populationCreator.getPersonHomeAndWorkLocations());
		this.write();
	}
	
	private void init() {
		/*
		 * Create the scenario
		 */
		Config config = ConfigUtils.createConfig();
		this.scenario = ScenarioUtils.createScenario(config);
		new MatsimFacilitiesReader(this.scenario).readFile(facilitiesFile);
	}
	
	private void write() {
		PopulationWriter populationWriter = new PopulationWriter(this.scenario.getPopulation(), this.scenario.getNetwork());
		populationWriter.write("./output/plans.xml.gz");
		log.info("Number of persons: " + this.scenario.getPopulation().getPersons().size());
	}
}
