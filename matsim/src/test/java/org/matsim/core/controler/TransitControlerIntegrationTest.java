/* *********************************************************************** *
 * project: org.matsim.*
 * TransitControlerIntegrationTest.java
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

package org.matsim.core.controler;

import java.util.ArrayList;
import java.util.Collections;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.RoutingConfigGroup;
import org.matsim.core.config.groups.ScoringConfigGroup.ActivityParams;
import org.matsim.core.config.groups.ReplanningConfigGroup.StrategySettings;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.routes.DefaultTransitPassengerRoute;
import org.matsim.pt.routes.TransitPassengerRoute;
import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitScheduleFactory;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.matsim.pt.utils.CreateVehiclesForSchedule;
import org.matsim.testcases.MatsimTestUtils;

public class TransitControlerIntegrationTest {

	@RegisterExtension
	private MatsimTestUtils utils = new MatsimTestUtils();


	@Test
	void testTransitRouteCopy() {
		Config config = utils.loadConfig((String)null);
		config.routing().setNetworkRouteConsistencyCheck(RoutingConfigGroup.NetworkRouteConsistencyCheck.disable);
		config.transit().setUseTransit(true);
		MutableScenario scenario = (MutableScenario) ScenarioUtils.createScenario(config);

		Id<Node> nodeId1 = Id.create("1", Node.class);
		Id<Node> nodeId2 = Id.create("2", Node.class);
		Id<Node> nodeId3 = Id.create("3", Node.class);
		Id<Link> linkId1 = Id.create("1", Link.class);
		Id<Link> linkId2 = Id.create("2", Link.class);

		// build network
		Network network = scenario.getNetwork();
		NetworkFactory nBuilder = network.getFactory();
		Node node1 = nBuilder.createNode(nodeId1, new Coord((double) 0, (double) 0));
		Node node2 = nBuilder.createNode(nodeId2, new Coord((double) 1000, (double) 0));
		Node node3 = nBuilder.createNode(nodeId3, new Coord((double) 2000, (double) 0));
		network.addNode(node1);
		network.addNode(node2);
		network.addNode(node3);
		Link link1 = nBuilder.createLink(linkId1, node1, node2);
		Link link2 = nBuilder.createLink(linkId2, node2, node3);
		network.addLink(link1);
		network.addLink(link2);

		// build schedule
		TransitSchedule schedule = scenario.getTransitSchedule();
		TransitScheduleFactory sBuilder = schedule.getFactory();

		TransitStopFacility stopF1 = sBuilder.createTransitStopFacility(Id.create("1", TransitStopFacility.class), new Coord(1000.0, (double) 0), false);
		TransitStopFacility stopF2 = sBuilder.createTransitStopFacility(Id.create("2", TransitStopFacility.class), new Coord(2000.0, (double) 0), false);
		stopF1.setLinkId(link1.getId());
		stopF2.setLinkId(link2.getId());
		schedule.addStopFacility(stopF1);
		schedule.addStopFacility(stopF2);

		TransitLine tLine1 = sBuilder.createTransitLine(Id.create("1", TransitLine.class));

		TransitRouteStop stop1 = sBuilder.createTransitRouteStop(stopF1, 0, 0);
		TransitRouteStop stop2 = sBuilder.createTransitRouteStop(stopF2, 100, 100);
		ArrayList<TransitRouteStop> stops = new ArrayList<TransitRouteStop>();
		stops.add(stop1);
		stops.add(stop2);

		NetworkRoute netRoute = RouteUtils.createLinkNetworkRouteImpl(link1.getId(), link2.getId());
		netRoute.setLinkIds(link1.getId(), Collections.<Id<Link>>emptyList(), link2.getId());
		TransitRoute tRoute1 = sBuilder.createTransitRoute(Id.create("1", TransitRoute.class), netRoute, stops, "bus");

		tRoute1.addDeparture(sBuilder.createDeparture(Id.create("1", Departure.class), 7.0*3600));
		tLine1.addRoute(tRoute1);
		schedule.addTransitLine(tLine1);

		// build vehicles
		new CreateVehiclesForSchedule(schedule, scenario.getTransitVehicles()).run();

		// build population
		Population population = scenario.getPopulation();
		PopulationFactory pBuilder = population.getFactory();
		Person person1 = pBuilder.createPerson(Id.create("1", Person.class));
		Plan plan = pBuilder.createPlan();
		Activity homeAct = pBuilder.createActivityFromLinkId("h", linkId1);
		homeAct.setEndTime(7.0*3600);
		plan.addActivity(homeAct);
		Leg leg = pBuilder.createLeg(TransportMode.pt);
		TransitPassengerRoute tRoute = new DefaultTransitPassengerRoute(stopF1, tLine1, tRoute1, stopF2);
		leg.setRoute(tRoute);
		plan.addLeg(leg);
		plan.addActivity(pBuilder.createActivityFromLinkId("w", linkId2));
		person1.addPlan(plan);
		population.addPerson(person1);

		// prepare config
		config.controller().setFirstIteration(0);
		config.controller().setLastIteration(1);

		ActivityParams params = new ActivityParams("h");
		params.setTypicalDuration(16.0*3600);
		config.scoring().addActivityParams(params);
		params = new ActivityParams("w");
		params.setTypicalDuration(8.0*3600);
		config.scoring().addActivityParams(params);

		StrategySettings tam = new StrategySettings(Id.create(1, StrategySettings.class));
		tam.setStrategyName("TimeAllocationMutator");
		tam.setWeight(1.0);
		config.replanning().addStrategySettings(tam);

		// run
		Controler controler = new Controler(scenario);
		controler.getConfig().controller().setWriteEventsInterval(0);
		controler.getConfig().controller().setCreateGraphs(false);
		controler.run();

		// checks
		assertEquals(1, population.getPersons().size());
		assertEquals(2, person1.getPlans().size());
		assertEquals(DefaultTransitPassengerRoute.class, ((Leg) person1.getPlans().get(0).getPlanElements().get(1)).getRoute().getClass());
		assertEquals(DefaultTransitPassengerRoute.class, ((Leg) person1.getPlans().get(1).getPlanElements().get(1)).getRoute().getClass());
	}
}
