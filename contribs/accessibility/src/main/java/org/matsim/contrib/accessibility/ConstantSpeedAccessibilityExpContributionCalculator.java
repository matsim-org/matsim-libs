/* *********************************************************************** *
 * project: org.matsim.*                                                   *
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

package org.matsim.contrib.accessibility;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.contrib.accessibility.utils.AggregationObject;
import org.matsim.contrib.accessibility.utils.Distances;
import org.matsim.contrib.accessibility.utils.NetworkUtil;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.trafficmonitoring.FreeSpeedTravelTime;
import org.matsim.facilities.ActivityFacility;
import org.matsim.utils.leastcostpathtree.LeastCostPathTree;

/**
 * @author thibautd, dziemke
 */
public final class ConstantSpeedAccessibilityExpContributionCalculator implements AccessibilityContributionCalculator {
	private static final Logger LOG = Logger.getLogger(ConstantSpeedAccessibilityExpContributionCalculator.class);

	// Estimates travel time by a constant speed along network, considering all links (including highways, which seems
	// to be realistic in South Africa, but less elsewhere)
	private final LeastCostPathTree lcptTravelDistance = new LeastCostPathTree(new FreeSpeedTravelTime(), new LinkLengthTravelDisutility());

	private final Network network;
	
	private double logitScaleParameter;
	
	private double betaModeTT;	// in MATSim this is [utils/h]: cnScoringGroup.getTravelingBike_utils_hr() - cnScoringGroup.getPerforming_utils_hr()
	private double betaModeTD;	// in MATSim this is 0 !!! since getMonetaryDistanceCostRateBike doesn't exist:
	private double constMode;
	private double modeSpeed_m_h = -1;
	
	private final double betaWalkTT;
	private final double betaWalkTD;
	private final double walkSpeed_m_h;

	private Node fromNode = null;


	public ConstantSpeedAccessibilityExpContributionCalculator(final String mode, Config config, Network network) {
		this.network = network;
		final PlanCalcScoreConfigGroup planCalcScoreConfigGroup = config.planCalcScore() ;

		if (planCalcScoreConfigGroup.getOrCreateModeParams(mode).getMonetaryDistanceRate() != 0.) {
			LOG.error("Monetary distance cost rate for " + mode + " different from zero, but not used in accessibility computations");
		}

		logitScaleParameter = planCalcScoreConfigGroup.getBrainExpBeta();
		
		if (config.plansCalcRoute().getTeleportedModeSpeeds().get(mode) == null) {
			LOG.error("No teleported mode speed for mode " + mode + " set.");
		}
		this.modeSpeed_m_h = config.plansCalcRoute().getTeleportedModeSpeeds().get(mode) * 3600.;
		
		final PlanCalcScoreConfigGroup.ModeParams modeParams = planCalcScoreConfigGroup.getOrCreateModeParams(mode);
		betaModeTT = modeParams.getMarginalUtilityOfTraveling() - planCalcScoreConfigGroup.getPerforming_utils_hr();
		betaModeTD = modeParams.getMarginalUtilityOfDistance();
		constMode = modeParams.getConstant();

		betaWalkTT = planCalcScoreConfigGroup.getModes().get(TransportMode.walk).getMarginalUtilityOfTraveling() - planCalcScoreConfigGroup.getPerforming_utils_hr();
		betaWalkTD = planCalcScoreConfigGroup.getModes().get(TransportMode.walk).getMarginalUtilityOfDistance();
		this.walkSpeed_m_h = config.plansCalcRoute().getTeleportedModeSpeeds().get(TransportMode.walk) * 3600;
	}

	
	@Override
	public void notifyNewOriginNode(Node fromNode, Double departureTime) {
		this.fromNode = fromNode;
		this.lcptTravelDistance.calculate(network, fromNode, departureTime);
	}

	
	@Override
	public double computeContributionOfOpportunity(ActivityFacility origin, AggregationObject destination, Double departureTime) {
		// TODO departure time is not used, dz, apr'17
		Link nearestLinkToOrigin = NetworkUtils.getNearestLinkExactly(network, origin.getCoord());

		// Captures the distance between the origin via the link to the node:
		Distances distances = NetworkUtil.getDistances2NodeViaGivenLink(origin.getCoord(), nearestLinkToOrigin, fromNode);

		// TODO: extract this walk part?
		// In the state found before modularization (june 15), this was anyway not consistent accross modes
		// (different for PtMatrix), pointing to the fact that making this mode-specific might make sense.
		// distance to road, and then to node:
		
		// Utility to get on the network by walking
		double distancePoint2Intersection_m = distances.getDistancePoint2Intersection();
		double utilityMeasuringPoint2Road = (distancePoint2Intersection_m / this.walkSpeed_m_h * betaWalkTT)	+ (distancePoint2Intersection_m * betaWalkTD);
		
		// Utility on the network to first node
		double distanceIntersection2Node_m = distances.getDistanceIntersection2Node();
		double utilityRoad2Node = (distanceIntersection2Node_m / modeSpeed_m_h * betaModeTT)	+ (distanceIntersection2Node_m * betaModeTD); // toll or money ???
	
		// Uutility on the network from first node to destination node
		double travelDistance_m = lcptTravelDistance.getTree().get(destination.getNearestNode().getId()).getCost(); // travel link distances on road network for bicycle and walk
		double utility = ((travelDistance_m / modeSpeed_m_h * betaModeTT) + (travelDistance_m * betaModeTD)); // toll or money ???

		// Utility based on opportunities that are attached to destination node
		double sumExpVjkWalk = destination.getSum();
		
		// exp(beta * a) * exp(beta * b) = exp(beta * (a+b))
		return Math.exp(logitScaleParameter * (constMode + utilityMeasuringPoint2Road + utilityRoad2Node + utility)) * sumExpVjkWalk;
	}
}