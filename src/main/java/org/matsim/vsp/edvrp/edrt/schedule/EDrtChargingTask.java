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

package org.matsim.vsp.edvrp.edrt.schedule;

import org.matsim.contrib.drt.schedule.DrtTask;
import org.matsim.contrib.ev.ev.data.Charger;
import org.matsim.contrib.ev.ev.data.ElectricVehicle;
import org.matsim.contrib.ev.ev.dvrp.ChargingTaskImpl;

public class EDrtChargingTask extends ChargingTaskImpl implements DrtTask {
	public EDrtChargingTask(double beginTime, double endTime, Charger charger, ElectricVehicle ev, double totalEnergy) {
		super(beginTime, endTime, charger, ev, totalEnergy);
	}

	@Override
	public DrtTaskType getDrtTaskType() {
		return DrtTaskType.STAY;
	}
}
