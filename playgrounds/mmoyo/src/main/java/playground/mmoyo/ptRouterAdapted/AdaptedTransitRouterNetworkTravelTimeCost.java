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
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.utils.misc.Time;
import org.matsim.pt.router.DepartureTimeCache;
import org.matsim.pt.router.TransitRouterNetwork.TransitRouterNetworkLink;
import org.matsim.pt.router.TransitRouterNetworkTravelTimeAndDisutility;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.vehicles.Vehicle;

/**
 *  This version of TransitRouterNetworkTravelTimeCost reads values from a MyTransitRouterConfig object
 */
public class AdaptedTransitRouterNetworkTravelTimeCost extends TransitRouterNetworkTravelTimeAndDisutility {

	private static final Logger log = Logger.getLogger(AdaptedTransitRouterNetworkTravelTimeCost.class);
	
	private final DepartureTimeCache data = new DepartureTimeCache();

	private MyTransitRouterConfig myConfig;	
	public AdaptedTransitRouterNetworkTravelTimeCost(MyTransitRouterConfig config ) {
		super( config ) ;
		log.error("a problem at this point is that the walk speed comes from the config" );
		myConfig= (MyTransitRouterConfig) this.config; 
	}

	@Override
	public double getLinkTravelDisutility(final Link link, final double time, final Person person, final Vehicle vehicle) {
		double cost;
		if (((TransitRouterNetworkLink) link).getRoute() == null) {
			// "route" here means "pt route".  If no pt route is attached, it means that it is a transfer link.

			double transfertime = getLinkTravelTime(link, time);
			
			// one can configure an "additionalTransferTime" which leaves a bit more time between transfers.  That time
			// is probably spent waiting, so we will weigh it with the waiting disutility.
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
			
			double inVehTime = getLinkTravelTime(link,time) - offVehWaitTime ;
			
			cost = - inVehTime                   * this.myConfig.getMarginalUtilityOfTravelTimePt_utl_s() 
					-offVehWaitTime				 * this.myConfig.getMarginalUtiltityOfWaiting_utl_s()
					-link.getLength() 			 * this.myConfig.getMarginalUtilityOfTravelDistancePt_utl_m();
		}
		return cost;
	}

	
	
	//variables for caching offVehWaitTime
	Link previousWaitLink;
	double previousWaitTime;
	double cachedVehArrivalTime;
	
	double getVehArrivalTime(final Link link, final double now){
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
		
		double nextDepartureTime = data.getNextDepartureTime(wrapped.getRoute(), fromStop, now);
		
		double fromStopArrivalOffset = (fromStop.getArrivalOffset() != Time.UNDEFINED_TIME) ? fromStop.getArrivalOffset() : fromStop.getDepartureOffset();
		double vehWaitAtStopTime = fromStop.getDepartureOffset()- fromStopArrivalOffset; //time in which the veh stops at station
		double vehArrivalTime = nextDepartureTime - vehWaitAtStopTime; //instead of a method "bestArrivalTime" we calculate the bestDeparture- stopTime 
		cachedVehArrivalTime = vehArrivalTime ;
		return vehArrivalTime ;		
	}
	
}
