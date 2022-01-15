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

package org.matsim.contrib.drt.optimizer.insertion;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.matsim.contrib.drt.optimizer.VehicleEntry;
import org.matsim.contrib.drt.optimizer.insertion.InsertionGenerator.Insertion;
import org.matsim.contrib.drt.passenger.DrtRequest;
import org.matsim.contrib.dvrp.path.OneToManyPathSearch.PathData;

import com.google.common.annotations.VisibleForTesting;

/**
 * @author michalm
 */
public final class DefaultDrtInsertionSearch implements DrtInsertionSearch {
	public interface InsertionProvider {
		List<Insertion> getInsertions(DrtRequest drtRequest, Collection<VehicleEntry> vehicleEntries);
	}

	private final InsertionProvider insertionProvider;
	private final DetourPathCalculator detourPathCalculator;
	private final InsertionDetourTimeCalculator<PathData> detourTimeCalculator;
	private final BestInsertionFinder bestInsertionFinder;

	public DefaultDrtInsertionSearch(InsertionProvider insertionProvider, DetourPathCalculator detourPathCalculator,
			InsertionCostCalculator insertionCostCalculator, double stopDuration) {
		this(insertionProvider, detourPathCalculator, new BestInsertionFinder(insertionCostCalculator),
				new InsertionDetourTimeCalculator<>(stopDuration, PathData::getTravelTime, null));
	}

	@VisibleForTesting
	DefaultDrtInsertionSearch(InsertionProvider insertionProvider, DetourPathCalculator detourPathCalculator,
			BestInsertionFinder bestInsertionFinder, InsertionDetourTimeCalculator<PathData> detourTimeCalculator) {
		this.insertionProvider = insertionProvider;
		this.detourPathCalculator = detourPathCalculator;
		this.detourTimeCalculator = detourTimeCalculator;
		this.bestInsertionFinder = bestInsertionFinder;
	}

	@Override
	public Optional<InsertionWithDetourData> findBestInsertion(DrtRequest drtRequest,
			Collection<VehicleEntry> vehicleEntries) {
		var insertions = insertionProvider.getInsertions(drtRequest, vehicleEntries);
		if (insertions.isEmpty()) {
			return Optional.empty();
		}

		DetourPathDataCache pathData = detourPathCalculator.calculatePaths(drtRequest, insertions);
		return bestInsertionFinder.findBestInsertion(drtRequest, insertions.stream().map(i -> {
			var insertionDetourData = pathData.createInsertionDetourData(i);
			return new InsertionWithDetourData(i, insertionDetourData,
					detourTimeCalculator.calculateDetourTimeInfo(i, insertionDetourData));
		}));
	}
}
