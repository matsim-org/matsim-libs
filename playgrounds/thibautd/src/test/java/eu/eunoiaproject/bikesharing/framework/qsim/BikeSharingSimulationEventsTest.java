/* *********************************************************************** *
 * project: org.matsim.*
 * BikeSharingSimulationEventsTest.java
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
package eu.eunoiaproject.bikesharing.framework.qsim;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.handler.ActivityEndEventHandler;
import org.matsim.api.core.v01.events.handler.ActivityStartEventHandler;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.population.routes.GenericRouteImpl;
import org.matsim.core.scenario.ScenarioUtils;

import eu.eunoiaproject.bikesharing.framework.BikeSharingConstants;
import eu.eunoiaproject.bikesharing.framework.scenario.BikeSharingConfigGroup;
import eu.eunoiaproject.bikesharing.framework.scenario.BikeSharingFacilities;
import eu.eunoiaproject.bikesharing.framework.scenario.BikeSharingFacility;
import eu.eunoiaproject.bikesharing.framework.scenario.BikeSharingRoute;

/**
 * Tests that the expected simulation events are thrown.
 * At the time of implementation of the simulation, the agent was responsible
 * to throw the arrival event, but this may change. This test should allow to
 * make sure no duplicate event is thrown in this case.
 *
 * @author thibautd
 */
@Ignore( "fails since refactoring in DriverAgent. to fix!!!" )
public class BikeSharingSimulationEventsTest {


	private static EventCounter runSimulationAndCountEvents() {
		final Scenario scenario = ScenarioUtils.createScenario( ConfigUtils.createConfig() );
		scenario.getConfig().addModule( new BikeSharingConfigGroup() );

		final Id<Link> linkId = Id.create( "link" , Link.class);
		final Id<BikeSharingFacility> stationId = Id.create( "station" , BikeSharingFacility.class);

		final BikeSharingFacilities stations = new BikeSharingFacilities();
		scenario.addScenarioElement( BikeSharingFacilities.ELEMENT_NAME , stations );

		final BikeSharingFacility station =
				stations.getFactory().createBikeSharingFacility(
					stationId,
						new Coord((double) 0, (double) 0),
					linkId,
					2,
					1 );
		stations.addFacility( station );

		/* network creation */ {
			final Node node1 = scenario.getNetwork().getFactory().createNode( Id.create( 1 , Node.class) , new Coord((double) 0, (double) 1));
			final Node node2 = scenario.getNetwork().getFactory().createNode( Id.create( 2 , Node.class ) , new Coord((double) 1, (double) 0));
			final Link link = scenario.getNetwork().getFactory().createLink( linkId , node1 , node2 );

			scenario.getNetwork().addNode( node1 );
			scenario.getNetwork().addNode( node2 );
			scenario.getNetwork().addLink( link );
		}

		final Person person = scenario.getPopulation().getFactory().createPerson( Id.create( "p", Person.class ) );
		scenario.getPopulation().addPerson( person );
		final Plan plan = scenario.getPopulation().getFactory().createPlan();
		plan.setPerson( person );
		person.addPlan( plan );

		/* scope of firstActivity */ {
			final Activity firstActivity = scenario.getPopulation().getFactory().createActivityFromLinkId( "h" , linkId );
			firstActivity.setEndTime( 10  );
			plan.addActivity( firstActivity );
		}

		/* scope of leg */ {
			final Leg leg = scenario.getPopulation().getFactory().createLeg( TransportMode.walk );
			leg.setRoute( new GenericRouteImpl( linkId , linkId ) );
			leg.setTravelTime( 10 );
			plan.addLeg( leg );
		}

		/* scope of interaction */ {
			final Activity interaction =
				scenario.getPopulation().getFactory().createActivityFromLinkId(
						BikeSharingConstants.INTERACTION_TYPE,
						linkId );
			interaction.setMaximumDuration( 0 );
			plan.addActivity( interaction );
		}

		/* scope of leg */ {
			final Leg leg = scenario.getPopulation().getFactory().createLeg( BikeSharingConstants.MODE );
			final BikeSharingRoute route = new BikeSharingRoute( station , station );
			leg.setRoute( route );
			leg.setTravelTime( 10 );
			plan.addLeg( leg );
		}

		/* scope of interaction */ {
			final Activity interaction =
				scenario.getPopulation().getFactory().createActivityFromLinkId(
						BikeSharingConstants.INTERACTION_TYPE,
						linkId );
			interaction.setMaximumDuration( 0 );
			plan.addActivity( interaction );
		}

		/* scope of leg */ {
			final Leg leg = scenario.getPopulation().getFactory().createLeg( TransportMode.walk );
			leg.setRoute( new GenericRouteImpl( linkId , linkId ) );
			leg.setTravelTime( 10 );
			plan.addLeg( leg );
		}

		/* scope of lastActivity */ {
			final Activity lastActivity = scenario.getPopulation().getFactory().createActivityFromLinkId( "h" , linkId );
			plan.addActivity( lastActivity );
		}

		final EventsManager events = EventsUtils.createEventsManager();
		final EventCounter counter = new EventCounter();
		events.addHandler( counter );

		new BikeSharingWithoutRelocationQsimFactory().createMobsim(
				scenario,
				events ).run();

		return counter;
	}


	@Test
	public void testExpectedDepartureEvents() {
		final EventCounter counter = runSimulationAndCountEvents();
		Assert.assertEquals(
				"unexpected number of departures",
				3,
				counter.nDepartures );
	}

	@Test
	public void testExpectedArrivalEvents() {
		final EventCounter counter = runSimulationAndCountEvents();
		Assert.assertEquals(
				"unexpected number of arrivals",
				3,
				counter.nArrivals );
	}

	@Test
	public void testExpectedStartEvents() {
		final EventCounter counter = runSimulationAndCountEvents();
		Assert.assertEquals(
				"unexpected number of act starts",
				3,
				counter.nStarts );
	}

	@Test
	public void testExpectedEndEvents() {
		final EventCounter counter = runSimulationAndCountEvents();
		Assert.assertEquals(
				"unexpected number of act ends",
				3,
				counter.nEnds );
	}

	private static class EventCounter implements
				ActivityStartEventHandler,
				ActivityEndEventHandler,
				PersonDepartureEventHandler,
				PersonArrivalEventHandler {
		int nStarts = 0;
		int nEnds = 0;
		int nDepartures = 0;
		int nArrivals = 0;

		@Override
		public void reset(int iteration) {}

		@Override
		public void handleEvent(PersonArrivalEvent event) {
			nArrivals++;
		}

		@Override
		public void handleEvent(PersonDepartureEvent event) {
			nDepartures++;
		}

		@Override
		public void handleEvent(ActivityEndEvent event) {
			nEnds++;
		}

		@Override
		public void handleEvent(ActivityStartEvent event) {
			nStarts++;
		}
	}
}

