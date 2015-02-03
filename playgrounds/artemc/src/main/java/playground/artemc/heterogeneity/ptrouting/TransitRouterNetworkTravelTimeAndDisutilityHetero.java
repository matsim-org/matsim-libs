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

package playground.artemc.heterogeneity.ptrouting;


import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.misc.Time;
import org.matsim.pt.router.CustomDataManager;
import org.matsim.pt.router.PreparedTransitSchedule;
import org.matsim.pt.router.TransitRouterConfig;
import org.matsim.pt.router.TransitRouterNetworkTravelTimeAndDisutility;
import org.matsim.pt.router.TransitTravelDisutility;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.vehicles.Vehicle;

import playground.artemc.heterogeneity.ptrouting.TransitRouterNetwork.TransitRouterNetworkLink;

/**
 * TravelTime and TravelCost calculator to be used with the transit network used for transit routing.
 *
 * <em>This class is NOT thread-safe!</em>
 *
 * @author mrieser
 */
public class TransitRouterNetworkTravelTimeAndDisutilityHetero extends TransitRouterNetworkTravelTimeAndDisutility  implements TravelTime, TransitTravelDisutility {

	final static double MIDNIGHT = 24.0*3600;

	protected final TransitRouterConfig config;
	private Link previousLink = null;
	private double previousTime = Double.NaN;
	private double cachedTravelTime = Double.NaN;

	private final PreparedTransitSchedule preparedTransitSchedule;

	/*
	 * If this constructor is used, every instance used its own PreparedTransitSchedule which might
	 * consume a lot of memory.
	 * 
	 * cdobler, nov'12
	 */
	@Deprecated
	public TransitRouterNetworkTravelTimeAndDisutilityHetero(final TransitRouterConfig config) {
		this(config, new PreparedTransitSchedule());
	}
	
	public TransitRouterNetworkTravelTimeAndDisutilityHetero(final TransitRouterConfig config, PreparedTransitSchedule preparedTransitSchedule) {
		super(config, preparedTransitSchedule);
		this.config = config;
		this.preparedTransitSchedule = preparedTransitSchedule;
	}

	@Override
	public double getLinkTravelDisutility(final Link link, final double time, final Person person, final Vehicle vehicle, final CustomDataManager dataManager) {
		double cost;
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

	@Override
	public double getLinkTravelTime(final Link link, final double time, Person person, Vehicle vehicle) {
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
			this.cachedTravelTime = time2;
			return time2;
		}
		// different transit routes, so it must be a line switch
		double distance = wrapped.getLength();
		double time2 = distance / this.config.getBeelineWalkSpeed() + this.config.additionalTransferTime;
		this.cachedTravelTime = time2;
		return time2;
	}
	
	//variables for caching offVehWaitTime
	Link previousWaitLink;
	double previousWaitTime;
	double cachedVehArrivalTime;
	
	public double getVehArrivalTime(final Link link, final double now){
		if ((link == this.previousWaitLink) && (now == this.previousWaitTime)) {
			return this.cachedVehArrivalTime;
		}
		this.previousWaitLink = link;
		this.previousWaitTime = now;
		
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
		cachedVehArrivalTime = vehArrivalTime ;
		return vehArrivalTime ;		
	}

	public double getTravelDisutility(Person person, Coord coord, Coord toCoord) {
		//  getMarginalUtilityOfTravelTimeWalk INCLUDES the opportunity cost of time.  kai, dec'12
		double timeCost = - getTravelTime(person, coord, toCoord) * config.getMarginalUtilityOfTravelTimeWalk_utl_s() ;
		// (sign: margUtl is negative; overall it should be positive because it is a cost.)
		
		double distanceCost = - CoordUtils.calcDistance(coord,toCoord) * config.getMarginalUtilityOfTravelDistancePt_utl_m() ;
		// (sign: same as above)
		
		return timeCost + distanceCost ;
	}

	public double getTravelTime(Person person, Coord coord, Coord toCoord) {
		double distance = CoordUtils.calcDistance(coord, toCoord);
		double initialTime = distance / config.getBeelineWalkSpeed();
		return initialTime;
	}

}
