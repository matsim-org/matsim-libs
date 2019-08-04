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

import com.google.common.collect.Iterables;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.contrib.accessibility.AccessibilityConfigGroup.AccessibilityMeasureType;
import org.matsim.contrib.accessibility.interfaces.FacilityDataExchangeInterface;
import org.matsim.contrib.accessibility.utils.AggregationObject;
import org.matsim.contrib.accessibility.utils.ProgressBar;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.network.NetworkUtils;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.ActivityFacility;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author thomas, nagel, dziemke
 */
public final class AccessibilityCalculator {
	private static final Logger LOG = Logger.getLogger(AccessibilityCalculator.class);

	private final ActivityFacilities measuringPoints;
	private final Map<String, AccessibilityContributionCalculator> calculators = new LinkedHashMap<>();
	// (test may depend on that this is a "Linked" Hash Map. kai, dec'16)

	private final PlanCalcScoreConfigGroup cnScoringGroup;
	private final AccessibilityConfigGroup acg;
	private final Network network;
	private final double walkSpeed_m_h;

	private final ArrayList<FacilityDataExchangeInterface> zoneDataExchangeListeners = new ArrayList<>();


	public AccessibilityCalculator(Scenario scenario, ActivityFacilities measuringPoints, Network network) {
		this.network = network;
		this.measuringPoints = measuringPoints;
		this.acg = ConfigUtils.addOrGetModule(scenario.getConfig(), AccessibilityConfigGroup.GROUP_NAME, AccessibilityConfigGroup.class);
		this.cnScoringGroup = scenario.getConfig().planCalcScore();

		if (cnScoringGroup.getOrCreateModeParams(TransportMode.car).getMarginalUtilityOfDistance() != 0.) {
			LOG.error("Marginal utility of distance for car different from zero, but not used in accessibility computations");
		}
		if (cnScoringGroup.getOrCreateModeParams(TransportMode.pt).getMarginalUtilityOfDistance() != 0.) {
			LOG.error("Marginal utility of distance for pt different from zero, but not used in accessibility computations");
		}
		if (cnScoringGroup.getOrCreateModeParams(TransportMode.bike).getMonetaryDistanceRate() != 0.) {
			LOG.error("Monetary distance cost rate for bike different from zero, but not used in accessibility computations");
		}
		if (cnScoringGroup.getOrCreateModeParams(TransportMode.walk).getMonetaryDistanceRate() != 0.) {
			LOG.error("Monetary distance cost rate for walk different from zero, but not used in accessibility computations");
		}

		this.walkSpeed_m_h = scenario.getConfig().plansCalcRoute().getTeleportedModeSpeeds().get(TransportMode.walk) * 3600.;
	}

	public final void computeAccessibilities(Double departureTime, ActivityFacilities opportunities) {
		Map<Id<Node>, AggregationObject> aggregatedOpportunities = aggregateOpportunitiesWithSameNearestNode(opportunities, network);
		Map<Id<Node>, ArrayList<ActivityFacility>> aggregatedOrigins = aggregateMeasurePointsWithSameNearestNode();
		Collection<Id<Node>> aggregatedOriginNodes = new LinkedList<>();
		for (Id<Node> nodeId : aggregatedOrigins.keySet()) {
			aggregatedOriginNodes.add(nodeId);
		}

		LOG.info("Iterating over all aggregated measuring points...");
		int numberOfProcessors = Runtime.getRuntime().availableProcessors();
		LOG.info("There are " + numberOfProcessors + " available processors.");
		final int partitionSize = (int) ((double) aggregatedOrigins.size() / numberOfProcessors) + 1;
		LOG.info("partitionSize " + partitionSize);
		Iterable<List<Id<Node>>> partitions = Iterables.partition(aggregatedOriginNodes, partitionSize);

		ProgressBar progressBar = new ProgressBar(aggregatedOrigins.size());

		ConcurrentExecutor<Void> executor = ConcurrentExecutor.fixedPoolService(numberOfProcessors);
		for (final List<Id<Node>> partition : partitions) {
			executor.addTaskToQueue(() -> {
				try {
					Map<String, AccessibilityContributionCalculator> calculatorsForPartition = new LinkedHashMap<>();
					for (String mode : new HashSet<>(calculators.keySet())) {
						calculatorsForPartition.put(mode, calculators.get(mode).duplicate());
					}

					// Go through all nodes that have a measuring point assigned
					for (Id<Node> nodeId : partition) {
						progressBar.update();

						Node fromNode = network.getNodes().get(nodeId);

						for (AccessibilityContributionCalculator calculator : calculatorsForPartition.values()) {
							Gbl.assertNotNull(calculator);
							calculator.notifyNewOriginNode(fromNode, departureTime);
						}

						// Go through all measuring points assigned to current node
						for (ActivityFacility origin : aggregatedOrigins.get(nodeId)) {
							assert(origin.getCoord() != null);

							Map<String,Double> expSums = new ConcurrentHashMap<>();
							for (String mode : calculators.keySet()) {
								expSums.put(mode, 0.);
							}

							// TODO Check what this really means
							// Gbl.assertIf(aggregatedOpportunities.length > 0);
							// yyyyyy a test fails when this line is made active; cannot say why an execution path where there are now opportunities can make sense for a test.  kai, mar'17

							// Go through all aggregated opportunities (i.e. network nodes to which at least one opportunity is assigned)
							for (final AggregationObject aggregatedOpportunity : aggregatedOpportunities.values()) {
								// Go through all calculators
								for (String mode : calculatorsForPartition.keySet()) {
									final double expVhk = calculatorsForPartition.get(mode).computeContributionOfOpportunity(origin, aggregatedOpportunity, departureTime);
									expSums.put(mode, expSums.get(mode) + expVhk);
								}
							}

							Map<String, Double> accessibilities  = new ConcurrentHashMap<>();

							for (String mode : calculatorsForPartition.keySet()) {
								if (acg.getAccessibilityMeasureType() == AccessibilityMeasureType.logSum) {
									accessibilities.put(mode, (1/this.cnScoringGroup.getBrainExpBeta()) * Math.log(expSums.get(mode)));
								} else if (acg.getAccessibilityMeasureType() == AccessibilityMeasureType.rawSum) {
									accessibilities.put(mode, expSums.get(mode));
								} else if (acg.getAccessibilityMeasureType() == AccessibilityMeasureType.gravity) {
									throw new IllegalArgumentException("This accessibility measure is not yet implemented.");
								} else {
									throw new IllegalArgumentException("No valid accessibility measure type chosen.");
								}
							}

							for (FacilityDataExchangeInterface zoneDataExchangeInterface : this.zoneDataExchangeListeners) {
								zoneDataExchangeInterface.setFacilityAccessibilities(origin, departureTime, accessibilities);
							}
						}
					}
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
				return null;
			});
		}
		executor.execute();

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
	private final Map<Id<Node>, AggregationObject> aggregateOpportunitiesWithSameNearestNode(final ActivityFacilities opportunities, Network network) {
		// yyyy this method ignores the "capacities" of the facilities. kai, mar'14
		// for now, we decided not to add "capacities" as it is not needed for current projects. dz, feb'16

		LOG.info("Aggregating " + opportunities.getFacilities().size() + " opportunities with same nearest node...");
		Map<Id<Node>, AggregationObject> opportunityClusterMap = new ConcurrentHashMap<>();
		ProgressBar progressBar = new ProgressBar(opportunities.getFacilities().size());

		for (ActivityFacility opportunity : opportunities.getFacilities().values()) {
			progressBar.update();

			Node nearestNode = NetworkUtils.getNearestNode(network, opportunity.getCoord());
			double distance_m = NetworkUtils.getEuclideanDistance(opportunity.getCoord(), nearestNode.getCoord());

			// in MATSim this is [utils/h]: cnScoringGroup.getTravelingWalk_utils_hr() - cnScoringGroup.getPerforming_utils_hr()
			double walkBetaTT_utils_h = this.cnScoringGroup.getModes().get(TransportMode.walk).getMarginalUtilityOfTraveling()
					- this.cnScoringGroup.getPerforming_utils_hr(); // default values: -12 = (-6.) - (6.)
			double VjkWalkTravelTime = walkBetaTT_utils_h * (distance_m / this.walkSpeed_m_h);
			// System.out.println("VjkWalkTravelTime = " + VjkWalkTravelTime);

			// in MATSim this is 0 !!! since getMonetaryDistanceCostRateWalk doesn't exist:
			double walkBetaTD_utils_m = cnScoringGroup.getModes().get(TransportMode.walk).getMarginalUtilityOfDistance(); // default value: 0.
			double VjkWalkDistance = walkBetaTD_utils_m * distance_m;
			// System.out.println("VjkWalkDistance = " + VjkWalkDistance);

			double expVjk = Math.exp(this.cnScoringGroup.getBrainExpBeta() * (VjkWalkTravelTime + VjkWalkDistance));

			// add Vjk to sum
			AggregationObject jco = opportunityClusterMap.get(nearestNode.getId()); // Why "jco"?
			if (jco == null) {
				jco = new AggregationObject(opportunity.getId(), null, null, nearestNode, 0.); // Important: Initialize with zero!
				// This is a bit counter-intuitive. The first opportunity is added twice to the aggregation object, but the first time with zero
				// "impact". Leave it as is for the time being as urbanSim code uses this, dz, july'17
				opportunityClusterMap.put(nearestNode.getId(), jco);
			}
			if (acg.isUseOpportunityWeights()) {
				if (opportunity.getAttributes().getAttribute(AccessibilityAttributes.WEIGHT) == null) {
					throw new RuntimeException("If option \"useOpportunityWeights\" is used, the facilities must have an attribute with key " + AccessibilityAttributes.WEIGHT + ".");
				} else {
					double weight = Double.parseDouble(opportunity.getAttributes().getAttribute(AccessibilityAttributes.WEIGHT).toString());
					jco.addObject(opportunity.getId(), expVjk * Math.pow(weight, acg.getWeightExponent()));
				}
			} else {
				jco.addObject(opportunity.getId(), expVjk);
			}
//			LOG.info("--- numberOfObjects = " + jco.getNumberOfObjects() + " --- objectIds = " + jco.getObjectIds());
		}
		LOG.info("Quite convoluted aggregation here...");
		LOG.info("Aggregated " + opportunities.getFacilities().size() + " opportunities to " + opportunityClusterMap.size() + " nodes.");
		return opportunityClusterMap;
	}


	private Map<Id<Node>, ArrayList<ActivityFacility>> aggregateMeasurePointsWithSameNearestNode() {
		Map<Id<Node>,ArrayList<ActivityFacility>> aggregatedOrigins = new ConcurrentHashMap<>();

		Gbl.assertNotNull(measuringPoints);
		Gbl.assertNotNull(measuringPoints.getFacilities()) ;
		for (ActivityFacility measuringPoint : measuringPoints.getFacilities().values()) {

			// Determine nearest network node (from- or toNode) based on the link
			Node nearestNode = NetworkUtils.getCloserNodeOnLink(measuringPoint.getCoord(),	NetworkUtils.getNearestLinkExactly(network, measuringPoint.getCoord()));
			Id<Node> nearestNodeId = nearestNode.getId();

			// Create new entry if key does not exist!
			if(!aggregatedOrigins.containsKey(nearestNodeId)) {
				aggregatedOrigins.put(nearestNodeId, new ArrayList<ActivityFacility>());
			}
			// Assign measure point (origin) to it's nearest node
			aggregatedOrigins.get(nearestNodeId).add(measuringPoint);
		}
		LOG.info("Number of measuring points: " + measuringPoints.getFacilities().values().size());
		LOG.info("Number of aggregated measuring points: " + aggregatedOrigins.size());
		return aggregatedOrigins;
	}


	public final void putAccessibilityContributionCalculator(String mode, AccessibilityContributionCalculator calculator) {
		LOG.info("Adding accessibility contribution calculator for " + mode + ".");
		Gbl.assertNotNull(calculator);
		this.calculators.put(mode, calculator) ;
	}


	public Set<String> getModes() {
		return this.calculators.keySet() ;
	}


	public void addFacilityDataExchangeListener(FacilityDataExchangeInterface listener){
		this.zoneDataExchangeListeners.add(listener);
	}
}