/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2017 by the members listed in the COPYING,        *
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

package org.matsim.contrib.drt.optimizer.insertion.selective;

import static org.matsim.contrib.drt.optimizer.insertion.InsertionCostCalculator.INFEASIBLE_SOLUTION_COST;
import static org.matsim.contrib.drt.optimizer.insertion.InsertionDetourTimeCalculator.DetourTimeInfo;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.math.stat.descriptive.SummaryStatistics;
import org.matsim.contrib.drt.optimizer.VehicleEntry;
import org.matsim.contrib.drt.optimizer.insertion.DrtInsertionSearch;
import org.matsim.contrib.drt.optimizer.insertion.InsertionCostCalculator;
import org.matsim.contrib.drt.optimizer.insertion.InsertionDetourTimeCalculator;
import org.matsim.contrib.drt.optimizer.insertion.InsertionWithDetourData;
import org.matsim.contrib.drt.passenger.DrtRequest;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.core.controler.MatsimServices;
import org.matsim.core.mobsim.framework.events.MobsimBeforeCleanupEvent;
import org.matsim.core.mobsim.framework.listeners.MobsimBeforeCleanupListener;

import com.opencsv.CSVWriter;

/**
 * @author michalm
 */
final class SelectiveInsertionSearch implements DrtInsertionSearch, MobsimBeforeCleanupListener {

	private final SelectiveInsertionProvider insertionProvider;
	private final SingleInsertionDetourPathCalculator detourPathCalculator;
	private final InsertionDetourTimeCalculator detourTimeCalculator;
	private final InsertionCostCalculator insertionCostCalculator;
	private final MatsimServices matsimServices;
	private final String mode;

	public SelectiveInsertionSearch(SelectiveInsertionProvider insertionProvider,
			SingleInsertionDetourPathCalculator detourPathCalculator, InsertionCostCalculator insertionCostCalculator,
			DrtConfigGroup drtCfg, MatsimServices matsimServices) {
		this.insertionProvider = insertionProvider;
		this.detourPathCalculator = detourPathCalculator;
		this.insertionCostCalculator = insertionCostCalculator;
		this.detourTimeCalculator = new InsertionDetourTimeCalculator(drtCfg.getStopDuration(), null);
		this.matsimServices = matsimServices;
		this.mode = drtCfg.getMode();
	}

	@Override
	public Optional<InsertionWithDetourData> findBestInsertion(DrtRequest drtRequest,
			Collection<VehicleEntry> vehicleEntries) {
		var selectedInsertion = insertionProvider.getInsertion(drtRequest, vehicleEntries);
		if (selectedInsertion.isEmpty()) {
			return Optional.empty();
		}

		var insertion = selectedInsertion.get().insertion;
		var insertionDetourData = detourPathCalculator.calculatePaths(drtRequest, insertion);
		var insertionWithDetourData = new InsertionWithDetourData(insertion, insertionDetourData,
				detourTimeCalculator.calculateDetourTimeInfo(insertion, insertionDetourData));

		collectDifferences(drtRequest, selectedInsertion.get().detourTimeInfo, insertionWithDetourData.detourTimeInfo);

		double insertionCost = insertionCostCalculator.calculate(drtRequest, insertion,
				insertionWithDetourData.detourTimeInfo);
		return insertionCost >= INFEASIBLE_SOLUTION_COST ? Optional.empty() : Optional.of(insertionWithDetourData);
	}

	private final Map<Integer, SummaryStatistics> pickupTimeLossStats = new LinkedHashMap<>();
	private final Map<Integer, SummaryStatistics> dropoffTimeLossStats = new LinkedHashMap<>();

	private void collectDifferences(DrtRequest request, DetourTimeInfo matrixTimeInfo, DetourTimeInfo networkTimeInfo) {
		addRelativeDiff(matrixTimeInfo.pickupDetourInfo.pickupTimeLoss, networkTimeInfo.pickupDetourInfo.pickupTimeLoss,
				networkTimeInfo.pickupDetourInfo.departureTime, pickupTimeLossStats);
		addRelativeDiff(matrixTimeInfo.dropoffDetourInfo.dropoffTimeLoss,
				networkTimeInfo.dropoffDetourInfo.dropoffTimeLoss, networkTimeInfo.dropoffDetourInfo.arrivalTime,
				dropoffTimeLossStats);
	}

	private void addRelativeDiff(double matrixVal, double networkVal, double eventTime,
			Map<Integer, SummaryStatistics> summaryStatsMap) {
		int timeBin = (int)(eventTime / 3600);
		var stats = summaryStatsMap.computeIfAbsent(timeBin, b -> new SummaryStatistics());
		if (matrixVal == 0 && networkVal == 0) {
			stats.addValue(0);
		} else if (networkVal != 0) {
			stats.addValue((matrixVal - networkVal) / networkVal);
		}
	}

	@Override
	public void notifyMobsimBeforeCleanup(@SuppressWarnings("rawtypes") MobsimBeforeCleanupEvent event) {
		String filename = matsimServices.getControlerIO()
				.getIterationFilename(matsimServices.getIterationNumber(),
						mode + "_selective_insertion_detour_time_estimation_errors.csv");
		try (CSVWriter writer = new CSVWriter(Files.newBufferedWriter(Paths.get(filename)), ';', '"', '"', "\n");) {
			writer.writeNext(new String[] { "type", "hour", "count", "mean", "std_dev", "min", "max" }, false);
			pickupTimeLossStats.forEach((hour, stats) -> printStats(writer, "pickup", hour, stats));
			dropoffTimeLossStats.forEach((hour, stats) -> printStats(writer, "dropoff", hour, stats));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void printStats(CSVWriter writer, String type, int hour, SummaryStatistics stats) {
		writer.writeNext(new String[] { type, hour + "", stats.getN() + "", stats.getMean() + "",
				stats.getStandardDeviation() + "", stats.getMin() + "", stats.getMax() + "" }, false);
	}
}
