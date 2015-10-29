/* *********************************************************************** *
 * project: org.matsim.*
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
package org.matsim.contrib.minibus.hook;

import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.population.algorithms.PlanAlgorithm;

/**
 * @author droeder
 *
 */
class PersonReRouteStuckFactoryImpl implements PersonReRouteStuckFactory {

	@SuppressWarnings("unused")
	private static final Logger log = Logger.getLogger(PersonReRouteStuckFactoryImpl.class);

	public PersonReRouteStuckFactoryImpl() {
		
	}

	@Override
	public AbstractPersonReRouteStuck getReRouteStuck(final PlanAlgorithm router, final MutableScenario scenario, Set<Id<Person>> agentsStuck) {
		return new PersonReRouteStuck(router, scenario, agentsStuck);
	}
}

