/*
 * *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2020 by the members listed in the COPYING,        *
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

package org.matsim.contrib.util.stats;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.matsim.contrib.util.stats.DurationStats.stateDurationByTimeBinAndState;

import java.util.Map;
import java.util.SortedMap;
import java.util.stream.Stream;

import org.junit.Test;
import org.matsim.contrib.util.stats.DurationStats.State;

/**
 * @author Michal Maciejewski (michalm)
 */
public class DurationStatsTest {
	@Test
	public void totalStateDurationByTimeBinAndType_empty() {
		SortedMap<Integer, Map<String, Double>> durations = stateDurationByTimeBinAndState(Stream.of(), 100);
		assertThat(durations).isEmpty();
	}

	@Test
	public void totalStateDurationByTimeBinAndType_oneType_manyBins() {
		SortedMap<Integer, Map<String, Double>> durations = stateDurationByTimeBinAndState(
				Stream.of(sample(0, "A", 10, 300), sample(1, "A", 10, 300), sample(2, "A", 10, 300)), 100);
		assertThat(durations).containsExactly(entry(0, Map.of("A", 90.)), entry(1, Map.of("A", 100.)),
				entry(2, Map.of("A", 100.)));
	}

	@Test
	public void totalStateDurationByTimeBinAndType_manyTypes_oneBin() {
		SortedMap<Integer, Map<String, Double>> durations = stateDurationByTimeBinAndState(
				Stream.of(sample(1, "A", 10, 300), sample(1, "B", 150, 300), sample(1, "C", 100, 120)), 100);
		assertThat(durations).containsExactly(entry(1, Map.of("A", 100., "B", 50., "C", 20.)));
	}

	private TimeBinSample<State<String>> sample(int timeBin, String value, double beginTime, double endTime) {
		return new TimeBinSample<>(timeBin, new State<>(value, beginTime, endTime));
	}
}
