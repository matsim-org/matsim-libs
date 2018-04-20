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

package org.matsim.vsp.edvrp.etaxi;

import org.matsim.contrib.taxi.schedule.TaxiTask;
import org.matsim.vsp.ev.data.Charger;
import org.matsim.vsp.ev.data.ElectricVehicle;
import org.matsim.vsp.ev.dvrp.ChargingTaskImpl;

public class ETaxiChargingTask extends ChargingTaskImpl implements TaxiTask {
	public ETaxiChargingTask(double beginTime, double endTime, Charger charger, ElectricVehicle ev,
			double totalEnergy) {
		super(beginTime, endTime, charger, ev, totalEnergy);
	}

	@Override
	public TaxiTaskType getTaxiTaskType() {
		return TaxiTaskType.STAY;
	}
}
