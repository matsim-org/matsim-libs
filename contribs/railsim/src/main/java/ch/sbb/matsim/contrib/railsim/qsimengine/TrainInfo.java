/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
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
 * *********************************************************************** */

package ch.sbb.matsim.contrib.railsim.qsimengine;

import ch.sbb.matsim.contrib.railsim.RailsimUtils;
import ch.sbb.matsim.contrib.railsim.config.RailsimConfigGroup;
import org.matsim.api.core.v01.Id;
import org.matsim.vehicles.VehicleType;

/**
 * Non-mutable static information for a single train.
 */
public record TrainInfo(
	Id<VehicleType> id,
	double length,
	double maxVelocity,
	double acceleration,
	double deceleration,
	double maxDeceleration
) {

	TrainInfo(VehicleType vehicle, RailsimConfigGroup config) {
		this(
			vehicle.getId(),
			vehicle.getLength(),
			vehicle.getMaximumVelocity(),
			RailsimUtils.getTrainAcceleration(vehicle, config),
			RailsimUtils.getTrainDeceleration(vehicle, config),
			RailsimUtils.getTrainDeceleration(vehicle, config)
		);
	}

	void checkConsistency() {
		if (!Double.isFinite(maxVelocity) || maxVelocity <= 0)
			throw new IllegalArgumentException("Train of type " + id + " does not have a finite maximumVelocity.");

		if (!Double.isFinite(acceleration) || acceleration <= 0)
			throw new IllegalArgumentException("Train of type " + id + " does not have a finite and positive acceleration.");

		if (!Double.isFinite(deceleration) || deceleration <= 0)
			throw new IllegalArgumentException("Train of type " + id + " does not have a finite and positive deceleration.");
	}
}
