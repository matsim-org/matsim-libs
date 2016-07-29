/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package org.matsim.contrib.transEnergySim.vehicles.api;

import org.matsim.api.core.v01.Identifiable;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.transEnergySim.agents.VehicleAgent;
import org.matsim.contrib.transEnergySim.vehicles.energyConsumption.EnergyConsumptionModel;

public interface Vehicle extends Identifiable<Vehicle>{

	public double updateEnergyUse(Link link, double averageSpeedDrivenInMetersPerSecond);
	
	public double updateEnergyUse(double drivenDistanceInMeters, double maxSpeedOnLink, double averageSpeedDriven);
	
	public void reset();

	public VehicleAgent getVehicleAgent();
}
