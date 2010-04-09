/* *********************************************************************** *
 * project: org.matsim.*
 * TransitRouterNetworkCost.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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

package playground.mmoyo.ptRouterAdapted;

import org.matsim.api.core.v01.network.Link;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.misc.Time;
import org.matsim.pt.router.TransitRouterConfig;
import org.matsim.pt.router.TransitRouterNetworkTravelTimeCost;
import playground.mmoyo.PTRouter.PTValues;
import playground.mmoyo.ptRouterAdapted.TransitRouterNetwork2.TransitRouterNetworkLink;

public class TransitRouterNetworkTravelTimeCost2 extends TransitRouterNetworkTravelTimeCost {
	Link previousLink;
	double cachedTravelTime;
	double previousTime;
	private final static double MIDNIGHT = 24.0*3600;
	
	public TransitRouterNetworkTravelTimeCost2(TransitRouterConfig config) {
		super(config);
	}

	public double getLinkTravelCost(final Link link, final double time) {
		double cost;
		if (((TransitRouterNetworkLink) link).route == null) {
			// transfer link
			cost = (getLinkTravelTime(link, time) * PTValues.timeCoefficient) + PTValues.transferPenalty;
		} else {
			//pt link
			cost = (getLinkTravelTime(link, time) * PTValues.timeCoefficient) + (link.getLength() * PTValues.distanceCoefficient);
		}
		return cost;
	}

	public double getLinkTravelTime(final Link link, final double time) {
		if ((link == this.previousLink) && (time == this.previousTime)) {
			return this.cachedTravelTime;
		}
		this.previousLink = link;
		this.previousTime = time;

		TransitRouterNetworkLink wrapped = (TransitRouterNetworkLink) link;
		if (wrapped.route != null) {
			// agent stays on the same route, so use transit line travel time
			double bestDepartureTime = getNextDepartureTime(wrapped.route, wrapped.fromNode.stop, time);

			double arrivalOffset = (wrapped.toNode.stop.getArrivalOffset() != Time.UNDEFINED_TIME) ? wrapped.toNode.stop.getArrivalOffset() : wrapped.toNode.stop.getDepartureOffset();
			double time2 = (bestDepartureTime - time) + (arrivalOffset - wrapped.fromNode.stop.getDepartureOffset());
			if (time2 < 0) {
				time2 += MIDNIGHT;
			}
			this.cachedTravelTime = time2;
			return time2;
		}
		
		// different transit routes, so it must be a line switch
		double distance = CoordUtils.calcDistance(wrapped.fromNode.stop.getStopFacility().getCoord(), wrapped.toNode.stop.getStopFacility().getCoord());
		double time2 = distance * PTValues.AV_WALKING_SPEED; /***/
		this.cachedTravelTime = time2;
		return time2;
	}
	
}
