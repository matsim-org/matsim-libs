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
package playground.thibautd.parknride;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.router.util.PersonalizableTravelCost;
import org.matsim.core.router.util.PersonalizableTravelTime;
import org.matsim.pt.router.TransitRouterNetwork.TransitRouterNetworkLink;
import org.matsim.pt.router.TransitRouterNetworkTravelTimeCost;

import playground.thibautd.parknride.ParkAndRideRouterNetwork.ParkAndRideLink;

/**
 * A cost calculator for routing park and ride.
 * Idea: wraps an estimator for car, and one for pt, and is used on a ParkNRideNetwork
 *
 * @author thibautd
 */
public class ParkAndRideCostAggregator implements PersonalizableTravelTime , PersonalizableTravelCost {
	private final PersonalizableTravelTime carTravelTime;
	private final PersonalizableTravelCost carTravelCost;
	private final TransitRouterNetworkTravelTimeCost transitTravelTimeCost;
	private final PersonalizableTravelTime pnrTravelTime;
	private final PersonalizableTravelCost pnrTravelCost;

	public ParkAndRideCostAggregator(
			final PersonalizableTravelTime carTravelTime,
			final PersonalizableTravelCost carTravelCost,
			final TransitRouterNetworkTravelTimeCost transitTravelTimeCost,
			final PersonalizableTravelTime pnrTravelTime,
			final PersonalizableTravelCost pnrTravelCost) {
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
	public double getLinkGeneralizedTravelCost(
			final Link link,
			final double time) {
		if (link instanceof TransitRouterNetworkLink) {
			return transitTravelTimeCost.getLinkGeneralizedTravelCost( link , time );
		}
		if (link instanceof ParkAndRideLink) {
			return pnrTravelCost.getLinkGeneralizedTravelCost( link , time );
		}
		return carTravelCost.getLinkGeneralizedTravelCost( link , time );
	}

	@Override
	public void setPerson(final Person person) {
		carTravelTime.setPerson( person );
		carTravelCost.setPerson( person );

		pnrTravelTime.setPerson( person );
		pnrTravelCost.setPerson( person );
	}
}
