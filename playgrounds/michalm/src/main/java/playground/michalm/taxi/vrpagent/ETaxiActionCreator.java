/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
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

package playground.michalm.taxi.vrpagent;

import org.matsim.contrib.dvrp.data.Vehicle;
import org.matsim.contrib.dvrp.optimizer.VrpOptimizer;
import org.matsim.contrib.dvrp.passenger.PassengerEngine;
import org.matsim.contrib.dvrp.schedule.Task;
import org.matsim.contrib.dynagent.*;
import org.matsim.contrib.taxi.run.TaxiConfigGroup;
import org.matsim.contrib.taxi.vrpagent.TaxiActionCreator;
import org.matsim.core.mobsim.qsim.QSim;

import com.google.inject.Inject;

import playground.michalm.taxi.schedule.ETaxiChargingTask;

/**
 * @author michalm
 */
public class ETaxiActionCreator extends TaxiActionCreator {
	@Inject
	public ETaxiActionCreator(PassengerEngine passengerEngine, TaxiConfigGroup taxiCfg, VrpOptimizer optimizer,
			QSim qSim) {
		super(passengerEngine, taxiCfg, optimizer, qSim);
	}

	@Override
	public DynAction createAction(DynAgent dynAgent, Vehicle vehicle, double now) {
		Task task = vehicle.getSchedule().getCurrentTask();
		if (task instanceof ETaxiChargingTask) {
			return new ETaxiAtChargerActivity((ETaxiChargingTask)task);
		}

		return super.createAction(dynAgent, vehicle, now);
	}
}
