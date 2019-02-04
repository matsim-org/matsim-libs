/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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

package org.matsim.contrib.dvrp.data;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Queue;

import org.matsim.contrib.util.random.RandomUtils;
import org.matsim.contrib.util.random.UniformRandom;

/**
 * @author michalm
 */
public class VehicleGenerator {
	public interface VehicleCreator {
		DvrpVehicleSpecification createVehicleSpecification(double t0, double t1);
	}

	private final UniformRandom uniform = RandomUtils.getGlobalUniform();
	private final List<DvrpVehicleSpecification> vehicles = new ArrayList<>();

	private final double minWorkTime;
	private final double maxWorkTime;
	private final VehicleCreator vehicleCreator;

	private Queue<DvrpVehicleSpecification> activeVehicles;
	private double previousTime;
	private double currentTime;

	public VehicleGenerator(double minWorkTime, double maxWorkTime, VehicleCreator vehicleCreator) {
		this.minWorkTime = minWorkTime;
		this.maxWorkTime = maxWorkTime;
		this.vehicleCreator = vehicleCreator;
	}

	public void generateVehicles(double[] vehicleCounts, double startTime, double periodDuration) {
		if (periodDuration > minWorkTime) {
			throw new IllegalArgumentException();
		}

		activeVehicles = new PriorityQueue<>((int)Arrays.stream(vehicleCounts).max().getAsDouble() + 1,
				Comparator.comparingDouble(DvrpVehicleSpecification::getServiceEndTime));

		// only iteration 0; in order to have zero vehicles before startTime
		previousTime = startTime;
		currentTime = startTime;

		for (int i = 0; i < vehicleCounts.length; i++) {
			removeVehiclesOnT1();
			reachExpectedVehicleCount(vehicleCounts[i]);
			previousTime = currentTime;
			currentTime += periodDuration;
		}

		// get down to 0 after the last vehicle count
		currentTime = previousTime;
		reachExpectedVehicleCount(0);

		vehicles.sort(Comparator.comparing(DvrpVehicleSpecification::getId));//or startTime??
	}

	// reach the expected vehicle count at currentTime
	private void reachExpectedVehicleCount(double expectedVehicleCount) {
		int vehsToAdd = calculateNumberOfVehiclesToAdd(expectedVehicleCount);
		if (vehsToAdd > 0) {
			addVehicles(vehsToAdd);
		} else {
			removeVehiclesBeforeT1(-vehsToAdd);
		}
	}

	private void removeVehiclesOnT1() {
		while (!activeVehicles.isEmpty()) {
			if (activeVehicles.peek().getServiceEndTime() >= currentTime) {
				return;
			}

			vehicles.add(activeVehicles.poll());
		}
	}

	private int calculateNumberOfVehiclesToAdd(double expectedVehicleCount) {
		if (expectedVehicleCount < 0) {
			throw new IllegalArgumentException();
		}

		return (int)uniform.floorOrCeil(expectedVehicleCount) - activeVehicles.size();
	}

	private void addVehicles(int count) {
		for (int i = 0; i < count; i++) {
			double t0 = Math.floor(uniform.nextDouble(previousTime, currentTime));
			double workTime = Math.round(uniform.nextDouble(minWorkTime, maxWorkTime));
			DvrpVehicleSpecification specification = vehicleCreator.createVehicleSpecification(t0, t0 + workTime);
			activeVehicles.add(specification);
		}
	}

	private void removeVehiclesBeforeT1(int count) {
		for (int i = 0; i < count; i++) {
			DvrpVehicleSpecification veh = activeVehicles.poll();
			double t1 = Math.floor(uniform.nextDouble(previousTime, currentTime));
			vehicles.add(ImmutableDvrpVehicleSpecification.newBuilder(veh).serviceEndTime(t1).build());
		}
	}

	public List<DvrpVehicleSpecification> getVehicleSpecifications() {
		return vehicles;
	}
}
