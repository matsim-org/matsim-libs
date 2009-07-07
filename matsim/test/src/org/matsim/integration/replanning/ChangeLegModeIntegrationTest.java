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

import org.matsim.api.basic.v01.TransportMode;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.StrategyConfigGroup.StrategySettings;
import org.matsim.core.controler.Controler;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.network.NodeImpl;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.population.PopulationImpl;
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
		final StrategySettings strategySettings = new StrategySettings(new IdImpl("1"));
		strategySettings.setModuleName("ChangeLegMode");
		strategySettings.setProbability(1.0);
		config.strategy().addStrategySettings(strategySettings);
		config.setParam("changeLegMode", "modes", "car,walk");

		// setup network
		NetworkLayer network = new NetworkLayer();
		NodeImpl node1 = network.createNode(new IdImpl(1), new CoordImpl(0, 0));
		NodeImpl node2 = network.createNode(new IdImpl(2), new CoordImpl(1000, 0));
		LinkImpl link = network.createLink(new IdImpl(1), node1, node2, 1000, 10, 3600, 1);

		// setup population with one person
		PopulationImpl population = new PopulationImpl();
		PersonImpl person = new PersonImpl(new IdImpl(1));
		population.getPersons().put(person.getId(), person);
		PlanImpl plan = person.createPlan(true);
		ActivityImpl act = plan.createActivity("home", new CoordImpl(0, 0));
		act.setLink(link);
		act.setEndTime(8.0 * 3600);
		plan.createLeg(TransportMode.car);
		act = plan.createActivity("work", new CoordImpl(0, 500));
		act.setLink(link);

		// setup strategy manager and load from config
		Controler controler = new Controler(config, network, population);
//		controler.setFreespeedTravelTimeCost(new FreespeedTravelTimeCost());
		controler.setLeastCostPathCalculatorFactory(new DijkstraFactory());
		final StrategyManager manager = new StrategyManager();
		StrategyManagerConfigLoader.load(controler, config, manager);
		manager.run(population);

		// test that everything worked as expected
		assertEquals("number of plans in person.", 2, person.getPlans().size());
		PlanImpl newPlan = person.getSelectedPlan();
		LegImpl newLeg = (LegImpl) newPlan.getPlanElements().get(1);
		assertEquals(TransportMode.walk, newLeg.getMode());
		assertNotNull("the leg should now have a route.", newLeg.getRoute());
	}

}
