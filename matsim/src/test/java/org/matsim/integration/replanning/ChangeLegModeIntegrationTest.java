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

import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.StrategyConfigGroup.StrategySettings;
import org.matsim.core.controler.Controler;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.replanning.StrategyManager;
import org.matsim.core.replanning.StrategyManagerConfigLoader;
import org.matsim.core.router.util.DijkstraFactory;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.testcases.MatsimTestCase;

/**
 * @author mrieser
 */
public class ChangeLegModeIntegrationTest extends MatsimTestCase {

	public void testStrategyManagerConfigLoaderIntegration() {
		// setup config
		final Config config = loadConfig(null);
		ScenarioImpl scenario = new ScenarioImpl(config);
		final StrategySettings strategySettings = new StrategySettings(new IdImpl("1"));
		strategySettings.setModuleName("ChangeLegMode");
		strategySettings.setProbability(1.0);
		config.strategy().addStrategySettings(strategySettings);
		config.setParam("changeLegMode", "modes", "car,walk");

		// setup network
		NetworkLayer network = scenario.getNetwork();
		Node node1 = network.createAndAddNode(new IdImpl(1), new CoordImpl(0, 0));
		Node node2 = network.createAndAddNode(new IdImpl(2), new CoordImpl(1000, 0));
		Link link = network.createAndAddLink(new IdImpl(1), node1, node2, 1000, 10, 3600, 1);

		// setup population with one person
		PopulationImpl population = scenario.getPopulation();
		PersonImpl person = new PersonImpl(new IdImpl(1));
		population.addPerson(person);
		PlanImpl plan = person.createAndAddPlan(true);
		ActivityImpl act = plan.createAndAddActivity("home", new CoordImpl(0, 0));
		act.setLinkId(link.getId());
		act.setEndTime(8.0 * 3600);
		plan.createAndAddLeg(TransportMode.car);
		act = plan.createAndAddActivity("work", new CoordImpl(0, 500));
		act.setLinkId(link.getId());

		// setup strategy manager and load from config
		Controler controler = new Controler(scenario);
//		controler.setFreespeedTravelTimeCost(new FreespeedTravelTimeCost());
		controler.setLeastCostPathCalculatorFactory(new DijkstraFactory());
		final StrategyManager manager = new StrategyManager();
		StrategyManagerConfigLoader.load(controler, manager);
		manager.run(population);

		// test that everything worked as expected
		assertEquals("number of plans in person.", 2, person.getPlans().size());
		Plan newPlan = person.getSelectedPlan();
		LegImpl newLeg = (LegImpl) newPlan.getPlanElements().get(1);
		assertEquals(TransportMode.walk, newLeg.getMode());
		assertNotNull("the leg should now have a route.", newLeg.getRoute());
	}

}
