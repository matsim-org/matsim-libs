/* *********************************************************************** *
 * project: org.matsim.*
 * ChangeLegModeIntegration.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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

package org.matsim.integration.replanning;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.StrategyConfigGroup.StrategySettings;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.ControlerI;
import org.matsim.core.controler.Injector;
import org.matsim.core.events.EventsManagerModule;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.population.*;
import org.matsim.core.replanning.ReplanningContext;
import org.matsim.core.replanning.StrategyManager;
import org.matsim.core.replanning.StrategyManagerModule;
import org.matsim.core.router.TripRouterModule;
import org.matsim.core.router.costcalculators.TravelDisutilityModule;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioByInstanceModule;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.scoring.functions.CharyparNagelScoringFunctionModule;
import org.matsim.core.trafficmonitoring.TravelTimeCalculatorModule;
import org.matsim.testcases.MatsimTestCase;

/**
 * @author mrieser
 */
public class ChangeTripModeIntegrationTest extends MatsimTestCase {

	public void testStrategyManagerConfigLoaderIntegration() {
		// setup config
		final Config config = loadConfig(null);
		final MutableScenario scenario = (MutableScenario) ScenarioUtils.createScenario(config);
		final StrategySettings strategySettings = new StrategySettings(Id.create("1", StrategySettings.class));
		strategySettings.setStrategyName("ChangeTripMode");
		strategySettings.setWeight(1.0);
		config.strategy().addStrategySettings(strategySettings);
		config.setParam("changeLegMode", "modes", "car,walk");

		// setup network
		NetworkImpl network = (NetworkImpl) scenario.getNetwork();
        Node node1 = network.createAndAddNode(Id.create(1, Node.class), new Coord((double) 0, (double) 0));
        Node node2 = network.createAndAddNode(Id.create(2, Node.class), new Coord((double) 1000, (double) 0));
		Link link = network.createAndAddLink(Id.create(1, Link.class), node1, node2, 1000, 10, 3600, 1);

		// setup population with one person
		Population population = scenario.getPopulation();
		Person person = PopulationUtils.createPerson(Id.create(1, Person.class));
		population.addPerson(person);
		PlanImpl plan = PersonUtils.createAndAddPlan(person, true);
		ActivityImpl act = plan.createAndAddActivity("home", new Coord(0, 0));
		act.setLinkId(link.getId());
		act.setEndTime(8.0 * 3600);
		plan.createAndAddLeg(TransportMode.car);
        act = plan.createAndAddActivity("work", new Coord((double) 0, (double) 500));
		act.setLinkId(link.getId());

        com.google.inject.Injector injector = Injector.createInjector(config, new AbstractModule() {
            @Override
            public void install() {
                install(new ScenarioByInstanceModule(scenario));
                install(new EventsManagerModule());
                install(new StrategyManagerModule());
				install(new CharyparNagelScoringFunctionModule());
                install(new TripRouterModule());
				install(new TravelTimeCalculatorModule());
				install(new TravelDisutilityModule());
            }
        });
		final StrategyManager manager = injector.getInstance(StrategyManager.class);
		manager.run(population, injector.getInstance(ReplanningContext.class));

		// test that everything worked as expected
		assertEquals("number of plans in person.", 2, person.getPlans().size());
		Plan newPlan = person.getSelectedPlan();
		LegImpl newLeg = (LegImpl) newPlan.getPlanElements().get(1);
		assertEquals(TransportMode.walk, newLeg.getMode());
		assertNotNull("the leg should now have a route.", newLeg.getRoute());
	}

}
