
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.matsim.contrib.drt.optimizer.VehicleEntry;
import org.matsim.contrib.drt.optimizer.insertion.*;
import org.matsim.contrib.drt.passenger.DrtRequest;
import org.matsim.contrib.drt.stops.StopTimeCalculator;

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
									StopTimeCalculator stopTimeCalculator) {
		this.insertionProvider = insertionProvider;
		this.detourPathCalculator = detourPathCalculator;
		this.detourTimeCalculator = new InsertionDetourTimeCalculator(stopTimeCalculator, null);
		this.bestInsertionFinder = new BestInsertionFinder(insertionCostCalculator);
	}

	@Override
	public Optional<InsertionWithDetourData> findBestInsertion(DrtRequest drtRequest,
															   Collection<VehicleEntry> vehicleEntries) {
		var insertions = insertionProvider.getInsertions(drtRequest, vehicleEntries);
		if (insertions.isEmpty()) {
			return Optional.empty();
		}

		var pathDataByVariant = calculatePathsPerVariant(detourPathCalculator, insertions);
		var best = bestInsertionFinder.findBestInsertion(drtRequest, insertions.stream().map(i -> {
			var insertionDetourData = pathDataByVariant.get(i.getVariant().index()).createInsertionDetourData(i);
			return new InsertionWithDetourData(i, insertionDetourData,
				detourTimeCalculator.calculateDetourTimeInfo(i, insertionDetourData, i.getRequest()));
		}));
		int lastCalculatedVariant = pathDataByVariant.keySet().stream().reduce((first, second) -> second).orElseThrow();
		return best.map(b -> withPathsOfItsVariant(detourPathCalculator, b, lastCalculatedVariant));
	}

	/**
	 * The path searches are reused for all variants, and (with lazy path creation) a {@link org.matsim.contrib.dvrp.path.OneToManyPathSearch.PathData} builds its path
	 * from the search tree only when the path is requested. After the searches for later variants, the paths of an
	 * earlier variant would therefore follow the tree of the last variant (wrong pickup/dropoff link), while the travel
	 * times (computed eagerly) are correct. If the chosen insertion belongs to another than the last calculated variant,
	 * its paths are calculated again, so that it can be scheduled. Travel times and detour times are kept.
	 */
	static InsertionWithDetourData withPathsOfItsVariant(MultiInsertionDetourPathCalculator detourPathCalculator,
			InsertionWithDetourData best, int lastCalculatedVariant) {
		if (best.insertion.getVariant().index() == lastCalculatedVariant) {
			return best;
		}
		var detourData = detourPathCalculator.calculatePaths(best.insertion.getVariant().request(),
				List.of(best.insertion)).createInsertionDetourData(best.insertion);
		return new InsertionWithDetourData(best.insertion, detourData, best.detourTimeInfo);
	}

	/**
	 * Paths are calculated per request variant, because variants differ in their pickup and dropoff links. The result
	 * is keyed by {@link RequestVariant#index()}, so it does not depend on the identity or equality of the variants'
	 * {@link DrtRequest} objects.
	 */
	static Map<Integer, DetourPathDataCache> calculatePathsPerVariant(
			MultiInsertionDetourPathCalculator detourPathCalculator, List<InsertionGenerator.Insertion> insertions) {
		Map<Integer, List<InsertionGenerator.Insertion>> insertionsByVariant = insertions.stream()
				.collect(Collectors.groupingBy(i -> i.getVariant().index(), LinkedHashMap::new, Collectors.toList()));
		// in the order of calculation (see withPathsOfItsVariant)
		Map<Integer, DetourPathDataCache> pathDataByVariant = new LinkedHashMap<>();
		insertionsByVariant.forEach((index, variantInsertions) -> pathDataByVariant.put(index,
				detourPathCalculator.calculatePaths(variantInsertions.get(0).getVariant().request(), variantInsertions)));
		return pathDataByVariant;
	}
}
