/*
 * *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2022 by the members listed in the COPYING,        *
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
 * *********************************************************************** *
 */

package org.matsim.contrib.drt.optimizer.insertion.extensive;

import java.util.Collection;
import java.util.Optional;

import org.matsim.contrib.drt.optimizer.VehicleEntry;
import org.matsim.contrib.drt.optimizer.insertion.BestInsertionFinder;
import org.matsim.contrib.drt.optimizer.insertion.DrtInsertionSearch;
import org.matsim.contrib.drt.optimizer.insertion.InsertionCostCalculator;
import org.matsim.contrib.drt.optimizer.insertion.InsertionDetourTimeCalculator;
import org.matsim.contrib.drt.optimizer.insertion.InsertionWithDetourData;
import org.matsim.contrib.drt.passenger.DrtRequest;

/**
 * @author michalm
 */
final class ExtensiveInsertionSearch implements DrtInsertionSearch {
	private final ExtensiveInsertionProvider insertionProvider;
	private final MultiInsertionDetourPathCalculator detourPathCalculator;
	private final InsertionDetourTimeCalculator detourTimeCalculator;
	private final BestInsertionFinder bestInsertionFinder;

	public ExtensiveInsertionSearch(ExtensiveInsertionProvider insertionProvider,
			MultiInsertionDetourPathCalculator detourPathCalculator, InsertionCostCalculator insertionCostCalculator,
			double stopDuration) {
		this.insertionProvider = insertionProvider;
		this.detourPathCalculator = detourPathCalculator;
		this.detourTimeCalculator = new InsertionDetourTimeCalculator(stopDuration, null);
		this.bestInsertionFinder = new BestInsertionFinder(insertionCostCalculator);
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
