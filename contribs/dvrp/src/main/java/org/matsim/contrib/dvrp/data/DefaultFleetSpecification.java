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

package org.matsim.contrib.dvrp.data;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import org.matsim.api.core.v01.Id;

/**
 * @author Michal Maciejewski (michalm)
 */
public final class DefaultFleetSpecification implements FleetSpecification {
	private final Map<Id<Vehicle>, DvrpVehicleSpecification> specifications = new LinkedHashMap<>();

	@Override
	public Map<Id<Vehicle>, DvrpVehicleSpecification> getSpecifications() {
		return Collections.unmodifiableMap(specifications);
	}

	public void addSpecification(DvrpVehicleSpecification specification) {
		if (specifications.putIfAbsent(specification.getId(), specification) != null) {
			throw new RuntimeException(
					"A vehicle specification for vehicle id=" + specification.getId() + " already exists");
		}
	}

	public void modifySpecification(DvrpVehicleSpecification specification) {
		if (specifications.computeIfPresent(specification.getId(), (k, v) -> specification) != null) {
			throw new RuntimeException(
					"A vehicle specification for vehicle id=" + specification.getId() + " does not exist");
		}
	}

	public void removeSpecification(DvrpVehicleSpecification specification) {
		if (specifications.remove(Objects.requireNonNull(specification)) == null) {
			throw new RuntimeException(
					"A vehicle specification for vehicle id=" + specification.getId() + " does not exist");
		}
	}
}

