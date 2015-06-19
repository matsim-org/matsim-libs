/* *********************************************************************** *
 * project: org.matsim.*
 * MyTransitRouterNetworkTravelTimeAndDisutility.java
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

package org.matsim.pt.router;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.router.util.RoutingNetworkLink;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.misc.Time;
import org.matsim.pt.router.TransitRouterNetwork.TransitRouterNetworkLink;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.vehicles.Vehicle;

/**
 * TravelTime and TravelCost calculator to be used with the transit network used for transit routing.
 *
 * Due to the usage of ThreadLocal variables, this class should be thread-safe.
 * However, this might be a performance issue...
 *
 * @author cdobler
 */
public class MyTransitRouterNetworkTravelTimeAndDisutility implements TravelTime, TravelDisutility {

	final static double MIDNIGHT = 24.0*3600;

	protected final TransitRouterConfig config;
	
	/*
	 * A single data object that holds all thread-local data.
	 */
	private final ThreadLocal<ThreadLocalData> data = new ThreadLocal<ThreadLocalData>();

	private final PreparedTransitSchedule preparedTransitSchedule;
	
	public MyTransitRouterNetworkTravelTimeAndDisutility(final TransitRouterConfig config, PreparedTransitSchedule preparedTransitSchedule) {
		this.config = config;
		this.preparedTransitSchedule = preparedTransitSchedule;
	}

	@Override
	public double getLinkTravelDisutility(Link link, final double time, final Person person, final Vehicle vehicle) {
		double cost;
		
		if (link instanceof RoutingNetworkLink) link = ((RoutingNetworkLink) link).getLink();
		
		if (((TransitRouterNetworkLink) link).getRoute() == null) {
			// "route" here means "pt route".  If no pt route is attached, it means that it is a transfer link.

			cost = defaultTransferCost(link, time, person, vehicle);
		} else {
			double offVehWaitTime = offVehicleWaitTime(link, time);		
			double inVehTime = getLinkTravelTime(link,time, person, vehicle) - offVehWaitTime;		
			cost = - inVehTime       * this.config.getMarginalUtilityOfTravelTimePt_utl_s() 
			       -offVehWaitTime   * this.config.getMarginalUtilityOfWaitingPt_utl_s()
			       -link.getLength() * this.config.getMarginalUtilityOfTravelDistancePt_utl_m();
		}
		return cost;
	}
	
	@Override
	public double getLinkMinimumTravelDisutility(Link link) {
		return 0;
	}
	
	/**
	 * method to allow inclusion of offVehicleWaitTime without code replication.  kai, oct'12
	 * 
	 * @param link
	 * @param time
	 * @return
	 */
	protected double offVehicleWaitTime(final Link link, final double time) {
		double offVehWaitTime=0;
		double nextVehArrivalTime = getVehArrivalTime(link, time);
		if (time < nextVehArrivalTime){ // it means the agent waits outside the veh				
			offVehWaitTime = nextVehArrivalTime-time;
		}
		return offVehWaitTime;

	}

	/**
	 * convenience method for derived classes in order to bring Manuel's version closer to this one here.
	 * kai, oct'12
	 */
	protected final double defaultTransferCost(final Link link, final double time,
			final Person person, final Vehicle vehicle) {
		double cost;
		double transfertime = getLinkTravelTime(link, time, person, vehicle);
		double waittime = this.config.getAdditionalTransferTime();
		
		// say that the effective walk time is the transfer time minus some "buffer"
		double walktime = transfertime - waittime;
		
		double walkDistance = link.getLength();
		
		// weigh this "buffer" not with the walk time disutility, but with the wait time disutility:
		// (note that this is the same "additional disutl of wait" as in the scoring function.  Its default is zero.
		// only if you are "including the opportunity cost of time into the router", then the disutility of waiting will
		// be the same as the marginal opprotunity cost of time).  kai, nov'11
		cost = - walktime * this.config.getMarginalUtilityOfTravelTimeWalk_utl_s()
		       - walkDistance * this.config.getMarginalUtilityOfTravelDistancePt_utl_m() 
		       - waittime * this.config.getMarginalUtilityOfWaitingPt_utl_s()
		       - this.config.getUtilityOfLineSwitch_utl();
		return cost;
	}
	
	@Override
	public double getLinkTravelTime(Link link, final double time, Person person, Vehicle vehicle) {
		
		ThreadLocalData threadLocalData = this.data.get();
		if (threadLocalData == null) {
			threadLocalData = new ThreadLocalData();
			this.data.set(threadLocalData);
		}
		if ((link == threadLocalData.previousLink) && (time == threadLocalData.previousTime)) {
			return threadLocalData.cachedTravelTime;
		}
		threadLocalData.previousLink = link;
		threadLocalData.previousTime = time;
//		if ((link == this.previousLink) && (time == this.previousTime)) {
//			return this.cachedTravelTime;
//		}
//		this.previousLink = link;
//		this.previousTime = time;

		if (link instanceof RoutingNetworkLink) link = ((RoutingNetworkLink) link).getLink();
		
		TransitRouterNetworkLink wrapped = (TransitRouterNetworkLink) link;
		TransitRouteStop fromStop = wrapped.fromNode.stop;
		TransitRouteStop toStop = wrapped.toNode.stop;
		if (wrapped.route != null) {
			// (agent stays on the same route, so use transit line travel time)
			
			// get the next departure time:
			double bestDepartureTime = preparedTransitSchedule.getNextDepartureTime(wrapped.route, fromStop, time);

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
			threadLocalData.cachedTravelTime = time2;
//			this.cachedTravelTime = time2;
			return time2;
		}
		// different transit routes, so it must be a line switch
		double distance = wrapped.getLength();
		double time2 = distance / this.config.getBeelineWalkSpeed() + this.config.getAdditionalTransferTime();
		threadLocalData.cachedTravelTime = time2;
//		this.cachedTravelTime = time2;
		return time2;
	}
	
	public double getVehArrivalTime(final Link link, final double now){
		
		ThreadLocalData threadLocalData = this.data.get();
		if (threadLocalData == null) {
			threadLocalData = new ThreadLocalData();
			this.data.set(threadLocalData);
		}
		if ((link == threadLocalData.previousWaitLink) && (now == threadLocalData.previousWaitTime)) {
			return threadLocalData.cachedVehArrivalTime;
		}
		threadLocalData.previousWaitLink = link;
		threadLocalData.previousWaitTime = now;
//		if ((link == this.previousWaitLink) && (now == this.previousWaitTime)) {
//			return this.cachedVehArrivalTime;
//		}
//		this.previousWaitLink = link;
//		this.previousWaitTime = now;
		
		//first find out vehicle arrival time to fromStop according to transit schedule
		TransitRouterNetworkLink wrapped = (TransitRouterNetworkLink) link;
		if (wrapped.getRoute() == null) { 
			throw new RuntimeException("should not happen") ;
		}
		TransitRouteStop fromStop = wrapped.fromNode.stop;
		
		double nextDepartureTime = preparedTransitSchedule.getNextDepartureTime(wrapped.getRoute(), fromStop, now);
		
		double fromStopArrivalOffset = (fromStop.getArrivalOffset() != Time.UNDEFINED_TIME) ? fromStop.getArrivalOffset() : fromStop.getDepartureOffset();
		double vehWaitAtStopTime = fromStop.getDepartureOffset() - fromStopArrivalOffset; //time in which the veh stops at station
		double vehArrivalTime = nextDepartureTime - vehWaitAtStopTime;
		threadLocalData.cachedVehArrivalTime = vehArrivalTime;
//		cachedVehArrivalTime = vehArrivalTime;
		return vehArrivalTime;
	}

	public double getTravelDisutility(Person person, Coord coord, Coord toCoord) {
		//  getMarginalUtilityOfTravelTimeWalk INCLUDES the opportunity cost of time.  kai, dec'12
		double timeCost = - getTravelTime(person, coord, toCoord) * config.getMarginalUtilityOfTravelTimeWalk_utl_s();
		// (sign: margUtl is negative; overall it should be positive because it is a cost.)
		
		double distanceCost = - CoordUtils.calcDistance(coord,toCoord) * config.getMarginalUtilityOfTravelDistancePt_utl_m();
		// (sign: same as above)
		
		return timeCost + distanceCost;
	}

	public double getTravelTime(Person person, Coord coord, Coord toCoord) {
		double distance = CoordUtils.calcDistance(coord, toCoord);
		double initialTime = distance / this.config.getBeelineWalkSpeed();
		return initialTime;
	}

	private static class ThreadLocalData {
		
		private Link previousLink;
		private double previousTime;
		private double cachedTravelTime;
		
		//variables for caching offVehWaitTime
		private Link previousWaitLink;
		private double previousWaitTime;
		private double cachedVehArrivalTime;
	}
}
