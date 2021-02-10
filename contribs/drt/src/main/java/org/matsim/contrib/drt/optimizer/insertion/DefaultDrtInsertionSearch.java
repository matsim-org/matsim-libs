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
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.dvrp.path.OneToManyPathSearch.PathData;
import org.matsim.core.mobsim.framework.MobsimTimer;

import com.google.common.annotations.VisibleForTesting;

/**
 * @author michalm
 */
public class DefaultDrtInsertionSearch implements DrtInsertionSearch<PathData> {
	public interface InsertionProvider {
		List<Insertion> getInsertions(DrtRequest drtRequest, Collection<VehicleEntry> vehicleEntries);
	}

	private final InsertionProvider insertionProvider;
	private final DetourPathCalculator detourPathCalculator;
	private final BestInsertionFinder<PathData> bestInsertionFinder;

	public DefaultDrtInsertionSearch(InsertionProvider insertionProvider, DetourPathCalculator detourPathCalculator,
			CostCalculationStrategy costCalculationStrategy, DrtConfigGroup drtCfg, MobsimTimer timer) {
		this(insertionProvider, detourPathCalculator, new BestInsertionFinder<>(
				new InsertionCostCalculator<>(drtCfg, timer, costCalculationStrategy, PathData::getTravelTime, null)));
	}

	@VisibleForTesting
	DefaultDrtInsertionSearch(InsertionProvider insertionProvider, DetourPathCalculator detourPathCalculator,
			BestInsertionFinder<PathData> bestInsertionFinder) {
		this.insertionProvider = insertionProvider;
		this.detourPathCalculator = detourPathCalculator;
		this.bestInsertionFinder = bestInsertionFinder;
	}

	@Override
	public Optional<InsertionWithDetourData<PathData>> findBestInsertion(DrtRequest drtRequest,
			Collection<VehicleEntry> vehicleEntries) {
		var insertions = insertionProvider.getInsertions(drtRequest, vehicleEntries);
		if (insertions.isEmpty()) {
			return Optional.empty();
		}

		DetourData<PathData> pathData = detourPathCalculator.calculatePaths(drtRequest, insertions);
		return bestInsertionFinder.findBestInsertion(drtRequest,
				insertions.stream().map(pathData::createInsertionWithDetourData));
	}
}
