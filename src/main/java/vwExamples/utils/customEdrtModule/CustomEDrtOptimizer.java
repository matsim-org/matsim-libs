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

package vwExamples.utils.customEdrtModule;

import org.matsim.contrib.drt.optimizer.DefaultDrtOptimizer;
import org.matsim.contrib.drt.optimizer.DrtOptimizer;
import org.matsim.contrib.dvrp.optimizer.Request;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
import org.matsim.contrib.dvrp.fleet.Fleet;
import org.matsim.contrib.dvrp.schedule.Schedule;
import org.matsim.contrib.dvrp.schedule.Schedule.ScheduleStatus;
import org.matsim.contrib.edrt.scheduler.EmptyVehicleChargingScheduler;
import org.matsim.contrib.ev.data.Battery;
import org.matsim.contrib.ev.data.ElectricVehicle;
import org.matsim.contrib.ev.dvrp.EvDvrpVehicle;
import org.matsim.core.mobsim.framework.MobsimTimer;
import org.matsim.core.mobsim.framework.events.MobsimBeforeSimStepEvent;

/**
 * @author axer
 */
public class CustomEDrtOptimizer implements DrtOptimizer {
	private final DefaultDrtOptimizer optimizer;
	private final EmptyVehicleChargingScheduler chargingScheduler;
	private final MobsimTimer timer;

	public CustomEDrtOptimizer(DefaultDrtOptimizer optimizer, EmptyVehicleChargingScheduler chargingScheduler,
			MobsimTimer timer) {
		this.optimizer = optimizer;
		this.chargingScheduler = chargingScheduler;
		this.timer = timer;
	}

	@Override
	public void nextTask(DvrpVehicle vehicle) {
		optimizer.nextTask(vehicle);

		// if starting a STAY at depot then start charging
		Schedule schedule = vehicle.getSchedule();
		if (schedule.getStatus() == ScheduleStatus.STARTED // only active vehicles
				&& schedule.getCurrentTask().getTaskIdx() == schedule.getTaskCount() - 1) {
			//No time constraint for charging
			//But minSOC for charging should be considered
			
			ElectricVehicle ev = ((EvDvrpVehicle)vehicle).getElectricVehicle();
			Battery b = ev.getBattery();
			double relativeSoc = b.getSoc() / b.getCapacity();
			if(relativeSoc<0.5)
			{
				//System.out.println("Found vehicle with low SOC");
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