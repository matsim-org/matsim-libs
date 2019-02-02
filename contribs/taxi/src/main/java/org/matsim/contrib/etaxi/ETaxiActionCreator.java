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

package org.matsim.contrib.etaxi;

import org.matsim.contrib.dvrp.data.DvrpVehicle;
import org.matsim.contrib.dvrp.schedule.Task;
import org.matsim.contrib.dvrp.vrpagent.VrpAgentLogic;
import org.matsim.contrib.dynagent.DynAction;
import org.matsim.contrib.dynagent.DynAgent;
import org.matsim.contrib.ev.dvrp.ChargingActivity;
import org.matsim.contrib.taxi.vrpagent.TaxiActionCreator;

import com.google.inject.Inject;

/**
 * @author michalm
 */
public class ETaxiActionCreator implements VrpAgentLogic.DynActionCreator {
	private final TaxiActionCreator taxiActionCreator;

	@Inject
	public ETaxiActionCreator(TaxiActionCreator taxiActionCreator) {
		this.taxiActionCreator = taxiActionCreator;
	}

	@Override
	public DynAction createAction(DynAgent dynAgent, DvrpVehicle vehicle, double now) {
		Task task = vehicle.getSchedule().getCurrentTask();
		return task instanceof ETaxiChargingTask //
				? new ChargingActivity((ETaxiChargingTask)task) //
				: taxiActionCreator.createAction(dynAgent, vehicle, now);
	}
}
