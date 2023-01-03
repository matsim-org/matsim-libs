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

import org.matsim.api.core.v01.Id;
import org.matsim.vehicles.Vehicle;

import com.google.common.collect.ImmutableMap;

/**
 * Contains all ElectricVehicles generated for a given iteration. Its lifespan is limited to a single QSim simulation.
 * <p>
 * Fleet (ond the contained ElectricVehicles) are created from ElectricFleetSpecification (and the contained ElectricVehicleSpecifications)
 *
 * @author michalm
 */
public interface ElectricFleet {
	ImmutableMap<Id<Vehicle>, ElectricVehicle> getElectricVehicles();
}
