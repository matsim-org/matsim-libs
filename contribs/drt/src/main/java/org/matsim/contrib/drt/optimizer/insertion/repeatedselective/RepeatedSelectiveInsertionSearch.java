/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2023 by the members listed in the COPYING,        *
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

package org.matsim.contrib.drt.optimizer.insertion.repeatedselective;

import com.opencsv.CSVWriter;
import org.apache.commons.math3.stat.descriptive.SummaryStatistics;
import org.matsim.contrib.drt.optimizer.VehicleEntry;
import org.matsim.contrib.drt.optimizer.insertion.*;
import org.matsim.contrib.drt.optimizer.insertion.selective.SingleInsertionDetourPathCalculator;
import org.matsim.contrib.drt.passenger.DrtRequest;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.drt.stops.StopTimeCalculator;
import org.matsim.contrib.dvrp.path.OneToManyPathSearch;
import org.matsim.contrib.zone.skims.AdaptiveTravelTimeMatrix;
import org.matsim.contrib.zone.skims.TravelTimeMatrix;
import org.matsim.core.controler.MatsimServices;
import org.matsim.core.mobsim.framework.events.MobsimBeforeCleanupEvent;
import org.matsim.core.mobsim.framework.listeners.MobsimBeforeCleanupListener;
import org.matsim.core.router.util.LeastCostPathCalculator;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.matsim.contrib.drt.optimizer.insertion.InsertionCostCalculator.INFEASIBLE_SOLUTION_COST;
import static org.matsim.contrib.drt.optimizer.insertion.InsertionDetourTimeCalculator.DetourTimeInfo;

/**
 * @author steffenaxer
 */
final class RepeatedSelectiveInsertionSearch implements DrtInsertionSearch, MobsimBeforeCleanupListener {
	private final RepeatedSelectiveInsertionProvider insertionProvider;
	private final SingleInsertionDetourPathCalculator detourPathCalculator;
	private final InsertionDetourTimeCalculator detourTimeCalculator;
	private final InsertionCostCalculator insertionCostCalculator;
	private final MatsimServices matsimServices;
	private final String mode;
	private final TravelTimeMatrix travelTimeMatrix;
	private final AdaptiveTravelTimeMatrix adaptiveTravelTimeMatrix;
	private final RepeatedSelectiveInsertionSearchParams insertionSearchParams;

	public RepeatedSelectiveInsertionSearch(RepeatedSelectiveInsertionProvider insertionProvider,
											SingleInsertionDetourPathCalculator detourPathCalculator, InsertionCostCalculator insertionCostCalculator,
											DrtConfigGroup drtCfg, MatsimServices matsimServices, StopTimeCalculator stopTimeCalculator, TravelTimeMatrix travelTimeMatrix, AdaptiveTravelTimeMatrix adaptiveTravelTimeMatrix) {
		this.insertionSearchParams = (RepeatedSelectiveInsertionSearchParams) drtCfg.getDrtInsertionSearchParams();
		this.insertionProvider = insertionProvider;
		this.detourPathCalculator = detourPathCalculator;
		this.insertionCostCalculator = insertionCostCalculator;
		this.detourTimeCalculator = new InsertionDetourTimeCalculator(stopTimeCalculator, null);
		this.matsimServices = matsimServices;
		this.mode = drtCfg.getMode();
		this.travelTimeMatrix = travelTimeMatrix;
		this.adaptiveTravelTimeMatrix = adaptiveTravelTimeMatrix;
	}

	@Override
	public Optional<InsertionWithDetourData> findBestInsertion(DrtRequest drtRequest,
			Collection<VehicleEntry> vehicleEntries) {
	      List<InsertionWithDetourData> sortedInsertions = insertionProvider.getInsertions(drtRequest, vehicleEntries);
		if (sortedInsertions.isEmpty()) {
			return Optional.empty();
		}
        return validateInsertionWithPathCalculation(sortedInsertions, drtRequest, insertionSearchParams.retryInsertion);
	}

    Optional<InsertionWithDetourData> validateInsertionWithPathCalculation(List<InsertionWithDetourData> selectedInsertionList,
            DrtRequest drtRequest, int tryKBest) {
        int n = Math.min(selectedInsertionList.size(), tryKBest);
        for (int i = 0; i < n; i++) {
            var selectedInsertion = selectedInsertionList.get(i);
            var insertion = selectedInsertion.insertion;
            var insertionDetourData = detourPathCalculator.calculatePaths(drtRequest, insertion);
            var insertionWithDetourData = new InsertionWithDetourData(insertion, insertionDetourData,
                    detourTimeCalculator.calculateDetourTimeInfo(insertion, insertionDetourData, drtRequest));

            collectDifferences(drtRequest, selectedInsertion.detourTimeInfo, insertionWithDetourData.detourTimeInfo);

            double insertionCost = insertionCostCalculator.calculate(drtRequest, insertion,
                    insertionWithDetourData.detourTimeInfo);

            // For each realized routing, we update the adaptiveTravelTimeMatrix
			// The idea is to get a passively updated travel time estimation, without additional routing costs
            updateMatrix(drtRequest, travelTimeMatrix, adaptiveTravelTimeMatrix, insertionWithDetourData);

            if (insertionCost < INFEASIBLE_SOLUTION_COST) {
                return Optional.of(insertionWithDetourData);
            }
        }
        return Optional.empty();
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

	private void updateMatrix(DrtRequest request, TravelTimeMatrix travelTimeMatrix, AdaptiveTravelTimeMatrix updatableTravelTimeMatrix, InsertionWithDetourData insertionWithDetourData) {
		updateMatrix(request, travelTimeMatrix, updatableTravelTimeMatrix, insertionWithDetourData.detourData.detourToPickup);
		updateMatrix(request, travelTimeMatrix, updatableTravelTimeMatrix, insertionWithDetourData.detourData.detourFromPickup);
		updateMatrix(request, travelTimeMatrix, updatableTravelTimeMatrix, insertionWithDetourData.detourData.detourToDropoff);
		updateMatrix(request, travelTimeMatrix, updatableTravelTimeMatrix, insertionWithDetourData.detourData.detourFromDropoff);
	}

	private static void updateMatrix(DrtRequest request, TravelTimeMatrix travelTimeMatrix, AdaptiveTravelTimeMatrix updatableTravelTimeMatrix, OneToManyPathSearch.PathData pathData) {
		if(pathData!=null && pathData.getPath().links!=null)
		{
			LeastCostPathCalculator.Path path = pathData.getPath();
			double ttRoutedEstimate = pathData.getTravelTime();
			updatableTravelTimeMatrix.setTravelTime(path.getFromNode(), path.getToNode(), ttRoutedEstimate, request.getEarliestStartTime());
		}
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
