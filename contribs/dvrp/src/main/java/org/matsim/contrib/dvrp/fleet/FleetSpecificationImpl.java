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

package org.matsim.contrib.dvrp.fleet;

import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.contrib.util.SpecificationContainer;

/**
 * @author Michal Maciejewski (michalm)
 */
public final class FleetSpecificationImpl implements FleetSpecification {
	private final SpecificationContainer<DvrpVehicle, DvrpVehicleSpecification> container = new SpecificationContainer<>();

	@Override
	public Map<Id<DvrpVehicle>, DvrpVehicleSpecification> getVehicleSpecifications() {
		return container.getSpecifications();
	}

	@Override
	public void addVehicleSpecification(DvrpVehicleSpecification specification) {
		container.addSpecification(specification);
	}

	@Override
	public void replaceVehicleSpecification(DvrpVehicleSpecification specification) {
		container.replaceSpecification(specification);
	}

	@Override
	public void removeVehicleSpecification(Id<DvrpVehicle> vehicleId) {
		container.removeSpecification(vehicleId);
	}
}

