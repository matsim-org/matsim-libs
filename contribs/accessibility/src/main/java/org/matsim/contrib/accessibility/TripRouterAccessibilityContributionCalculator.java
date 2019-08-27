/* *********************************************************************** *
 * project: org.matsim.*                                                   *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Route;
import org.matsim.contrib.accessibility.utils.AccessibilityUtils;
import org.matsim.contrib.accessibility.utils.AggregationObject;
import org.matsim.contrib.accessibility.utils.Distances;
import org.matsim.contrib.accessibility.utils.NetworkUtil;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.router.TripRouter;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.facilities.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author nagel, dziemke
 */
public class TripRouterAccessibilityContributionCalculator implements AccessibilityContributionCalculator {
	private static final Logger LOG = Logger.getLogger( TripRouterAccessibilityContributionCalculator.class );
	private TripRouter tripRouter ;
	private String mode;
	private PlanCalcScoreConfigGroup planCalcScoreConfigGroup;
	private Scenario scenario;

    Map<Id<Node>, ArrayList<ActivityFacility>> aggregatedMeasurePoints;
    Map<Id<Node>, AggregationObject> aggregatedOpportunities;

	private Network subNetwork;

	private double betaWalkTT;
	private double walkSpeed_m_s;

	private Node fromNode = null;

	private final TravelDisutility travelDisutility;
	private final TravelDisutilityFactory travelDisutilityFactory;
	private final TravelTime travelTime;


	public TripRouterAccessibilityContributionCalculator(String mode, TripRouter tripRouter, PlanCalcScoreConfigGroup planCalcScoreConfigGroup, Scenario scenario,
														 TravelTime travelTime, TravelDisutilityFactory travelDisutilityFactory) {
		this.mode = mode;
		this.tripRouter = tripRouter;
		this.planCalcScoreConfigGroup = planCalcScoreConfigGroup;
		this.scenario = scenario;

		betaWalkTT = planCalcScoreConfigGroup.getModes().get(TransportMode.walk).getMarginalUtilityOfTraveling() - planCalcScoreConfigGroup.getPerforming_utils_hr();

		this.walkSpeed_m_s = scenario.getConfig().plansCalcRoute().getTeleportedModeSpeeds().get(TransportMode.walk);

		this.travelTime = travelTime;
		this.travelDisutilityFactory = travelDisutilityFactory;
		this.travelDisutility = travelDisutilityFactory.createTravelDisutility(travelTime);
	}


    @Override
    public void initialize(ActivityFacilities measuringPoints, ActivityFacilities opportunities) {
		LOG.warn("Initializing calculator for mode " + mode + "...");
		subNetwork = AccessibilityUtils.createModeSpecificSubNetwork(scenario.getNetwork(), mode);

		this.aggregatedMeasurePoints = AccessibilityUtils.aggregateMeasurePointsWithSameNearestNode(measuringPoints, subNetwork);
        this.aggregatedOpportunities = AccessibilityUtils.aggregateOpportunitiesWithSameNearestNode(opportunities, subNetwork, scenario.getConfig());
    }


	@Override
	public void notifyNewOriginNode(Id<Node> fromNodeId, Double departureTime) {
		this.fromNode = subNetwork.getNodes().get(fromNodeId);
	}


	@Override
	public double computeContributionOfOpportunity(ActivityFacility origin, final AggregationObject destination, Double departureTime) {
		Link nearestLink = getNearestLinkInCorrectDirection(origin, subNetwork, fromNode);
		((ActivityFacilityImpl) origin).setLinkId(nearestLink.getId()); // Set nearest link to origin so that router really starts fomr here

		// Orthogonal walk to nearest link
		Distances distance = NetworkUtil.getDistances2NodeViaGivenLink(origin.getCoord(), nearestLink, fromNode);
		double walkTravelTimeMeasuringPoint2Road_h 	= distance.getDistancePoint2Intersection() / (this.walkSpeed_m_s * 3600);
		double walkUtilityMeasuringPoint2Road = (walkTravelTimeMeasuringPoint2Road_h * betaWalkTT);

		// Travel on section of first link to first node
		double distanceFraction = distance.getDistanceIntersection2Node() / nearestLink.getLength();
		double congestedCarUtilityRoad2Node = -travelDisutility.getLinkTravelDisutility(nearestLink, departureTime, null, null) * distanceFraction;

		ActivityFacilitiesFactory activityFacilitiesFactory = new ActivityFacilitiesFactoryImpl();
		ActivityFacility destinationFacility = activityFacilitiesFactory.createActivityFacility(null, destination.getNearestNode().getCoord());

		Gbl.assertNotNull(tripRouter);
		List<? extends PlanElement> plan = tripRouter.calcRoute(mode, origin, destinationFacility, departureTime, null);

		double utility = 0.;
		List<Leg> legs = TripStructureUtils.getLegs(plan);
		// TODO Doing it like this, the pt interaction (e.g. waiting) times will be omitted!
		Gbl.assertIf(!legs.isEmpty());

		for (Leg leg : legs) {
			Route route = leg.getRoute();
			Link endLink = subNetwork.getLinks().get(route.getEndLinkId());
			double endLinkLength = endLink.getLength();
			double endLinkTT = endLinkLength / endLink.getFreespeed(); // This is an assumption as it is only the freespeed

			utility += (leg.getRoute().getDistance() + endLinkLength) * this.planCalcScoreConfigGroup.getModes().get(leg.getMode()).getMarginalUtilityOfDistance();
			utility += (leg.getRoute().getTravelTime() + endLinkTT) * this.planCalcScoreConfigGroup.getModes().get(leg.getMode()).getMarginalUtilityOfTraveling() / 3600.;
			utility += -(leg.getRoute().getTravelTime() + endLinkTT) * this.planCalcScoreConfigGroup.getPerforming_utils_hr() / 3600.;
		}

		// Utility based on opportunities that are attached to destination node
		double sumExpVjkWalk = destination.getSum();

		// exp(beta * a) * exp(beta * b) = exp(beta * (a+b))
		double modeSpecificConstant = AccessibilityUtils.getModeSpecificConstantForAccessibilities(mode, planCalcScoreConfigGroup);
		return Math.exp(this.planCalcScoreConfigGroup.getBrainExpBeta() * (utility + modeSpecificConstant + walkUtilityMeasuringPoint2Road + congestedCarUtilityRoad2Node)) * sumExpVjkWalk;
	}




	@Override
	public TripRouterAccessibilityContributionCalculator duplicate() {
		TripRouterAccessibilityContributionCalculator tripRouterAccessibilityContributionCalculator =
				new TripRouterAccessibilityContributionCalculator(this.mode, this.tripRouter, this.planCalcScoreConfigGroup,
						this.scenario, this.travelTime, this.travelDisutilityFactory);
		tripRouterAccessibilityContributionCalculator.subNetwork = this.subNetwork;
		tripRouterAccessibilityContributionCalculator.aggregatedMeasurePoints = this.aggregatedMeasurePoints;
		tripRouterAccessibilityContributionCalculator.aggregatedOpportunities = this.aggregatedOpportunities;
		return tripRouterAccessibilityContributionCalculator;
	}


    @Override
    public Map<Id<Node>, ArrayList<ActivityFacility>> getAggregatedMeasurePoints() {
        return aggregatedMeasurePoints;
    }


    @Override
    public Map<Id<Node>, AggregationObject> getAgregatedOpportunities() {
        return aggregatedOpportunities;
    }


	private Link getNearestLinkInCorrectDirection(ActivityFacility origin, Network network, Node nodeInWhichLinkHasToEnd) {
		Link nearestLink = NetworkUtils.getNearestLinkExactly(network, origin.getCoord());
		Link reverseLink = NetworkUtils.getConnectingLink(nearestLink.getToNode(), nearestLink.getFromNode());
		if (nearestLink.getToNode().getId() != nodeInWhichLinkHasToEnd.getId()) {
			LOG.info("Link does not have nearest node " + nodeInWhichLinkHasToEnd.getId() + " of origin as toNode. ToNode is " + nearestLink.getToNode().getId());
			if (reverseLink.getToNode().getId() == nodeInWhichLinkHasToEnd.getId()) {
				LOG.info("Use reverse direction of link " + nearestLink.getId() + ", i.e. use link " + reverseLink.getId());
				nearestLink = reverseLink;
			}
		}
		return nearestLink;
	}
}