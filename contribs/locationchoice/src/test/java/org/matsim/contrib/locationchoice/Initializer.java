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

package org.matsim.contrib.locationchoice;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.MatsimServices;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.population.algorithms.PersonCalcTimes;
import org.matsim.core.population.algorithms.PersonPrepareForSim;
import org.matsim.core.population.algorithms.PlanAlgorithm;
import org.matsim.core.router.PlanRouter;
import org.matsim.core.router.TripRouterFactoryBuilderWithDefaults;
import org.matsim.core.router.costcalculators.FreespeedTravelTimeAndDisutility;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.timing.TimeInterpretation;
import org.matsim.testcases.MatsimTestUtils;

public class Initializer {

	private Controler controler;

	public void init(MatsimTestUtils utils) {
		// lnk does not work. get path to locationchcoice
		String path = utils.getPackageInputDirectory() + "config.xml";

		Config config = ConfigUtils.loadConfig(path, new DestinationChoiceConfigGroup());

		//Config config = testCase.loadConfig(path);
		Scenario scenario = ScenarioUtils.loadScenario(config);
		preparePlans(scenario);

		this.controler = new Controler(scenario);
		this.controler.getConfig().controller().setCreateGraphs(false);
		this.controler.getConfig().controller().setWriteEventsInterval(0); // disables events-writing
		this.controler.getConfig()
				.controller()
				.setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles);
		this.controler.run();
	}

	public MatsimServices getControler() {
		return controler;
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

		final FreespeedTravelTimeAndDisutility timeCostCalc = new FreespeedTravelTimeAndDisutility(
				scenario.getConfig().scoring());
		PlanAlgorithm router = new PlanRouter(new TripRouterFactoryBuilderWithDefaults().build(scenario).get(), TimeInterpretation.create(scenario.getConfig()));
		PersonPrepareForSim pp4s = new PersonPrepareForSim(router, scenario);
		scenario.getPopulation().getPersons().values().forEach(pp4s::run);

		scenario.getPopulation().getPersons().values().forEach(new PersonCalcTimes()::run);

//		//a bit hacky way of setting endTimes for activities
//		scenario.getPopulation().getPersons().values().stream().map(HasPlansAndId::getSelectedPlan).forEach(plan -> {
//			for (int i = 0; i < plan.getPlanElements().size(); i++) {
//				PlanElement planElement = plan.getPlanElements().get(i);
//				if (planElement instanceof Leg) {
//					Leg leg = (Leg)planElement;
//					Activity previousActivity = (Activity)plan.getPlanElements().get(i - 1);
//					previousActivity.setEndTime(leg.getDepartureTime().seconds());
//				}
//			}
//		});

	}
}
