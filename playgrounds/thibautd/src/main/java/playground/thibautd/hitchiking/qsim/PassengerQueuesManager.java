/* *********************************************************************** *
 * project: org.matsim.*
 * PassengerQueuesManager.java
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
package playground.thibautd.hitchiking.qsim;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.qsim.InternalInterface;
import org.matsim.core.mobsim.qsim.interfaces.DepartureHandler;
import org.matsim.core.mobsim.qsim.interfaces.MobsimEngine;
import org.matsim.core.utils.collections.Tuple;
import playground.thibautd.hitchiking.HitchHikingConstants;
import playground.thibautd.hitchiking.qsim.PassengerQueuesPerLink.Queue;
import playground.thibautd.hitchiking.qsim.events.PassengerEndsWaitingEvent;
import playground.thibautd.hitchiking.qsim.events.PassengerStartsWaitingEvent;

import java.util.Collection;
import java.util.List;

/**
 * A departure handler for the "hitch hiking passenger" mode.
 *
 * @author thibautd
 */
public class PassengerQueuesManager implements MobsimEngine, DepartureHandler {
	private final PassengerQueuesPerLink queues = new PassengerQueuesPerLink();
	private final EventsManager events;
	private InternalInterface internalInterface = null;

	public PassengerQueuesManager(final EventsManager events) {
		this.events = events;
	}

	/**
	 * If the agent getMode method returns the hitch hiking passenger mode,
	 * the agent is added to the wait line for its destination at its origin,
	 * and an event of type {@link PassengerStartsWaitingEvent} is fired.
	 *
	 * @return true iff the agent has been identified as a passenger and added to a line
	 */
	@Override
	public boolean handleDeparture(
			final double now,
			final MobsimAgent agent,
			final Id linkId) {
		if (agent.getMode().equals( HitchHikingConstants.PASSENGER_MODE ) ) {
			passengerArrivesAndWaits( agent , now , linkId , agent.getDestinationLinkId() );
			return true;
		}
		return false;
	}

	private void passengerArrivesAndWaits(
			final MobsimAgent passenger,
			final double now,
			final Id link,
			final Id destination) {
		queues.getQueuesAtLink( link ).getQueueForDestination( destination ).addWaitingAgent( passenger );

		events.processEvent(
				new PassengerStartsWaitingEvent(
					now,
					passenger.getId(),
					link ) );
	}

	/**
	 * Gets the maximum number of passengers from the first non-empty queue encountered
	 * when parsing the possible destinations in the order they are provided.
	 * Throws a {@link PassengerEndsWaitingEvent} for each returned passenger.
	 * @param now  the time
	 * @param pickUpLink the link
	 * @param possibleDestinations the possible destination links, to search for queues
	 * @param nPassengers the maximum number of passengers to pick up
	 * @return a tuple destination link id -- passengers.
	 */
	public Tuple<Id, Collection<MobsimAgent>> getPassengersFromFirstNonEmptyQueue(
			final double now,
			final Id<Link> pickUpLink,
			final List<Id<Link>> possibleDestinations,
			final int nPassengers) {
		for (Id dest : possibleDestinations) {
			Queue q = queues.getQueuesAtLink( pickUpLink ).getQueueForDestination( dest );

			if (q.size() > 0) {
				// return agents from the first non-empty queue
				Collection<MobsimAgent> passengers = q.callAgents( nPassengers );

				for (MobsimAgent p : passengers) {
					events.processEvent(
							new PassengerEndsWaitingEvent(
								now,
								p.getId(),
								pickUpLink ) );
				}

				return new Tuple<Id, Collection<MobsimAgent>>( dest , passengers );
			}
		}
		return null;
	}

	public void arrangePassengersArrivals(
			final Collection<MobsimAgent> passengers,
			final Id link,
			final double time) {
		for (MobsimAgent p : passengers) {
			p.notifyArrivalOnLinkByNonNetworkMode( link );
			p.endLegAndComputeNextState( time );
			internalInterface.arrangeNextAgentState( p );
		}
	}

	@Override
	public void doSimStep(final double time) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onPrepareSim() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void afterSim() {
		queues.endIteration(
			internalInterface.getMobsim().getSimTimer().getTimeOfDay(),
			events);
	}

	@Override
	public void setInternalInterface(final InternalInterface internalInterface) {
		this.internalInterface = internalInterface;
	}
}
