/* *********************************************************************** *
 * project: org.matsim.*
 * CharyparNagelOpenTimesScoringFunctionFactory.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package org.matsim.contrib.bicycle;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.scoring.functions.CharyparNagelLegScoring;
import org.matsim.core.scoring.functions.ModeUtilityParameters;
import org.matsim.core.scoring.functions.ScoringParameters;

/**
 * @author dziemke
 */
public class BicycleLegScoring extends CharyparNagelLegScoring {
	protected double score;

	/** The parameters used for scoring */
	protected final ScoringParameters params;
	protected Network network;

	private final double marginalUtilityOfInfrastructure_m;
	private final double marginalUtilityOfComfort_m;
	private final double marginalUtilityOfGradient_m_100m;

	public BicycleLegScoring(final ScoringParameters params, Network network, BicycleConfigGroup bicycleConfigGroup) {
		super(params, network);
		
		this.params = params;
		this.network = network;

		this.marginalUtilityOfInfrastructure_m = bicycleConfigGroup.getMarginalUtilityOfInfrastructure_m();
		this.marginalUtilityOfComfort_m = bicycleConfigGroup.getMarginalUtilityOfComfort_m();
		this.marginalUtilityOfGradient_m_100m = bicycleConfigGroup.getMarginalUtilityOfGradient_m_100m();
	}

	private static int ccc=0 ;
	
	protected double calcLegScore(final double departureTime, final double arrivalTime, final Leg leg) {
		double tmpScore = 0.0;
		double travelTime = arrivalTime - departureTime; // travel time in seconds	
		ModeUtilityParameters modeParams = this.params.modeParams.get(leg.getMode());
		if (modeParams == null) {
			if (leg.getMode().equals(TransportMode.transit_walk) || leg.getMode().equals(TransportMode.access_walk) 
					|| leg.getMode().equals(TransportMode.egress_walk) ) {
				modeParams = this.params.modeParams.get(TransportMode.walk);
			} else {
//				modeParams = this.params.modeParams.get(TransportMode.other);
				throw new RuntimeException("just encountered mode for which no scoring parameters are defined: " + leg.getMode().toString() ) ;
			}
		}
		tmpScore += travelTime * modeParams.marginalUtilityOfTraveling_s;
		if (modeParams.marginalUtilityOfDistance_m != 0.0
				|| modeParams.monetaryDistanceCostRate != 0.0) {
			Route route = leg.getRoute();
			double dist = route.getDistance(); // distance in meters
			if ( Double.isNaN(dist) ) {
				if ( ccc<10 ) {
					ccc++ ;
					Logger.getLogger(this.getClass()).warn("distance is NaN. Will make score of this plan NaN. Possible reason: Simulation does not report " +
							"a distance for this trip. Possible reason for that: mode is teleported and router does not " +
							"write distance into plan.  Needs to be fixed or these plans will die out.") ;
					if ( ccc==10 ) {
						Logger.getLogger(this.getClass()).warn(Gbl.FUTURE_SUPPRESSED) ;
					}
				}
			}
			tmpScore += modeParams.marginalUtilityOfDistance_m * dist;
			tmpScore += modeParams.monetaryDistanceCostRate * this.params.marginalUtilityOfMoney * dist;
		}
		//
		tmpScore += addBicycleScoringComponent(leg.getRoute());
		tmpScore += modeParams.constant;
		// (yyyy once we have multiple legs without "real" activities in between, this will produce wrong results.  kai, dec'12)
		// (yy NOTE: the constant is added for _every_ pt leg.  This is not how such models are estimated.  kai, nov'12)
		return tmpScore;
	}
	
	
	private double addBicycleScoringComponent(Route route) {
		NetworkRoute networkRoute = (NetworkRoute) route;
		List<Id<Link>> linkIds = new ArrayList<>();
		linkIds.addAll(networkRoute.getLinkIds());
		linkIds.add(networkRoute.getEndLinkId());
		
		double score = 0.;
		
		for (Id<Link> linkId : linkIds) {
			Link link = network.getLinks().get(linkId);
			double scoreOnLink = getTravelDisutilityBasedOnTTime(link);
//			LOG.info("Bicycle score on link " + linkId + " is = " + scoreOnLink + ".");
			score += scoreOnLink;
		}
//		LOG.info("Bicycle leg score component is = " + score + " (on links " + linkIds + ").");
		return score;
	}
	

	public double getTravelDisutilityBasedOnTTime(Link link) {
		String surface = (String) link.getAttributes().getAttribute(BicycleLabels.SURFACE);
		String type = (String) link.getAttributes().getAttribute("type");
		String cyclewaytype = (String) link.getAttributes().getAttribute(BicycleLabels.CYCLEWAY);

		double distance = link.getLength();
		
		double comfortFactor = BicycleUtilityUtils.getComfortFactor(surface, type);
		double comfortDisutility = marginalUtilityOfComfort_m * (1. - comfortFactor) * distance;
		
		double infrastructureFactor = BicycleUtilityUtils.getInfrastructureFactor(type, cyclewaytype);
		double infrastructureDisutility = marginalUtilityOfInfrastructure_m * (1. - infrastructureFactor) * distance;
		
		double gradientFactor = BicycleUtilityUtils.getGradientFactor(link);
		double gradientDisutility = marginalUtilityOfGradient_m_100m * gradientFactor * distance;
		return (infrastructureDisutility + comfortDisutility + gradientDisutility);
	}
}