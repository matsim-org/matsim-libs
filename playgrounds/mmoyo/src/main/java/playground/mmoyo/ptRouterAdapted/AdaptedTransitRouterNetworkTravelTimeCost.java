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

	private static final Logger log = Logger.getLogger(AdaptedTransitRouterNetworkTravelTimeCost.class);

	private MyTransitRouterConfig myConfig;	
	public AdaptedTransitRouterNetworkTravelTimeCost(MyTransitRouterConfig config ) {
		super( config ) ;
		log.error("a problem at this point is that the walk speed comes from the config" );
		myConfig= (MyTransitRouterConfig) this.config; 
	}

	@Override
	public double getLinkTravelDisutility(final Link link, final double now) {
		double cost;
		if (((TransitRouterNetworkLink) link).getRoute() == null) {
			// "route" here means "pt route".  If no pt route is attached, it means that it is a transfer link.

			double transfertime = getLinkTravelTime(link, now);
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
			double nextVehArrivalTime = getVehArrivalTime(link, now);
			if (now < nextVehArrivalTime){ // it means the agent waits outside the veh				
				offVehWaitTime = nextVehArrivalTime-now;
			}
			
			double inVehTime = getLinkTravelTime(link,now) - offVehWaitTime ;
			
			cost = - inVehTime                   * this.myConfig.getMarginalUtilityOfTravelTimePt_utl_s() 
					-offVehWaitTime				 * this.myConfig.getMarginalUtiltityOfWaiting_utl_s()
					-link.getLength() 			 * this.myConfig.getMarginalUtilityOfTravelDistancePt_utl_m();
		}
		return cost;
	}

	
	
	//variables for caching offVehWaitTime
	Link previousWaitLink;
	double previousWaitTime;
	double cachedOffVehWaitTime;
	
	double getVehArrivalTime(final Link link, final double now){
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
		TransitRouteStop toStop = wrapped.toNode.stop;
		
		double bestDepartureTime = getNextDepartureTime(wrapped.getRoute(), fromStop, now);
		
		double arrivalOffset = (toStop.getArrivalOffset() != Time.UNDEFINED_TIME) ? toStop.getArrivalOffset() : toStop.getDepartureOffset();
		double waitInVehTime = fromStop.getDepartureOffset()- arrivalOffset; //time in which the veh stops at station
		return bestDepartureTime - waitInVehTime ;		//instead of a method "bestArrivalTime" we calculate the bestDeparture- stopTime 
	}
	
}
