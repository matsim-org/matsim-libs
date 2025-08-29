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
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.BasicLocation;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.contrib.accessibility.utils.AggregationObject;
import org.matsim.contrib.accessibility.utils.ProgressBar;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.ConfigWriter;
import org.matsim.core.config.groups.ScoringConfigGroup;
import org.matsim.core.controler.events.ShutdownEvent;
import org.matsim.core.controler.listener.ShutdownListener;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.ActivityFacility;

import javax.xml.crypto.Data;

/**
 * @author dziemke
 */
final class AccessibilityComputationShutdownListener implements ShutdownListener {
	private static final Logger LOG = LogManager.getLogger(AccessibilityComputationShutdownListener.class);

    private final ActivityFacilities measuringPoints;
	private final Scenario scenario;
	private ActivityFacilities opportunities;

	private String outputDirectory;

	private final Map<String, AccessibilityContributionCalculator> calculators = new LinkedHashMap<>();
	private DataExchangeInterface accessibilityAggregator;
	private final ArrayList<DataExchangeInterface> zoneDataExchangeListeners = new ArrayList<>();

	private Config config;
	private AccessibilityConfigGroup acg;
	private final ScoringConfigGroup cnScoringGroup;


	public AccessibilityComputationShutdownListener(Scenario scenario, ActivityFacilities measuringPoints, ActivityFacilities opportunities,
										   String outputDirectory) {
	    this.measuringPoints = measuringPoints;
	    this.opportunities = opportunities;

		this.outputDirectory = outputDirectory;

		this.scenario = scenario;

		this.config = scenario.getConfig();

		this.acg = ConfigUtils.addOrGetModule(scenario.getConfig(), AccessibilityConfigGroup.GROUP_NAME, AccessibilityConfigGroup.class);
		this.cnScoringGroup = scenario.getConfig().scoring();

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
//	// consider refactoring the rest of this class into the method process and  call this from the simwrapper (see kelheim emissions dashboard)

//		process();
//	}
//
//	public void process() {
		LOG.info("Initializing accessibility computation...");

		if(acg.isPersonBased()){
			accessibilityAggregator = new AccessibilityAggregatorPersonBased();
		}else {
			accessibilityAggregator = new AccessibilityAggregator();

		}
		addFacilityDataExchangeListener(accessibilityAggregator);

		if (outputDirectory != null) {
			File file = new File(outputDirectory);
			file.mkdirs();
		}

		LOG.info("Start computing accessibilities.");
//		for (double timeOfDay : acg.getTimeOfDay()) {
		computeAccessibilities(acg.getTimeOfDay(), opportunities);
//		}
		LOG.info("Finished computing accessibilities.");

		if(acg.isPersonBased()){
			writeCSVFilePersonBased(outputDirectory);
		}else {
			writeCSVFile(outputDirectory);
		}
		writePoiFile(outputDirectory);
		writeConfigUsedForAccessibilityComputation(outputDirectory, config);

	}

	private void writePoiFile(String outputDirectory) {
		LOG.info("Start writing POI output to " + outputDirectory + ".");

		final CSVWriter writer = new CSVWriter(outputDirectory + "/" + CSVWriter.POI_FILE_NAME ) ;

		// Write header
//		writer.writeField(Labels.ID);
		writer.writeField(Labels.X_COORDINATE);
		writer.writeField(Labels.Y_COORDINATE);
		writer.writeNewLine();

		opportunities.getFacilities().values().forEach(facility -> {
			writer.writeField(facility.getCoord().getX());
			writer.writeField(facility.getCoord().getY());
			writer.writeNewLine();
		});

		writer.close() ;
		LOG.info("Finished writing POI output to " + outputDirectory + ".");
	}


	public void computeAccessibilities(List<Double> departureTimes, ActivityFacilities opportunities) {
		for (String mode : calculators.keySet()) {

			for (Double departureTime : departureTimes) {
				AccessibilityContributionCalculator calculator = calculators.get(mode);
				calculator.initialize(measuringPoints, opportunities);

				// TODO
				Map<Id<? extends BasicLocation>, ArrayList<ActivityFacility>> aggregatedOrigins = calculator.getAggregatedMeasurePoints();
				Map<Id<? extends BasicLocation>, AggregationObject> aggregatedOpportunities = calculator.getAgregatedOpportunities();

				Collection<Id<? extends BasicLocation>> aggregatedOriginIds = new LinkedList<>();
				for (Id<? extends BasicLocation> nodeId : aggregatedOrigins.keySet()) {
					aggregatedOriginIds.add(nodeId);
				}

				LOG.info("Iterating over all aggregated measuring points...");

				if (acg.isUseParallelization()) {
					int numberOfProcessors = Runtime.getRuntime().availableProcessors();
					LOG.info("There are " + numberOfProcessors + " available processors.");

					final int partitionSize = (int) ((double) aggregatedOrigins.size() / numberOfProcessors) + 1;
					LOG.info("Size of partitions = " + partitionSize);
					Iterable<List<Id<? extends BasicLocation>>> partitions = Iterables.partition(aggregatedOriginIds, partitionSize);

					ProgressBar progressBar = new ProgressBar(aggregatedOrigins.size());

					ExecutorService service = Executors.newFixedThreadPool(numberOfProcessors);
					List<Callable<Void>> tasks = new ArrayList<>();
					for (final List<Id<? extends BasicLocation>> partition : partitions) {
						tasks.add(() -> {
							try {
								if(acg.isPersonBased()){
									computePersonBased(mode, departureTime, aggregatedOpportunities, scenario.getPopulation());
								}else {
									compute(mode, departureTime, aggregatedOpportunities, aggregatedOrigins, partition, progressBar);
								}
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
					if(acg.isPersonBased()){
						computePersonBased(mode, departureTime, aggregatedOpportunities, scenario.getPopulation());
					}else {
						compute(mode, departureTime, aggregatedOpportunities, aggregatedOrigins, aggregatedOriginIds, progressBar);				}
				}

				if (!mode.equals(Modes4Accessibility.pt.toString())) {
					break;
				}
			}
			for (DataExchangeInterface zoneDataExchangeInterface : this.zoneDataExchangeListeners) {
				zoneDataExchangeInterface.finish();
			}
		}
	}


	private void compute(String mode, Double departureTime, Map<Id<? extends BasicLocation>, AggregationObject> aggregatedOpportunities,
						 Map<Id<? extends BasicLocation>, ArrayList<ActivityFacility>> aggregatedOrigins,
						 Collection<Id<? extends BasicLocation>> subsetOfNodes, ProgressBar progressBar) {

		AccessibilityContributionCalculator calculator;
		if (acg.isUseParallelization()) {
			calculator = calculators.get(mode).duplicate();
		} else {
			calculator = calculators.get(mode);
		}

		// Go through all nodes that have a measuring point assigned
		for (Id<? extends BasicLocation> fromNodeId : subsetOfNodes) {
			progressBar.update();

			Gbl.assertNotNull(calculator);
			calculator.notifyNewOriginNode(fromNodeId, departureTime);

			// Go through all measuring points assigned to current node
			for (ActivityFacility origin : aggregatedOrigins.get(fromNodeId)) {
				assert(origin.getCoord() != null);

                double expSum = calculator.computeContributionOfOpportunity(origin, aggregatedOpportunities, departureTime);

				double accessibility;
                if (acg.getAccessibilityMeasureType() == AccessibilityConfigGroup.AccessibilityMeasureType.logSum) {
					accessibility = (1/this.cnScoringGroup.getBrainExpBeta()) * Math.log(expSum);
                } else if (acg.getAccessibilityMeasureType() == AccessibilityConfigGroup.AccessibilityMeasureType.rawSum) {
					accessibility = expSum;
                } else if (acg.getAccessibilityMeasureType() == AccessibilityConfigGroup.AccessibilityMeasureType.gravity) {
                    throw new IllegalArgumentException("This accessibility measure is not yet implemented.");
                } else {
                    throw new IllegalArgumentException("No valid accessibility measure type chosen.");
                }

				for (DataExchangeInterface zoneDataExchangeInterface : this.zoneDataExchangeListeners) {

					if(zoneDataExchangeInterface instanceof PersonDataExchangeInterface){
						throw new IllegalStateException("The accessibility computation is not set to be person-based, but a PersonDataExchangeInterface was added as listener. Aborting...");
					}
					((FacilityDataExchangeInterface) zoneDataExchangeInterface).setFacilityAccessibilities(origin, departureTime, mode, accessibility);
				}
			}
		}
	}

	private void computePersonBased(String mode, Double departureTime, Map<Id<? extends BasicLocation>, AggregationObject> aggregatedOpportunities,
						  Population population) {

		AccessibilityContributionCalculator calculator;
		if (acg.isUseParallelization()) {
			calculator = calculators.get(mode).duplicate();
		} else {
			calculator = calculators.get(mode);
		}


			// Go through all person assigned to current node
		for (Person person : population.getPersons().values()) {

//				assert(origin.getCoord() != null);

			assert (calculator instanceof TeleportedModeContributionCalculator);

			double expSum = ((TeleportedModeContributionCalculator) calculator).computeContributionOfOpportunityPerson(person, aggregatedOpportunities, departureTime);

			double accessibility;
			if (acg.getAccessibilityMeasureType() == AccessibilityConfigGroup.AccessibilityMeasureType.logSum) {
				accessibility = (1 / this.cnScoringGroup.getBrainExpBeta()) * Math.log(expSum);
			} else if (acg.getAccessibilityMeasureType() == AccessibilityConfigGroup.AccessibilityMeasureType.rawSum) {
				accessibility = expSum;
			} else if (acg.getAccessibilityMeasureType() == AccessibilityConfigGroup.AccessibilityMeasureType.gravity) {
				throw new IllegalArgumentException("This accessibility measure is not yet implemented.");
			} else {
				throw new IllegalArgumentException("No valid accessibility measure type chosen.");
			}


			// todo: what does this do?
				for (DataExchangeInterface zoneDataExchangeInterface : this.zoneDataExchangeListeners) {
					if(zoneDataExchangeInterface instanceof FacilityDataExchangeInterface){
						throw new IllegalStateException("The accessibility computation is not set to be facility-based, but a FacilityDataExchangeInterface was added as listener. Aborting...");
					}
					((PersonDataExchangeInterface) zoneDataExchangeInterface).setPersonAccessibilities(person, departureTime, mode, accessibility);

				}
		}
	}

	private void writeConfigUsedForAccessibilityComputation(String adaptedOutputDirectory, Config config) {
		LOG.info("Start writing accessibility config to " + adaptedOutputDirectory + ".");

		new ConfigWriter(config).write(adaptedOutputDirectory + "/" + AccessibilityModule.CONFIG_FILENAME_ACCESSIBILITY);

		LOG.info("Finished writing accessibility config to " + adaptedOutputDirectory + ".");
	}

	private void writeCSVFile(String adaptedOutputDirectory) {
		LOG.info("Start writing accessibility output to " + adaptedOutputDirectory + ".");

		Map<Tuple<ActivityFacility, Double>, Map<String,Double>> accessibilitiesMap = ((AccessibilityAggregator) accessibilityAggregator).getAccessibilitiesMap();
		final CSVWriter writer = new CSVWriter(adaptedOutputDirectory + "/" + CSVWriter.FILE_NAME ) ;

		// Write header
		writer.writeField(Labels.ID);
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
			writer.writeField(facility.getId().toString());
			writer.writeField(facility.getCoord().getX());
			writer.writeField(facility.getCoord().getY());
			writer.writeField(tuple.getSecond());

			for (String mode : getModes() ) {
				// Defaulting to NaN allows accessibility calculations to be skipped for certain time slices for certain modes
				final double value = accessibilitiesMap.get(tuple).getOrDefault(mode, Double.NaN);
				if (!Double.isNaN(value)) {
					writer.writeField(value);
				} else {
					writer.writeField(Double.NaN);
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

	private void writeCSVFilePersonBased(String adaptedOutputDirectory) {
		LOG.info("Start writing accessibility output to " + adaptedOutputDirectory + ".");

		Map<Tuple<Person, Double>, Map<String,Double>> accessibilitiesMap = ((AccessibilityAggregatorPersonBased) accessibilityAggregator).getAccessibilitiesMap();
		final CSVWriter writer = new CSVWriter(adaptedOutputDirectory + "/" + CSVWriter.FILE_NAME ) ;

		// Write header
		writer.writeField(Labels.ID);
//		writer.writeField(Labels.X_COORDINATE);
//		writer.writeField(Labels.Y_COORDINATE);
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
		for (Tuple<Person, Double> tuple : accessibilitiesMap.keySet()) {
			Person person = tuple.getFirst();
			writer.writeField(person.getId().toString());
//			writer.writeField(facility.getCoord().getX());
//			writer.writeField(facility.getCoord().getY());
			writer.writeField(tuple.getSecond());

			for (String mode : getModes() ) {
				// Defaulting to NaN allows accessibility calculations to be skipped for certain time slices for certain modes
				final double value = accessibilitiesMap.get(tuple).getOrDefault(mode, Double.NaN);
				if (!Double.isNaN(value)) {
					writer.writeField(value);
				} else {
					writer.writeField(Double.NaN);
				}
			}
//			for (ActivityFacilities additionalDataFacilities : this.additionalFacilityData) { // Again: Iterate over all additional data collections
//				String additionalDataName = additionalDataFacilities.getName();
//				int value = (int) facility.getAttributes().getAttribute(additionalDataName);
//				writer.writeField(value);
//			}
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


	public void addFacilityDataExchangeListener(DataExchangeInterface listener){
		this.zoneDataExchangeListeners.add(listener);
	}


	public Set<String> getModes() {
		return this.calculators.keySet() ;
	}
}
