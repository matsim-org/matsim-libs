/*
 * *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2019 by the members listed in the COPYING,        *
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

package org.matsim.contrib.ev.fleet;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import org.matsim.api.core.v01.Id;

/**
 * @author Michal Maciejewski (michalm)
 */
public final class ElectricFleetSpecificationImpl implements ElectricFleetSpecification {
	private final Map<Id<ElectricVehicle>, ElectricVehicleSpecification> specifications = new LinkedHashMap<>();

	@Override
	public Map<Id<ElectricVehicle>, ElectricVehicleSpecification> getVehicleSpecifications() {
		return Collections.unmodifiableMap(specifications);
	}

	@Override
	public void addVehicleSpecification(ElectricVehicleSpecification specification) {
		if (specifications.putIfAbsent(specification.getId(), specification) != null) {
			throw new RuntimeException(
					"A vehicle specification for vehicle id=" + specification.getId() + " already exists");
		}
	}

	@Override
	public void replaceVehicleSpecification(ElectricVehicleSpecification specification) {
		if (specifications.computeIfPresent(specification.getId(), (k, v) -> specification) == null) {
			throw new RuntimeException(
					"A vehicle specification for vehicle id=" + specification.getId() + " does not exist");
		}
	}

	@Override
	public void removeVehicleSpecification(Id<ElectricVehicle> vehicleId) {
		if (specifications.remove(Objects.requireNonNull(vehicleId)) == null) {
			throw new RuntimeException("A vehicle specification for vehicle id=" + vehicleId + " does not exist");
		}
	}
}

