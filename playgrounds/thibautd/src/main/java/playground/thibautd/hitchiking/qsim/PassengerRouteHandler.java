/* *********************************************************************** *
 * project: org.matsim.*
 * PassengerRouteHandler.java
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
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.mobsim.framework.MobsimAgent.State;
import playground.thibautd.hitchiking.HitchHikingConstants;

/**
 * The handler for the passenger mode.
 * Actually does not do anything special.
 * @author thibautd
 */
public class PassengerRouteHandler implements HitchHikingHandler {
	private final Route route;
	private boolean arrived = false;

	public PassengerRouteHandler(
			final Route route) {
		this.route = route;
	}

	@Override
	public Id getCurrentLinkId() {
		if (arrived) {
			return route.getEndLinkId();
		}
		else {
			return route.getStartLinkId();
		}
	}

	@Override
	public Id chooseNextLinkId() {
		throw new UnsupportedOperationException( "The passenger does not have to choose a route!" );
	}

	@Override
	public Id getDestinationLinkId() {
		return route.getEndLinkId();
	}

	@Override
	public State getState() {
		return State.LEG;
	}

	@Override
	public double getActivityEndTime() {
		throw new IllegalStateException( "not in activity" );
	}

	@Override
	public boolean endActivityAndComputeNextState(double now) {
		throw new IllegalStateException( "not in activity" );
	}

	@Override
	public boolean endLegAndComputeNextState(double now) {
		arrived = true;
		return false;
	}

	@Override
	public String getMode() {
		return HitchHikingConstants.PASSENGER_MODE;
	}

	@Override
	public void notifyMoveOverNode(Id newLinkId) {
		// TODO Auto-generated method stub
		
	}
}

