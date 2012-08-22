/* *********************************************************************** *
 * project: org.matsim.*
 * ParkNRideCostCalculator.java
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
package playground.thibautd.parknride.routingapproach;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.router.util.PersonalizableTravelTime;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.pt.router.TransitRouterNetwork.TransitRouterNetworkLink;
import org.matsim.pt.router.TransitRouterNetworkTravelTimeAndDisutility;
import org.matsim.vehicles.Vehicle;

import playground.thibautd.parknride.routingapproach.ParkAndRideRouterNetwork.ParkAndRideLink;

/**
 * A cost calculator for routing park and ride.
 * Idea: wraps an estimator for car, and one for pt, and is used on a ParkNRideNetwork
 *
 * @author thibautd
 */
public class ParkAndRideCostAggregator implements PersonalizableTravelTime, TravelDisutility {
	private final TravelTime carTravelTime;
	private final TravelDisutility carTravelCost;
	private final TransitRouterNetworkTravelTimeAndDisutility transitTravelTimeCost;
	private final TravelTime pnrTravelTime;
	private final TravelDisutility pnrTravelCost;

	public ParkAndRideCostAggregator(
			final TravelTime carTravelTime,
			final TravelDisutility carTravelCost,
			final TransitRouterNetworkTravelTimeAndDisutility transitTravelTimeCost,
			final TravelTime pnrTravelTime,
			final TravelDisutility pnrTravelCost) {
		this.carTravelTime = carTravelTime;
		this.carTravelCost = carTravelCost;
		this.transitTravelTimeCost = transitTravelTimeCost;
		this.pnrTravelTime = pnrTravelTime;
		this.pnrTravelCost = pnrTravelCost;
	}

	@Override
	public double getLinkTravelTime(
			final Link link,
			final double time) {
		if (link instanceof TransitRouterNetworkLink) {
			return transitTravelTimeCost.getLinkTravelTime( link , time );
		}
		if (link instanceof ParkAndRideLink) {
			return pnrTravelTime.getLinkTravelTime( link , time );
		}
		return carTravelTime.getLinkTravelTime( link , time );
	}

	@Override
	public double getLinkTravelDisutility(final Link link, final double time, final Person person, final Vehicle vehicle) {
		if (link instanceof TransitRouterNetworkLink) {
			return transitTravelTimeCost.getLinkTravelDisutility( link , time , person , vehicle );
		}
		if (link instanceof ParkAndRideLink) {
			return pnrTravelCost.getLinkTravelDisutility( link , time , person , vehicle );
		}
		return carTravelCost.getLinkTravelDisutility( link , time , person , vehicle);
	}
	
	@Override
	public double getLinkMinimumTravelDisutility(Link link) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setPerson(final Person person) {
		if (carTravelTime instanceof PersonalizableTravelTime) {
			((PersonalizableTravelTime) carTravelTime).setPerson( person );
		}
		if (pnrTravelTime instanceof PersonalizableTravelTime) {
			((PersonalizableTravelTime) pnrTravelTime).setPerson( person );
		}
		
	}
}
