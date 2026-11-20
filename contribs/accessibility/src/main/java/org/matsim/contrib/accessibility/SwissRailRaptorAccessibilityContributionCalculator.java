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
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.*;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.population.PersonUtils;
import org.matsim.contrib.accessibility.utils.AggregationObject;
import org.matsim.contrib.accessibility.utils.Distances;
import org.matsim.contrib.accessibility.utils.NetworkUtil;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.ScoringConfigGroup;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.router.TripRouter;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.facilities.*;
import org.matsim.pt.transitSchedule.TransitStopFacilityImpl;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.matsim.utils.objectattributes.attributable.AttributesImpl;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static org.matsim.contrib.accessibility.AccessibilityUtils.extractLeg;

/**
 * @author dziemke
 */
class SwissRailRaptorAccessibilityContributionCalculator implements AccessibilityContributionCalculator {
	private static final Logger LOG = LogManager.getLogger( SwissRailRaptorAccessibilityContributionCalculator.class );
	private final SwissRailRaptor raptor;
	private final String mode;
	private final ScoringConfigGroup scoringConfigGroup;
	private final Scenario scenario;

	private final TripRouter tripRouter;
	private final double betaWalkTT_h;
	private final double betaWalkDist_m;

	private final double teleportedWalkSpeed_m_s;
	private final AccessibilityConfigGroup acg;

	private final double ptDailyMonetaryConstant;
	private final double marginalUtilityOfMoney;
	private final Double globalAverageIncome;

	Map<Id<? extends BasicLocation>, ArrayList<ActivityFacility>> aggregatedMeasurePoints;
    Map<Id<? extends BasicLocation>, AggregationObject> aggregatedOpportunities;

    SwissRailRaptorData raptorData;

    Map<Id<ActivityFacility>, Collection<TransitStopFacility>> stopsPerAggregatedOpportunity = new LinkedHashMap<>();


    public SwissRailRaptorAccessibilityContributionCalculator(String mode, ScoringConfigGroup scoringConfigGroup, Scenario scenario, TripRouter tripRouter) {
		this(mode, scoringConfigGroup, scenario, tripRouter, createRaptorData(scenario));
	}

	/**
	 * The timetable data (SwissRailRaptorData) is read-only and can be shared by all threads;
	 * only the SwissRailRaptor router itself is per thread. Sharing it avoids one full copy of
	 * the timetable (incl. ~10 million transfers for Berlin) per thread, which otherwise runs
	 * out of memory on machines with many cores.
	 */
	private SwissRailRaptorAccessibilityContributionCalculator(String mode, ScoringConfigGroup scoringConfigGroup, Scenario scenario,
															   TripRouter tripRouter, SwissRailRaptorData raptorData) {
		this.mode = mode;

		this.raptorData = raptorData;
		this.raptor = new SwissRailRaptor.Builder(raptorData, scenario.getConfig()).build();
		this.scoringConfigGroup = scoringConfigGroup;
		this.scenario = scenario;
		this.tripRouter = tripRouter;

		this.betaWalkTT_h = scoringConfigGroup.getModes().get(TransportMode.walk).getMarginalUtilityOfTraveling() - scoringConfigGroup.getPerforming_utils_hr();
		this.betaWalkDist_m = scoringConfigGroup.getModes().get(TransportMode.walk).getMarginalUtilityOfDistance();

		this.teleportedWalkSpeed_m_s = scenario.getConfig().routing().getTeleportedModeSpeeds().get(TransportMode.walk);
//		this.acg = ConfigUtils.addOrGetModule(this.scenario.getConfig(), AccessibilityConfigGroup.GROUP_NAME, AccessibilityConfigGroup.class); this or the next one
		acg = (AccessibilityConfigGroup) this.scenario.getConfig().getModules().get(AccessibilityConfigGroup.GROUP_NAME);

		this.ptDailyMonetaryConstant = scoringConfigGroup.getModes().get(mode).getDailyMonetaryConstant();
		this.marginalUtilityOfMoney = scoringConfigGroup.getMarginalUtilityOfMoney();

		// reference income for the income-dependent marginal utility of money, as in the car calculator
		if (acg.isPersonBased()) {
			this.globalAverageIncome = scenario.getPopulation().getPersons().values().stream()
				.filter(person -> PersonUtils.getIncome(person) != null)
				.mapToDouble(PersonUtils::getIncome)
				.filter(dd -> dd > 0)
				.average().getAsDouble();
		} else {
			this.globalAverageIncome = null;
		}
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
//        AccessibilityConfigGroup acg = ConfigUtils.addOrGetModule(scenario.getConfig(), AccessibilityConfigGroup.GROUP_NAME, AccessibilityConfigGroup.class);
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

		Person person = null;

		// optional fixed monetary costs of a pt trip (daily monetary constant), weighted by the
		// income-dependent marginal utility of money; the same for every destination of this origin
		double utilityPtFixedMonetary = 0.;
		if (acg.isIncludeDailyMonetaryConstant()) {
			double incomeFactor = 1.;
			if (acg.isPersonBased()) {
				Person originPerson = (Person) origin.getAttributes().getAttribute("person");
				incomeFactor = this.globalAverageIncome / PersonUtils.getIncome(originPerson);
			}
			utilityPtFixedMonetary = this.ptDailyMonetaryConstant * this.marginalUtilityOfMoney * incomeFactor;
		}
        final Map<Id<TransitStopFacility>, SwissRailRaptorCore.TravelInfo> idTravelInfoMap = raptor.calcTree(origin, departureTime, null, new AttributesImpl());

        for (final AggregationObject destination : aggregatedOpportunities.values()) {
            //compute direct walk costs
			// opportunities are clustered around nearest pt stop. Thus, can only calculate direct walk to the nearest stop and not to the opportunity itself.
			ActivityFacility nearestStop = ((ActivityFacility) destination.getNearestBasicLocation());

			//todo alternative more efficient - teleportedwalkutility
			double teleportedWalkDist_m = NetworkUtils.getEuclideanDistance(origin.getCoord(), nearestStop.getCoord())*1.3; //todo euclideandistance/beeline factor
			double teleportedWalkTime_h = teleportedWalkDist_m/teleportedWalkSpeed_m_s/3600;
			double utilityTeleportedWalk = teleportedWalkTime_h * betaWalkTT_h + teleportedWalkDist_m * betaWalkDist_m + AccessibilityUtils.getModeSpecificConstantForAccessibilities(TransportMode.walk, scoringConfigGroup);


//			List<? extends PlanElement> planElementsDirectWalk = tripRouter.calcRoute(TransportMode.walk, origin, nearestStop, departureTime, null, null);
//			Leg directWalkLeg = extractLeg(planElementsDirectWalk, TransportMode.walk);
//			double directWalkTime_h = directWalkLeg.getTravelTime().seconds() / 3600;
//			double directWalkDist_m = directWalkLeg.getRoute().getDistance();
//			double utilityDirectWalk = directWalkTime_h * betaWalkTT_h + directWalkDist_m * betaWalkDist_m +
//				AccessibilityUtils.getModeSpecificConstantForAccessibilities(TransportMode.walk, scoringConfigGroup);

			// now, we take the collection of stops near the "nearestStop", and find the one which offers the highest utility.
            double travelUtility = -Double.MAX_VALUE;
            Collection<TransitStopFacility> stops = stopsPerAggregatedOpportunity.get(nearestStop.getId());

			double ascPt = AccessibilityUtils.getModeSpecificConstantForAccessibilities(mode, scoringConfigGroup);
			for (TransitStopFacility stop : stops) {
				final SwissRailRaptorCore.TravelInfo travelInfo = idTravelInfoMap.get(stop.getId());
				if (travelInfo != null) {
					List<? extends PlanElement> planElementsEgress = tripRouter.calcRoute(TransportMode.walk, stop, nearestStop, departureTime, null, null);
					Leg egressWalkLeg = extractLeg(planElementsEgress, TransportMode.walk);
					double egressWalkTime_h = egressWalkLeg.getTravelTime().seconds() / 3600;
					double egressWalkDist_m = egressWalkLeg.getRoute().getDistance();
					double utilityEgressWalk = egressWalkTime_h * betaWalkTT_h + egressWalkDist_m * betaWalkDist_m;
					double utility = -travelInfo.accessCost - travelInfo.travelCost  - travelInfo.waitingCost + utilityEgressWalk + ascPt + utilityPtFixedMonetary;
					travelUtility = Math.max(travelUtility, utility);
                }
            }

            //check whether direct walk time is cheaper
			travelUtility = Math.max(travelUtility, utilityTeleportedWalk); //change back to utilityDirectWalk

			expSum += Math.exp(this.scoringConfigGroup.getBrainExpBeta() * travelUtility);

        }
        return expSum;
	}


	private static SwissRailRaptorData createRaptorData(Scenario scenario) {
		TransitSchedule schedule = scenario.getTransitSchedule();
		Network ptNetwork = scenario.getNetwork();

		RaptorStaticConfig raptorConfig = RaptorUtils.createStaticConfig(scenario.getConfig());
		raptorConfig.setOptimization(RaptorStaticConfig.RaptorOptimization.OneToAllRouting);
		return SwissRailRaptorData.create(schedule, null, raptorConfig, ptNetwork, null);
	}

	@Override
	public SwissRailRaptorAccessibilityContributionCalculator duplicate() {
		SwissRailRaptorAccessibilityContributionCalculator swissRailRaptorAccessibilityContributionCalculator =
				new SwissRailRaptorAccessibilityContributionCalculator(this.mode, this.scoringConfigGroup, this.scenario, this.tripRouter, this.raptorData);
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
