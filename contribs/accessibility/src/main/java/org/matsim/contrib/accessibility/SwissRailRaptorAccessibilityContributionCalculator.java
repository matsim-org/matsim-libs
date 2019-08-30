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

import ch.sbb.matsim.routing.pt.raptor.*;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.*;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.contrib.accessibility.utils.AccessibilityUtils;
import org.matsim.contrib.accessibility.utils.AggregationObject;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.facilities.*;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author dziemke
 */
public class SwissRailRaptorAccessibilityContributionCalculator implements AccessibilityContributionCalculator {
	private static final Logger LOG = Logger.getLogger( SwissRailRaptorAccessibilityContributionCalculator.class );
	private SwissRailRaptor raptor;
	private String mode;
	private PlanCalcScoreConfigGroup planCalcScoreConfigGroup;
	private Scenario scenario;

    Map<Id<? extends BasicLocation>, ArrayList<ActivityFacility>> aggregatedMeasurePoints;
    Map<Id<? extends BasicLocation>, AggregationObject> aggregatedOpportunities;

	private double betaWalkTT;
	private double walkSpeed_m_s;

    SwissRailRaptorData raptorData;

    Map<ActivityFacility, Collection<TransitStopFacility>> stopsPerZone = new LinkedHashMap<>();


    public SwissRailRaptorAccessibilityContributionCalculator(String mode, PlanCalcScoreConfigGroup planCalcScoreConfigGroup, Scenario scenario) {
		this.mode = mode;

		TransitSchedule schedule = null;
		Network ptNetwork = null;

		RaptorStaticConfig raptorConfig = RaptorUtils.createStaticConfig(scenario.getConfig());
		raptorConfig.setOptimization(RaptorStaticConfig.RaptorOptimization.OneToAllRouting);
		this.raptorData = SwissRailRaptorData.create(schedule, raptorConfig, ptNetwork);

		DefaultRaptorParametersForPerson parametersForPerson = new DefaultRaptorParametersForPerson(scenario.getConfig());
		DefaultRaptorStopFinder defaultRaptorStopFinder = new DefaultRaptorStopFinder(null, new DefaultRaptorIntermodalAccessEgress(), null);
		LeastCostRaptorRouteSelector routeSelector = new LeastCostRaptorRouteSelector();

		this.raptor = new SwissRailRaptor(raptorData, parametersForPerson, routeSelector, defaultRaptorStopFinder);
		this.planCalcScoreConfigGroup = planCalcScoreConfigGroup;
		this.scenario = scenario;

		betaWalkTT = planCalcScoreConfigGroup.getModes().get(TransportMode.walk).getMarginalUtilityOfTraveling() - planCalcScoreConfigGroup.getPerforming_utils_hr();

		this.walkSpeed_m_s = scenario.getConfig().plansCalcRoute().getTeleportedModeSpeeds().get(TransportMode.walk);
	}


    @Override
    public void initialize(ActivityFacilities measuringPoints, ActivityFacilities opportunities) {
		LOG.warn("Initializing calculator for mode " + mode + "...");

        // Prepare measure points
        aggregatedMeasurePoints = new ConcurrentHashMap<>();
        Gbl.assertNotNull(measuringPoints);
        Gbl.assertNotNull(measuringPoints.getFacilities());

        for (ActivityFacility measuringPoint : measuringPoints.getFacilities().values()) {
            Id<ActivityFacility> facilityId = measuringPoint.getId();
            if(!aggregatedMeasurePoints.containsKey(facilityId)) {
                aggregatedMeasurePoints.put(facilityId, new ArrayList<>());
            }
            aggregatedMeasurePoints.get(facilityId).add(measuringPoint);
        }


        // Prepare opportunities
        aggregatedOpportunities = new ConcurrentHashMap<>();
        AccessibilityConfigGroup acg = ConfigUtils.addOrGetModule(scenario.getConfig(), AccessibilityConfigGroup.GROUP_NAME, AccessibilityConfigGroup.class);
        for (ActivityFacility opportunity : opportunities.getFacilities().values()) {
            AggregationObject jco = aggregatedOpportunities.get(opportunity.getId());
            if (jco == null) {
                jco = new AggregationObject(opportunity.getId(), null, null, opportunity, 0.);
                aggregatedOpportunities.put(opportunity.getId(), jco);
            }
            if (acg.isUseOpportunityWeights()) {
                if (opportunity.getAttributes().getAttribute(AccessibilityAttributes.WEIGHT) == null) {
                    throw new RuntimeException("If option \"useOpportunityWeights\" is used, the facilities must have an attribute with key " + AccessibilityAttributes.WEIGHT + ".");
                } else {
                    double weight = Double.parseDouble(opportunity.getAttributes().getAttribute(AccessibilityAttributes.WEIGHT).toString());
                    jco.addObject(opportunity.getId(), 1. * Math.pow(weight, acg.getWeightExponent()));
                }
            } else {
                jco.addObject(opportunity.getId(), 1.);
            }
        }
        LOG.info("Aggregated " + opportunities.getFacilities().size() + " opportunities to " + aggregatedOpportunities.size() + " nodes.");


        // Compute closest egress stops per zone
        for (ActivityFacility facility : opportunities.getFacilities().values()) {
            final Coord coord = facility.getCoord();
            // Collection<TransitStopFacility> stops = raptorData.findNearbyStops(coord.getX(), coord.getY(), parameters.getSearchRadius());
            Collection<TransitStopFacility> stops = raptorData.findNearbyStops(coord.getX(), coord.getY(), 1000.);
            if (stops.isEmpty()) {
                TransitStopFacility nearest = raptorData.findNearestStop(coord.getX(), coord.getY());
                double nearestStopDistance = CoordUtils.calcEuclideanDistance(coord, nearest.getCoord());
                // stops = raptorData.findNearbyStops(coord.getX(), coord.getY(), nearestStopDistance + parameters.getExtensionRadius());
                stops = raptorData.findNearbyStops(coord.getX(), coord.getY(), nearestStopDistance + 200.);
            }
            stopsPerZone.put(facility, stops);
        }
    }


	@Override
	public void notifyNewOriginNode(Id<? extends BasicLocation> fromNodeId, Double departureTime) {
	}


	@Override
	public double computeContributionOfOpportunity(ActivityFacility origin,
            Map<Id<? extends BasicLocation>, AggregationObject> aggregatedOpportunities, Double departureTime) {
        double expSum = 0.;

        final Map<Id<TransitStopFacility>, SwissRailRaptorCore.TravelInfo> idTravelInfoMap = raptor.calcTree(origin, departureTime, null);

        for (final AggregationObject destination : aggregatedOpportunities.values()) {
            //compute direct walk costs
            final Coord toCoord = destination.getNearestNode().getCoord();
            double directDistance = CoordUtils.calcEuclideanDistance(origin.getCoord(), toCoord);
            double directWalkCost = directDistance / walkSpeed_m_s * betaWalkTT;

            double travelCost = Double.MAX_VALUE;
            for (TransitStopFacility stop : stopsPerZone.get(destination)) {
                final SwissRailRaptorCore.TravelInfo travelInfo = idTravelInfoMap.get(stop.getId());
                if (travelInfo != null) {
                    double distance = CoordUtils.calcEuclideanDistance(stop.getCoord(), toCoord);
                    double egressWalkCost = distance / walkSpeed_m_s  * betaWalkTT;
                    //total travel time includes travel, access, egress and waiting costs
                    double cost = travelInfo.accessCost + travelInfo.travelCost + travelInfo.waitingCost + egressWalkCost;
                    //take the most optimistic time up until now
                    travelCost = Math.min(travelCost, cost);
                }
            }

            //check whether direct walk time is cheaper
            travelCost = Math.min(travelCost, directWalkCost);

            double modeSpecificConstant = AccessibilityUtils.getModeSpecificConstantForAccessibilities(mode, planCalcScoreConfigGroup);
            expSum += Math.exp(this.planCalcScoreConfigGroup.getBrainExpBeta() * (travelCost + modeSpecificConstant));
        }
        return expSum;
	}


	@Override
	public SwissRailRaptorAccessibilityContributionCalculator duplicate() {
		SwissRailRaptorAccessibilityContributionCalculator swissRailRaptorAccessibilityContributionCalculator =
				new SwissRailRaptorAccessibilityContributionCalculator(this.mode, this.planCalcScoreConfigGroup, this.scenario);
        swissRailRaptorAccessibilityContributionCalculator.aggregatedMeasurePoints = this.aggregatedMeasurePoints;
        swissRailRaptorAccessibilityContributionCalculator.aggregatedOpportunities = this.aggregatedOpportunities;
		return swissRailRaptorAccessibilityContributionCalculator;
	}


    @Override
    public Map<Id<? extends BasicLocation>, ArrayList<ActivityFacility>> getAggregatedMeasurePoints() {
        return aggregatedMeasurePoints;
    }


    @Override
    public Map<Id<? extends BasicLocation>, AggregationObject> getAgregatedOpportunities() {
        return aggregatedOpportunities;
    }
}