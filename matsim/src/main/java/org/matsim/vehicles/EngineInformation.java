/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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

package org.matsim.vehicles;

/**
 * @author dgrether
 */
public interface EngineInformation {

	public enum FuelType {diesel, gasoline, electricity, biodiesel}

	public FuelType getFuelType();

	@Deprecated // use VehicleUtils... method instead
	public double getFuelConsumption();

	public void setFuelType(FuelType fueltype);

	@Deprecated // use VehicleUtils... method instead
	public void setFuelConsumption(double literPerMeter);

}
