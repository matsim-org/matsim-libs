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

package org.matsim.contrib.locationchoice.timegeography;

import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.contrib.locationchoice.Initializer;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.utils.timing.TimeInterpretation;
import org.matsim.testcases.MatsimTestUtils;

public class LocationMutatorwChoiceSetTest  {

	@RegisterExtension
	private MatsimTestUtils utils = new MatsimTestUtils();


	private MutableScenario scenario;

	private RecursiveLocationMutator initialize() {
		Initializer initializer = new Initializer();
		initializer.init(utils);
		scenario = (MutableScenario) initializer.getControler().getScenario();
		return new RecursiveLocationMutator(scenario, initializer.getControler().getTripRouterProvider().get(), TimeInterpretation.create(initializer.getControler().getScenario().getConfig()), new Random(4711));
	}

	@Test
	void testConstructor() {
		RecursiveLocationMutator locationmutator = this.initialize();
		assertEquals(locationmutator.getMaxRecursions(), 10);
		assertEquals(locationmutator.getRecursionTravelSpeedChange(), 0.1, MatsimTestUtils.EPSILON);
	}


	@Test
	void testHandlePlan() {
		RecursiveLocationMutator locationmutator = this.initialize();
		Plan plan = scenario.getPopulation().getPersons().get(Id.create("1", Person.class)).getSelectedPlan();
		locationmutator.run(plan);
		assertEquals(PopulationUtils.getFirstActivity( ((Plan) plan) ).getCoord().getX(), -25000.0, MatsimTestUtils.EPSILON);
		assertEquals(PopulationUtils.getNextLeg(((Plan) plan), PopulationUtils.getFirstActivity( ((Plan) plan) )).getRoute(), null);
	}

	@Test
	void testCalcActChains() {
		RecursiveLocationMutator locationmutator = this.initialize();
		Plan plan = scenario.getPopulation().getPersons().get(Id.create("1", Person.class)).getSelectedPlan();
		List<SubChain> list = locationmutator.calcActChains(plan);
		assertEquals(list.size(), 1);
	}
}
