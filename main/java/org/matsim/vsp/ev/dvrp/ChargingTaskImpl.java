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

package org.matsim.vsp.ev.dvrp;

import org.matsim.contrib.dvrp.schedule.StayTaskImpl;
import org.matsim.vsp.ev.charging.ChargingWithQueueingAndAssignmentLogic;
import org.matsim.vsp.ev.data.Charger;
import org.matsim.vsp.ev.data.ElectricVehicle;

/**
 * @author michalm
 */
public class ChargingTaskImpl extends StayTaskImpl implements ChargingTask {
	private final ChargingWithQueueingAndAssignmentLogic chargingLogic;
	private final ElectricVehicle ev;
	private double chargingStartedTime;
	private double totalEnergy;

	public ChargingTaskImpl(double beginTime, double endTime, Charger charger, ElectricVehicle ev) {
		super(beginTime, endTime, charger.getLink());
		this.chargingLogic = (ChargingWithQueueingAndAssignmentLogic)charger.getLogic();
		this.ev = ev;
		totalEnergy = -chargingLogic.getChargingStrategy().calcRemainingEnergyToCharge(ev);
	}

	@Override
	public double getTotalEnergy() {
		return totalEnergy;
	}

	@Override
	public ChargingWithQueueingAndAssignmentLogic getChargingLogic() {
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
	protected String commonToString() {
		return "[CHARGING]" + super.commonToString();
	}
}
