/* *********************************************************************** *
 * project: org.matsim.*
 * PseudoQsimEngine.java
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
package playground.thibautd.mobsim.pseudoqsimengine;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;

import org.matsim.api.core.v01.events.PersonStuckEvent;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.PersonLeavesVehicleEvent;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.mobsim.framework.DriverAgent;
import org.matsim.core.mobsim.framework.HasPerson;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.framework.MobsimDriverAgent;
import org.matsim.core.mobsim.qsim.InternalInterface;
import org.matsim.core.mobsim.qsim.interfaces.DepartureHandler;
import org.matsim.core.mobsim.qsim.interfaces.MobsimEngine;
import org.matsim.core.mobsim.qsim.pt.AbstractTransitDriver;
import org.matsim.core.mobsim.qsim.qnetsimengine.QVehicle;
import org.matsim.core.router.util.TravelTime;
import org.matsim.vehicles.VehicleUtils;

/**
 * An engine aimed at replacing the QnetsimEngine for a "PSim" like behavior.
 * The advantage compared to the PSim is that the rest of the QSim architecture
 * is still used.
 *
 * This is, for instance, very useful if the results of the simulation depend
 * on special agents, such as is the case for ride sharing.
 *
 * It does NOT allow to reuse special QLink implementations, for obvious reasons
 * @author thibautd
 */
public class PseudoQsimEngine implements MobsimEngine, DepartureHandler {
	private final Collection<String> transportModes;
	private final TravelTime travelTimeCalculator;
	private final Network network;

	private final Queue<InternalArrivalEvent> arrivalQueue = new PriorityQueue<InternalArrivalEvent>();
	private final Map<Id, QVehicle> vehicles = new HashMap<Id, QVehicle>();

	private InternalInterface internalInterface = null;
	
	public PseudoQsimEngine(
			final Collection<String> transportModes,
			final TravelTime travelTimeCalculator,
			final Network network) {
		this.transportModes = transportModes;
		this.travelTimeCalculator = travelTimeCalculator;
		this.network = network;
	}

	@Override
	public void doSimStep(final double time) {
		// TODO: handle transit drivers their own way.
		while ( !arrivalQueue.isEmpty() &&
				arrivalQueue.peek().time <= time ) {
			final InternalArrivalEvent event = arrivalQueue.poll();
			final MobsimDriverAgent agent = event.vehicle.getDriver();
			final Id nextLinkId = agent.chooseNextLinkId();

			final EventsManager eventsManager =
				internalInterface.getMobsim().getEventsManager();
			if ( nextLinkId != null ) {
				eventsManager.processEvent(
					new LinkLeaveEvent(
						time,
						agent.getId(),
						event.linkId,
						event.vehicle.getId() ) );

				agent.notifyMoveOverNode( nextLinkId );

				arrivalQueue.add(
						calcArrival(
							time,
							event.vehicle,
							nextLinkId) );
			}
			else {
				eventsManager.processEvent(
						new PersonLeavesVehicleEvent(
							time,
							agent.getId(),
							event.vehicle.getId()));
				// reset vehicles driver
				event.vehicle.setDriver(null);

				agent.endLegAndComputeNextState( time );
				internalInterface.arrangeNextAgentState( agent );
			}
		}
	}

	@Override
	public boolean handleDeparture(
			final double now,
			final MobsimAgent magent,
			final Id linkId) {
		if ( !transportModes.contains( magent.getMode() ) ) return false;

		// code adapted from VehicularDepartureHandler
		
		final DriverAgent agent = (DriverAgent) magent;

		final Id vehicleId = agent.getPlannedVehicleId() ;
		// TODO: check that vehicle not in use
		// TODO: insert vehicles at beginning and track them on links
		// (NetsimEngine like).
		// The problem is that currently only the NetsimEngine can do that.
		final QVehicle vehicle = getVehicle( vehicleId );

		// Treat the situation where startLink == endLink.
		// Transit vehicles do this differently than others, because there could be a stop on it.
		// Other vehicles _do not_ traverse their only link but arrive right away.
		if ( ! (agent instanceof AbstractTransitDriver) ) { 	
			if (linkId.equals(agent.getDestinationLinkId())) {
				if ( agent.chooseNextLinkId() == null ) {
                    vehicle.setDriver(agent);
                    agent.setVehicle(vehicle);

					final EventsManager eventsManager = internalInterface.getMobsim().getEventsManager();
					eventsManager.processEvent(
							new PersonEntersVehicleEvent(
								now,
								agent.getId(),
								vehicleId) );

					magent.endLegAndComputeNextState(now) ;
					this.internalInterface.arrangeNextAgentState(magent) ;
					return true;
				}
			}
		}		

		if (vehicle == null) {
			throw new RuntimeException( "TODO" );
			//if (vehicleBehavior == VehicleBehavior.TELEPORT) {
			//	vehicle = qNetsimEngine.getVehicles().get(vehicleId);
			//	if ( vehicle==null ) {
			//		throw new RuntimeException("could not find requested vehicle in simulation; aborting ...") ;
			//	}
			//	teleportVehicleTo(vehicle, linkId);

			//	vehicle.setDriver(agent);
			//	agent.setVehicle(vehicle) ;

			//	qlink.letVehicleDepart(vehicle, now);
			//	// (since the "teleportVehicle" does not physically move the vehicle, this is finally achieved in the departure
			//	// logic.  kai, nov'11)
			//} else if (vehicleBehavior == VehicleBehavior.WAIT_UNTIL_IT_COMES_ALONG) {
			//	// While we are waiting for our car
			//	qlink.registerDriverAgentWaitingForCar(agent);
			//} else {
			//	throw new RuntimeException("vehicle " + vehicleId + " not available for agent " + agent.getId() + " on link " + linkId);
			//}
		}
		else {
			vehicle.setDriver(agent);
			agent.setVehicle(vehicle) ;

			final EventsManager eventsManager = internalInterface.getMobsim().getEventsManager();
			eventsManager.processEvent(
					new PersonEntersVehicleEvent(
						now,
						agent.getId(),
						vehicleId) );

			//vehicle.setCurrentLink( linkId );

			arrivalQueue.add(
					calcArrival(
						now,
						vehicle,
						linkId ) );

			eventsManager.processEvent(
					new LinkEnterEvent(
						now,
						agent.getId(),
						linkId,
						vehicleId) );
		}

		return true;
	}

	private QVehicle getVehicle(final Id id) {
		QVehicle v = vehicles.get( id );

		if ( v == null ) {
			v = new QVehicle(
				VehicleUtils.getFactory().createVehicle(
					id,
					VehicleUtils.getDefaultVehicleType() ) );
			vehicles.put( id , v );
		}

		return v;
	}

	private InternalArrivalEvent calcArrival(
			final double now,
			final QVehicle vehicle,
			final Id linkId) {
		final double travelTime =
			travelTimeCalculator.getLinkTravelTime(
					network.getLinks().get( linkId ),
					now,
					((HasPerson) vehicle.getDriver()).getPerson(),
					null );
		return new InternalArrivalEvent(
				now + travelTime,
				linkId,
				vehicle);
	}

	@Override
	public void onPrepareSim() {}

	@Override
	public void afterSim() {
		for (InternalArrivalEvent event : arrivalQueue) {
			final QVehicle veh = event.vehicle;
			internalInterface.getMobsim().getEventsManager().processEvent(
					new PersonStuckEvent(
						internalInterface.getMobsim().getSimTimer().getTimeOfDay(),
						veh.getDriver().getId(),
						veh.getCurrentLink().getId(),
						veh.getDriver().getMode()));
			internalInterface.getMobsim().getAgentCounter().incLost();
			internalInterface.getMobsim().getAgentCounter().decLiving();
		}
	}

	@Override
	public void setInternalInterface(final InternalInterface internalInterface) {
		this.internalInterface = internalInterface;
	}

	private static class InternalArrivalEvent implements Comparable<InternalArrivalEvent> {
		private final double time;
		private final Id linkId;
		private final QVehicle vehicle;

		public InternalArrivalEvent(
				final double time,
				final Id linkId,
				final QVehicle vehicle) {
			this.time = time;
			this.linkId = linkId;
			this.vehicle = vehicle;
		}

		@Override
		public int compareTo(final InternalArrivalEvent o) {
			return Double.compare( time , o.time );
		}
	}
}

