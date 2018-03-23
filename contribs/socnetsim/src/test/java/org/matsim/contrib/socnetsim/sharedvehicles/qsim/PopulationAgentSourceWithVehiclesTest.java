/* *********************************************************************** *
 * project: org.matsim.*
 * PopulationAgentSourceWithVehiclesTest.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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
package org.matsim.contrib.socnetsim.sharedvehicles.qsim;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Collections;

import org.junit.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.mobsim.framework.MobsimTimer;
import org.matsim.core.mobsim.qsim.ActiveQSimBridge;
import org.matsim.core.mobsim.qsim.AgentCounterImpl;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.agents.DefaultAgentFactory;
import org.matsim.core.mobsim.qsim.interfaces.AgentCounter;
import org.matsim.core.mobsim.qsim.qnetsimengine.DefaultQNetworkFactory;
import org.matsim.core.mobsim.qsim.qnetsimengine.QNetsimEngine;
import org.matsim.core.mobsim.qsim.qnetsimengine.QNetworkFactory;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.vehicles.Vehicle;

import org.matsim.contrib.socnetsim.sharedvehicles.qsim.PopulationAgentSourceWithVehicles.InconsistentVehiculeSpecificationsException;

/**
 * @author thibautd
 */
public class PopulationAgentSourceWithVehiclesTest {
	//TODO: test that vehicles added where they should be

	@Test
	public void testFailsIfOnlySomeRoutesHaveAVehicle() throws Exception {
		final Config config = ConfigUtils.createConfig();
		final Scenario scenario = ScenarioUtils.createScenario( config );

		final Id<Node> node1 = Id.create( "node1" , Node.class );
		final Id<Node> node2 = Id.create( "node2" , Node.class );
		final Id<Link> link = Id.create( "the_link" , Link.class );

		scenario.getNetwork().addNode(
				scenario.getNetwork().getFactory().createNode( node1 , new Coord((double) 0, (double) 0)));

		scenario.getNetwork().addNode(
				scenario.getNetwork().getFactory().createNode( node2 , new Coord((double) 100, (double) 100)));

		scenario.getNetwork().addLink(
				scenario.getNetwork().getFactory().createLink(
					link,
					scenario.getNetwork().getNodes().get( node1 ),
					scenario.getNetwork().getNodes().get( node2 ) ) );

		final Person withVeh = scenario.getPopulation().getFactory().createPerson( Id.create( "jojo" , Person.class) );
		scenario.getPopulation().addPerson( withVeh );
		final Plan planWithVeh = scenario.getPopulation().getFactory().createPlan();
		planWithVeh.setPerson( withVeh );
		withVeh.addPlan( planWithVeh );
		planWithVeh.addActivity( scenario.getPopulation().getFactory().createActivityFromLinkId( "h" , link ) );
		final Leg legWithVeh = scenario.getPopulation().getFactory().createLeg( TransportMode.car );
		final NetworkRoute routeWithVeh = RouteUtils.createLinkNetworkRouteImpl(link, Collections.<Id<Link>>emptyList(), link);
		routeWithVeh.setVehicleId( Id.create( "a_pink_pony" , Vehicle.class) );
		legWithVeh.setRoute( routeWithVeh );
		planWithVeh.addLeg( legWithVeh );

		final Person withoutVeh = scenario.getPopulation().getFactory().createPerson( Id.create( "toto" , Person.class) );
		scenario.getPopulation().addPerson( withoutVeh );
		final Plan planWithoutVeh = scenario.getPopulation().getFactory().createPlan();
		planWithoutVeh.setPerson( withoutVeh );
		withoutVeh.addPlan( planWithoutVeh );
		planWithoutVeh.addActivity( scenario.getPopulation().getFactory().createActivityFromLinkId( "h" , link ) );
		final Leg legWithoutVeh = scenario.getPopulation().getFactory().createLeg( TransportMode.car );
		final NetworkRoute routeWithoutVeh = RouteUtils.createLinkNetworkRouteImpl(link, Collections.<Id<Link>>emptyList(), link);
		legWithoutVeh.setRoute( routeWithoutVeh );
		planWithoutVeh.addLeg( legWithoutVeh );
		
		MobsimTimer mobsimTimer = new MobsimTimer(config);
		AgentCounter agentCounter = new AgentCounterImpl();
		EventsManager eventsManager = EventsUtils.createEventsManager();
		ActiveQSimBridge activeQSimBridge = new ActiveQSimBridge();
		
		final QSim qSim = new QSim( scenario , eventsManager, agentCounter, mobsimTimer, activeQSimBridge );
		QNetworkFactory networkFactory = new DefaultQNetworkFactory(eventsManager, scenario, mobsimTimer, agentCounter);
		qSim.addMobsimEngine( new QNetsimEngine( networkFactory, config, scenario, eventsManager, mobsimTimer, agentCounter, qSim.getInternalInterface() ) );
		final PopulationAgentSourceWithVehicles testee =
			new PopulationAgentSourceWithVehicles(
					scenario.getPopulation(),
					new DefaultAgentFactory( scenario, eventsManager, mobsimTimer ),
					qSim, scenario );

		boolean gotException = false;
		try {
			testee.insertAgentsIntoMobsim();
		}
		catch ( InconsistentVehiculeSpecificationsException e ) {
			gotException = true;
		}

		assertTrue(
				"did not get an exception with inconsistent setting",
				gotException);
	}

	@Test
	public void testNoFailIfAllHaveVehicles() throws Exception {
		testNoFail( true );
	}

	@Test
	public void testNoFailIfNoneHaveVehicles() throws Exception {
		testNoFail( false );
	}

	public void testNoFail(final boolean vehicles) throws Exception {
		final Config config = ConfigUtils.createConfig();
		final Scenario scenario = ScenarioUtils.createScenario( config );

		final Id<Node> node1 = Id.create( "node1" , Node.class );
		final Id<Node> node2 = Id.create( "node2" , Node.class );
		final Id<Link> link = Id.create( "the_link" , Link.class );

		scenario.getNetwork().addNode(
				scenario.getNetwork().getFactory().createNode( node1 , new Coord((double) 0, (double) 0)));

		scenario.getNetwork().addNode(
				scenario.getNetwork().getFactory().createNode( node2 , new Coord((double) 100, (double) 100)));

		scenario.getNetwork().addLink(
				scenario.getNetwork().getFactory().createLink(
					link,
					scenario.getNetwork().getNodes().get( node1 ),
					scenario.getNetwork().getNodes().get( node2 ) ) );

		final Person withVeh = scenario.getPopulation().getFactory().createPerson( Id.create( "jojo" , Person.class ) );
		scenario.getPopulation().addPerson( withVeh );
		final Plan planWithVeh = scenario.getPopulation().getFactory().createPlan();
		planWithVeh.setPerson( withVeh );
		withVeh.addPlan( planWithVeh );
		planWithVeh.addActivity( scenario.getPopulation().getFactory().createActivityFromLinkId( "h" , link ) );
		final Leg walkLegWithVeh = scenario.getPopulation().getFactory().createLeg( TransportMode.walk );
		walkLegWithVeh.setRoute( RouteUtils.createGenericRouteImpl(link, link) );
		planWithVeh.addLeg( walkLegWithVeh );
		final Leg legWithVeh = scenario.getPopulation().getFactory().createLeg( TransportMode.car );
		final NetworkRoute routeWithVeh = RouteUtils.createLinkNetworkRouteImpl(link, Collections.<Id<Link>>emptyList(), link);
		if (vehicles) routeWithVeh.setVehicleId( Id.create( "a_pink_pony" , Vehicle.class) );
		legWithVeh.setRoute( routeWithVeh );
		planWithVeh.addLeg( legWithVeh );

		final Person withoutVeh = scenario.getPopulation().getFactory().createPerson( Id.create( "toto" , Person.class) );
		scenario.getPopulation().addPerson( withoutVeh );
		final Plan planWithoutVeh = scenario.getPopulation().getFactory().createPlan();
		planWithoutVeh.setPerson( withoutVeh );
		withoutVeh.addPlan( planWithoutVeh );
		planWithoutVeh.addActivity( scenario.getPopulation().getFactory().createActivityFromLinkId( "h" , link ) );
		final Leg legWithoutVeh = scenario.getPopulation().getFactory().createLeg( TransportMode.car );
		final NetworkRoute routeWithoutVeh = RouteUtils.createLinkNetworkRouteImpl(link, Collections.<Id<Link>>emptyList(), link);
		if (vehicles) routeWithoutVeh.setVehicleId( Id.create( "a_hummer" , Vehicle.class) );
		legWithoutVeh.setRoute( routeWithoutVeh );
		planWithoutVeh.addLeg( legWithoutVeh );

		MobsimTimer mobsimTimer = new MobsimTimer(config);
		AgentCounter agentCounter = new AgentCounterImpl();
		EventsManager eventsManager = EventsUtils.createEventsManager();
		ActiveQSimBridge activeQSimBridge = new ActiveQSimBridge();
		
		final QSim qSim = new QSim( scenario , eventsManager, agentCounter, mobsimTimer, activeQSimBridge );
		QNetworkFactory networkFactory = new DefaultQNetworkFactory(eventsManager, scenario, mobsimTimer, agentCounter);
		qSim.addMobsimEngine( new QNetsimEngine( networkFactory, config, scenario, eventsManager, mobsimTimer, agentCounter, qSim.getInternalInterface() ) );
		final PopulationAgentSourceWithVehicles testee =
			new PopulationAgentSourceWithVehicles(
					scenario.getPopulation(),
					new DefaultAgentFactory( scenario, eventsManager, mobsimTimer ),
					qSim, scenario );

		boolean gotException = false;
		try {
			testee.insertAgentsIntoMobsim();
		}
		catch ( InconsistentVehiculeSpecificationsException e ) {
			gotException = true;
		}

		assertFalse(
				"got an exception with consistent setting",
				gotException);
	}
}

