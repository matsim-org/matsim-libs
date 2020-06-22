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

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.summingDouble;

import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.stream.Stream;

import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
import org.matsim.contrib.dvrp.schedule.Task;

/**
 * @author Michal Maciejewski (michalm)
 */
public class DurationStats {
	public static final class State<V> {
		public final V value;
		public final double beginTime;
		public final double endTime;

		public State(V value, double beginTime, double endTime) {
			this.value = value;
			this.beginTime = beginTime;
			this.endTime = endTime;
		}
	}

	public static SortedMap<Integer, Map<Task.TaskType, Double>> taskDurationByTimeBinAndType(DvrpVehicle vehicle,
			int binSize) {
		return taskDurationByTimeBinAndType(vehicle.getSchedule().tasks(), binSize);
	}

	public static SortedMap<Integer, Map<Task.TaskType, Double>> taskDurationByTimeBinAndType(
			Stream<? extends Task> tasks, int binSize) {
		return tasks.flatMap(task -> TimeBinSamples.taskSamples(task, binSize))
				.collect(groupingBy(TimeBinSample::timeBin, TreeMap::new,
						groupingBy(sample -> sample.value.getTaskType(),
								summingDouble(sample -> taskDurationInTimeBin(sample, binSize)))));
	}

	public static <V> SortedMap<Integer, Map<V, Double>> stateDurationByTimeBinAndState(
			Stream<TimeBinSample<State<V>>> stateSamples, int binSize) {
		return stateSamples.collect(groupingBy(TimeBinSample::timeBin, TreeMap::new,
				groupingBy(sample -> sample.value.value,
						summingDouble(sample -> stateDurationInTimeBin(sample, binSize)))));
	}

	private static double taskDurationInTimeBin(TimeBinSample<Task> taskSample, int binSize) {
		Task task = taskSample.value;
		return durationInTimeBin(task.getBeginTime(), task.getEndTime(), taskSample.timeBin, binSize);
	}

	private static <V> double stateDurationInTimeBin(TimeBinSample<State<V>> stateSample, int binSize) {
		State<V> state = stateSample.value;
		return durationInTimeBin(state.beginTime, state.endTime, stateSample.timeBin, binSize);
	}

	private static double durationInTimeBin(double beginTime, double endTime, int timeBin, int binSize) {
		double from = Math.max(beginTime, timeBin * binSize);
		double to = Math.min(endTime, (timeBin + 1) * binSize);
		return to - from;
	}
}
