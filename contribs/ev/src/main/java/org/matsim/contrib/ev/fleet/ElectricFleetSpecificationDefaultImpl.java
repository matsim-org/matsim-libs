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

import org.matsim.api.core.v01.Id;
import org.matsim.vehicles.Vehicle;

/**
 * @author Michal Maciejewski (michalm)
 */
final class ElectricFleetSpecificationDefaultImpl implements ElectricFleetSpecification {
	private final Map<Id<Vehicle>, ElectricVehicleSpecification> specifications = new LinkedHashMap<>();

	@Override
	public Map<Id<Vehicle>, ElectricVehicleSpecification> getVehicleSpecifications() {
		return Collections.unmodifiableMap(specifications);
	}

	@Override
	public void addVehicleSpecification(ElectricVehicleSpecification specification) {
		specifications.put(specification.getId(), specification);
	}

	@Override
	public void clear() {
		specifications.clear();
	}
}

