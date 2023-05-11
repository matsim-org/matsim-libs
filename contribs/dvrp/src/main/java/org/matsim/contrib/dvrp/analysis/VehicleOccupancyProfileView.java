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
package org.matsim.contrib.dvrp.analysis;

import java.awt.Paint;
import java.util.Comparator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Stream;

import org.apache.commons.lang3.tuple.Pair;
import org.matsim.contrib.common.timeprofile.ProfileWriter;
import org.matsim.contrib.dvrp.schedule.Task;

import com.google.common.collect.ImmutableMap;

import one.util.streamex.EntryStream;

/**
 * @author michalm (Michal Maciejewski)
 */
public class VehicleOccupancyProfileView implements ProfileWriter.ProfileView {

	private final VehicleOccupancyProfileCalculator calculator;
	private final Comparator<Task.TaskType> nonPassengerTaskTypeComparator;
	private final Map<String, Paint> seriesPaints;

	public VehicleOccupancyProfileView(VehicleOccupancyProfileCalculator calculator, Comparator<Task.TaskType> nonPassengerTaskTypeComparator,
			Map<Task.TaskType, Paint> taskTypePaints) {
		this.calculator = calculator;
		this.nonPassengerTaskTypeComparator = nonPassengerTaskTypeComparator;
		seriesPaints = EntryStream.of(taskTypePaints).mapKeys(Task.TaskType::name).toMap();
	}

	@Override
	public ImmutableMap<String, double[]> profiles() {
		// stream tasks which are not related to passenger (unoccupied vehicle)
		var nonPassengerTaskProfiles = calculator.getNonPassengerServingTaskProfiles()
				.entrySet()
				.stream()
				.sorted(Entry.comparingByKey(nonPassengerTaskTypeComparator))
				.map(e -> Pair.of(e.getKey().name(), e.getValue()));

		// occupancy profiles (for tasks related to passengers)
		var occupancyProfiles = EntryStream.of(calculator.getVehicleOccupancyProfiles()).map(e -> Pair.of(e.getKey() + " pax", e.getValue()));

		return Stream.concat(nonPassengerTaskProfiles, occupancyProfiles).collect(ImmutableMap.toImmutableMap(Entry::getKey, Entry::getValue));
	}

	@Override
	public Map<String, Paint> seriesPaints() {
		return seriesPaints;
	}

	@Override
	public double[] times() {
		return calculator.getTimeDiscretizer().getTimes();
	}
}
