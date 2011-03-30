/* *********************************************************************** *
 * project: org.matsim.*
 * ActTypeAggregator.java
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
package playground.johannes.socialnetworks.utils;



import org.apache.log4j.Logger;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.contrib.sna.util.ProgressLogger;
import org.matsim.core.config.Config;
import org.matsim.core.config.MatsimConfigReader;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;

/**
 * @author illenberger
 *
 */
public class ActTypeAggregator {

	private static final String HOME_TYPE = "home";
	
	private static final String HOME_PREFIX = "h";
	
	private static final String WORK_TYPE = "work";
	
	private static final String WORK_PREFIX = "w";
	
	private static final String EDU_TYPE = "edu";
	
	private static final String EDU_PREFIX = "e";
	
	private static final String SHOP_TYPE = "shop";
	
	private static final String SHOP_PREFIX = "s";
	
	private static final String LEISURE_TYPE = "leisure";
	
	private static final String LEISURE_PREFIX = "l";
	
	private static final Logger logger = Logger.getLogger(ActTypeAggregator.class);
	
	public static void aggregate(Population population) {
		logger.info("Aggregating activity types...");
		ProgressLogger.init(population.getPersons().size(), 1, 5);
		
		for(Person person : population.getPersons().values()) {
			for(Plan plan : person.getPlans()) {
				for(int i = 0; i < plan.getPlanElements().size(); i += 2) {
					Activity act = (Activity) plan.getPlanElements().get(i);
					
					if(act.getType().startsWith(HOME_PREFIX)) {
						act.setType(HOME_TYPE);
					} else if(act.getType().startsWith(WORK_PREFIX)) {
						act.setType(WORK_TYPE);
					} else if(act.getType().startsWith(EDU_PREFIX)) {
						act.setType(EDU_TYPE);
					} else if(act.getType().startsWith(SHOP_PREFIX)) {
						act.setType(SHOP_TYPE);
					} else if(act.getType().startsWith(LEISURE_PREFIX)) {
						act.setType(LEISURE_TYPE);
					} else {
						logger.warn(String.format("Act type \"%1$s\" remains unchanged.", act.getType()));
					}
				}
			}
			
			ProgressLogger.step();
		}
		
		ProgressLogger.termiante();
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Config config = new Config();
		MatsimConfigReader creader = new MatsimConfigReader(config);
		creader.readFile(args[0]);
		ScenarioImpl scenario = (ScenarioImpl) ScenarioUtils.createScenario(config);
		
		ActTypeAggregator.aggregate(scenario.getPopulation());
		
		new PopulationWriter(scenario.getPopulation(), scenario.getNetwork()).write(scenario.getConfig().getParam("popfilter", "outputPlansFile"));
	}

}
