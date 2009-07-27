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

package playground.marcel.pt.router;

import org.matsim.core.api.experimental.network.Link;
import org.matsim.core.router.util.TravelCost;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.misc.Time;
import org.matsim.transitSchedule.api.Departure;

import playground.marcel.pt.router.TransitRouterNetworkWrapper.LinkWrapper;

/**
 * TravelTime and TravelCost calculator to be used with the transit network used for transit routing.
 *
 * @author mrieser
 */
public class TransitRouterNetworkTravelTimeCost implements TravelCost, TravelTime {

	private final static double MIDNIGHT = 24.0*3600;

	private final TransitRouterConfig config;
//	private final Link previousLink = null;
//	private final double previousTime = Double.NaN;
//	private double cachedTravelTime = Double.NaN;

	public TransitRouterNetworkTravelTimeCost(final TransitRouterConfig config) {
		this.config = config;
	}

	public double getLinkTravelCost(final Link link, final double time) {
		double cost;
		if (((LinkWrapper) link).link.route == null) {
			// it's a transfer link (walk)
			cost = -getLinkTravelTime(link, time) * this.config.marginalUtilityOfTravelTimeWalk + this.config.costLineSwitch;
		} else {
			cost = -getLinkTravelTime(link, time) * this.config.marginalUtilityOfTravelTimeTransit;
		}
//		System.out.println(((LinkWrapper)link).link.fromNode.stop.getStopFacility().getId() + " c " + ((LinkWrapper)link).link.toNode.stop.getStopFacility().getId() + " = " + cost);
		return cost;
	}

	public double getLinkTravelTime(final Link link, final double time) {
//		if ((link == this.previousLink) && (time == this.previousTime)) {
//			return this.cachedTravelTime;
//		}
//		this.previousLink = link;
//		this.previousTime = time;

		LinkWrapper wrapped = (LinkWrapper) link;
		if (wrapped.link.route != null) {
			// agent stays on the same route, so use transit line travel time
			// TODO [MR] very similar code to TransitRouter.getNextDepartureTime, consolidate
			double earliestDepartureTime = time - wrapped.link.fromNode.stop.getDepartureOffset();
			if (earliestDepartureTime >= MIDNIGHT) {
				earliestDepartureTime = earliestDepartureTime % MIDNIGHT;
			}
			double bestDepartureTime = Double.POSITIVE_INFINITY;
			for (Departure dep : wrapped.link.route.getDepartures().values()) {
				// TODO [MR] replace linear search with something faster
				double depTime = dep.getDepartureTime();
				if (depTime >= MIDNIGHT) {
					depTime = depTime % MIDNIGHT;
				}
				if (depTime >= earliestDepartureTime && depTime < bestDepartureTime) {
					bestDepartureTime = depTime;
				}
			}
			if (bestDepartureTime == Double.POSITIVE_INFINITY) {
				// okay, seems we didn't find anything usable, so take the first one in the morning
				for (Departure dep : wrapped.link.route.getDepartures().values()) {
					double depTime = dep.getDepartureTime();
					if (depTime >= MIDNIGHT) {
						depTime = depTime % MIDNIGHT;
					}
					if (depTime < bestDepartureTime) {
						bestDepartureTime = depTime;
					}
				}
			}
			double arrivalOffset = (wrapped.link.toNode.stop.getArrivalOffset() != Time.UNDEFINED_TIME) ? wrapped.link.toNode.stop.getArrivalOffset() : wrapped.link.toNode.stop.getDepartureOffset();
			double time2 = (bestDepartureTime - earliestDepartureTime) + (arrivalOffset - wrapped.link.fromNode.stop.getDepartureOffset());
			if (time2 < 0) {
				time2 += MIDNIGHT;
			}
//			System.out.print(wrapped.link.fromNode.stop.getStopFacility().getId() + " > " + wrapped.link.toNode.stop.getStopFacility().getId() + " = " + (int) time2 + "\t@ " + Time.writeTime(time));
//			System.out.println(" wait-time: " + (int) (bestDepartureTime - earliestDepartureTime) + " travel-time: " + (int) (arrivalOffset - wrapped.link.fromNode.stop.getDepartureOffset()));
//			this.cachedTravelTime = time2;
			return time2;
		}
		// different transit routes, so it must be a line switch
		double distance = CoordUtils.calcDistance(wrapped.link.fromNode.stop.getStopFacility().getCoord(), wrapped.link.toNode.stop.getStopFacility().getCoord());
		double time2 = distance / this.config.beelineWalkSpeed;
//		System.out.println(wrapped.link.fromNode.stop.getStopFacility().getId() + "..." + wrapped.link.toNode.stop.getStopFacility().getId() + " = " + (int) time2 + "\t@ " + Time.writeTime(time));
//		this.cachedTravelTime = time2;
		return time2;
	}

}
