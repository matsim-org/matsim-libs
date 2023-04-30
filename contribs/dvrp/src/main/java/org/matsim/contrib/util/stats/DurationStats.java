/*
 * *********************************************************************** *
 * project: org.matsim.*
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
 * *********************************************************************** *
 */

package org.matsim.contrib.util.stats;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.summingDouble;

import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.stream.Stream;

import org.matsim.contrib.dvrp.analysis.ExecutedTask;
import org.matsim.contrib.dvrp.schedule.Task;

import com.google.common.base.Preconditions;

/**
 * @author Michal Maciejewski (michalm)
 */
public class DurationStats {
	public record State<V>(V value, double beginTime, double endTime) {
		public State(V value, double beginTime, double endTime) {
			Preconditions.checkArgument(beginTime <= endTime);
			this.value = Preconditions.checkNotNull(value);
			this.beginTime = beginTime;
			this.endTime = endTime;
		}
	}

	public static SortedMap<Integer, Map<Task.TaskType, Double>> taskDurationByTimeBinAndType(Stream<? extends ExecutedTask> tasks, int binSize) {
		return tasks.flatMap(task -> TimeBinSamples.taskSamples(task, binSize))
				.collect(groupingBy(TimeBinSample::timeBin, TreeMap::new,
						groupingBy(sample -> sample.value().taskType, summingDouble(sample -> taskDurationInTimeBin(sample, binSize)))));
	}

	public static <V> SortedMap<Integer, Map<V, Double>> stateDurationByTimeBinAndState(Stream<TimeBinSample<State<V>>> stateSamples, int binSize) {
		return stateSamples.collect(groupingBy(TimeBinSample::timeBin, TreeMap::new,
				groupingBy(sample -> sample.value().value, summingDouble(sample -> stateDurationInTimeBin(sample, binSize)))));
	}

	private static double taskDurationInTimeBin(TimeBinSample<ExecutedTask> taskSample, int binSize) {
		return durationInTimeBin(taskSample.value().beginTime, taskSample.value().endTime, taskSample.timeBin(), binSize);
	}

	private static <V> double stateDurationInTimeBin(TimeBinSample<State<V>> stateSample, int binSize) {
		return durationInTimeBin(stateSample.value().beginTime, stateSample.value().endTime, stateSample.timeBin(), binSize);
	}

	private static double durationInTimeBin(double beginTime, double endTime, int timeBin, int binSize) {
		double from = Math.max(beginTime, timeBin * binSize);
		double to = Math.min(endTime, (timeBin + 1) * binSize);
		return to - from;
	}
}
