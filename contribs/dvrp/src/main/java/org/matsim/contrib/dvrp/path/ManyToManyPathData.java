/* *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2018 by the members listed in the COPYING,        *
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

package org.matsim.contrib.dvrp.path;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.dvrp.path.OneToManyPathSearch.PathData;
import org.matsim.contrib.dvrp.util.TimeDiscretizer;
import org.matsim.contrib.util.ExecutorServiceWithResource;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;

import com.google.common.collect.ArrayTable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Table;

/**
 * @author michalm
 */
public class ManyToManyPathData {
	private static final Logger log = Logger.getLogger(ManyToManyPathData.class);

	private final Table<Id<Link>, Id<Link>, PathData>[] tables;
	private final TimeDiscretizer discretizer;

	public ManyToManyPathData(Network network, TravelTime travelTime, TravelDisutility travelDisutility,
			List<Link> links, TimeDiscretizer discretizer, int threads) {
		this.discretizer = discretizer;
		this.tables = createTables(links);
		updateTable(links, threads,
				() -> OneToManyPathSearch.createForwardSearch(network, travelTime, travelDisutility));
	}

	private Table<Id<Link>, Id<Link>, PathData>[] createTables(List<Link> links) {
		ImmutableList<Id<Link>> linkIds = links.stream().map(l -> l.getId()).collect(ImmutableList.toImmutableList());
		@SuppressWarnings("unchecked")
		Table<Id<Link>, Id<Link>, PathData>[] tables = new Table[discretizer.getIntervalCount()];
		for (int i = 0; i < tables.length; i++) {
			tables[i] = ArrayTable.create(linkIds, linkIds);
		}
		return tables;
	}

	private void updateTable(List<Link> links, int threads, Supplier<OneToManyPathSearch> oneToManyPathSearchProvider) {
		log.info("Matrix calculation started");
		ExecutorServiceWithResource<OneToManyPathSearch> executorService = new ExecutorServiceWithResource<>(
				IntStream.range(0, threads).mapToObj(i -> oneToManyPathSearchProvider.get())
						.collect(Collectors.toList()));

		executorService.submitRunnablesAndWait(//
				IntStream.range(0, tables.length).boxed() // in each table (i)
						.flatMap(i -> links.stream() // for each link (fromLink)
								.map(fromLink -> (search -> updateRow(search, fromLink, links, i)))));

		executorService.shutdown();
		log.info("Matrix calculation finished");
	}

	private void updateRow(OneToManyPathSearch search, Link fromLink, Collection<Link> toLinks, int timeIdx) {
		int startTime = timeIdx * discretizer.getTimeInterval();
		Map<Id<Link>, PathData> pathData = search.calcPathDataMap(fromLink, toLinks, startTime);
		for (Map.Entry<Id<Link>, PathData> e : pathData.entrySet()) {
			tables[timeIdx].put(fromLink.getId(), e.getKey(), e.getValue());
		}
	}

	public PathData getPathData(Id<Link> fromLink, Id<Link> toLink, double startTime) {
		return tables[discretizer.getIdx(startTime)].get(fromLink, toLink);
	}

	public Map<Id<Link>, PathData> getOutgoingPathData(Id<Link> fromLink, double startTime) {
		// to avoid delegation via Collections.unmodifiableMap(), one could change ArrayTable to ImmutableTable
		// (guava would likely choose DenseImmutableTable instead of SparseImmutableTable -- though no explicit choice)
		// same for getIncomingPathData
		return Collections.unmodifiableMap(tables[discretizer.getIdx(startTime)].row(fromLink));
	}

	public Map<Id<Link>, PathData> getIncomingPathData(Id<Link> toLink, double startTime) {
		return Collections.unmodifiableMap(tables[discretizer.getIdx(startTime)].column(toLink));
	}
}
