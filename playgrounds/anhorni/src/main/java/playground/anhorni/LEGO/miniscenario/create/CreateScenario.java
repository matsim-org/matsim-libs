/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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

package playground.anhorni.LEGO.miniscenario.create;

import org.apache.log4j.Logger;
import org.matsim.core.config.Config;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioLoaderImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.ConfigUtils;
import playground.anhorni.random.RandomFromVarDistr;

public class CreateScenario {

	private final static Logger log = Logger.getLogger(CreateScenario.class);
	private ScenarioImpl scenario = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());	
	private RandomFromVarDistr rnd;
	
	private long seed;
	private String outPath = "src/main/java/playground/anhorni/input/LEGO/";
	private String configFile;
	
	private final String LCEXP = "locationchoiceExperimental";
		
	public static void main(final String[] args) {
		CreateScenario scenarioCreator = new CreateScenario();	
		scenarioCreator.init(args[0]);
		scenarioCreator.run();			
		log.info("Scenario creation finished \n ----------------------------------------------------");
	}
	
	public void init(String configFile) {			
		rnd = new RandomFromVarDistr();
		this.configFile = configFile;
	}
	
	public void run() {
		this.create();
	}
	
	private void create() {			
		ScenarioImpl scenario = ScenarioLoaderImpl.createScenarioLoaderImplAndResetRandomSeed(configFile).getScenario();
		Config config = scenario.getConfig();
		
		this.seed = Long.parseLong(config.findParam(LCEXP, "randomSeed"));
		rnd.setSeed(this.seed);
		
		CreateNetwork networkCreator = new CreateNetwork();
		networkCreator.createNetwork(this.scenario, config);
				
		CreatePopulation populationCreator = new CreatePopulation();
		populationCreator.createPopulation(this.scenario, config, rnd);	
		log.info("Writing population ...");
		populationCreator.write(this.outPath);
		log.info("Writing network ...");
		networkCreator.write(this.outPath);
	}

	public void setOutPath(String outPath) {
		this.outPath = outPath;
	}
}
