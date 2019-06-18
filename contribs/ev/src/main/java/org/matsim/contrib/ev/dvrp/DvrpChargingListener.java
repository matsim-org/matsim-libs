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

import org.matsim.contrib.ev.charging.ChargingListener;
import org.matsim.contrib.ev.fleet.ElectricVehicle;

/**
 * @author michalm
 */
public class DvrpChargingListener implements ChargingListener {
	private final ChargingActivity chargingActivity;

	public DvrpChargingListener(ChargingActivity chargingActivity) {
		this.chargingActivity = chargingActivity;
	}

	@Override
	public void notifyVehicleQueued(ElectricVehicle ev, double now) {
		chargingActivity.vehicleQueued(now);
	}

	@Override
	public void notifyChargingStarted(ElectricVehicle ev, double now) {
		chargingActivity.chargingStarted(now);
	}

	@Override
	public void notifyChargingEnded(ElectricVehicle ev, double now) {
		chargingActivity.chargingEnded(now);
	}
}
