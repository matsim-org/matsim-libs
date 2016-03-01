/*
 *  *********************************************************************** *
 *  * project: org.matsim.*
 *  * DefaultControlerModules.java
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  * copyright       : (C) 2014 by the members listed in the COPYING, *
 *  *                   LICENSE and WARRANTY file.                            *
 *  * email           : info at matsim dot org                                *
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  *   This program is free software; you can redistribute it and/or modify  *
 *  *   it under the terms of the GNU General Public License as published by  *
 *  *   the Free Software Foundation; either version 2 of the License, or     *
 *  *   (at your option) any later version.                                   *
 *  *   See also COPYING, LICENSE and WARRANTY file                           *
 *  *                                                                         *
 *  * ***********************************************************************
 */
package utils;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.scenario.ScenarioUtils;

/**
 * Class to modify a population file.
 * It removes all link information of activities and all routes.
 * 
 * @author tthunig
 */
public class ModifyPopulation {

	private static final String INPUT_BASE_DIR = "../../../shared-svn/projects/cottbus/data/scenarios/cottbus_scenario/";
	
	public static void main(String[] args) {
		
		Config config = ConfigUtils.createConfig();
		config.network().setInputFile(INPUT_BASE_DIR + "network_wgs84_utm33n.xml.gz");
		config.plans().setInputFile(INPUT_BASE_DIR + "cb_spn_gemeinde_nachfrage_landuse/"
				+ "commuter_population_wgs84_utm33n_car_only.xml.gz");
		
		Scenario scenario = ScenarioUtils.loadScenario(config);
		
		for (Person p : scenario.getPopulation().getPersons().values()) {
			Plan plan = p.getPlans().get(0);
			for (PlanElement pe : plan.getPlanElements()) {
				if (pe instanceof Activity) {
					// remove link
					((Activity) pe).setLinkId(null);
				}
				if (pe instanceof Leg) {
					// remove route
					((Leg) pe).setRoute(null);
				}
			}
		}
		
		new PopulationWriter(scenario.getPopulation()).write(INPUT_BASE_DIR + "cb_spn_gemeinde_nachfrage_landuse/"
				+ "commuter_population_wgs84_utm33n_car_only_woLinks.xml.gz");
	}

}
