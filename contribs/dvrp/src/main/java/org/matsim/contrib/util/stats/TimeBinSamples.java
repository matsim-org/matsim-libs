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

import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.matsim.contrib.dvrp.analysis.ExecutedTask;
import org.matsim.contrib.util.stats.DurationStats.State;

/**
 * @author Michal Maciejewski (michalm)
 */
public class TimeBinSamples {

	public static Stream<TimeBinSample<ExecutedTask>> taskSamples(ExecutedTask task, int binSize) {
		return samples(task, task.beginTime, task.endTime, binSize);
	}

	public static <V> Stream<TimeBinSample<State<V>>> stateSamples(State<V> state, int binSize) {
		return samples(state, state.beginTime, state.endTime, binSize);
	}

	public static <V> Stream<TimeBinSample<V>> samples(V value, double from, double to, int binSize) {
		if (from == to) {
			return Stream.empty();
		}

		//beginTime is inclusive
		int fromTimeBin = (int)(from / binSize);

		//endTime is exclusive -- special handling needed
		double toTimeBinDouble = to / binSize;
		int toTimeBin = (int)toTimeBinDouble;

		//if endTime == toTimeBin * binSize ==> the task ends with the end of the previous time bin
		if (toTimeBinDouble == toTimeBin) {
			toTimeBin--;
		}
		return IntStream.rangeClosed(fromTimeBin, toTimeBin).mapToObj(t -> new TimeBinSample<>(t, value));
	}
}
