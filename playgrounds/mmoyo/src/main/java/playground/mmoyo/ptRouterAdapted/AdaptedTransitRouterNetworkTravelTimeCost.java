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
import org.matsim.pt.router.DepartureTimeCache;
import org.matsim.pt.router.TransitRouterConfig;
import org.matsim.pt.router.TransitRouterNetworkTravelTimeAndDisutility;
import org.matsim.pt.router.TransitRouterNetwork.TransitRouterNetworkLink;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;

/**
 *  This version of TransitRouterNetworkTravelTimeCost reads values from a MyTransitRouterConfig object
 */
public class AdaptedTransitRouterNetworkTravelTimeCost extends TransitRouterNetworkTravelTimeAndDisutility {

	private static final Logger log = Logger.getLogger(AdaptedTransitRouterNetworkTravelTimeCost.class);
	
	private final DepartureTimeCache data = new DepartureTimeCache();

//	private MyTransitRouterConfig config;	
	public AdaptedTransitRouterNetworkTravelTimeCost(TransitRouterConfig config ) {
		super( config ) ;
		log.error("a problem at this point is that the walk speed comes from the config" );
		config= this.config; 
	}

//	@Override
//	public double getLinkTravelDisutility(final Link link, final double time, final Person person, final Vehicle vehicle, final CustomDataManager dataManager) {
//		double cost;
//		if (((TransitRouterNetworkLink) link).getRoute() == null) {
//			// "route" here means "pt route".  If no pt route is attached, it means that it is a transfer link.
//
//			cost = defaultTransferCost(link, time, person, vehicle);
//			
//		} else {
//
//			double offVehWaitTime = offVehicleWaitTime(link, time);
//			
//			double inVehTime = getLinkTravelTime(link,time, person, vehicle) - offVehWaitTime ;
//			
//			cost = - inVehTime                   * this.config.getMarginalUtilityOfTravelTimePt_utl_s() 
//					-offVehWaitTime				 * this.config.getMarginalUtiltityOfWaitingPt_utl_s()
//					-link.getLength() 			 * this.config.getMarginalUtilityOfTravelDistancePt_utl_m();
//		}
//		return cost;
//	}

//	@Override
//	protected double offVehicleWaitTime(final Link link, final double time) {
//		//calculate off vehicle waiting time as new router parameter
//		double offVehWaitTime=0;
//		double nextVehArrivalTime = getVehArrivalTime(link, time);
//		if (time < nextVehArrivalTime){ // it means the agent waits outside the veh				
//			offVehWaitTime = nextVehArrivalTime-time;
//		}
//		return offVehWaitTime;
//	}

	
	
//	//variables for caching offVehWaitTime
//	Link previousWaitLink;
//	double previousWaitTime;
//	double cachedVehArrivalTime;
//	
//	/* package (for a test) */ double getVehArrivalTime(final Link link, final double now){
//		if ((link == this.previousWaitLink) && (now == this.previousWaitTime)) {
//			return this.cachedVehArrivalTime;
//		}
//		this.previousWaitLink = link;
//		this.previousWaitTime = now;
//		
//		//first find out vehicle arrival time to fromStop according to transit schedule
//		TransitRouterNetworkLink wrapped = (TransitRouterNetworkLink) link;
//		if (wrapped.getRoute() == null) { 
//			throw new RuntimeException("should not happen") ;
//		}
//		TransitRouteStop fromStop = wrapped.fromNode.stop;
//		
//		double nextDepartureTime = data.getNextDepartureTime(wrapped.getRoute(), fromStop, now);
//		
//		double fromStopArrivalOffset = (fromStop.getArrivalOffset() != Time.UNDEFINED_TIME) ? fromStop.getArrivalOffset() : fromStop.getDepartureOffset();
//		double vehWaitAtStopTime = fromStop.getDepartureOffset()- fromStopArrivalOffset; //time in which the veh stops at station
//		double vehArrivalTime = nextDepartureTime - vehWaitAtStopTime; //instead of a method "bestArrivalTime" we calculate the bestDeparture- stopTime 
//		cachedVehArrivalTime = vehArrivalTime ;
//		return vehArrivalTime ;		
//	}
	
}
