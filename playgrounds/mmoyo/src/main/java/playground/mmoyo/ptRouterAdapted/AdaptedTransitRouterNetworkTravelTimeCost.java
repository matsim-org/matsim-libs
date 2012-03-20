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

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.utils.misc.Time;
import org.matsim.pt.router.TransitRouterNetworkTravelTimeAndDisutility;
import org.matsim.pt.router.TransitRouterNetwork.TransitRouterNetworkLink;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;

/**
 *  This version of TransitRouterNetworkTravelTimeCost reads values from a MyTransitRouterConfig object
 */
public class AdaptedTransitRouterNetworkTravelTimeCost extends TransitRouterNetworkTravelTimeAndDisutility {
	private final static double MIDNIGHT = 24.0*3600;
	
	//these variables are protected in super class
	private Link previousLink = null;
	private double previousTime = Double.NaN;
	private double cachedTravelTime = Double.NaN;

	private static final Logger log = Logger.getLogger(AdaptedTransitRouterNetworkTravelTimeCost.class);

	private MyTransitRouterConfig myConfig;	
	public AdaptedTransitRouterNetworkTravelTimeCost(MyTransitRouterConfig config ) {
		super( config ) ;
		log.error("a problem at this point is that the walk speed comes from the config" );
		myConfig= (MyTransitRouterConfig) this.config; 
	}

	@Override
	public double getLinkTravelDisutility(final Link link, final double time) {
		double cost;
		if (((TransitRouterNetworkLink) link).getRoute() == null) {

			double transfertime = getLinkTravelTime(link, time);
			double waittime = this.myConfig.additionalTransferTime;
			double walktime = transfertime - waittime; // say that the effective walk time is the transfer time minus some "buffer"
			
			cost = 	-walktime * this.myConfig.getMarginalUtilityOfTravelTimeWalk_utl_s()
		       		-waittime * this.myConfig.getMarginalUtiltityOfWaiting_utl_s()
		       		- this.myConfig.getUtilityOfLineSwitch_utl();
			
		} else {
			//pt link
			//original
			//cost = -getLinkTravelTime(link, time) * this.myConfig.getMarginalUtilityOfTravelTimePt_utl_s() 
			//- link.getLength() * this.myConfig.getMarginalUtilityOfTravelDistancePt_utl_m();

			//calculate off vehicle waiting time as new router parameter
			double offVehWaitTime=0;
			double nextVehArrivalTime = getVehArrivalTime(link, time);
			if (time < nextVehArrivalTime){ // it means the agent waits outside the veh				
				offVehWaitTime = nextVehArrivalTime-time;
			}
			
			cost = - getLinkTravelTime(link, time) * this.myConfig.getMarginalUtilityOfTravelTimePt_utl_s() 
					-offVehWaitTime				   * this.myConfig.getMarginalUtiltityOfWaiting_utl_s()
					-link.getLength() 			   * this.myConfig.getMarginalUtilityOfTravelDistancePt_utl_m();
		}
		return cost;
	}
	
	@Override   //time from stop to next stop arrivals
	public double getLinkTravelTime(final Link link, final double now) {
		if ((link == this.previousLink) && (now == this.previousTime)) {
			return this.cachedTravelTime;
		}
		this.previousLink = link;
		this.previousTime = now;

		TransitRouterNetworkLink wrapped = (TransitRouterNetworkLink) link;
		TransitRouteStop fromStop = wrapped.fromNode.stop;
		TransitRouteStop toStop = wrapped.toNode.stop;
		if (wrapped.getRoute() != null) { // (agent is on board, so use transit route travel time)
			
			// this modified version considers only effective travel time in vehicle, not waiting time at station, Manuel

			// the travel time on the link is 
			//   the time until the departure (``dpTime - now'')
			//   + the travel time on the link (there.arrivalTime - here.departureTime)
			
			// But quite often, we only have the departure time at the next stop.  Then we use that:
			double toStopArrivalOffset = (toStop.getArrivalOffset() != Time.UNDEFINED_TIME) ? toStop.getArrivalOffset() : toStop.getDepartureOffset();
			// in the same way, we might only have the departure time at the "from" stop.  Then we use that: //Manuel mar 2012
			double fromStopArrivalOffset = (fromStop.getArrivalOffset() != Time.UNDEFINED_TIME) ? fromStop.getArrivalOffset() : fromStop.getDepartureOffset();
			
			double trueVehicleInMotionTimeToNextStop = toStopArrivalOffset - fromStopArrivalOffset ;//<--adapted here
			if (trueVehicleInMotionTimeToNextStop < 0) {
				// ( this can only happen, I think, when ``bestDepartureTime'' is after midnight but ``time'' was before )
				trueVehicleInMotionTimeToNextStop += MIDNIGHT;
			}
			this.cachedTravelTime = trueVehicleInMotionTimeToNextStop;
			return trueVehicleInMotionTimeToNextStop;
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
	double cachedOffVehWaitTime;
	
	public double getVehArrivalTime(final Link link, final double now){
		if ((link == this.previousWaitLink) && (now == this.previousWaitTime)) {
			return this.cachedOffVehWaitTime;
		}
		this.previousWaitLink = link;
		this.previousWaitTime = now;
		
		//first find out vehicle arrival time to fromStop according to transit schedule
		TransitRouterNetworkLink wrapped = (TransitRouterNetworkLink) link;
		if (wrapped.getRoute() == null) { 
			return 0;  //transfer link
		}
		TransitRouteStop fromStop = wrapped.fromNode.stop;
		double bestDepartureTime = getNextDepartureTime(wrapped.getRoute(), fromStop, now);
		double waitInVehTime = fromStop.getDepartureOffset()- fromStop.getArrivalOffset(); //time in which the veh stops at station
		return bestDepartureTime - waitInVehTime ;		//instead of a method "bestArrivalTime" we calculate the bestDeparture- stopTime 
	}
	
}
