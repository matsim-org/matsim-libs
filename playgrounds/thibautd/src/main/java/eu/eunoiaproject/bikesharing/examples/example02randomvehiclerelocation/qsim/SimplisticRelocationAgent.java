/* *********************************************************************** *
 * project: org.matsim.*
 * SimplisticRelocationAgent.java
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
package eu.eunoiaproject.bikesharing.examples.example02randomvehiclerelocation.qsim;

import java.util.Iterator;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.mobsim.framework.MobsimDriverAgent;
import org.matsim.core.mobsim.qsim.interfaces.MobsimVehicle;
import org.matsim.vehicles.Vehicle;

import eu.eunoiaproject.bikesharing.framework.scenario.BikeSharingFacility;

/**
 * An agent driving around randomly to relocate bikes.
 * For demonstration purpose only.
 * @author thibautd
 */
public class SimplisticRelocationAgent implements MobsimDriverAgent /*MobsimAgent, DriverAgent*/ {
	private static final Logger log =
		Logger.getLogger(SimplisticRelocationAgent.class);

	private final Id<Person> id;

	private Id<Link> currentLinkId = null;
	private Id<Link> destinationLinkId = null;
	private Id <Link>nextLinkId = null;
	private Iterator<Id<Link>> currentRoute = null;
	private int load = 0;

	private State state = State.LEG;

	private final EventsManager events;

	public SimplisticRelocationAgent(
			final EventsManager events,
			final Id<Person> id,
			final Id<Link> initialLinkId) {
		this.events = events;
		this.id = id;
		this.currentLinkId = initialLinkId;
	}

	public void loadBikes(final int nBikes) {
		if ( nBikes < 0 ) throw new IllegalArgumentException( "cannot load negative amount of bikes "+nBikes );
		this.load += nBikes;
	}

	public int getLoad() {
		return this.load;
	}

	public int unloadBikes( final int desiredAmount ) {
		if ( desiredAmount < 0 ) throw new IllegalArgumentException( "cannot unload negative amount of bikes "+desiredAmount );

		assert load >= 0;
		// cannot return more bikes than available
		final int actualAmount = Math.min( desiredAmount , load );
		load -= actualAmount;
		assert load >= 0;

		return actualAmount;
	}

	public void setNextDestination(
			final BikeSharingFacility destination,
			final Iterable<Id<Link>> route) {
		this.destinationLinkId = destination.getLinkId();
		this.currentRoute = route.iterator();
		this.nextLinkId = null;
		this.state = State.LEG;
	}

	// ///////////////////////////////////////////////////////////////////////
	// agent interface
	@Override
	public Id<Link> getCurrentLinkId() {
		return currentLinkId;
	}

	@Override
	public Id<Link> getDestinationLinkId() {
		return destinationLinkId;
	}

	@Override
	public Id<Person> getId() {
		return id;
	}

	@Override
	public State getState() {
		return state;
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
		destinationLinkId = null;
		currentRoute = null;
		// we want the agent to be handled by its manager
		state = State.ACTIVITY;

		// yes, the AGENT is responsible of throwing the arrival event.
		events.processEvent(
				new PersonArrivalEvent(
					now,
					getId(),
					getDestinationLinkId(),
					getMode() ) );
	}

	@Override
	public void setStateToAbort(double now) {
		log.warn( "ABORTING bike sharing relocator "+getId() );
		this.state = State.ABORT;
	}

	@Override
	public Double getExpectedTravelTime() {
		throw new UnsupportedOperationException();
	}

	@Override
	public String getMode() {
		return TransportMode.car;
	}

	@Override
	public void notifyArrivalOnLinkByNonNetworkMode(Id<Link> linkId) {
		throw new RuntimeException( getClass().getName()+" should not use non-network modes!" );
	}

	private MobsimVehicle veh = null;
	@Override
	public void setVehicle(final MobsimVehicle veh) {
		this.veh = veh;
	}

	@Override
	public MobsimVehicle getVehicle() {
		return veh;
	}

	@Override
	public Id<Vehicle> getPlannedVehicleId() {
		return Id.create(getId(), Vehicle.class);
	}

	@Override
	public Id<Link> chooseNextLinkId() {
		assert destinationLinkId != null;
		if ( currentLinkId.equals( destinationLinkId ) ) return null;
		while ( nextLinkId == null || nextLinkId.equals( currentLinkId ) ) {
			nextLinkId = currentRoute.hasNext() ? currentRoute.next() : destinationLinkId;
		}

		return nextLinkId;
	}

	@Override
	public void notifyMoveOverNode(final Id<Link> newLinkId) {
		if ( !newLinkId.equals( nextLinkId ) ) {
			throw new RuntimeException( "unexpected next link "+newLinkId+": expected "+nextLinkId  );
		}

		this.currentLinkId = newLinkId;
		this.nextLinkId = null;
	}
}

