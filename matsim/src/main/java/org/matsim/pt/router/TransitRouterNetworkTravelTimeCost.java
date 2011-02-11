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

package org.matsim.pt.router;

import java.util.Arrays;
import java.util.HashMap;

import org.matsim.api.core.v01.network.Link;
import org.matsim.core.router.util.TravelCost;
import org.matsim.core.router.util.TravelMinCost;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.utils.misc.Time;
import org.matsim.pt.router.TransitRouterNetwork.TransitRouterNetworkLink;
import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;

/**
 * TravelTime and TravelCost calculator to be used with the transit network used for transit routing.
 *
 * <em>This class is NOT thread-safe!</em>
 *
 * @author mrieser
 */
public class TransitRouterNetworkTravelTimeCost implements TravelTime, TravelMinCost, TravelCost {

	private final static double MIDNIGHT = 24.0*3600;

	protected final TransitRouterConfig config;
	private Link previousLink = null;
	private double previousTime = Double.NaN;
	private double cachedTravelTime = Double.NaN;

	public TransitRouterNetworkTravelTimeCost(final TransitRouterConfig config) {
		this.config = config;
	}

	@Override
	public double getLinkGeneralizedTravelCost(final Link link, final double time) {
		double cost;
		if (((TransitRouterNetworkLink) link).route == null) {
			// it's a transfer link (walk)
//			cost = -getLinkTravelTime(link, time) * this.config.getEffectiveMarginalUtilityOfTravelTimeWalk_utl_s() + this.config.getUtilityOfLineSwitch_utl();
			cost = -getLinkTravelTime(link, time) * this.config.getEffectiveMarginalUtilityOfTravelTimeWalk_utl_s() - this.config.getUtilityOfLineSwitch_utl();
		} else {
			cost = -getLinkTravelTime(link, time) * this.config.getEffectiveMarginalUtilityOfTravelTimePt_utl_s() - link.getLength() * this.config.getMarginalUtilityOfTravelDistancePt_utl_m();
		}
		return cost;
	}

	@Override
	public double getLinkMinimumTravelCost(Link link) {
		double cost;
		if (((TransitRouterNetworkLink) link).route == null) {
			// it's a transfer link (walk)
//			cost = -getMinLinkTravelTime(link) * this.config.getEffectiveMarginalUtilityOfTravelTimeWalk_utl_s() + this.config.getUtilityOfLineSwitch_utl();
			cost = -getMinLinkTravelTime(link) * this.config.getEffectiveMarginalUtilityOfTravelTimeWalk_utl_s() - this.config.getUtilityOfLineSwitch_utl();
		} else {
			cost = -getMinLinkTravelTime(link) * this.config.getEffectiveMarginalUtilityOfTravelTimePt_utl_s() - link.getLength() * this.config.getMarginalUtilityOfTravelDistancePt_utl_m();
		}
		return cost;
	}

	private double getMinLinkTravelTime(final Link link) {
		TransitRouterNetworkLink wrapped = (TransitRouterNetworkLink) link;
		if (wrapped.route != null) {
			// agent stays on the same route, so use transit line travel time
			double arrivalOffset = (wrapped.toNode.stop.getArrivalOffset() != Time.UNDEFINED_TIME) ? wrapped.toNode.stop.getArrivalOffset() : wrapped.toNode.stop.getDepartureOffset();
			return arrivalOffset - wrapped.fromNode.stop.getDepartureOffset();
		}
		// different transit routes, so it must be a line switch
		double time2 = wrapped.getLength() / this.config.getBeelineWalkSpeed();
		return time2;
	}


	@Override
	public double getLinkTravelTime(final Link link, final double time) {
		if ((link == this.previousLink) && (time == this.previousTime)) {
			return this.cachedTravelTime;
		}
		this.previousLink = link;
		this.previousTime = time;

		TransitRouterNetworkLink wrapped = (TransitRouterNetworkLink) link;
		TransitRouteStop fromStop = wrapped.fromNode.stop;
		TransitRouteStop toStop = wrapped.toNode.stop;
		if (wrapped.route != null) {
			// agent stays on the same route, so use transit line travel time
			double bestDepartureTime = getNextDepartureTime(wrapped.route, fromStop, time);

			double arrivalOffset = (toStop.getArrivalOffset() != Time.UNDEFINED_TIME) ? toStop.getArrivalOffset() : toStop.getDepartureOffset();
			double time2 = (bestDepartureTime - time) + (arrivalOffset - fromStop.getDepartureOffset());
			if (time2 < 0) {
				time2 += MIDNIGHT;
			}
			this.cachedTravelTime = time2;
			return time2;
		}
		// different transit routes, so it must be a line switch
		double distance = wrapped.getLength();
		double time2 = distance / this.config.getBeelineWalkSpeed();
		this.cachedTravelTime = time2;
		return time2;
	}

	private final HashMap<TransitRoute, double[]> sortedDepartureCache = new HashMap<TransitRoute, double[]>();

	public double getNextDepartureTime(final TransitRoute route, final TransitRouteStop stop, final double depTime) {
		double earliestDepartureTime = depTime - stop.getDepartureOffset();

		if (earliestDepartureTime >= MIDNIGHT) {
			earliestDepartureTime = earliestDepartureTime % MIDNIGHT;
		}

		double[] cache = this.sortedDepartureCache.get(route);
		if (cache == null) {
			cache = new double[route.getDepartures().size()];
			int i = 0;
			for (Departure dep : route.getDepartures().values()) {
				cache[i++] = dep.getDepartureTime();
			}
			Arrays.sort(cache);
			this.sortedDepartureCache.put(route, cache);
		}
		int pos = Arrays.binarySearch(cache, earliestDepartureTime);
		if (pos < 0) {
			pos = -(pos + 1);
		}
		if (pos >= cache.length) {
			pos = 0; // there is no later departure time, take the first in the morning
		}
		double bestDepartureTime = cache[pos];

		bestDepartureTime += stop.getDepartureOffset();
		while (bestDepartureTime < depTime) {
			bestDepartureTime += MIDNIGHT;
		}
		return bestDepartureTime;
	}

}
