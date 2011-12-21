/* *********************************************************************** *
 * project: org.matsim.*
 * LogPopStats.java
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
package playground.thibautd.utils;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;

/**
 * Simple executable which loads a scenario and prints statistics about the population
 *
 * for the moment, only says if a mono activity plan was found.
 *
 * @author thibautd
 */
public class LogPopStats {
	private static final Logger log =
		Logger.getLogger(LogPopStats.class);

	public static void main(final String[] args) {
		Config config = ConfigUtils.loadConfig( args[ 0 ] );
		Scenario scen = ScenarioUtils.loadScenario( config );

		for (Person pers : scen.getPopulation().getPersons().values()) {
			for (Plan plan : pers.getPlans()) {
				if (plan.getPlanElements().size() == 1) {
					log.info( "mono activity plan found" );
					return;
				}
			}
		}

		log.info( "NO mono activity plan found" );
	}
}

