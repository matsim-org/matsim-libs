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
package eu.eunoiaproject.bikesharing.qsim;

import java.util.Iterator;

import org.apache.log4j.Logger;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.mobsim.framework.DriverAgent;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.qsim.interfaces.MobsimVehicle;

import eu.eunoiaproject.bikesharing.scenario.BikeSharingFacility;

/**
 * @author thibautd
 */
public class SimplisticRelocationAgent implements MobsimAgent, DriverAgent {
	private static final Logger log =
		Logger.getLogger(SimplisticRelocationAgent.class);

	private final Id id;

	private Id currentLinkId;
	private Id destinationLinkId;
	private Iterator<Id> currentRoute = null;
	private int load = 0;

	private State state = State.LEG;

	public SimplisticRelocationAgent(
			final Id id,
			final Id initialLinkId) {
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
			final Iterable<Id> route) {
		this.destinationLinkId = destination.getLinkId();
		this.currentRoute = route.iterator();
	}

	// ///////////////////////////////////////////////////////////////////////
	// agent interface
	@Override
	public Id getCurrentLinkId() {
		return currentLinkId;
	}

	@Override
	public Id getDestinationLinkId() {
		return destinationLinkId;
	}

	@Override
	public Id getId() {
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
	}

	@Override
	public void abort(double now) {
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
	public void notifyArrivalOnLinkByNonNetworkMode(Id linkId) {
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
	public Id getPlannedVehicleId() {
		return getId();
	}

	@Override
	public Id chooseNextLinkId() {
		return currentRoute.next();
	}

	@Override
	public void notifyMoveOverNode(final Id newLinkId) {
		this.currentLinkId = newLinkId;
	}
}

