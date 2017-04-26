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

package playground.michalm.taxi.schedule;

import org.matsim.contrib.dvrp.schedule.StayTaskImpl;
import org.matsim.contrib.taxi.schedule.TaxiTask;

import playground.michalm.ev.data.Charger;
import playground.michalm.taxi.data.EvrpVehicle.Ev;
import playground.michalm.taxi.ev.ETaxiChargingLogic;

public class ETaxiChargingTask extends StayTaskImpl implements TaxiTask {
	private final Charger charger;
	private final ETaxiChargingLogic logic;
	private final Ev ev;
	private double chargingStartedTime;

	public ETaxiChargingTask(double beginTime, double endTime, Charger charger, Ev ev) {
		super(beginTime, endTime, charger.getLink());
		this.charger = charger;
		this.ev = ev;
		logic = (ETaxiChargingLogic)charger.getLogic();

		logic.addAssignedVehicle(ev);
	}

	public void removeFromChargerLogic() {
		logic.removeAssignedVehicle(ev);
	}

	public Charger getCharger() {
		return charger;
	}

	public ETaxiChargingLogic getLogic() {
		return logic;
	}

	public Ev getEv() {
		return ev;
	}

	public void setChargingStartedTime(double chargingStartedTime) {
		this.chargingStartedTime = chargingStartedTime;
	}

	public double getChargingStartedTime() {
		return chargingStartedTime;
	}

	@Override
	public TaxiTaskType getTaxiTaskType() {
		return TaxiTaskType.STAY;
	}

	@Override
	protected String commonToString() {
		return "[CHARGING]" + super.commonToString();
	}
}
