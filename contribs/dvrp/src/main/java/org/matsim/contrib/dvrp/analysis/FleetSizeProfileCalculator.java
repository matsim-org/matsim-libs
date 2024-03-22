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

import org.matsim.contrib.common.timeprofile.TimeDiscretizer;
import org.matsim.contrib.dvrp.fleet.DvrpVehicleSpecification;
import org.matsim.contrib.dvrp.fleet.FleetSpecification;
import org.matsim.contrib.dvrp.fleet.VehicleAddedEvent;
import org.matsim.contrib.dvrp.fleet.VehicleAddedEventHandler;
import org.matsim.contrib.dvrp.fleet.VehicleRemovedEvent;
import org.matsim.contrib.dvrp.fleet.VehicleRemovedEventHandler;
import org.matsim.core.config.groups.QSimConfigGroup;

/**
 * @author Sebastian Hörl (sebhoerl), IRT SystemX
 */
public class FleetSizeProfileCalculator implements VehicleAddedEventHandler, VehicleRemovedEventHandler {

	private final String dvrpMode;

	private final double analysisEndTime;
	private final TimeDiscretizer timeDiscretizer;

	private double[] activeCount;

	public FleetSizeProfileCalculator(String dvrpMode, FleetSpecification fleet, int timeInterval,
			QSimConfigGroup qsimConfig) {
		this.dvrpMode = dvrpMode;

		double qsimEndTime = qsimConfig.getEndTime().orElse(0);
		double maxServiceEndTime = fleet.getVehicleSpecifications().values().stream()
				.mapToDouble(DvrpVehicleSpecification::getServiceEndTime).max().orElse(0);
		analysisEndTime = Math.max(qsimEndTime, maxServiceEndTime);
		timeDiscretizer = new TimeDiscretizer((int) Math.ceil(analysisEndTime), timeInterval);

		activeCount = new double[timeDiscretizer.getIntervalCount()];
	}

	@Override
	public void handleEvent(VehicleAddedEvent event) {
		if (event.getDvrpMode().equals(dvrpMode)) {
			increment(event.getTime());
		}
	}

	@Override
	public void handleEvent(VehicleRemovedEvent event) {
		if (event.getDvrpMode().equals(dvrpMode)) {
			decrement(event.getTime());
		}
	}

	private void increment(double startTime) {
		double timeInterval = timeDiscretizer.getTimeInterval();
		int fromIdx = timeDiscretizer.getIdx(startTime);

		for (int i = fromIdx; i < timeDiscretizer.getIntervalCount(); i++) {
			activeCount[i] += 1.0;
		}

		activeCount[fromIdx] -= (startTime % timeInterval) / timeInterval;
	}

	private void decrement(double startTime) {
		double timeInterval = timeDiscretizer.getTimeInterval();
		int fromIdx = timeDiscretizer.getIdx(startTime);

		for (int i = fromIdx; i < timeDiscretizer.getIntervalCount(); i++) {
			activeCount[i] -= 1.0;
		}

		activeCount[fromIdx] += (startTime % timeInterval) / timeInterval;
	}

	public double[] getActiveVehiclesProfile() {
		return activeCount;
	}

	public TimeDiscretizer getTimeDiscretizer() {
		return timeDiscretizer;
	}

	@Override
	public void reset(int iteration) {
		activeCount = new double[timeDiscretizer.getIntervalCount()];
	}
}
