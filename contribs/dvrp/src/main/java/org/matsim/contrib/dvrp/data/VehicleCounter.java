/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
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
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Queue;

/**
 * @author michalm
 */
public class VehicleCounter {
	private final Collection<? extends DvrpVehicle> vehicles;
	private final Queue<DvrpVehicle> waitingVehicles;
	private final Queue<DvrpVehicle> activeVehicles;

	public VehicleCounter(Collection<? extends DvrpVehicle> vehicles) {
		this.vehicles = vehicles;

		int queueCapacity = vehicles.size();
		this.waitingVehicles = new PriorityQueue<>(queueCapacity,
				Comparator.comparingDouble(DvrpVehicle::getServiceBeginTime));
		this.activeVehicles = new PriorityQueue<>(queueCapacity,
				Comparator.comparingDouble(DvrpVehicle::getServiceEndTime));
	}

	public List<Integer> countVehiclesOverTime(double timeStep) {
		List<Integer> vehicleCounts = new ArrayList<>();
		double currentTime = 0;
		waitingVehicles.addAll(vehicles);

		while (!waitingVehicles.isEmpty() || !activeVehicles.isEmpty()) {
			// move waiting->active
			while (!waitingVehicles.isEmpty()) {
				if (waitingVehicles.peek().getServiceBeginTime() > currentTime) {
					break;
				}

				DvrpVehicle newActiveVehicle = waitingVehicles.poll();
				activeVehicles.add(newActiveVehicle);
			}

			// remove from active
			while (!activeVehicles.isEmpty()) {
				if (activeVehicles.peek().getServiceEndTime() > currentTime) {
					break;
				}

				activeVehicles.poll();
			}

			vehicleCounts.add(activeVehicles.size());
			currentTime += timeStep;
		}

		return vehicleCounts;
	}
}
