/* *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2018 by the members listed in the COPYING,        *
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

package org.matsim.contrib.ev.dvrp;

import com.google.common.base.MoreObjects;
import com.google.common.base.Preconditions;
import org.matsim.contrib.dvrp.schedule.StayTask;
import org.matsim.contrib.ev.charging.ChargingWithAssignmentLogic;
import org.matsim.contrib.ev.fleet.ElectricVehicle;
import org.matsim.contrib.ev.infrastructure.Charger;

/**
 * @author michalm
 */
public class ChargingTaskImpl extends StayTask implements ChargingTask {
	private final ChargingWithAssignmentLogic chargingLogic;
	private final ElectricVehicle ev;
	private Double chargingStartedTime;
	private final double totalEnergy;

	public ChargingTaskImpl(TaskType taskType, double beginTime, double endTime, Charger charger, ElectricVehicle ev,
			double totalEnergy) {
		super(taskType, beginTime, endTime, charger.getLink());
		Preconditions.checkArgument(totalEnergy < 0, "Total energy consumption is not negative: %s", totalEnergy);

		this.chargingLogic = (ChargingWithAssignmentLogic)charger.getLogic();
		this.ev = ev;
		this.totalEnergy = totalEnergy;
	}

	@Override
	public double getTotalEnergy() {
		return totalEnergy;
	}

	@Override
	public ChargingWithAssignmentLogic getChargingLogic() {
		return chargingLogic;
	}

	@Override
	public ElectricVehicle getElectricVehicle() {
		return ev;
	}

	@Override
	public void setChargingStartedTime(double chargingStartedTime) {
		this.chargingStartedTime = chargingStartedTime;
	}

	@Override
	public double getChargingStartedTime() {
		return chargingStartedTime;
	}

	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this)
				.add("chargingLogic", chargingLogic)
				.add("ev", ev)
				.add("chargingStartedTime", chargingStartedTime)
				.add("totalEnergy", totalEnergy)
				.add("super", super.toString())
				.toString();
	}
}
