/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
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

package playground.michalm.ev.discharging;

import org.matsim.api.core.v01.network.Link;

import playground.michalm.ev.data.ElectricVehicle;

public class EnergyConsumptions {
	public static void consumeFixedDriveEnergy(ElectricVehicle ev, double rate, Link link) {
		ev.getBattery().discharge(rate * link.getLength());
	}

	public static void consumeFixedAuxEnergy(ElectricVehicle ev, double auxPower, double period) {
		ev.getBattery().discharge(auxPower * period);
	}
}
