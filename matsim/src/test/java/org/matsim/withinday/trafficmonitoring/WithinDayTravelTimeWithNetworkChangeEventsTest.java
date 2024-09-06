/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2017 by the members listed in the COPYING,        *
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

package org.matsim.withinday.trafficmonitoring;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.ControllerConfigGroup;
import org.matsim.core.config.groups.RoutingConfigGroup;
import org.matsim.core.config.groups.ScoringConfigGroup.ActivityParams;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.network.NetworkChangeEvent;
import org.matsim.core.network.NetworkChangeEvent.ChangeValue;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.population.routes.LinkNetworkRouteFactory;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.testcases.MatsimTestUtils;

/**
* Tests if network change events are considered by {@link WithinDayTravelTime}.
*
* @author ikaddoura
*
*/
public class WithinDayTravelTimeWithNetworkChangeEventsTest {

	@RegisterExtension
	private MatsimTestUtils testUtils = new MatsimTestUtils();

	private Id<Link> link01 = Id.createLinkId("link_0_1");
	private Id<Link> link12 = Id.createLinkId("link_1_2");
	private Id<Link> link23 = Id.createLinkId("link_2_3");

	@Test
	final void testTTviaMobSimAfterSimStepListener() {

		String outputDirectory = testUtils.getOutputDirectory() + "output_TTviaMobsimAfterSimStepListener/";

		final Config config = ConfigUtils.createConfig();
		config.routing().setNetworkRouteConsistencyCheck(RoutingConfigGroup.NetworkRouteConsistencyCheck.disable);

		config.controller().setFirstIteration(0);
		config.controller().setLastIteration(0);
		config.controller().setOutputDirectory(outputDirectory);
		config.controller().setRoutingAlgorithmType( ControllerConfigGroup.RoutingAlgorithmType.Dijkstra );

		config.qsim().setStartTime(6. * 3600.);
		config.qsim().setEndTime(11 * 3600.);

		ActivityParams paramsA = new ActivityParams();
		paramsA.setActivityType("home");
		paramsA.setTypicalDuration(1234.);
		config.scoring().addActivityParams(paramsA);

		ActivityParams paramsB = new ActivityParams();
		paramsB.setActivityType("work");
		paramsB.setTypicalDuration(1234.);
		config.scoring().addActivityParams(paramsB);

		config.network().setTimeVariantNetwork(true);

		final Scenario scenario = ScenarioUtils.createScenario(config);
		createNetwork(scenario);
		createPopulation(scenario);

		NetworkChangeEvent nce = new NetworkChangeEvent(10. * 3600.);
		nce.addLink(scenario.getNetwork().getLinks().get(link12));
		nce.setFreespeedChange(new ChangeValue(NetworkChangeEvent.ChangeType.ABSOLUTE_IN_SI_UNITS, 1.));
		NetworkUtils.addNetworkChangeEvent(scenario.getNetwork(), nce);

		final Controler controler = new Controler(scenario);
		controler.getConfig().controller().setOverwriteFileSetting(OverwriteFileSetting.overwriteExistingFiles);

		Set<String> analyzedModes = new HashSet<>();
		analyzedModes.add(TransportMode.car);
		final WithinDayTravelTime travelTime = new WithinDayTravelTime(controler.getScenario(), analyzedModes);

		final TtmobsimListener ttmobsimListener = new TtmobsimListener(nce);

		controler.addOverridingModule( new AbstractModule() {
			@Override public void install() {

				this.bind(TravelTime.class).toInstance(travelTime);
				this.addEventHandlerBinding().toInstance(travelTime);
				this.addMobsimListenerBinding().toInstance(travelTime);

				this.addMobsimListenerBinding().toInstance(ttmobsimListener);

			}
		}) ;

		controler.run();

		Assertions.assertEquals(true, ttmobsimListener.isCase1());
		Assertions.assertEquals(true, ttmobsimListener.isCase2());

	}

	private void createPopulation(Scenario scenario) {
		Population population = scenario.getPopulation();
        PopulationFactory popFactory = scenario.getPopulation().getFactory();
		LinkNetworkRouteFactory routeFactory = new LinkNetworkRouteFactory();

		Leg leg = popFactory.createLeg("car");
		List<Id<Link>> linkIds = new ArrayList<Id<Link>>();
		linkIds.add(link12);

		NetworkRoute route = (NetworkRoute) routeFactory.createRoute(link01, link23);
		route.setLinkIds(link01, linkIds, link23);
		leg.setRoute(route);

		Person person0 = popFactory.createPerson(Id.createPersonId("person0"));
		{
			Plan plan0 = popFactory.createPlan();
			Activity homeAct0 = popFactory.createActivityFromLinkId("home", link01);
			Activity workAct0 = popFactory.createActivityFromLinkId("work", link23);
			homeAct0.setEndTime(8 * 3600.);
			plan0.addActivity(homeAct0);
			plan0.addLeg(leg);
			plan0.addActivity(workAct0);
			person0.addPlan(plan0);
		}


		Person person1 = popFactory.createPerson(Id.createPersonId("person1"));
		{
			Plan plan1 = popFactory.createPlan();
			Activity homeAct1 = popFactory.createActivityFromLinkId("home", link01);
			Activity workAct1 = popFactory.createActivityFromLinkId("work", link23);
			homeAct1.setEndTime(12 * 3600.);
			plan1.addActivity(homeAct1);
			plan1.addLeg(leg);
			plan1.addActivity(workAct1);
			person1.addPlan(plan1);
		}

		population.addPerson(person0);
		population.addPerson(person1);
	}

	private void createNetwork(Scenario scenario) {

		Network network = scenario.getNetwork();
		network.setEffectiveCellSize(7.5);
		network.setCapacityPeriod(3600.);

		Node node0 = network.getFactory().createNode(Id.create("0", Node.class), new Coord(0., 0.));
		Node node1 = network.getFactory().createNode(Id.create("1", Node.class), new Coord(1000., 0.));
		Node node2 = network.getFactory().createNode(Id.create("2", Node.class), new Coord(2000., 0.));
		Node node3 = network.getFactory().createNode(Id.create("3", Node.class), new Coord(3000., 0.));

		Link link1 = network.getFactory().createLink(link01 , node0, node1);
		Link link2 = network.getFactory().createLink(link12, node1, node2);
		Link link3 = network.getFactory().createLink(link23, node2, node3);

		Set<String> modes = new HashSet<String>();
		modes.add("car");

		link1.setAllowedModes(modes);
		link1.setCapacity(7200);
		link1.setFreespeed(10.123);
		link1.setNumberOfLanes(2);
		link1.setLength(1000);

		link2.setAllowedModes(modes);
		link2.setCapacity(7200);
		link2.setFreespeed(10.123);
		link2.setNumberOfLanes(2);
		link2.setLength(1000);

		link3.setAllowedModes(modes);
		link3.setCapacity(7200);
		link3.setFreespeed(10.123);
		link3.setNumberOfLanes(2);
		link3.setLength(1000);

		network.addNode(node0);
		network.addNode(node1);
		network.addNode(node2);
		network.addNode(node3);

		network.addLink(link1);
		network.addLink(link2);
		network.addLink(link3);
	}
}

