/* *********************************************************************** *
 * project: org.matsim.*                                                   *
 * BicycleLegScoring.java                                                  *
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
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.scoring.functions.CharyparNagelLegScoring;
import org.matsim.core.scoring.functions.ScoringParameters;

/**
 * @author dziemke
 */
public class BicycleLegScoring extends CharyparNagelLegScoring {
	// private static final Logger LOG = Logger.getLogger(BicycleLegScoring.class);

	private final double marginalUtilityOfInfrastructure_m;
	private final double marginalUtilityOfComfort_m;
	private final double marginalUtilityOfGradient_m_100m;

	public BicycleLegScoring(final ScoringParameters params, Network network, BicycleConfigGroup bicycleConfigGroup) {
		super(params, network);

		this.marginalUtilityOfInfrastructure_m = bicycleConfigGroup.getMarginalUtilityOfInfrastructure_m();
		this.marginalUtilityOfComfort_m = bicycleConfigGroup.getMarginalUtilityOfComfort_m();
		this.marginalUtilityOfGradient_m_100m = bicycleConfigGroup.getMarginalUtilityOfGradient_m_100m();
	}
	
	protected double calcLegScore(final double departureTime, final double arrivalTime, final Leg leg) {
		// Get leg score from regular CharyparNagelLegScoring
		double legScore = super.calcLegScore(departureTime, arrivalTime, leg);
		// LOG.warn("----- legScore = " + legScore);
		
		NetworkRoute networkRoute = (NetworkRoute) leg.getRoute();
		
		List<Id<Link>> linkIds = new ArrayList<>();
		linkIds.addAll(networkRoute.getLinkIds());
		linkIds.add(networkRoute.getEndLinkId());
		
		// Iterate over all links of the route
		for (Id<Link> linkId : linkIds) {
			double scoreOnLink = BicycleUtilityUtils.computeLinkBasedScore(network.getLinks().get(linkId),
					marginalUtilityOfComfort_m, marginalUtilityOfInfrastructure_m, marginalUtilityOfGradient_m_100m);
			// LOG.warn("----- link = " + linkId + " -- scoreOnLink = " + scoreOnLink);
			legScore += scoreOnLink;
		}
		return legScore;
	}
}