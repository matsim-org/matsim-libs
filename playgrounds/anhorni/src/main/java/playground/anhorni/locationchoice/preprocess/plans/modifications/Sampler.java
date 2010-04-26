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

package playground.anhorni.locationchoice.preprocess.plans.modifications;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.gbl.MatsimRandom;

public class Sampler {

	private final static Logger log = Logger.getLogger(Sampler.class);

	public Population sample(Population plans) {
		Population sampledPopulation = new ScenarioImpl().getPopulation();

		for (Person person : plans.getPersons().values()) {
			double r = MatsimRandom.getRandom().nextDouble();

			if (r > 0.9) {
				sampledPopulation.addPerson(person);
			}
		}
		log.info("Population size after sampling: " + sampledPopulation.getPersons().size());
		return sampledPopulation;
	}

}
