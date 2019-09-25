/*
 * *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2019 by the members listed in the COPYING,        *
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
 * *********************************************************************** *
 */

package org.matsim.contrib.dvrp.passenger;

import java.util.Map;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.mobsim.qsim.interfaces.TripInfoRequest;
import org.matsim.core.mobsim.qsim.interfaces.TripInfoWithRequiredBooking;
import org.matsim.facilities.Facility;

/**
 * @author Michal Maciejewski (michalm)
 */
class DvrpTripInfo implements TripInfoWithRequiredBooking {
	private final String mode;
	private final Link pickupLink;
	private final Link dropoffLink;
	private final double departureTime;
	private final double timeStamp;
	private final TripInfoRequest originalRequest;

	public DvrpTripInfo( String mode, Link pickupLink, Link dropoffLink, double departureTime, double timeStamp,
				   TripInfoRequest originalRequest ) {
		this.mode = mode;
		this.pickupLink = pickupLink;
		this.dropoffLink = dropoffLink;
		this.departureTime = departureTime;
		this.timeStamp = timeStamp;
		this.originalRequest = originalRequest ;
	}

	@Override
	public Facility getPickupLocation() {
		return createAdHocFacility(pickupLink);
	}

	@Override
	public double getExpectedBoardingTime() {
		return departureTime;
	}

	@Override
	public Facility getDropoffLocation() {
		return createAdHocFacility(dropoffLink);
	}

	@Override
	public double getExpectedTravelTime() {
		throw new RuntimeException("not implemented");
	}

	@Override
	public double getMonetaryPrice() {
		throw new RuntimeException("not implemented");
	}

	@Override
	public Map<String, String> getAdditionalAttributes() {
		return null;
	}

	@Override
	public String getMode() {
		return mode;
	}

	@Override
	public double getLatestDecisionTime() {
		// we currently allow only one time step to make the decision:
		return timeStamp + 1;
	}

	private static Facility createAdHocFacility(Link link) {
		return new Facility() {
			@Override public Id<Facility> getId(){
				throw new RuntimeException( "not implemented" );
			}
			@Override
			public Map<String, Object> getCustomAttributes() {
				throw new RuntimeException("not implemented");
			}

			@Override
			public Coord getCoord() {
//				throw new RuntimeException("not implemented");
				return null ;
			}

			@Override
			public Id<Link> getLinkId() {
				return link.getId();
			}
		};
	}

	@Override
	public TripInfoRequest getOriginalRequest(){
		return originalRequest;
	}
}
