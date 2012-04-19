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


import org.matsim.api.core.v01.network.Link;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.utils.misc.Time;
import org.matsim.pt.router.TransitRouterNetwork.TransitRouterNetworkLink;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;

/**
 * TravelTime and TravelCost calculator to be used with the transit network used for transit routing.
 *
 * <em>This class is NOT thread-safe!</em>
 *
 * @author mrieser
 */
public class TransitRouterNetworkTravelTimeAndDisutility implements TravelTime, TravelDisutility {

	final static double MIDNIGHT = 24.0*3600;

	protected final TransitRouterConfig config;
	private Link previousLink = null;
	private double previousTime = Double.NaN;
	private double cachedTravelTime = Double.NaN;

	private DepartureTimeCache data = new DepartureTimeCache();

	public TransitRouterNetworkTravelTimeAndDisutility(final TransitRouterConfig config) {
		this.config = config;
	}

	@Override
	public double getLinkTravelDisutility(final Link link, final double time) {
		double cost;
		if (((TransitRouterNetworkLink) link).route == null) {
			// it's a transfer link (walk)

			//			cost = -getLinkTravelTime(link, time) * this.config.getEffectiveMarginalUtilityOfTravelTimeWalk_utl_s() + this.config.getUtilityOfLineSwitch_utl();
			// (old specification)
			
			double transfertime = getLinkTravelTime(link, time);
			double waittime = this.config.additionalTransferTime;
			
			// say that the effective walk time is the transfer time minus some "buffer"
			double walktime = transfertime - waittime;
			
			// weigh the "buffer" not with the walk time disutility, but with the wait time disutility:
			// (note that this is the same "additional disutl of wait" as in the scoring function.  Its default is zero.
			// only if you are "including the opportunity cost of time into the router", then the disutility of waiting will
			// be the same as the marginal opprotunity cost of time).  kai, nov'11
			cost = -walktime * this.config.getMarginalUtilityOfTravelTimeWalk_utl_s()
			       -waittime * this.config.getMarginalUtiltityOfWaiting_utl_s()
			       - this.config.getUtilityOfLineSwitch_utl();
			
		} else {
			cost = - getLinkTravelTime(link, time) * this.config.getMarginalUtilityOfTravelTimePt_utl_s() 
			       - link.getLength() * this.config.getMarginalUtilityOfTravelDistancePt_utl_m();
		}
		return cost;
	}
	
	@Override
	public double getLinkMinimumTravelDisutility(Link link) {
		throw new UnsupportedOperationException();
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
			// (agent stays on the same route, so use transit line travel time)
			
			// get the next departure time:
			double bestDepartureTime = data.getNextDepartureTime(wrapped.route, fromStop, time);

			// the travel time on the link is 
			//   the time until the departure (``dpTime - now'')
			//   + the travel time on the link (there.arrivalTime - here.departureTime)
			// But quite often, we only have the departure time at the next stop.  Then we use that:
			double arrivalOffset = (toStop.getArrivalOffset() != Time.UNDEFINED_TIME) ? toStop.getArrivalOffset() : toStop.getDepartureOffset();
			double time2 = (bestDepartureTime - time) + (arrivalOffset - fromStop.getDepartureOffset());
			if (time2 < 0) {
				// ( this can only happen, I think, when ``bestDepartureTime'' is after midnight but ``time'' was before )
				time2 += MIDNIGHT;
			}
			this.cachedTravelTime = time2;
			return time2;
		}
		// different transit routes, so it must be a line switch
		double distance = wrapped.getLength();
		double time2 = distance / this.config.getBeelineWalkSpeed() + this.config.additionalTransferTime;
		this.cachedTravelTime = time2;
		return time2;
	}

}
