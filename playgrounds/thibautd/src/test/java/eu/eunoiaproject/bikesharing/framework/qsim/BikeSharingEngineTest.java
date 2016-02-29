/* *********************************************************************** *
 * project: org.matsim.*
 * BikeSharingEngineTest.java
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

import eu.eunoiaproject.bikesharing.framework.BikeSharingConstants;
import eu.eunoiaproject.bikesharing.framework.scenario.BikeSharingConfigGroup;
import eu.eunoiaproject.bikesharing.framework.scenario.BikeSharingFacilities;
import eu.eunoiaproject.bikesharing.framework.scenario.BikeSharingFacility;
import eu.eunoiaproject.bikesharing.framework.scenario.BikeSharingRoute;
import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.framework.MobsimTimer;
import org.matsim.core.mobsim.framework.PlanAgent;
import org.matsim.core.mobsim.framework.listeners.MobsimListener;
import org.matsim.core.mobsim.qsim.InternalInterface;
import org.matsim.core.mobsim.qsim.interfaces.AgentCounter;
import org.matsim.core.mobsim.qsim.interfaces.Netsim;
import org.matsim.core.mobsim.qsim.qnetsimengine.NetsimNetwork;
import org.matsim.core.population.LegImpl;
import org.matsim.facilities.Facility;

/**
 * @author thibautd
 */
public class BikeSharingEngineTest {
	private static final Logger log =
		Logger.getLogger(BikeSharingEngineTest.class);

	@Test
	public void testAmountOfBikesAtFacilities() throws Exception {
		test( 1 , 2 , 0 , 2 , true );
	}

	@Test
	public void testNoBike() throws Exception {
		test( 0 , 2 , 0 , 0 , false );
	}

	@Test
	public void testNoSlot() throws Exception {
		test( 2 , 2 , 1 , 2 , false );
	}

	private static void test(
			final int initialNBikes,
			final int capacity,
			final int expectedNBikesAtDeparture,
			final int expectedNBikesAtArrival,
			final boolean expectArrival ) {
		// create two stations with enough free bikes and free slots
		final BikeSharingFacilities facilities = new BikeSharingFacilities();

		final BikeSharingFacility departureFacility =
			facilities.getFactory().createBikeSharingFacility(
					Id.create( "departure" , BikeSharingFacility.class),
					new Coord((double) 0, (double) 0),
					Id.create( "departure_link" , Link.class),
					capacity,
					initialNBikes );
		facilities.addFacility( departureFacility );

		final BikeSharingFacility arrivalFacility =
			facilities.getFactory().createBikeSharingFacility(
					Id.create( "arrival" , BikeSharingFacility.class),
					new Coord((double) 10, (double) 10),
					Id.create( "arrival_link" , Link.class),
					capacity,
					initialNBikes );
		facilities.addFacility( arrivalFacility );

		// create engine
		final BikeSharingManager manager =
			new BikeSharingManagerImpl(
					new BikeSharingConfigGroup(),
					facilities );
		final BikeSharingEngine engine =
			new BikeSharingEngine(
					manager, EventsUtils.createEventsManager());

		// create agent
		final Leg leg = new LegImpl( BikeSharingConstants.MODE );
		final BikeSharingRoute route =
			new BikeSharingRoute(
					departureFacility,
					arrivalFacility );
		leg.setRoute( route );

		leg.setTravelTime( 10 );
		route.setTravelTime( 10 );

		final LegAgent agent = new LegAgent( leg );

		// run
		final InternalInterface internalInterface = new DummyInternalInterface();
		internalInterface.getMobsim().getSimTimer().setTime( 0 );
		engine.onPrepareSim();
		engine.setInternalInterface( internalInterface );
		engine.handleDeparture(
				0, 
				agent,
				departureFacility.getLinkId() );
		internalInterface.getMobsim().getSimTimer().setTime( 20 );
		engine.doSimStep( 20 );

		// test
		Assert.assertEquals(
				"unexpected number of bikes in departure station",
				expectedNBikesAtDeparture,
				manager.getFacilities().get( departureFacility.getId() ).getNumberOfBikes() );
		Assert.assertEquals(
				"unexpected number of bikes in arrival station",
				expectedNBikesAtArrival,
				manager.getFacilities().get( arrivalFacility.getId() ).getNumberOfBikes() );

		if ( expectArrival ) {
			Assert.assertEquals(
					"agent at wrong link",
					arrivalFacility.getLinkId(),
					agent.getCurrentLinkId() );
		}
		else {
			Assert.assertFalse(
					"agent at arrival link but should not",
					arrivalFacility.getLinkId().equals(
						agent.getCurrentLinkId() ) );

		}
	}

	public static class DummyInternalInterface implements InternalInterface {
		private final EventsManager events = EventsUtils.createEventsManager();
		private final MobsimTimer timer = new MobsimTimer();

		private final Netsim netsim =
			new Netsim() {
				@Override
				public EventsManager getEventsManager() {
					return events;
				}

				@Override
				public AgentCounter getAgentCounter() {
					throw new UnsupportedOperationException();
				}

				@Override
				public Scenario getScenario() {
					throw new UnsupportedOperationException();
				}

				@Override
				public MobsimTimer getSimTimer() {
					return timer;
				}

				@Override
				public void addQueueSimulationListeners(MobsimListener listener) {
					throw new UnsupportedOperationException();
				}

				@Override
				public void run() {
					throw new UnsupportedOperationException();
				}

				@Override
				public NetsimNetwork getNetsimNetwork() {
					throw new UnsupportedOperationException();
				}
			};


		@Override
		public Netsim getMobsim() {
			return netsim;
		}

		@Override
		public void arrangeNextAgentState(MobsimAgent a) {
			log.info ( "call to arrangeNextAgentState on "+a );
		}

		@Override
		public void registerAdditionalAgentOnLink(MobsimAgent a) {
			log.info ( "call to registerAdditionalAgentOnLink on "+a );
		}

		@Override
		public MobsimAgent unregisterAdditionalAgentOnLink(
				Id<Person> agentId, Id<Link> linkId) {
			log.info ( "call to unregisterAdditionalAgentOnLink on "+agentId+" on link "+linkId );
			return null;
		}

		@Override
		public void rescheduleActivityEnd(MobsimAgent a) {
			log.info ( "call to rescheduleActivityEnd on "+a );
		}
	}

	private static class LegAgent implements MobsimAgent, PlanAgent {
		private final Leg leg;
		private Id<Link> linkId;

		public LegAgent(final Leg leg) {
			this.leg = leg;
			this.linkId = leg.getRoute().getStartLinkId();
		}

		@Override
		public Id<Link> getCurrentLinkId() {
			return linkId;
		}

		@Override
		public Id<Link> getDestinationLinkId() {
			return leg.getRoute().getEndLinkId();
		}

		@Override
		public Id<Person> getId() {
			return Id.create( "tintin" , Person.class);
		}

		@Override
		public PlanElement getCurrentPlanElement() {
			return leg;
		}

		@Override
		public PlanElement getNextPlanElement() {
			throw new UnsupportedOperationException();
		}

		@Override
		public Plan getCurrentPlan() {
			throw new UnsupportedOperationException();
		}

		@Override
		public State getState() {
			return State.LEG;
		}

		@Override
		public double getActivityEndTime() {
			throw new UnsupportedOperationException();
		}

		@Override
		public void endActivityAndComputeNextState(double now) {
			throw new UnsupportedOperationException();
		}

		@Override
		public void endLegAndComputeNextState(double now) {
		}

		@Override
		public void setStateToAbort(double now) {
		}

		@Override
		public Double getExpectedTravelTime() {
			return leg.getTravelTime();
		}

        @Override
        public Double getExpectedTravelDistance() {
            return leg.getRoute().getDistance();
        }

        @Override
		public String getMode() {
			return leg.getMode();
		}

		@Override
		public void notifyArrivalOnLinkByNonNetworkMode(Id<Link> linkIdArg) {
			this.linkId = linkIdArg;
		}

		@Override
		public PlanElement getPreviousPlanElement() {
			// TODO Auto-generated method stub
			throw new RuntimeException("not implemented") ;
		}

		@Override
		public Facility<? extends Facility<?>> getCurrentFacility() {
			// TODO Auto-generated method stub
			throw new RuntimeException("not implemented") ;
		}

		@Override
		public Facility<? extends Facility<?>> getDestinationFacility() {
			// TODO Auto-generated method stub
			throw new RuntimeException("not implemented") ;
		}
	}
}

