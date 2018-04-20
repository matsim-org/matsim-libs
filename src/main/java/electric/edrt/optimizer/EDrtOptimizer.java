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

package electric.edrt.optimizer;

import javax.inject.Inject;

import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.drt.optimizer.DefaultDrtOptimizer;
import org.matsim.contrib.drt.optimizer.DrtOptimizer;
import org.matsim.contrib.dvrp.data.Request;
import org.matsim.contrib.dvrp.data.Vehicle;
import org.matsim.contrib.dvrp.schedule.Schedule;
import org.matsim.contrib.dvrp.schedule.Schedule.ScheduleStatus;
import org.matsim.core.mobsim.framework.MobsimTimer;
import org.matsim.core.mobsim.framework.events.MobsimBeforeSimStepEvent;

import electric.edrt.scheduler.EmptyVehicleChargingScheduler;


/**
 * @author michalm
 */
public class EDrtOptimizer implements DrtOptimizer {
	private final DefaultDrtOptimizer optimizer;
	private final EmptyVehicleChargingScheduler chargingScheduler;
	
	@Inject
	private MobsimTimer timer;

	@Inject
	public EDrtOptimizer(DefaultDrtOptimizer optimizer, EmptyVehicleChargingScheduler chargingScheduler) {
		this.optimizer = optimizer;
		this.chargingScheduler = chargingScheduler;
	}

	@Override
	public void nextTask(Vehicle vehicle) {
		optimizer.nextTask(vehicle);

		// if starting a STAY at depot then start charging
		Schedule schedule = vehicle.getSchedule();
		if (schedule.getStatus() == ScheduleStatus.STARTED // only active vehicles
				&& schedule.getCurrentTask().getTaskIdx() == schedule.getTaskCount() - 1) {
			if (timer.getTimeOfDay() > 21600) {
				chargingScheduler.chargeVehicle(vehicle);
			}
		}
	}

	@Override
	public void notifyMobsimBeforeSimStep(@SuppressWarnings("rawtypes") MobsimBeforeSimStepEvent e) {
		optimizer.notifyMobsimBeforeSimStep(e);
	}

	@Override
	public void requestSubmitted(Request request) {
		optimizer.requestSubmitted(request);
	}

	@Override
	public void vehicleEnteredNextLink(Vehicle vehicle, Link nextLink) {
		optimizer.vehicleEnteredNextLink(vehicle, nextLink);
	}
}
