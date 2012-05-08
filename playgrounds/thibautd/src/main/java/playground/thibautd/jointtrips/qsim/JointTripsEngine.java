/* *********************************************************************** *
 * project: org.matsim.*
 * JointTripsDepartureHandler.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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
package playground.thibautd.jointtrips.qsim;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.core.api.experimental.events.AgentArrivalEvent;
import org.matsim.core.api.experimental.events.handler.AgentArrivalEventHandler;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.qsim.interfaces.DepartureHandler;
import org.matsim.core.mobsim.qsim.interfaces.MobsimEngine;
import org.matsim.core.mobsim.qsim.InternalInterface;
import org.matsim.core.mobsim.qsim.QSim;

import playground.thibautd.jointtrips.population.JointActingTypes;

/**
 * Handles departure for passenger and driver modes.
 * @author thibautd
 */
public class JointTripsEngine implements DepartureHandler, MobsimEngine {
	public static final String DRIVER_SIM_MODE = "simulatedCarDriver";
	private final QSim qsim;
	private InternalInterface internalInterface = null;
	private PassengerArrivalHandler arrivalHandler = null;
	private Map<Id, MobsimAgent> agents = null;

	public JointTripsEngine(final QSim qsim) {
		this.qsim = qsim;
	}

	@Override
	public boolean handleDeparture(
			final double now,
			final MobsimAgent agent,
			final Id linkId) {
		String mode = agent.getMode();

		if (JointActingTypes.PASSENGER.equals( mode )) {
			handlePassengerDeparture(now, agent, linkId);
			return true;
		}
		else if (JointActingTypes.DRIVER.equals( mode )) {
			handleDriverDeparture(now, agent, linkId);
			return true;
		}

		return false;
	}

	private void handleDriverDeparture(
			final double now,
			final MobsimAgent agent,
			final Id linkId) {
		JointTravelerAgent driver = (JointTravelerAgent) agent;

		if ( driver.isReadyForDeparture( linkId , now ) ) {
			for ( Id p : driver.getPassengersIds() ) {
				// add only at the time of departure, so that
				// we are sure we take into account the right
				// driver's arrival
				arrivalHandler.addPassengerToDriver( agent.getId() , getAgent( p ) );
			}
			driver.anounceDriverMode();
			driver.notifyJointDeparture( linkId );
			// the next line should result in repassing the agent
			// to the departures handlers, with the simulated driver
			// mode anounced. Thus, it should be handled by the
			// pertinent handler, moved around, and eventually arrive.
			// Its arrival will result in an arrival event, used
			// to notify the passengers they arrived as well.
			internalInterface.arrangeNextAgentState( driver );
		}
	}

	private void handlePassengerDeparture(
			final double now,
			final MobsimAgent agent,
			final Id linkId) {
		JointTravelerAgent passenger = (JointTravelerAgent) agent;
		JointTravelerAgent driver = getAgent( passenger.getDriverId() );
		
		driver.notifyPassengerArrivedAtLink(now, passenger.getId(), linkId);
		handleDriverDeparture( now , driver , linkId );
	}

	private JointTravelerAgent getAgent(final Id id) {
		if (agents == null) {
			agents = new HashMap<Id, MobsimAgent>();

			for (MobsimAgent agent : qsim.getAgents()) {
				agents.put( agent.getId() , agent );
			}
		}
		return (JointTravelerAgent) agents.get( id );
	}

	@Override
	public void doSimStep(final double time) {
		// TODO Auto-generated method stub
	}

	@Override
	public void onPrepareSim() {
		arrivalHandler = new PassengerArrivalHandler();
		qsim.getEventsManager().addHandler( arrivalHandler );
	}

	@Override
	public void afterSim() {
		qsim.getEventsManager().removeHandler( arrivalHandler );
	}

	@Override
	public void setInternalInterface(final InternalInterface internalInterface) {
		this.internalInterface = internalInterface;
	}


	private class PassengerArrivalHandler implements AgentArrivalEventHandler {
		private Map<Id, List<MobsimAgent>> passengersPerDriver = new HashMap<Id, List<MobsimAgent>>();

		@Override
		public void reset(final int iteration) {
			passengersPerDriver.clear();
			throw new RuntimeException( "this should not happen (handler should have been removed)" );
		}

		@Override
		public void handleEvent(final AgentArrivalEvent event) {
			List<MobsimAgent> passengers = passengersPerDriver.get( event.getPersonId() );

			if (passengers != null) {
				Id link = event.getLinkId();
				double now = event.getTime();
				Iterator<MobsimAgent> it = passengers.iterator();

				while (it.hasNext()) {
					MobsimAgent p = it.next();
					if (p.getDestinationLinkId().equals( link )) {
						// once a driver has a passenger list attached, it is not
						// detached (to allow arbitrary pu/do sequence, without
						// having to adapt passenger plans to the driver route).
						// Thus, each time a passenger arrives at destination, we
						// remove it from the passengers.
						it.remove();

						p.notifyTeleportToLink( link );
						p.endLegAndComputeNextState( now );
						internalInterface.arrangeNextAgentState( p );
					}
				}
			}
		}

		public void addPassengerToDriver(final Id d, final MobsimAgent p) {
			List<MobsimAgent> ps = passengersPerDriver.get( d );

			if (ps == null) {
				ps = new ArrayList<MobsimAgent>();
				passengersPerDriver.put( d , ps );
			}

			ps.add( p );
		}
	}
}

