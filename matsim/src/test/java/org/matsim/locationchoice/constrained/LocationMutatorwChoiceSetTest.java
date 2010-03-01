/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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

package org.matsim.locationchoice.constrained;

import java.util.List;

import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.locationchoice.Initializer;
import org.matsim.testcases.MatsimTestCase;

public class LocationMutatorwChoiceSetTest  extends MatsimTestCase {

	private LocationMutatorwChoiceSet initialize() {
		Initializer initializer = new Initializer();
		initializer.init(this);
		return new LocationMutatorwChoiceSet(initializer.getControler().getNetwork(),
				initializer.getControler(), initializer.getControler().getScenario().getKnowledges());
	}

	public void testConstructor() {
		LocationMutatorwChoiceSet locationmutator = this.initialize();
		assertEquals(locationmutator.getMaxRecursions(), 10);
		assertEquals(locationmutator.getRecursionTravelSpeedChange(), 0.1, EPSILON);
	}


	public void testHandlePlan() {
		LocationMutatorwChoiceSet locationmutator = this.initialize();
		Plan plan = locationmutator.getControler().getPopulation().getPersons().get(new IdImpl("1")).getSelectedPlan();
		locationmutator.handlePlan(plan);
		assertEquals(((PlanImpl) plan).getFirstActivity().getCoord().getX(), -25000.0, EPSILON);
		assertEquals(((PlanImpl) plan).getNextLeg(((PlanImpl) plan).getFirstActivity()).getRoute(), null);
	}

	public void testCalcActChains() {
		LocationMutatorwChoiceSet locationmutator = this.initialize();
		Plan plan = locationmutator.getControler().getPopulation().getPersons().get(new IdImpl("1")).getSelectedPlan();
		List<SubChain> list = locationmutator.calcActChains(plan);
		assertEquals(list.size(), 1);
	}
}