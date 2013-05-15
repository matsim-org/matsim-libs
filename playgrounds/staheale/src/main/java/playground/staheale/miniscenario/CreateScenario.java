/* *********************************************************************** *
 * project: org.matsim.*
 * CreateScenario.java
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

package playground.staheale.miniscenario;

import org.apache.log4j.Logger;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;


public class CreateScenario {

	private final static Logger log = Logger.getLogger(CreateScenario.class);
	private ScenarioImpl scenario = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
	private String configFile;
	
				
	public static void main(final String[] args) {		
		if (args.length != 1) {
			log.error("Provide correct number of arguments ...");
			System.exit(-1);
		}
		
		CreateScenario scenarioCreator = new CreateScenario();	
		scenarioCreator.init(args[0]);
		scenarioCreator.run();			
		log.info("Scenario creation finished \n ----------------------------------------------------");
	}
	
	public void init(String configFile) {
		this.configFile = configFile;
	}
	
	public void run() {
		this.create();
	}
	
	private void create() {				
		Config config = ConfigUtils.loadConfig(this.configFile);			
		this.scenario = (ScenarioImpl) ScenarioUtils.loadScenario(config);
								
		CreateNetwork networkCreator = new CreateNetwork();
		networkCreator.createNetwork(this.scenario, config);
				
		CreatePopulation populationCreator = new CreatePopulation();
		populationCreator.createPopulation(this.scenario, config);			
	}
}
