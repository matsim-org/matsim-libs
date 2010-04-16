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

import java.util.Arrays;
import java.util.HashMap;

import org.matsim.api.core.v01.network.Link;
import org.matsim.core.router.util.TravelCost;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.misc.Time;
//import org.matsim.pt.router.TransitRouterNetwork.TransitRouterNetworkLink;
import org.matsim.pt.router.TransitRouterConfig;
import org.matsim.transitSchedule.api.Departure;
import org.matsim.transitSchedule.api.TransitRoute;
import org.matsim.transitSchedule.api.TransitRouteStop;

import playground.mmoyo.PTRouter.PTValues;
import playground.mmoyo.ptRouterAdapted.AdaptedTransitRouterNetwork.TransitRouterNetworkLink;

/**
 * TravelTime and TravelCost calculator to be used with the transit network used for transit routing.
 *
 * @author mrieser
 */
public class AdaptedTransitRouterNetworkTravelTimeCost implements TravelCost, TravelTime {

	private final static double MIDNIGHT = 24.0*3600;

	//private final TransitRouterConfig config;
	private Link previousLink = null;
	private double previousTime = Double.NaN;
	private double cachedTravelTime = Double.NaN;

	public AdaptedTransitRouterNetworkTravelTimeCost() {
		//		this.config = config;
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
//			System.out.print(wrapped.link.fromNode.stop.getStopFacility().getId() + " > " + wrapped.link.toNode.stop.getStopFacility().getId() + " = " + (int) time2 + "\t@ " + Time.writeTime(time));
//			System.out.println(" wait-time: " + (int) (bestDepartureTime - earliestDepartureTime) + " travel-time: " + (int) (arrivalOffset - wrapped.link.fromNode.stop.getDepartureOffset()));
			this.cachedTravelTime = time2;
			return time2;
		}
		// different transit routes, so it must be a line switch
		double distance = CoordUtils.calcDistance(wrapped.fromNode.stop.getStopFacility().getCoord(), wrapped.toNode.stop.getStopFacility().getCoord());
		double time2 =  distance * PTValues.AV_WALKING_SPEED;
//		System.out.println(wrapped.link.fromNode.stop.getStopFacility().getId() + "..." + wrapped.link.toNode.stop.getStopFacility().getId() + " = " + (int) time2 + "\t@ " + Time.writeTime(time));
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
