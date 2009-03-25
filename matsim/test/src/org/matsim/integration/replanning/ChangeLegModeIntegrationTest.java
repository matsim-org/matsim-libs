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

import org.matsim.api.basic.v01.population.BasicLeg;
import org.matsim.core.api.network.Link;
import org.matsim.core.api.network.Node;
import org.matsim.core.api.population.Activity;
import org.matsim.core.api.population.Leg;
import org.matsim.core.api.population.Person;
import org.matsim.core.api.population.Plan;
import org.matsim.core.api.population.Population;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.StrategyConfigGroup.StrategySettings;
import org.matsim.core.controler.Controler;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.replanning.StrategyManager;
import org.matsim.core.replanning.StrategyManagerConfigLoader;
import org.matsim.core.router.util.DijkstraFactory;
import org.matsim.population.PersonImpl;
import org.matsim.population.PopulationImpl;
import org.matsim.testcases.MatsimTestCase;
import org.matsim.utils.geometry.CoordImpl;

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
		Node node1 = network.createNode(new IdImpl(1), new CoordImpl(0, 0));
		Node node2 = network.createNode(new IdImpl(2), new CoordImpl(1000, 0));
		Link link = network.createLink(new IdImpl(1), node1, node2, 1000, 10, 3600, 1);

		// setup population with one person
		Population population = new PopulationImpl(PopulationImpl.NO_STREAMING);
		Person person = new PersonImpl(new IdImpl(1));
		population.addPerson(person);
		Plan plan = person.createPlan(true);
		Activity act = plan.createAct("home", new CoordImpl(0, 0));
		act.setLink(link);
		act.setEndTime(8.0 * 3600);
		plan.createLeg(BasicLeg.Mode.car);
		act = plan.createAct("work", new CoordImpl(0, 500));
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
		Plan newPlan = person.getSelectedPlan();
		Leg newLeg = (Leg) newPlan.getPlanElements().get(1);
		assertEquals(BasicLeg.Mode.walk, newLeg.getMode());
		assertNotNull("the leg should now have a route.", newLeg.getRoute());
	}

}
