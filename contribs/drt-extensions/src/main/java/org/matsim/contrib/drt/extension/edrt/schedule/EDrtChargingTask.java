/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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

package org.matsim.contrib.drt.extension.edrt.schedule;

import org.matsim.contrib.drt.schedule.DrtTaskType;
import org.matsim.contrib.ev.charging.ChargingStrategy;
import org.matsim.contrib.ev.fleet.ElectricVehicle;
import org.matsim.contrib.ev.infrastructure.Charger;
import org.matsim.contrib.evrp.ChargingTaskImpl;

public class EDrtChargingTask extends ChargingTaskImpl {
	public static final DrtTaskType TYPE = new DrtTaskType("CHARGING");

	public EDrtChargingTask(double beginTime, double endTime, Charger charger, ElectricVehicle ev, double totalEnergy, ChargingStrategy chargingStrategy) {
		super(TYPE, beginTime, endTime, charger, ev, totalEnergy, chargingStrategy);
	}
}
