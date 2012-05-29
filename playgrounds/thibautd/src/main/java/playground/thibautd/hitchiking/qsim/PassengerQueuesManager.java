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

import java.util.Collection;
import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.qsim.interfaces.MobsimEngine;
import org.matsim.core.mobsim.qsim.InternalInterface;
import org.matsim.core.utils.collections.Tuple;

import playground.thibautd.hitchiking.qsim.events.PassengerEndsWaitingEvent;
import playground.thibautd.hitchiking.qsim.events.PassengerStartsWaitingEvent;
import playground.thibautd.hitchiking.qsim.PassengerQueuesPerLink.Queue;

/**
 * @author thibautd
 */
public class PassengerQueuesManager implements MobsimEngine {
	private final PassengerQueuesPerLink queues = new PassengerQueuesPerLink();
	private EventsManager events;
	private InternalInterface internalInterface = null;

	public PassengerQueuesManager(final EventsManager events) {
		this.events = events;
	}

	public void passengerArrivesAndWaits(
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

	public Tuple<Id, Collection<MobsimAgent>> getPassengersFromFirstNonEmptyQueue(
			final double now,
			final Id pickUpLink,
			final List<Id> possibleDestinations,
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
			p.notifyTeleportToLink( link );
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
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setInternalInterface(final InternalInterface internalInterface) {
		this.internalInterface = internalInterface;
	}
}
