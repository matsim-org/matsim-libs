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

import java.util.Collection;
import java.util.Optional;

import org.matsim.contrib.drt.optimizer.VehicleEntry;
import org.matsim.contrib.drt.optimizer.insertion.DrtInsertionSearch;
import org.matsim.contrib.drt.optimizer.insertion.InsertionCostCalculator;
import org.matsim.contrib.drt.optimizer.insertion.InsertionDetourTimeCalculator;
import org.matsim.contrib.drt.optimizer.insertion.InsertionWithDetourData;
import org.matsim.contrib.drt.passenger.DrtRequest;

import com.google.common.annotations.VisibleForTesting;

/**
 * @author michalm
 */
final class SelectiveInsertionSearch implements DrtInsertionSearch {
	private final SelectiveInsertionProvider insertionProvider;
	private final SingleInsertionDetourPathCalculator detourPathCalculator;
	private final InsertionDetourTimeCalculator detourTimeCalculator;
	private final InsertionCostCalculator insertionCostCalculator;

	public SelectiveInsertionSearch(SelectiveInsertionProvider insertionProvider,
			SingleInsertionDetourPathCalculator detourPathCalculator, InsertionCostCalculator insertionCostCalculator,
			double stopDuration) {
		this(insertionProvider, detourPathCalculator, insertionCostCalculator,
				new InsertionDetourTimeCalculator(stopDuration, null));
	}

	@VisibleForTesting
	SelectiveInsertionSearch(SelectiveInsertionProvider insertionProvider,
			SingleInsertionDetourPathCalculator detourPathCalculator, InsertionCostCalculator insertionCostCalculator,
			InsertionDetourTimeCalculator detourTimeCalculator) {
		this.insertionProvider = insertionProvider;
		this.detourPathCalculator = detourPathCalculator;
		this.detourTimeCalculator = detourTimeCalculator;
		this.insertionCostCalculator = insertionCostCalculator;
	}

	@Override
	public Optional<InsertionWithDetourData> findBestInsertion(DrtRequest drtRequest,
			Collection<VehicleEntry> vehicleEntries) {
		var optionalInsertion = insertionProvider.getInsertion(drtRequest, vehicleEntries);
		if (optionalInsertion.isEmpty()) {
			return Optional.empty();
		}

		var insertion = optionalInsertion.get();
		var insertionDetourData = detourPathCalculator.calculatePaths(drtRequest, insertion);
		var insertionWithDetourData = new InsertionWithDetourData(insertion, insertionDetourData,
				detourTimeCalculator.calculateDetourTimeInfo(insertion, insertionDetourData));
		double insertionCost = insertionCostCalculator.calculate(drtRequest, insertion,
				insertionWithDetourData.detourTimeInfo);

		return insertionCost >= INFEASIBLE_SOLUTION_COST ? Optional.empty() : Optional.of(insertionWithDetourData);
	}
}
