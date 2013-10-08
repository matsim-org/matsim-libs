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
package playground.thibautd.socnetsim.sharedvehicles.qsim;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Collections;

import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.agents.DefaultAgentFactory;
import org.matsim.core.mobsim.qsim.qnetsimengine.QNetsimEngine;
import org.matsim.core.population.routes.GenericRouteImpl;
import org.matsim.core.population.routes.LinkNetworkRouteImpl;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordImpl;

import playground.thibautd.socnetsim.sharedvehicles.qsim.PopulationAgentSourceWithVehicles.InconsistentVehiculeSpecificationsException;

/**
 * @author thibautd
 */
public class PopulationAgentSourceWithVehiclesTest {
	//TODO: test that vehicles added where they should be

	@Test
	public void testFailsIfOnlySomeRoutesHaveAVehicle() throws Exception {
		final Config config = ConfigUtils.createConfig();
		final Scenario scenario = ScenarioUtils.createScenario( config );

		final Id node1 = new IdImpl( "node1" );
		final Id node2 = new IdImpl( "node2" );
		final Id link = new IdImpl( "the_link" );

		scenario.getNetwork().addNode(
				scenario.getNetwork().getFactory().createNode( node1 , new CoordImpl( 0 , 0 ) ));

		scenario.getNetwork().addNode(
				scenario.getNetwork().getFactory().createNode( node2 , new CoordImpl( 100 , 100 ) ));

		scenario.getNetwork().addLink(
				scenario.getNetwork().getFactory().createLink(
					link,
					scenario.getNetwork().getNodes().get( node1 ),
					scenario.getNetwork().getNodes().get( node2 ) ) );

		final Person withVeh = scenario.getPopulation().getFactory().createPerson( new IdImpl( "jojo" ) );
		scenario.getPopulation().addPerson( withVeh );
		final Plan planWithVeh = scenario.getPopulation().getFactory().createPlan();
		planWithVeh.setPerson( withVeh );
		withVeh.addPlan( planWithVeh );
		planWithVeh.addActivity( scenario.getPopulation().getFactory().createActivityFromLinkId( "h" , link ) );
		final Leg legWithVeh = scenario.getPopulation().getFactory().createLeg( TransportMode.car );
		final NetworkRoute routeWithVeh = new LinkNetworkRouteImpl( link , Collections.<Id>emptyList() , link );
		routeWithVeh.setVehicleId( new IdImpl( "a_pink_pony" ) );
		legWithVeh.setRoute( routeWithVeh );
		planWithVeh.addLeg( legWithVeh );

		final Person withoutVeh = scenario.getPopulation().getFactory().createPerson( new IdImpl( "toto" ) );
		scenario.getPopulation().addPerson( withoutVeh );
		final Plan planWithoutVeh = scenario.getPopulation().getFactory().createPlan();
		planWithoutVeh.setPerson( withoutVeh );
		withoutVeh.addPlan( planWithoutVeh );
		planWithoutVeh.addActivity( scenario.getPopulation().getFactory().createActivityFromLinkId( "h" , link ) );
		final Leg legWithoutVeh = scenario.getPopulation().getFactory().createLeg( TransportMode.car );
		final NetworkRoute routeWithoutVeh = new LinkNetworkRouteImpl( link , Collections.<Id>emptyList() , link );
		legWithoutVeh.setRoute( routeWithoutVeh );
		planWithoutVeh.addLeg( legWithoutVeh );

		final QSim qSim = new QSim( scenario , EventsUtils.createEventsManager() );
		qSim.addMobsimEngine( new QNetsimEngine( qSim ) );
		final PopulationAgentSourceWithVehicles testee =
			new PopulationAgentSourceWithVehicles(
					scenario.getPopulation(),
					new DefaultAgentFactory( qSim ),
					qSim );

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

		final Id node1 = new IdImpl( "node1" );
		final Id node2 = new IdImpl( "node2" );
		final Id link = new IdImpl( "the_link" );

		scenario.getNetwork().addNode(
				scenario.getNetwork().getFactory().createNode( node1 , new CoordImpl( 0 , 0 ) ));

		scenario.getNetwork().addNode(
				scenario.getNetwork().getFactory().createNode( node2 , new CoordImpl( 100 , 100 ) ));

		scenario.getNetwork().addLink(
				scenario.getNetwork().getFactory().createLink(
					link,
					scenario.getNetwork().getNodes().get( node1 ),
					scenario.getNetwork().getNodes().get( node2 ) ) );

		final Person withVeh = scenario.getPopulation().getFactory().createPerson( new IdImpl( "jojo" ) );
		scenario.getPopulation().addPerson( withVeh );
		final Plan planWithVeh = scenario.getPopulation().getFactory().createPlan();
		planWithVeh.setPerson( withVeh );
		withVeh.addPlan( planWithVeh );
		planWithVeh.addActivity( scenario.getPopulation().getFactory().createActivityFromLinkId( "h" , link ) );
		final Leg walkLegWithVeh = scenario.getPopulation().getFactory().createLeg( TransportMode.walk );
		walkLegWithVeh.setRoute( new GenericRouteImpl( link , link ) );
		planWithVeh.addLeg( walkLegWithVeh );
		final Leg legWithVeh = scenario.getPopulation().getFactory().createLeg( TransportMode.car );
		final NetworkRoute routeWithVeh = new LinkNetworkRouteImpl( link , Collections.<Id>emptyList() , link );
		if (vehicles) routeWithVeh.setVehicleId( new IdImpl( "a_pink_pony" ) );
		legWithVeh.setRoute( routeWithVeh );
		planWithVeh.addLeg( legWithVeh );

		final Person withoutVeh = scenario.getPopulation().getFactory().createPerson( new IdImpl( "toto" ) );
		scenario.getPopulation().addPerson( withoutVeh );
		final Plan planWithoutVeh = scenario.getPopulation().getFactory().createPlan();
		planWithoutVeh.setPerson( withoutVeh );
		withoutVeh.addPlan( planWithoutVeh );
		planWithoutVeh.addActivity( scenario.getPopulation().getFactory().createActivityFromLinkId( "h" , link ) );
		final Leg legWithoutVeh = scenario.getPopulation().getFactory().createLeg( TransportMode.car );
		final NetworkRoute routeWithoutVeh = new LinkNetworkRouteImpl( link , Collections.<Id>emptyList() , link );
		if (vehicles) routeWithoutVeh.setVehicleId( new IdImpl( "a_hummer" ) );
		legWithoutVeh.setRoute( routeWithoutVeh );
		planWithoutVeh.addLeg( legWithoutVeh );

		final QSim qSim = new QSim( scenario , EventsUtils.createEventsManager() );
		qSim.addMobsimEngine( new QNetsimEngine( qSim ) );
		final PopulationAgentSourceWithVehicles testee =
			new PopulationAgentSourceWithVehicles(
					scenario.getPopulation(),
					new DefaultAgentFactory( qSim ),
					qSim );

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

