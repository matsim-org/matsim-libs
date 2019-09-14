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

package org.matsim.contrib.edrt.optimizer;

import org.matsim.contrib.drt.optimizer.DefaultDrtOptimizer;
import org.matsim.contrib.drt.optimizer.DrtOptimizer;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
import org.matsim.contrib.dvrp.optimizer.Request;
import org.matsim.contrib.dvrp.schedule.Schedule;
import org.matsim.contrib.dvrp.schedule.Schedule.ScheduleStatus;
import org.matsim.contrib.edrt.scheduler.EmptyVehicleChargingScheduler;
import org.matsim.core.mobsim.framework.events.MobsimBeforeSimStepEvent;

import com.google.common.base.Verify;

/**
 * @author michalm
 */
public class EDrtOptimizer implements DrtOptimizer {
	private final DefaultDrtOptimizer optimizer;
	private final EmptyVehicleChargingScheduler chargingScheduler;

	public EDrtOptimizer(DrtConfigGroup drtConfigGroup, DefaultDrtOptimizer optimizer,
			EmptyVehicleChargingScheduler chargingScheduler) {
		Verify.verify(drtConfigGroup.getIdleVehiclesReturnToDepots(),
				"DrtConfigGroup(mode=%s).idleVehiclesReturnToDepots is false."
						+ " Recharging is possible only if idle vehicles return to depots", drtConfigGroup.getMode());
		this.optimizer = optimizer;
		this.chargingScheduler = chargingScheduler;
	}

	@Override
	public void nextTask(DvrpVehicle vehicle) {
		optimizer.nextTask(vehicle);

		// if starting a STAY at depot then start charging
		Schedule schedule = vehicle.getSchedule();
		if (schedule.getStatus() == ScheduleStatus.STARTED // only active vehicles
				&& schedule.getCurrentTask().getTaskIdx() == schedule.getTaskCount() - 1) {
			//XXX if all vehicles are at depots at the very beginning, they will all start charging (incl. queueing for charging)
			// since charging is uninterruptible, all not fully charged vehicles (could be the whole fleet) would get frozen
			// to prevent this -- do not allow opportunistic charging when switching to the first (STAY) task
			if (schedule.getTasks().size() > 1) {
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
}
