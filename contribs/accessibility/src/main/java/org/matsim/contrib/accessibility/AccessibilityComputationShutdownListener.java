/* *********************************************************************** *
 * project: org.matsim.*												   *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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

import java.io.File;
import java.util.*;
import java.util.concurrent.*;

import com.google.common.collect.Iterables;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.contrib.accessibility.interfaces.FacilityDataExchangeInterface;
import org.matsim.contrib.accessibility.utils.AccessibilityUtils;
import org.matsim.contrib.accessibility.utils.AggregationObject;
import org.matsim.contrib.accessibility.utils.ProgressBar;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.controler.events.ShutdownEvent;
import org.matsim.core.controler.listener.ShutdownListener;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.ActivityFacility;

/**
 * @author dziemke
 */
public final class AccessibilityComputationShutdownListener implements ShutdownListener {
	private static final Logger LOG = Logger.getLogger(AccessibilityComputationShutdownListener.class);

    private final ActivityFacilities measuringPoints;
    private ActivityFacilities opportunities;

	private String outputDirectory;

	private final Map<String, AccessibilityContributionCalculator> calculators = new LinkedHashMap<>();
	private AccessibilityAggregator accessibilityAggregator;
	private final ArrayList<FacilityDataExchangeInterface> zoneDataExchangeListeners = new ArrayList<>();

	private AccessibilityConfigGroup acg;
	private final PlanCalcScoreConfigGroup cnScoringGroup;

	
	public AccessibilityComputationShutdownListener(Scenario scenario, ActivityFacilities measuringPoints, ActivityFacilities opportunities,
										   String outputDirectory) {
	    this.measuringPoints = measuringPoints;
	    this.opportunities = opportunities;

		this.outputDirectory = outputDirectory;

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
	}

	private List<ActivityFacilities> additionalFacilityData = new ArrayList<>() ;


	@Override
	public void notifyShutdown(ShutdownEvent event) {
		if (event.isUnexpected()) {
			return;
		}
		LOG.info("Initializing accessibility computation...");
		accessibilityAggregator = new AccessibilityAggregator();
		addFacilityDataExchangeListener(accessibilityAggregator);

		if (outputDirectory != null) {
			File file = new File(outputDirectory);
			file.mkdirs();
		}

		LOG.info("Start computing accessibilities.");
		computeAccessibilities(acg.getTimeOfDay(), opportunities);
		LOG.info("Finished computing accessibilities.");

		writeCSVFile(outputDirectory);
	}


	public final void computeAccessibilities(Double departureTime, ActivityFacilities opportunities) {
		for (String mode : calculators.keySet()) {
			AccessibilityContributionCalculator calculator = calculators.get(mode);
			calculator.initialize(measuringPoints, opportunities);

            Map<Id<Node>, ArrayList<ActivityFacility>> aggregatedOrigins = calculator.getAggregatedMeasurePoints();
            Map<Id<Node>, AggregationObject> aggregatedOpportunities = calculator.getAgregatedOpportunities();

            Collection<Id<Node>> aggregatedOriginNodes = new LinkedList<>();
			for (Id<Node> nodeId : aggregatedOrigins.keySet()) {
				aggregatedOriginNodes.add(nodeId);
			}

			LOG.info("Iterating over all aggregated measuring points...");

			if (acg.isUseParallelization()) {
				int numberOfProcessors = Runtime.getRuntime().availableProcessors();
				LOG.info("There are " + numberOfProcessors + " available processors.");

				final int partitionSize = (int) ((double) aggregatedOrigins.size() / numberOfProcessors) + 1;
				LOG.info("Size of partitions = " + partitionSize);
				Iterable<List<Id<Node>>> partitions = Iterables.partition(aggregatedOriginNodes, partitionSize);

				ProgressBar progressBar = new ProgressBar(aggregatedOrigins.size());

				ExecutorService service = Executors.newFixedThreadPool(numberOfProcessors);
				List<Callable<Void>> tasks = new ArrayList<>();

				for (final List<Id<Node>> partition : partitions) {
					tasks.add(() -> {
						try {
							compute(mode, departureTime, aggregatedOpportunities, aggregatedOrigins, partition, progressBar);
						} catch (Exception e) {
							throw new RuntimeException(e);
						}
						return null;
					});
				}

				try {
					List<Future<Void>> futures = service.invokeAll(tasks);
					for (Future<Void> future : futures) {
						future.get();
					}
				} catch (InterruptedException e) {
					throw new RuntimeException(e);
				} catch (ExecutionException e) {
					throw new RuntimeException(e);
				}
				service.shutdown();
			} else {
				LOG.info("Performing the computation without parallelization.");
				ProgressBar progressBar = new ProgressBar(aggregatedOrigins.size());
				compute(mode, departureTime, aggregatedOpportunities, aggregatedOrigins, aggregatedOriginNodes, progressBar);
			}

			for (FacilityDataExchangeInterface zoneDataExchangeInterface : this.zoneDataExchangeListeners) {
				zoneDataExchangeInterface.finish();
			}
		}
	}


	private void compute(String mode, Double departureTime, Map<Id<Node>, AggregationObject> aggregatedOpportunities, Map<Id<Node>,
			ArrayList<ActivityFacility>> aggregatedOrigins, Collection<Id<Node>> subsetOfNodes, ProgressBar progressBar) {

		AccessibilityContributionCalculator calculator = calculators.get(mode).duplicate();

		// Go through all nodes that have a measuring point assigned
		for (Id<Node> fromNodeId : subsetOfNodes) {
			progressBar.update();

			Gbl.assertNotNull(calculator);
			calculator.notifyNewOriginNode(fromNodeId, departureTime);

			// Go through all measuring points assigned to current node
			for (ActivityFacility origin : aggregatedOrigins.get(fromNodeId)) {
				assert(origin.getCoord() != null);

				Map<String,Double> expSums = new ConcurrentHashMap<>();
				expSums.put(mode, 0.);

				// Go through all aggregated opportunities (i.e. network nodes to which at least one opportunity is assigned)
				for (final AggregationObject aggregatedOpportunity : aggregatedOpportunities.values()) {
				    final double expVhk = calculator.computeContributionOfOpportunity(origin, aggregatedOpportunity, departureTime);
				    expSums.put(mode, expSums.get(mode) + expVhk);
				}

				double accessibility;
                if (acg.getAccessibilityMeasureType() == AccessibilityConfigGroup.AccessibilityMeasureType.logSum) {
                    accessibility = (1/this.cnScoringGroup.getBrainExpBeta()) * Math.log(expSums.get(mode));
                } else if (acg.getAccessibilityMeasureType() == AccessibilityConfigGroup.AccessibilityMeasureType.rawSum) {
                    accessibility = expSums.get(mode);
                } else if (acg.getAccessibilityMeasureType() == AccessibilityConfigGroup.AccessibilityMeasureType.gravity) {
                    throw new IllegalArgumentException("This accessibility measure is not yet implemented.");
                } else {
                    throw new IllegalArgumentException("No valid accessibility measure type chosen.");
                }

				for (FacilityDataExchangeInterface zoneDataExchangeInterface : this.zoneDataExchangeListeners) {
					zoneDataExchangeInterface.setFacilityAccessibilities(origin, departureTime, mode, accessibility);
				}
			}
		}
	}


	private void writeCSVFile(String adaptedOutputDirectory) {
		LOG.info("Start writing accessibility output to " + adaptedOutputDirectory + ".");

		Map<Tuple<ActivityFacility, Double>, Map<String,Double>> accessibilitiesMap = accessibilityAggregator.getAccessibilitiesMap();
		final CSVWriter writer = new CSVWriter(adaptedOutputDirectory + "/" + CSVWriter.FILE_NAME ) ;

		// Write header
		writer.writeField(Labels.X_COORDINATE);
		writer.writeField(Labels.Y_COORDINATE);
		writer.writeField(Labels.TIME);
		for (String mode : getModes() ) {
			writer.writeField(mode + "_accessibility");
		}
		for (ActivityFacilities additionalDataFacilities : this.additionalFacilityData) { // Iterate over all additional data collections
			String additionalDataName = additionalDataFacilities.getName();
			writer.writeField(additionalDataName);
		}
		writer.writeNewLine();

		// Write data
		for (Tuple<ActivityFacility, Double> tuple : accessibilitiesMap.keySet()) {
			ActivityFacility facility = tuple.getFirst();
			writer.writeField(facility.getCoord().getX());
			writer.writeField(facility.getCoord().getY());
			writer.writeField(tuple.getSecond());
			
			for (String mode : getModes() ) {
				final double value = accessibilitiesMap.get(tuple).get(mode);
				if (!Double.isNaN(value)) { 
					writer.writeField(value) ;
				} else {
					writer.writeField(Double.NaN) ;
				}
			}
			for (ActivityFacilities additionalDataFacilities : this.additionalFacilityData) { // Again: Iterate over all additional data collections
				String additionalDataName = additionalDataFacilities.getName();
				int value = (int) facility.getAttributes().getAttribute(additionalDataName);
				writer.writeField(value);
			}
			writer.writeNewLine();
		}
		writer.close() ;
		LOG.info("Finished writing accessibility output to " + adaptedOutputDirectory + ".");
	}


	public final void putAccessibilityContributionCalculator(String mode, AccessibilityContributionCalculator calculator) {
		LOG.info("Adding accessibility contribution calculator for " + mode + ".");
		Gbl.assertNotNull(calculator);
		this.calculators.put(mode, calculator) ;
	}


	public void addAdditionalFacilityData(ActivityFacilities facilities) {
		this.additionalFacilityData.add(facilities);
	}


	public void addFacilityDataExchangeListener(FacilityDataExchangeInterface listener){
		this.zoneDataExchangeListeners.add(listener);
	}


	public Set<String> getModes() {
		return this.calculators.keySet() ;
	}
}