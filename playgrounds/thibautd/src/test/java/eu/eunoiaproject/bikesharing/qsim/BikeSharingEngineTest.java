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
package eu.eunoiaproject.bikesharing.qsim;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.events.handler.EventHandler;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.framework.MobsimTimer;
import org.matsim.core.mobsim.framework.PlanAgent;
import org.matsim.core.mobsim.framework.listeners.MobsimListener;
import org.matsim.core.mobsim.qsim.InternalInterface;
import org.matsim.core.mobsim.qsim.interfaces.AgentCounterI;
import org.matsim.core.mobsim.qsim.interfaces.Netsim;
import org.matsim.core.mobsim.qsim.qnetsimengine.NetsimNetwork;
import org.matsim.core.population.LegImpl;
import org.matsim.core.utils.geometry.CoordImpl;

import eu.eunoiaproject.bikesharing.BikeSharingConstants;
import eu.eunoiaproject.bikesharing.scenario.BikeSharingFacilities;
import eu.eunoiaproject.bikesharing.scenario.BikeSharingFacility;
import eu.eunoiaproject.bikesharing.scenario.BikeSharingRoute;

/**
 * @author thibautd
 */
public class BikeSharingEngineTest {
	@Test
	public void testAmountOfBikesAtFacilities() throws Exception {
		test( 1 , 2 , 0 , 2 , true );
	}

	@Test
	@Ignore( "in error because no actual qsim returned by internal interface" )
	public void testNoBike() throws Exception {
		test( 0 , 2 , 0 , 0 , false );
	}

	@Test
	@Ignore( "in error because no actual qsim returned by internal interface" )
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
					new IdImpl( "departure" ),
					new CoordImpl( 0 , 0 ),
					new IdImpl( "departure_link" ),
					capacity,
					initialNBikes );
		facilities.addFacility( departureFacility );

		final BikeSharingFacility arrivalFacility =
			facilities.getFactory().createBikeSharingFacility(
					new IdImpl( "arrival" ),
					new CoordImpl( 10 , 10 ),
					new IdImpl( "arrival_link" ),
					capacity,
					initialNBikes );
		facilities.addFacility( arrivalFacility );

		// create engine
		final BikeSharingManager manager =
			new BikeSharingManager(
					facilities );
		final BikeSharingEngine engine =
			new BikeSharingEngine(
					manager );

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
		engine.onPrepareSim();
		engine.setInternalInterface(
				new DummyInternalInterface() );
		engine.handleDeparture(
				0, 
				agent,
				departureFacility.getLinkId() );
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
		@Override
		public Netsim getMobsim() {
			return new Netsim() {

				@Override
				public EventsManager getEventsManager() {
					return new EventsManager() {

						@Override
						public void processEvent(Event event) {
							// TODO Auto-generated method stub
							
						}

						@Override
						public void addHandler(EventHandler handler) {
							// TODO Auto-generated method stub
							
						}

						@Override
						public void removeHandler(EventHandler handler) {
							// TODO Auto-generated method stub
							
						}

						@Override
						public void resetHandlers(int iteration) {
							// TODO Auto-generated method stub
							
						}

						@Override
						public void initProcessing() {
							// TODO Auto-generated method stub
							
						}

						@Override
						public void afterSimStep(double time) {
							// TODO Auto-generated method stub
							
						}

						@Override
						public void finishProcessing() {
							// TODO Auto-generated method stub
							
						}
					};
				}

				@Override
				public AgentCounterI getAgentCounter() {
					// TODO Auto-generated method stub
					return null;
				}

				@Override
				public Scenario getScenario() {
					// TODO Auto-generated method stub
					return null;
				}

				@Override
				public MobsimTimer getSimTimer() {
					// TODO Auto-generated method stub
					return null;
				}

				@Override
				public void addQueueSimulationListeners(MobsimListener listener) {
					// TODO Auto-generated method stub
					
				}

				@Override
				public void run() {
					// TODO Auto-generated method stub
					
				}

				@Override
				public NetsimNetwork getNetsimNetwork() {
					// TODO Auto-generated method stub
					return null;
				}
			};
		}

		@Override
		public void arrangeNextAgentState(MobsimAgent a) {
		}

		@Override
		public void registerAdditionalAgentOnLink(MobsimAgent a) {
		}

		@Override
		public MobsimAgent unregisterAdditionalAgentOnLink(
				Id agentId, Id linkId) {
			return null;
		}

		@Override
		public void rescheduleActivityEnd(MobsimAgent a) {
		}
	}

	private static class LegAgent implements MobsimAgent, PlanAgent {
		private final Leg leg;
		private Id linkId;

		public LegAgent(final Leg leg) {
			this.leg = leg;
			this.linkId = leg.getRoute().getStartLinkId();
		}

		@Override
		public Id getCurrentLinkId() {
			return linkId;
		}

		@Override
		public Id getDestinationLinkId() {
			return leg.getRoute().getEndLinkId();
		}

		@Override
		public Id getId() {
			return new IdImpl( "tintin" );
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
		public Plan getSelectedPlan() {
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
		public void abort(double now) {
		}

		@Override
		public Double getExpectedTravelTime() {
			return leg.getTravelTime();
		}

		@Override
		public String getMode() {
			return leg.getMode();
		}

		@Override
		public void notifyArrivalOnLinkByNonNetworkMode(Id linkIdArg) {
			this.linkId = linkIdArg;
		}
	}
}

