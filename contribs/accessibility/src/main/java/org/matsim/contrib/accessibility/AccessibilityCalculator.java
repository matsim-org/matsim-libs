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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.contrib.accessibility.interfaces.FacilityDataExchangeInterface;
import org.matsim.contrib.accessibility.utils.AggregationObject;
import org.matsim.contrib.accessibility.utils.ProgressBar;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.network.NetworkUtils;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.ActivityFacilitiesImpl;
import org.matsim.facilities.ActivityFacility;

/**
 * @author thomas, nagel, dziemke
 */
public final class AccessibilityCalculator {
	private static final Logger LOG = Logger.getLogger(AccessibilityCalculator.class);

	private final ActivityFacilitiesImpl measuringPoints;
	private final Map<String, AccessibilityContributionCalculator> calculators = new LinkedHashMap<>();
	// (test may depend on that this is a "Linked" Hash Map. kai, dec'16)
	
	private PlanCalcScoreConfigGroup cnScoringGroup;
	private AccessibilityConfigGroup acg;
	private Network network;
	private double walkSpeedMeterPerHour;

	private final ArrayList<FacilityDataExchangeInterface> zoneDataExchangeListeners = new ArrayList<>();
	

	public AccessibilityCalculator(Scenario scenario, ActivityFacilitiesImpl measuringPoints) {
		this.network = scenario.getNetwork();
		this.measuringPoints = measuringPoints;
		this.acg = ConfigUtils.addOrGetModule(scenario.getConfig(), AccessibilityConfigGroup.GROUP_NAME, AccessibilityConfigGroup.class);
		this.cnScoringGroup = scenario.getConfig().planCalcScore();

		if (cnScoringGroup.getOrCreateModeParams(TransportMode.car).getMarginalUtilityOfDistance() != 0.) {
			LOG.error("marginal utility of distance for car different from zero but not used in accessibility computations");
		}
		if (cnScoringGroup.getOrCreateModeParams(TransportMode.pt).getMarginalUtilityOfDistance() != 0.) {
			LOG.error("marginal utility of distance for pt different from zero but not used in accessibility computations");
		}
		if (cnScoringGroup.getOrCreateModeParams(TransportMode.bike).getMonetaryDistanceRate() != 0.) {
			LOG.error("monetary distance cost rate for bike different from zero but not used in accessibility computations");
		}
		if (cnScoringGroup.getOrCreateModeParams(TransportMode.walk).getMonetaryDistanceRate() != 0.) {
			LOG.error("monetary distance cost rate for walk different from zero but not used in accessibility computations");
		}

		this.walkSpeedMeterPerHour = scenario.getConfig().plansCalcRoute().getTeleportedModeSpeeds().get(TransportMode.walk) * 3600.;
	}
	
	
	public final void computeAccessibilities(Double departureTime, ActivityFacilities opportunities) {
		AggregationObject[] aggregatedOpportunities = aggregateOpportunities(opportunities, network);

		Map<String,ExpSum> expSums = new HashMap<>() ;
		for (String mode : calculators.keySet()) {
			LOG.warn("Calculate ExpSum for mode = " + mode);
			expSums.put(mode, new ExpSum());
		}

		// Condense measuring points (origins) that have the same nearest node on the network
		Map<Id<Node>, ArrayList<ActivityFacility>> aggregatedOrigins = aggregateMeasurePointsWithSameNearestNode();

		LOG.info("Iterating over all (aggregated) origins:");
		// ProgressBar progressBar = new ProgressBar(aggregatedOrigins.size());
		
		for (Id<Node> nodeId : aggregatedOrigins.keySet()) { // Go through all nodes (keys) that have a measuring point (origin) assigned
			LOG.info("Calculate accessibility for node = " + nodeId);
			// progressBar.update();

			Node fromNode = network.getNodes().get(nodeId);

			for (AccessibilityContributionCalculator calculator : calculators.values()) {
				Gbl.assertNotNull(calculator);
				calculator.notifyNewOriginNode(fromNode, departureTime);
			}

			// get list with origins that are assigned to "fromNode" // TODO Why fromNode?
			for (ActivityFacility origin : aggregatedOrigins.get(nodeId)) {
				assert(origin.getCoord() != null);

				for (ExpSum expSum : expSums.values()) {
					expSum.reset();
				}
				
//				Gbl.assertIf( this.aggregatedOpportunities.length>0);
				// yyyyyy a test fails when this line is made active; cannot say why an execution path where there are now opportunities can make sense for a test.  kai, mar'17
				
				for (final AggregationObject aggregatedFacility : aggregatedOpportunities) {
//					computeAndAddExpUtilContributions( expSums, origin, aggregatedFacility, departureTime );
					for (Map.Entry<String, AccessibilityContributionCalculator> calculatorEntry : calculators.entrySet()) {
						final double expVhk = calculatorEntry.getValue().computeContributionOfOpportunity(origin , aggregatedFacility, departureTime);
						expSums.get(calculatorEntry.getKey()).addExpUtils(expVhk);
					}
				}
				// What does the aggregation of the starting locations save if we do the just ended loop for all starting
				// points separately anyways?  Answer: The trees need to be computed only once.  (But one could save more.) kai, feb'14

				// aggregated value
				Map<String, Double> accessibilities  = new LinkedHashMap<>();

				for (String mode : calculators.keySet()) {
					LOG.info("---- calculate accessibility for mode=" + mode );
					if(!acg.isUsingRawSumsWithoutLn()){
						LOG.info("expSums.get(mode).getSum() = " + expSums.get(mode).getSum());
						// logitScaleParameter = same as brainExpBeta on 2-aug-12. kai
						accessibilities.put(mode, (1/this.cnScoringGroup.getBrainExpBeta()) * Math.log( expSums.get(mode).getSum()));
					} else {
						// this was used by IVT within SustainCity.  Not sure if we should maintain this; they could, after all, just exp the log results. kai, may'15
						accessibilities.put(mode, expSums.get(mode).getSum()) ;
					}
				}

				LOG.warn("");
				LOG.warn("SENDING accessibilities; start zone=" + origin.getId() );
				for ( Entry<String, Double> entry : accessibilities.entrySet() ) {
					LOG.warn( "mode=" + entry.getKey() + "; accessibility=" + entry.getValue() ) ;
				}

				
				for (FacilityDataExchangeInterface zoneDataExchangeInterface : this.zoneDataExchangeListeners) {
					zoneDataExchangeInterface.setFacilityAccessibilities(origin, departureTime, accessibilities);
				}
			}
		}
		for (FacilityDataExchangeInterface zoneDataExchangeInterface : this.zoneDataExchangeListeners) {
			zoneDataExchangeInterface.finish();
		}
	}
	
	
	/**
	 * Aggregates disutilities Vjk to get from node j to all k that are attached to j and assign sum(Vjk) is to node j.
	 * 
	 *     j---k1 
	 *     |\
	 *     | \
	 *     k2 k3
	 */
	private final AggregationObject[] aggregateOpportunities(final ActivityFacilities opportunities, Network network) {
		// yyyy this method ignores the "capacities" of the facilities. kai, mar'14
		// for now, we decided not to add "capacities" as it is not needed for current projects. dz, feb'16

		LOG.info("Aggregating " + opportunities.getFacilities().size() + " opportunities with identical nearest node...");
		Map<Id<Node>, AggregationObject> opportunityClusterMap = new ConcurrentHashMap<>();
		ProgressBar progressBar = new ProgressBar(opportunities.getFacilities().size());

		for (ActivityFacility opportunity : opportunities.getFacilities().values()) {
			progressBar.update();

			Node nearestNode = NetworkUtils.getNearestNode(network,opportunity.getCoord());

			double distance_m = NetworkUtils.getEuclideanDistance(opportunity.getCoord(), nearestNode.getCoord());
			
			// in MATSim this is [utils/h]: cnScoringGroup.getTravelingWalk_utils_hr() - cnScoringGroup.getPerforming_utils_hr()
			double betaWalkTT = this.cnScoringGroup.getModes().get(TransportMode.walk).getMarginalUtilityOfTraveling() - this.cnScoringGroup.getPerforming_utils_hr();
			double VjkWalkTravelTime = betaWalkTT * (distance_m / this.walkSpeedMeterPerHour);
			
			// in MATSim this is 0 !!! since getMonetaryDistanceCostRateWalk doesn't exist:
			double betaWalkTD = cnScoringGroup.getModes().get(TransportMode.walk).getMarginalUtilityOfDistance();
			double VjkWalkDistance = betaWalkTD * distance_m;
			
			double expVjk = Math.exp(this.cnScoringGroup.getBrainExpBeta() * (VjkWalkTravelTime + VjkWalkDistance ) );

			// add Vjk to sum
			AggregationObject jco = opportunityClusterMap.get( nearestNode.getId() ) ;
			if ( jco == null ) {
				jco = new AggregationObject(opportunity.getId(), null, null, nearestNode, 0. ); // initialize with zero!
				opportunityClusterMap.put( nearestNode.getId(), jco ) ; 
			}
			jco.addObject(opportunity.getId(), expVjk);
		}
		LOG.info("Aggregated " + opportunities.getFacilities().size() + " opportunities to " + opportunityClusterMap.size() + " nodes.");
		return opportunityClusterMap.values().toArray(new AggregationObject[opportunityClusterMap.size()]);
	}


	private Map<Id<Node>, ArrayList<ActivityFacility>> aggregateMeasurePointsWithSameNearestNode() {
		Map<Id<Node>,ArrayList<ActivityFacility>> aggregatedOrigins = new ConcurrentHashMap<>();

		Gbl.assertNotNull(measuringPoints);
		Gbl.assertNotNull(measuringPoints.getFacilities()) ;
		for (ActivityFacility measuringPoint : measuringPoints.getFacilities().values()) {

			// determine nearest network node (from- or toNode) based on the link
			Node fromNode = NetworkUtils.getCloserNodeOnLink(measuringPoint.getCoord(),	NetworkUtils.getNearestLinkExactly(network, measuringPoint.getCoord()));

			// this is used as a key for hash map lookups
			Id<Node> nodeId = fromNode.getId();

			// create new entry if key does not exist!
			if(!aggregatedOrigins.containsKey(nodeId)) {
				aggregatedOrigins.put(nodeId, new ArrayList<ActivityFacility>());
			}
			// assign measure point (origin) to it's nearest node
			aggregatedOrigins.get(nodeId).add(measuringPoint);
		}
		LOG.info("Number of measurement points (origins): " + measuringPoints.getFacilities().values().size());
		LOG.info("Number of aggregated measurement points (origins): " + aggregatedOrigins.size());
		return aggregatedOrigins;
	}


	public final void putAccessibilityContributionCalculator(String mode, AccessibilityContributionCalculator calc) {
		LOG.warn("Adding accessibility calculator for mode = " + mode ) ;
		Gbl.assertNotNull(calc);
		this.calculators.put(mode , calc) ;
	}

	
	public Set<String> getModes() {
		return this.calculators.keySet() ;
	}
	
	
	public void addFacilityDataExchangeListener(FacilityDataExchangeInterface listener){
		this.zoneDataExchangeListeners.add(listener);
	}


	/**
	 * stores travel disutilities for different modes
	 */
	static class ExpSum {
		// could just use Map<Modes4Accessibility,Double>, but since it is fairly far inside the loop, I leave it with primitive
		// variables on the (unfounded) intuition that this helps with computational speed.  kai, 
		private double sum = 0.;

		void reset() {
			this.sum = 0.;
		}

		void addExpUtils(double val){
			this.sum += val;
		}

		double getSum(){
			return this.sum;
		}
	}
}