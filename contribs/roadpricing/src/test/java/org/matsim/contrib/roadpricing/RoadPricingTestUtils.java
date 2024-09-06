/* *********************************************************************** *
 * project: org.matsim.*
 * RoadPricingTestUtils.java
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

package org.matsim.contrib.roadpricing;

import java.util.List;

import org.junit.jupiter.api.Assertions;
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
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.controler.PrepareForSimUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.mobsim.framework.Mobsim;
import org.matsim.core.mobsim.qsim.QSimBuilder;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.scoring.EventsToScore;
import org.matsim.core.scoring.functions.CharyparNagelScoringFunctionFactory;
import org.matsim.core.utils.misc.Time;


/**
 * Some static methods to set up the road pricing scenarios in the test cases.
 *
 * @author mrieser
 */
/*package*/ class RoadPricingTestUtils {

	private RoadPricingTestUtils() {
		// static class
	}

	/** Creates a simple network consisting of 5 equal links in a row. */
	static void createNetwork1(final MutableScenario scenario) {
		/* This creates the following network:
		 *
		 * (1)-------(2)-------(3)-------(4)-------(5)-------(6)
		 *       0         1         2         3         4
		 */
		/* The vehicles can travel with 18km/h = 5m/s, so it should take them 20 seconds
		 * to travel along one link.		 */
		Network network = scenario.getNetwork();
		network.setCapacityPeriod(Time.parseTime("01:00:00"));
		Node node1 = NetworkUtils.createAndAddNode(network, Id.create(1, Node.class), new Coord((double) 0, (double) 0));
		Node node2 = NetworkUtils.createAndAddNode(network, Id.create(2, Node.class), new Coord((double) 100, (double) 0));
		Node node3 = NetworkUtils.createAndAddNode(network, Id.create(3, Node.class), new Coord((double) 200, (double) 0));
		Node node4 = NetworkUtils.createAndAddNode(network, Id.create(4, Node.class), new Coord((double) 300, (double) 0));
		Node node5 = NetworkUtils.createAndAddNode(network, Id.create(5, Node.class), new Coord((double) 400, (double) 0));
		Node node6 = NetworkUtils.createAndAddNode(network, Id.create(6, Node.class), new Coord((double) 500, (double) 0));
		final Node fromNode = node1;
		final Node toNode = node2;

		// freespeed 18km/h = 5m/s --> 20s for 100m
		NetworkUtils.createAndAddLink(network,Id.create(0, Link.class), fromNode, toNode, (double) 100, (double) 5, (double) 100, (double) 1 );
		final Node fromNode1 = node2;
		final Node toNode1 = node3;
		NetworkUtils.createAndAddLink(network,Id.create(1, Link.class), fromNode1, toNode1, (double) 100, (double) 5, (double) 100, (double) 1 );
		final Node fromNode2 = node3;
		final Node toNode2 = node4;
		NetworkUtils.createAndAddLink(network,Id.create(2, Link.class), fromNode2, toNode2, (double) 100, (double) 5, (double) 100, (double) 1 );
		final Node fromNode3 = node4;
		final Node toNode3 = node5;
		NetworkUtils.createAndAddLink(network,Id.create(3, Link.class), fromNode3, toNode3, (double) 100, (double) 5, (double) 100, (double) 1 );
		final Node fromNode4 = node5;
		final Node toNode4 = node6;
		NetworkUtils.createAndAddLink(network,Id.create(4, Link.class), fromNode4, toNode4, (double) 100, (double) 5, (double) 100, (double) 1 );
	}

	/** Creates a simple network with route alternatives in 2 places. */
	static void createNetwork2(final MutableScenario scenario) {
		/* This creates the following network:
		 *
		 *            3 .----(3)----. 4
		 *             /             \
		 * (1)-------(2)--------------(4)-------(5)
		 *  |    2            5             6    |
		 *  |1                                   |
		 *  |                                    |
		 * (0)                                   |
		 *                                     7 |
		 * (11)                                  |
		 *  |                                    |
		 *  |13                                  |
		 *  |    12          11             8    |
		 * (10)------(9)--------------(7)-------(6)
		 *             \             /
		 *           10 `----(8)----' 9
		 *
		 * each link is 100m long and can be traveled along with 18km/h = 5m/s = 20s for 100m
		 */
		Network network = (Network) scenario.getNetwork();
		network.setCapacityPeriod(Time.parseTime("01:00:00"));
		Node node0 = NetworkUtils.createAndAddNode(network, Id.create( "0", Node.class), new Coord((double) 0, (double) 10));
		Node node1 = NetworkUtils.createAndAddNode(network, Id.create( "1", Node.class), new Coord((double) 0, (double) 100));
		Node node2 = NetworkUtils.createAndAddNode(network, Id.create( "2", Node.class), new Coord((double) 100, (double) 100));
		Node node3 = NetworkUtils.createAndAddNode(network, Id.create( "3", Node.class), new Coord((double) 150, (double) 150));
		Node node4 = NetworkUtils.createAndAddNode(network, Id.create( "4", Node.class), new Coord((double) 200, (double) 100));
		Node node5 = NetworkUtils.createAndAddNode(network, Id.create( "5", Node.class), new Coord((double) 300, (double) 100));
		final double y5 = -100;
		Node node6 = NetworkUtils.createAndAddNode(network, Id.create( "6", Node.class), new Coord((double) 300, y5));
		final double y4 = -100;
		Node node7 = NetworkUtils.createAndAddNode(network, Id.create( "7", Node.class), new Coord((double) 200, y4));
		final double y3 = -150;
		Node node8 = NetworkUtils.createAndAddNode(network, Id.create( "8", Node.class), new Coord((double) 150, y3));
		final double y2 = -100;
		Node node9 = NetworkUtils.createAndAddNode(network, Id.create( "9", Node.class), new Coord((double) 100, y2));
		final double y1 = -100;
		Node node10 =NetworkUtils.createAndAddNode(network, Id.create("10", Node.class), new Coord((double) 0, y1));
		final double y = -10;
		Node node11 =NetworkUtils.createAndAddNode(network, Id.create("11", Node.class), new Coord((double) 0, y));
		final Node fromNode = node0;
		final Node toNode = node1;
		NetworkUtils.createAndAddLink(network,Id.create( "1", Link.class), fromNode, toNode, (double) 100, (double) 5, (double) 100, (double) 1 );
		final Node fromNode1 = node1;
		final Node toNode1 = node2;
		NetworkUtils.createAndAddLink(network,Id.create( "2", Link.class), fromNode1, toNode1, (double) 100, (double) 5, (double) 100, (double) 1 );
		final Node fromNode2 = node2;
		final Node toNode2 = node3;
		NetworkUtils.createAndAddLink(network,Id.create( "3", Link.class), fromNode2, toNode2, (double) 100, (double) 5, (double) 100, (double) 1 );
		final Node fromNode3 = node3;
		final Node toNode3 = node4;
		NetworkUtils.createAndAddLink(network,Id.create( "4", Link.class), fromNode3, toNode3, (double) 100, (double) 5, (double) 100, (double) 1 );
		final Node fromNode4 = node2;
		final Node toNode4 = node4;
		NetworkUtils.createAndAddLink(network,Id.create( "5", Link.class), fromNode4, toNode4, (double) 100, (double) 5, (double) 100, (double) 1 );
		final Node fromNode5 = node4;
		final Node toNode5 = node5;
		NetworkUtils.createAndAddLink(network,Id.create( "6", Link.class), fromNode5, toNode5, (double) 100, (double) 5, (double) 100, (double) 1 );
		final Node fromNode6 = node5;
		final Node toNode6 = node6;
		NetworkUtils.createAndAddLink(network,Id.create( "7", Link.class), fromNode6, toNode6, (double) 100, (double) 5, (double) 100, (double) 1 );
		final Node fromNode7 = node6;
		final Node toNode7 = node7;
		NetworkUtils.createAndAddLink(network,Id.create( "8", Link.class), fromNode7, toNode7, (double) 100, (double) 5, (double) 100, (double) 1 );
		final Node fromNode8 = node7;
		final Node toNode8 = node8;
		NetworkUtils.createAndAddLink(network,Id.create( "9", Link.class), fromNode8, toNode8, (double) 100, (double) 5, (double) 100, (double) 1 );
		final Node fromNode9 = node8;
		final Node toNode9 = node9;
		NetworkUtils.createAndAddLink(network,Id.create("10", Link.class), fromNode9, toNode9, (double) 100, (double) 5, (double) 100, (double) 1 );
		final Node fromNode10 = node7;
		final Node toNode10 = node9;
		NetworkUtils.createAndAddLink(network,Id.create("11", Link.class), fromNode10, toNode10, (double) 100, (double) 5, (double) 100, (double) 1 );
		final Node fromNode11 = node9;
		final Node toNode11 = node10;
		NetworkUtils.createAndAddLink(network,Id.create("12", Link.class), fromNode11, toNode11, (double) 100, (double) 5, (double) 100, (double) 1 );
		final Node fromNode12 = node10;
		final Node toNode12 = node11;
		NetworkUtils.createAndAddLink(network,Id.create("13", Link.class), fromNode12, toNode12, (double) 100, (double) 5, (double) 100, (double) 1 );
	}

	/**
	 * Creates a population for network1
	 **/
	static void createPopulation1(final MutableScenario scenario) {
		Population population = scenario.getPopulation();
		Network network = (Network) scenario.getNetwork();

		Link link0 = network.getLinks().get(Id.create(0, Link.class));
		Link link1 = network.getLinks().get(Id.create(1, Link.class));
		Link link2 = network.getLinks().get(Id.create(2, Link.class));
		Link link3 = network.getLinks().get(Id.create(3, Link.class));
		Link link4 = network.getLinks().get(Id.create(4, Link.class));
		RoadPricingTestUtils.addPersonToPopulation(RoadPricingTestUtils.createPerson1( 1, "07:00"   , link0.getId(), NetworkUtils.getLinkIds("1 2 3"), link4.getId()), population); // toll in 1st time slot
		RoadPricingTestUtils.addPersonToPopulation(RoadPricingTestUtils.createPerson1( 2, "11:00"   , link0.getId(), NetworkUtils.getLinkIds("1 2 3"), link4.getId()), population); // toll in 2nd time slot
		RoadPricingTestUtils.addPersonToPopulation(RoadPricingTestUtils.createPerson1( 3, "16:00"   , link0.getId(), NetworkUtils.getLinkIds("1 2 3"), link4.getId()), population); // toll in 3rd time slot
		RoadPricingTestUtils.addPersonToPopulation(RoadPricingTestUtils.createPerson1( 4, "09:59:50", link0.getId(), NetworkUtils.getLinkIds("1 2 3"), link4.getId()), population); // toll in 1st and 2nd time slot
		RoadPricingTestUtils.addPersonToPopulation(RoadPricingTestUtils.createPerson1( 5, "08:00:00", link1.getId(), NetworkUtils.getLinkIds("2 3"), link4.getId()), population); // starts on the 2nd link
		RoadPricingTestUtils.addPersonToPopulation(RoadPricingTestUtils.createPerson1( 6, "09:00:00", link0.getId(), NetworkUtils.getLinkIds("1 2"), link3.getId()), population); // ends not on the last link
		RoadPricingTestUtils.addPersonToPopulation(RoadPricingTestUtils.createPerson1( 7, "08:30:00", link1.getId(), NetworkUtils.getLinkIds("2"), link3.getId()), population); // starts and ends not on the first/last link
		RoadPricingTestUtils.addPersonToPopulation(RoadPricingTestUtils.createPerson1( 8, "08:35:00", link1.getId(), NetworkUtils.getLinkIds(""), link2.getId()), population); // starts and ends not on the first/last link
		RoadPricingTestUtils.addPersonToPopulation(RoadPricingTestUtils.createPerson1( 9, "08:40:00", link1.getId(), NetworkUtils.getLinkIds(""), link1.getId()), population); // two acts on the same link
		RoadPricingTestUtils.addPersonToPopulation(RoadPricingTestUtils.createPerson1(10, "08:45:00", link2.getId(), NetworkUtils.getLinkIds(""), link3.getId()), population);
	}

	private static void addPersonToPopulation(final Person person, final Population population) {
		population.addPerson(person);
	}

	/**
	 * Creates a population for network2
	 **/
	static void createPopulation2(final MutableScenario scenario) {
		Population population = scenario.getPopulation();
		Network network = scenario.getNetwork();

		RoadPricingTestUtils.addPersonToPopulation(RoadPricingTestUtils.createPerson2(1, "07:00", network.getLinks().get(Id.create("1", Link.class)), network.getLinks().get(Id.create("7", Link.class)), network.getLinks().get(Id.create("13", Link.class))), population);
	}

	private static Person createPerson1(final int personId, final String startTime, final Id<Link> homeLinkId, final List<Id<Link>> routeLinkIds, final Id<Link> workLinkId) {
		Person person = PopulationUtils.getFactory().createPerson(Id.create(personId, Person.class));
		Plan plan = PopulationUtils.createPlan(person);
		person.addPlan(plan);
		PopulationUtils.createAndAddActivityFromLinkId(plan, "h", (Id<Link>) homeLinkId).setEndTime(Time.parseTime(startTime));
		Leg leg = PopulationUtils.createAndAddLeg( plan, TransportMode.car );
		TripStructureUtils.setRoutingMode(leg, TransportMode.car);
		NetworkRoute route = RouteUtils.createLinkNetworkRouteImpl(homeLinkId, workLinkId);
		route.setLinkIds(homeLinkId, routeLinkIds, workLinkId);
		leg.setRoute(route);
		PopulationUtils.createAndAddActivityFromLinkId(plan, "w", (Id<Link>) workLinkId);
		return person;
	}

	private static Person createPerson2(final int personId, final String startTime, final Link homeLink, final Link workLink, final Link finishLink) {
		Person person = PopulationUtils.getFactory().createPerson(Id.create(personId, Person.class));
		Plan plan = PopulationUtils.createPlan(person);
		person.addPlan(plan);
		Activity act = PopulationUtils.createAndAddActivityFromLinkId(plan, "h", homeLink.getId());
		act.setCoord(homeLink.getCoord());
		act.setEndTime(Time.parseTime(startTime));
		Leg leg = PopulationUtils.createAndAddLeg( plan, TransportMode.car );
		TripStructureUtils.setRoutingMode(leg, TransportMode.car);
		act = PopulationUtils.createAndAddActivityFromLinkId(plan, "w", workLink.getId());
		act.setCoord(workLink.getCoord());
		act.setEndTime(16.0 * 3600);
		Leg leg2 = PopulationUtils.createAndAddLeg( plan, TransportMode.car );
		TripStructureUtils.setRoutingMode(leg2, TransportMode.car);
		act = PopulationUtils.createAndAddActivityFromLinkId(plan, "h", finishLink.getId());
		act.setCoord(finishLink.getCoord());
		return person;
	}

	protected static Population createReferencePopulation1(final Config config) {
		// run mobsim once without toll and get score for network1/population1
		MutableScenario scenario = (MutableScenario) ScenarioUtils.createScenario(config);
		RoadPricingTestUtils.createNetwork1(scenario);
		RoadPricingTestUtils.createPopulation1(scenario);
		Population referencePopulation = scenario.getPopulation();
		EventsManager events = EventsUtils.createEventsManager();
		EventsToScore scoring = EventsToScore.createWithScoreUpdating(scenario, new CharyparNagelScoringFunctionFactory(scenario), events);

		PrepareForSimUtils.createDefaultPrepareForSim(scenario).run();
		Mobsim sim = new QSimBuilder(scenario.getConfig()) //
				.useDefaults() //
				.build(scenario, events);
		scoring.beginIteration(0, false);
		sim.run();
		scoring.finish();

		return referencePopulation;
	}

	protected static void compareRoutes(final String expectedRoute, final NetworkRoute realRoute) {
		Assertions.assertNotNull(expectedRoute) ;
		Assertions.assertNotNull(realRoute);
		Assertions.assertNotNull(realRoute.getLinkIds()) ;

		StringBuilder strBuilder = new StringBuilder();

		for (Id<Link> linkId : realRoute.getLinkIds()) {
			strBuilder.append(linkId.toString());
			strBuilder.append(' ');
		}
		String route = strBuilder.toString();
		Assertions.assertEquals(expectedRoute + " ", route);
	}
}
