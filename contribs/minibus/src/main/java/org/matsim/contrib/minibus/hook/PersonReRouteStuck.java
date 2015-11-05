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

package org.matsim.contrib.minibus.hook;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.population.algorithms.PlanAlgorithm;
import org.matsim.pt.router.TransitActsRemover;

import java.util.Set;

/**
 * ReRoutes every person from a given set of person ids
 *
 * @author aneumann
 */
final class PersonReRouteStuck extends AbstractPersonReRouteStuck {


	private static final Logger log = Logger.getLogger(PersonReRouteStuck.class);
	
	private final TransitActsRemover transitActsRemover;

	public PersonReRouteStuck(final PlanAlgorithm router, final MutableScenario scenario, Set<Id<Person>> agentsStuck) {
		super(router, scenario, agentsStuck);
		this.transitActsRemover = new TransitActsRemover(); 
	}
	
	@Override
	public void run(final Person person) {
		Plan selectedPlan = person.getSelectedPlan();
		if (selectedPlan == null) {
			// the only way no plan can be selected should be when the person has no plans at all
			log.warn("Person " + person.getId() + " has no plans!");
			return;
		}
		
		if(this.agentsStuck.contains(person.getId())){
			this.transitActsRemover.run(selectedPlan);
			this.router.run(selectedPlan);
		}
	}
}