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

package playground.anhorni.utils;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigReader;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.config.ConfigUtils;

public class PlansSampler {
	private final static Logger log = Logger.getLogger(PlansSampler.class);	
	private Scenario scenario;
	private Population sampledPopulation;
	
	public static void main (final String[] args) { 
		PlansSampler sampler = new PlansSampler();
		sampler.init(args[0]);
		sampler.sample(Double.parseDouble(args[1]));
    }
	
	public void init(String configFile){
		Config config = new Config();
    	ConfigReader configReader = new ConfigReader(config);
    	configReader.readFile(configFile);
		this.scenario  = ScenarioUtils.loadScenario(ConfigUtils.createConfig());
	}
	
	public void sample(double sampleFraction) {
		this.sampledPopulation = this.sample(this.scenario.getPopulation(), sampleFraction);
	}

	public Population sample(Population plans, double sampleFraction) {
		log.info("Creating a " + sampleFraction * 100.0 + " sample");
		Population sampledPopulation = (ScenarioUtils.createScenario(ConfigUtils.createConfig())).getPopulation();

		for (Person person : plans.getPersons().values()) {
			double r = MatsimRandom.getRandom().nextDouble();
			if (r <= sampleFraction) {
				sampledPopulation.addPerson(person);
			}
		}
		log.info("Population size after sampling: " + sampledPopulation.getPersons().size());
		return sampledPopulation;
	}

	public Population getSampledPopulation() {
		return sampledPopulation;
	}

	public void setSampledPopulation(Population sampledPopulation) {
		this.sampledPopulation = sampledPopulation;
	}
}
