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
import org.matsim.pt.router.TransitRouterNetworkTravelTimeCost;
import org.matsim.pt.router.TransitRouterNetwork.TransitRouterNetworkLink;

/**
 *  This version of TransitRouterNetworkTravelTimeCost reads values from a MyTransitRouterConfig object
 */
public class AdaptedTransitRouterNetworkTravelTimeCost extends TransitRouterNetworkTravelTimeCost {
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
			// transfer link
//			cost = -getLinkTravelTime(link, time) * this.myConfig.getEffectiveMarginalUtilityOfTravelTimeWalk_utl_s() + this.myConfig.getUtilityOfLineSwitch_utl();
			cost = -getLinkTravelTime(link, time) * this.myConfig.getEffectiveMarginalUtilityOfTravelTimeWalk_utl_s() - this.myConfig.getUtilityOfLineSwitch_utl();
		} else {
			//pt link
			cost = -getLinkTravelTime(link, time) * this.myConfig.getEffectiveMarginalUtilityOfTravelTimePt_utl_s() - link.getLength() * this.myConfig.getMarginalUtilityOfTravelDistancePt_utl_m();
		}
		return cost;
	}
}
