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
import org.matsim.contrib.accessibility.utils.AggregationObject;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.facilities.*;
import org.matsim.pt.transitSchedule.TransitStopFacilityImpl;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author dziemke
 */
class SwissRailRaptorAccessibilityContributionCalculator implements AccessibilityContributionCalculator {
	private static final Logger LOG = Logger.getLogger( SwissRailRaptorAccessibilityContributionCalculator.class );
	private SwissRailRaptor raptor;
	private String mode;
	private PlanCalcScoreConfigGroup planCalcScoreConfigGroup;
	private Scenario scenario;

    Map<Id<? extends BasicLocation>, ArrayList<ActivityFacility>> aggregatedMeasurePoints;
    Map<Id<? extends BasicLocation>, AggregationObject> aggregatedOpportunities;

	private double betaWalkTT;
	private double walkSpeed_m_h;

    SwissRailRaptorData raptorData;

    Map<Id<ActivityFacility>, Collection<TransitStopFacility>> stopsPerAggregatedOpportunity = new LinkedHashMap<>();


    public SwissRailRaptorAccessibilityContributionCalculator(String mode, PlanCalcScoreConfigGroup planCalcScoreConfigGroup, Scenario scenario) {
		this.mode = mode;

		TransitSchedule schedule = scenario.getTransitSchedule();
		Network ptNetwork = scenario.getNetwork();

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

		this.walkSpeed_m_h = scenario.getConfig().plansCalcRoute().getTeleportedModeSpeeds().get(TransportMode.walk) * 3600.;
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

            // Find stops close to opportunity
            final Coord coord = opportunity.getCoord();
            Collection<TransitStopFacility> stops = raptorData.findNearbyStops(coord.getX(), coord.getY(), scenario.getConfig().transitRouter().getSearchRadius());
            if (stops.isEmpty()) {
                TransitStopFacilityImpl nearest = (TransitStopFacilityImpl) raptorData.findNearestStop(coord.getX(), coord.getY());
                double nearestStopDistance = CoordUtils.calcEuclideanDistance(coord, nearest.getCoord());
                stops = raptorData.findNearbyStops(coord.getX(), coord.getY(), nearestStopDistance + scenario.getConfig().transitRouter().getExtensionRadius());
            }

            AggregationObject jco = aggregatedOpportunities.get(opportunity.getId());
            if (jco == null) {
                jco = new AggregationObject(opportunity.getId(), null, null, opportunity, 0.);
                aggregatedOpportunities.put(opportunity.getId(), jco);
            }
            if (acg.isUseOpportunityWeights()) {
                if (opportunity.getAttributes().getAttribute( Labels.WEIGHT ) == null) {
                    throw new RuntimeException("If option \"useOpportunityWeights\" is used, the facilities must have an attribute with key " + Labels.WEIGHT + ".");
                } else {
                    double weight = Double.parseDouble(opportunity.getAttributes().getAttribute( Labels.WEIGHT ).toString() );
                    jco.addObject(opportunity.getId(), 1. * Math.pow(weight, acg.getWeightExponent()));
                }
            } else {
                jco.addObject(opportunity.getId(), 1.);
            }

            stopsPerAggregatedOpportunity.put(opportunity.getId(), stops);
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
            final Coord toCoord = destination.getNearestBasicLocation().getCoord();
            double directDistance_m = CoordUtils.calcEuclideanDistance(origin.getCoord(), toCoord);
            double directWalkCost = -directDistance_m / walkSpeed_m_h * betaWalkTT;

            double travelCost = Double.MAX_VALUE;
            ActivityFacility nearestStop = ((ActivityFacility) destination.getNearestBasicLocation());
            Collection<TransitStopFacility> stops = stopsPerAggregatedOpportunity.get(nearestStop.getId());

            for (TransitStopFacility stop : stops) {
                final SwissRailRaptorCore.TravelInfo travelInfo = idTravelInfoMap.get(stop.getId());
                if (travelInfo != null) {
                    double distance = CoordUtils.calcEuclideanDistance(stop.getCoord(), toCoord);
                    double egressWalkCost = - distance / walkSpeed_m_h  * betaWalkTT;
                    //total travel cost include travel, access, egress and waiting costs
                    double cost = travelInfo.accessCost + travelInfo.travelCost + travelInfo.waitingCost + egressWalkCost;
                    travelCost = Math.min(travelCost, cost);
                }
            }

            //check whether direct walk time is cheaper
            travelCost = Math.min(travelCost, directWalkCost);

            double modeSpecificConstant = AccessibilityUtils.getModeSpecificConstantForAccessibilities(mode, planCalcScoreConfigGroup);
            expSum += Math.exp(this.planCalcScoreConfigGroup.getBrainExpBeta() * (-travelCost + modeSpecificConstant));
        }
        return expSum;
	}


	@Override
	public SwissRailRaptorAccessibilityContributionCalculator duplicate() {
		SwissRailRaptorAccessibilityContributionCalculator swissRailRaptorAccessibilityContributionCalculator =
				new SwissRailRaptorAccessibilityContributionCalculator(this.mode, this.planCalcScoreConfigGroup, this.scenario);
        swissRailRaptorAccessibilityContributionCalculator.aggregatedMeasurePoints = this.aggregatedMeasurePoints;
        swissRailRaptorAccessibilityContributionCalculator.aggregatedOpportunities = this.aggregatedOpportunities;
        swissRailRaptorAccessibilityContributionCalculator.stopsPerAggregatedOpportunity = this.stopsPerAggregatedOpportunity;
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
