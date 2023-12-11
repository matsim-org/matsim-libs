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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.ReplanningConfigGroup.StrategySettings;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Injector;
import org.matsim.core.controler.NewControlerModule;
import org.matsim.core.controler.PrepareForMobsim;
import org.matsim.core.controler.PrepareForMobsimImpl;
import org.matsim.core.controler.PrepareForSim;
import org.matsim.core.controler.PrepareForSimImpl;
import org.matsim.core.controler.corelisteners.ControlerDefaultCoreListenersModule;
import org.matsim.core.events.EventsManagerModule;
import org.matsim.core.mobsim.DefaultMobsimModule;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.population.PersonUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.replanning.ReplanningContext;
import org.matsim.core.replanning.StrategyManager;
import org.matsim.core.replanning.StrategyManagerModule;
import org.matsim.core.router.TripRouterModule;
import org.matsim.core.router.costcalculators.TravelDisutilityModule;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioByInstanceModule;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.scoring.StandaloneExperiencedPlansModule;
import org.matsim.core.scoring.functions.CharyparNagelScoringFunctionModule;
import org.matsim.core.trafficmonitoring.TravelTimeCalculatorModule;
import org.matsim.core.utils.timing.TimeInterpretationModule;
import org.matsim.testcases.MatsimTestUtils;

/**
 * @author mrieser
 */
public class ChangeTripModeIntegrationTest {

	@RegisterExtension
	private MatsimTestUtils utils = new MatsimTestUtils();


	@Test
	void testStrategyManagerConfigLoaderIntegration() {
		// setup config
		final Config config = utils.loadConfig((String)null);
		final MutableScenario scenario = (MutableScenario) ScenarioUtils.createScenario(config);
		final StrategySettings strategySettings = new StrategySettings(Id.create("1", StrategySettings.class));
		strategySettings.setStrategyName("ChangeTripMode");
		strategySettings.setWeight(1.0);
		config.replanning().addStrategySettings(strategySettings);
		//		config.setParam("changeMode", "modes", "car,walk");
		String[] str = {"car","walk"} ;
		config.changeMode().setModes(str);

		// setup network
		Network network = (Network) scenario.getNetwork();
		Node node1 = NetworkUtils.createAndAddNode(network, Id.create(1, Node.class), new Coord((double) 0, (double) 0));
		Node node2 = NetworkUtils.createAndAddNode(network, Id.create(2, Node.class), new Coord((double) 1000, (double) 0));
		final Node fromNode = node1;
		final Node toNode = node2;
		Link link = NetworkUtils.createAndAddLink(network,Id.create(1, Link.class), fromNode, toNode, (double) 1000, (double) 10, (double) 3600, (double) 1 );

		// setup population with one person
		Population population = scenario.getPopulation();
		Person person = PopulationUtils.getFactory().createPerson(Id.create(1, Person.class));
		population.addPerson(person);
		Plan plan = PersonUtils.createAndAddPlan(person, true);
		Activity act = PopulationUtils.createAndAddActivityFromCoord(plan, "home", new Coord(0, 0));
		act.setLinkId(link.getId());
		act.setEndTime(8.0 * 3600);
		PopulationUtils.createAndAddLeg( plan, TransportMode.car );
		act = PopulationUtils.createAndAddActivityFromCoord(plan, "work", new Coord((double) 0, (double) 500));
		act.setLinkId(link.getId());

		com.google.inject.Injector injector = Injector.createInjector(config, new AbstractModule() {
			@Override
			public void install() {
				install(new ScenarioByInstanceModule(scenario));
				install(new NewControlerModule());
				install(new ControlerDefaultCoreListenersModule());
				install(new StandaloneExperiencedPlansModule());
				install(new DefaultMobsimModule());
				install(new EventsManagerModule());
				install(new StrategyManagerModule());
				install(new CharyparNagelScoringFunctionModule());
				install(new TripRouterModule());
				install(new TravelTimeCalculatorModule());
				install(new TravelDisutilityModule());
				install(new TimeInterpretationModule());
				bind( PrepareForSim.class ).to( PrepareForSimImpl.class ) ;
				bind( PrepareForMobsim.class ).to( PrepareForMobsimImpl.class ) ;
			}
		});
		final StrategyManager manager = injector.getInstance(StrategyManager.class);
		manager.run(population, 0, injector.getInstance(ReplanningContext.class));

		// test that everything worked as expected
		assertEquals(2, person.getPlans().size(), "number of plans in person.");
		Plan newPlan = person.getSelectedPlan();
		Leg newLeg = (Leg) newPlan.getPlanElements().get(1);
		assertEquals(TransportMode.walk, newLeg.getMode());
		assertNotNull(newLeg.getRoute(), "the leg should now have a route.");
	}

}
