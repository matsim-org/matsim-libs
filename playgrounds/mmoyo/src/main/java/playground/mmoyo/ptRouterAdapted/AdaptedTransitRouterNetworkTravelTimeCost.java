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
import org.matsim.pt.router.TransitRouterNetworkTravelTimeCost;
import org.matsim.pt.router.TransitRouterNetwork.TransitRouterNetworkLink;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;

/**
 *  This version of TransitRouterNetworkTravelTimeCost reads values from a MyTransitRouterConfig object
 */
public class AdaptedTransitRouterNetworkTravelTimeCost extends TransitRouterNetworkTravelTimeCost {
	private final static double MIDNIGHT = 24.0*3600;
	
	//these variables are protected in super class
	private Link previousLink = null;
	private double previousTime = Double.NaN;
	private double cachedTravelTime = Double.NaN;
	
	//for cached stopWaitTime
	private double waitPreviousTime = Double.NaN;
	private double waitCachedTravelTime = Double.NaN;
	private Link waitPreviousLink = null;
	
	//for cached offset vechicle departure
	double cachedOffsetTime= Double.NaN;
	private Link prevOffsetLink = null;
	
	private static final Logger log = Logger.getLogger(AdaptedTransitRouterNetworkTravelTimeCost.class);

	private MyTransitRouterConfig myConfig;	
	public AdaptedTransitRouterNetworkTravelTimeCost(MyTransitRouterConfig config ) {
		super( config ) ;
		log.error("a problem at this point is that the walk speed comes from the config" );
		myConfig= (MyTransitRouterConfig) this.config; 
	}

	@Override
	public double getLinkGeneralizedTravelCost(final Link link, final double time) {
		double cost;
		if (((TransitRouterNetworkLink) link).getRoute() == null) {

			double transfertime = getLinkTravelTime(link, time);
			double waittime = this.config.additionalTransferTime;
			double walktime = transfertime - waittime; // say that the effective walk time is the transfer time minus some "buffer"
			
			cost = 	-walktime * this.myConfig.getMarginalUtilityOfTravelTimeWalk_utl_s()
		       		-waittime * this.myConfig.getMarginalUtiltityOfWaiting_utl_s()
		       		- this.myConfig.getUtilityOfLineSwitch_utl();
			
		} else {
			//pt link
			//original
			//cost = -getLinkTravelTime(link, time) * this.myConfig.getMarginalUtilityOfTravelTimePt_utl_s() 
			//- link.getLength() * this.myConfig.getMarginalUtilityOfTravelDistancePt_utl_m();

			double waitingTime = -getWaitingTimeAtStop(link, time);  //we don't know yet if the agent is waiting inside or outside a pt veh
			double linkTravelTime = getLinkTravelTime(link, time);  //real veh travel time from (fromStop) departure until (toStop) arrival
			
			//in case the agent is already inside the pt vehicle, the waiting time is added to the link travel time
			double vehDepartureOffset = getVehDepartureOffset(link); //time that veh stops at station
			if (vehDepartureOffset == waitingTime ){  				 //are waiting times of agent and pt-veh the same? it means the agent is inside the veh
				linkTravelTime = linkTravelTime + vehDepartureOffset; //the waiting time is here part of link travel time
				waitingTime=0;   
			}
			
			//new version with separated effectiveWaitingTime as new parameter 
			cost = -linkTravelTime    	* this.myConfig.getMarginalUtilityOfTravelTimePt_utl_s() 
			       -waitingTime			* this.myConfig.getMarginalUtiltityOfWaiting_utl_s()  //create setters/getters in myConfig
			       -link.getLength()    * this.myConfig.getMarginalUtilityOfTravelDistancePt_utl_m();
			
		}
		return cost;
	}
	
	@Override
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

			double trueVehicleInMotionTimeToNextStop = toStopArrivalOffset - fromStop.getDepartureOffset();
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
	
	/**this is the time that the pt vehicle stops at the station (at from node)*/
	private double getVehDepartureOffset(Link link){
		//return cached value
		if (link == this.prevOffsetLink) {
			return this.cachedOffsetTime;
		}
		
		//calculate according to transit schedule
		TransitRouterNetworkLink wrapped = (TransitRouterNetworkLink) link;		
		TransitRouteStop fromStop = wrapped.fromNode.stop;
		double vehDepartureOffset = fromStop.getDepartureOffset() - fromStop.getArrivalOffset();

		//caching
		this.cachedOffsetTime = vehDepartureOffset;
		this.prevOffsetLink = link;
		
		return vehDepartureOffset;
	}
	
	double getWaitingTimeAtStop(final Link link, final double time){
		//return cached value
		if ((link == this.waitPreviousLink) && (time == this.waitPreviousTime)) {
			return this.waitCachedTravelTime;
		}
		this.waitPreviousLink = link;
		this.waitPreviousTime = time;
		
		//waiting time can't be calculated on a transfer link, throw an error
		TransitRouterNetworkLink wrapped = (TransitRouterNetworkLink) link;
		if (wrapped.getRoute() == null) {   
			throw new NullPointerException("The waiting time must be calculated always on a transit link" );
		} 
		
		//waiting time is the difference of (vehicle next departure) minus (now-time)
		double waitTimeAtStop=0;		
		waitTimeAtStop= getNextDepartureTime(wrapped.getRoute(), wrapped.fromNode.stop , time)  -  time;
		
		//cached value stored
		this.waitCachedTravelTime = waitTimeAtStop;
		return waitTimeAtStop;
	}
	
	/*
	//this class is not necessary now
	private Double getPtVehicleDepartureTime(final Link link, final double time) {
		TransitRouterNetworkLink wrapped = (TransitRouterNetworkLink) link;
		TransitRouteStop fromStop = wrapped.fromNode.stop;
		TransitRouteStop toStop = wrapped.toNode.stop;
		if (wrapped.getRoute() != null) {
			// (agent stays on the same route, so use transit line travel time)
			
			// get the next departure time:
			double bestDepartureTime = getNextDepartureTime(wrapped.getRoute(), fromStop, time);

			return bestDepartureTime ;
		}
		// no transit route, so we must be on a transfer link.
		return null ;
	}
	*/
}
