/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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

package playground.mrieser.core.sim.impl;

import org.matsim.vehicles.Vehicle;

import playground.mrieser.core.sim.api.DriverAgent;
import playground.mrieser.core.sim.api.SimVehicle;

/**
 * Implementation of SimVehicle which delegates most of the work to a {@link DriverAgent}.
 *
 * @author mrieser
 */
public class DefaultSimVehicle implements SimVehicle {

	private final Vehicle vehicle;
	private DriverAgent driver = null;

	public DefaultSimVehicle(final Vehicle vehicle) {
		this.vehicle = vehicle;
	}

	@Override
	public Vehicle getVehicle() {
		return this.vehicle;
	}

	public DriverAgent getDriver() {
		return this.driver;
	}

	public void setDriver(DriverAgent driver) {
		this.driver = driver;
	}

	@Override
	public double getSizeInEquivalents() {
		return 1.0;
	}

}
