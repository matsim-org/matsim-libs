/* *********************************************************************** *
 * project: org.matsim.*
 * VehicularTeleportationEngine.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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
package playground.thibautd.pseudoqsim;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.PersonStuckEvent;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.api.experimental.events.TeleportationArrivalEvent;
import org.matsim.core.config.groups.QSimConfigGroup.VehicleBehavior;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.framework.MobsimDriverAgent;
import org.matsim.core.mobsim.framework.PlanAgent;
import org.matsim.core.mobsim.qsim.InternalInterface;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.interfaces.DepartureHandler;
import org.matsim.core.mobsim.qsim.interfaces.MobsimEngine;
import org.matsim.core.mobsim.qsim.qnetsimengine.QVehicle;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.misc.Time;

import java.util.*;

/**
 * Handles vehicular modes using teleportation.
 * This will result in NO link enter/leave events,
 * but vehicles will still move around, and agents wait for them
 * if necessary.
 * @author thibautd
 */
public class VehicularTeleportationEngine implements DepartureHandler, MobsimEngine {
	private static final Logger log =
		Logger.getLogger(VehicularTeleportationEngine.class);

	/**
	 * Includes all agents that have transportation modes unknown to the
	 * QueueSimulation (i.e. != "car") or have two activities on the same link
	 */
	private final Queue<Tuple<Double, MobsimAgent>> teleportationList = new PriorityQueue< >(
			30, new Comparator<Tuple<Double, MobsimAgent>>() {

		@Override
		public int compare(Tuple<Double, MobsimAgent> o1, Tuple<Double, MobsimAgent> o2) {
			int ret = o1.getFirst().compareTo(o2.getFirst()); // first compare time information
			if (ret == 0) {
				ret = o2.getSecond().getId().compareTo(o1.getSecond().getId()); // if they're equal, compare the Ids: the one with the larger Id should be first
			}
			return ret;
		}
	});
	private InternalInterface internalInterface;

	private final Set<String> vehicularModes;
	private final VehicleBehavior vehicleBehavior;

	private final ParkedVehicleProvider vehicleProvider;

	private int teleported = 0;

	public VehicularTeleportationEngine(
			final Collection<String> modes,
			final ParkedVehicleProvider vehicleProvider,
			final VehicleBehavior vehicleBehavior) {
		this.vehicularModes = new HashSet<String>( modes );
		this.vehicleBehavior = vehicleBehavior;
		this.vehicleProvider = vehicleProvider;
	}

	@Override
	public boolean handleDeparture(
			final double now,
			final MobsimAgent agent,
			final Id linkId) {
		if ( !vehicularModes.contains( agent.getMode() ) ) return false;

		final Id vehicleId = ((MobsimDriverAgent) agent).getPlannedVehicleId() ;
		if ( !vehicleProvider.unpark( vehicleId , linkId ) ) {
			switch ( vehicleBehavior ) {
			case teleport:
				final Id parkLink = vehicleProvider.unpark( vehicleId );
				if ( parkLink == null ) {
					throw new IllegalStateException( "vehicle "+vehicleId+" is not parked anywhere. Cannot teleport it!" );
				}
				// "teleport" vehicle before handling 
				if ( teleported++ < 10 ) {
					log.warn( "teleporting vehicle "+vehicleId+" from link "+parkLink+" to "+linkId );
					if ( teleported == 10 ) log.warn( "future occurences of this warning are suppressed" );
				}
				final QVehicle veh = vehicleProvider.getVehicle( vehicleId );
				veh.setCurrentLink( ((QSim) internalInterface.getMobsim()).getScenario().getNetwork().getLinks().get( linkId ) );
				handleDeparture( now , (MobsimDriverAgent) agent );
				return true;
			case wait:
			case exception:
			default:
				throw new IllegalArgumentException( "unhandled behavior "+vehicleBehavior );
			}
		}

		handleDeparture( now , (MobsimDriverAgent) agent );

		return true;
	}

	@Override
	public void doSimStep(double time) {
		//handlePendingDepartures( time );
		handleTeleportationArrivals( time );
	}

	//private void handlePendingDepartures(final double now) {
	//	// TODO handle vehicle waiting
	//}

	private void handleDeparture(
			final double now,
			final MobsimDriverAgent agent) {
		final Id vehicleId = agent.getPlannedVehicleId() ;
		final QVehicle vehicle = vehicleProvider.getVehicle( vehicleId );

		vehicle.setDriver(agent);
		agent.setVehicle(vehicle) ;

		final double travelTime = agent.getExpectedTravelTime();
		if ( Double.isNaN( travelTime ) || Time.UNDEFINED_TIME == travelTime ) {
			throw new RuntimeException( "illegal travel time "+travelTime );
		}

		this.teleportationList.add(
				new Tuple<Double, MobsimAgent>(
					now + travelTime,
					agent));
	}

	private void handleTeleportationArrivals( final double now ) {
		while (teleportationList.peek() != null) {
			final Tuple<Double, MobsimAgent> entry = teleportationList.peek();
			if (entry.getFirst().doubleValue() > now) {
				break;
			}

			teleportationList.poll();
			final MobsimAgent personAgent = entry.getSecond();

			final QVehicle veh = (QVehicle) ((MobsimDriverAgent) personAgent).getVehicle();
			((MobsimDriverAgent) personAgent).setVehicle( null );
			veh.setDriver( null );
			veh.setCurrentLink(
					((QSim) internalInterface.getMobsim()).getScenario().getNetwork().getLinks().get(
						personAgent.getDestinationLinkId() ) );
			vehicleProvider.park( veh.getId() , personAgent.getDestinationLinkId() );

			personAgent.notifyArrivalOnLinkByNonNetworkMode(
					personAgent.getDestinationLinkId());
			final double distance = ((Leg) ((PlanAgent) personAgent).getCurrentPlanElement()).getRoute().getDistance();
			((QSim) this.internalInterface.getMobsim()).getEventsManager().processEvent(
					new TeleportationArrivalEvent(
						this.internalInterface.getMobsim().getSimTimer().getTimeOfDay(),
						personAgent.getId(),
						distance));
			personAgent.endLegAndComputeNextState(now);
			internalInterface.arrangeNextAgentState(personAgent);
		}
	}

	@Override
	public void onPrepareSim() {}

	@Override
	public void afterSim() {
		final double now = internalInterface.getMobsim().getSimTimer().getTimeOfDay();
		for (Tuple<Double, MobsimAgent> entry : teleportationList) {
			final MobsimAgent agent = entry.getSecond();
			final EventsManager eventsManager =
					((QSim) internalInterface.getMobsim()).getEventsManager();
			eventsManager.processEvent(
					new PersonStuckEvent(
						now,
						agent.getId(),
						agent.getDestinationLinkId(),
						agent.getMode()));
		}
		teleportationList.clear();

		log.info( teleported+" vehicles had to be teleported to their trip starting point." );
	}

	@Override
	public void setInternalInterface(InternalInterface internalInterface) {
		this.internalInterface = internalInterface;
	}
}
