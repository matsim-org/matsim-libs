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
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;

public class PlansRemoverById {
	private final static Logger log = Logger.getLogger(PlansRemoverById.class);	

	public Population remove(Population plans, Id<Person> maxId) {
		
		Population cleanedPopulation = (ScenarioUtils.createScenario(ConfigUtils.createConfig())).getPopulation();
		log.info("Population size before removal: " + plans.getPersons().size());
		for (Person person : plans.getPersons().values()) {
			// compareTo correct?
			if (Integer.parseInt(person.getId().toString()) < Integer.parseInt(maxId.toString())) {
				cleanedPopulation.addPerson(person);
			}
		}
		log.info("Population size after removal: " + cleanedPopulation.getPersons().size());
		return cleanedPopulation;
	}
}
