/*
 *  *********************************************************************** *
 *  * project: org.matsim.*
 *  * ExampleWithinDayControllerTest.java
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  * copyright       : (C) 2015 by the members listed in the COPYING, *
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

package org.matsim.withinday.controller;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.ControllerConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.population.algorithms.PersonPrepareForSim;
import org.matsim.core.population.algorithms.PlanAlgorithm;
import org.matsim.core.router.PlanRouter;
import org.matsim.core.router.TripRouterFactoryBuilderWithDefaults;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.timing.TimeInterpretation;
import org.matsim.testcases.MatsimTestUtils;

public class ExampleWithinDayControllerTest {

	@RegisterExtension
	private MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	void testRun() {
		Config config = utils.loadConfig("test/scenarios/equil/config.xml");
		config.controller().setLastIteration(1);
		config.controller().setRoutingAlgorithmType(ControllerConfigGroup.RoutingAlgorithmType.Dijkstra);
		Scenario scenario = ScenarioUtils.loadScenario(config);
		preparePlans(scenario);
		final Controler controler = new Controler(scenario);
		ExampleWithinDayController.configure(controler);
		controler.run();
	}

	private static void preparePlans(Scenario scenario) {
		//plans are missing departure times, so clear all routes to re-route all legs and provide some departure times
		scenario.getPopulation()
				.getPersons()
				.values()
				.stream()
				.flatMap(p -> p.getSelectedPlan().getPlanElements().stream())
				.filter(Leg.class::isInstance)
				.forEach(planElement -> ((Leg)planElement).setRoute(null));

		PlanAlgorithm router = new PlanRouter(new TripRouterFactoryBuilderWithDefaults().build(scenario).get(), TimeInterpretation.create(scenario.getConfig()));
		PersonPrepareForSim pp4s = new PersonPrepareForSim(router, scenario);
		scenario.getPopulation().getPersons().values().forEach(pp4s::run);
		// yyyyyy According to specs, these 3 lines should not be necessary: Having no route should just trigger the re-routing. Commenting
		// out these 3 lines indeed triggers the standard re-routing, but it fails with issues in the time computation. Presumably, this
		// points to some problem there, and should thus be fixed.  kai, mar'21

		// Once that is fixed, this whole "preparePlans" method might not be necessary any more.  kai, mar'21

	}

}
