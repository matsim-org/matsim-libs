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

package playground.mrieser.core.sim.fakes;

import org.matsim.api.core.v01.Id;

import playground.mrieser.core.sim.api.DriverAgent;
import playground.mrieser.core.sim.api.SimVehicle;

public class FakeSimVehicle implements SimVehicle {

	private DriverAgent driver = null;
	private final Id id;

	public FakeSimVehicle(final Id id) {
		this.id = id;
	}

	@Override
	public DriverAgent getDriver() {
		return this.driver;
	}

	@Override
	public Id getId() {
		return this.id;
	}

	@Override
	public double getSizeInEquivalents() {
		return 1.0;
	}

	@Override
	public void setDriver(DriverAgent driver) {
		this.driver = driver;
	}

}
