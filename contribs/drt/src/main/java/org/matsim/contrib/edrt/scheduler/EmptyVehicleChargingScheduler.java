/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2017 by the members listed in the COPYING,        *
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

package org.matsim.contrib.edrt.scheduler;

import java.util.Map;
import java.util.stream.Collectors;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.drt.schedule.DrtStayTask;
import org.matsim.contrib.drt.schedule.DrtTaskFactory;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
import org.matsim.contrib.dvrp.schedule.Schedule;
import org.matsim.contrib.edrt.schedule.EDrtTaskFactoryImpl;
import org.matsim.contrib.ev.charging.ChargingStrategy;
import org.matsim.contrib.ev.charging.ChargingWithQueueingAndAssignmentLogic;
import org.matsim.contrib.ev.dvrp.EvDvrpVehicle;
import org.matsim.contrib.ev.fleet.ElectricVehicle;
import org.matsim.contrib.ev.infrastructure.Charger;
import org.matsim.contrib.ev.infrastructure.ChargingInfrastructure;
import org.matsim.core.mobsim.framework.MobsimTimer;

/**
 * @author michalm
 */
public class EmptyVehicleChargingScheduler {
	private final MobsimTimer timer;
	private final EDrtTaskFactoryImpl taskFactory;
	private final Map<Id<Link>, Charger> linkToChargerMap;

	public EmptyVehicleChargingScheduler(Network network, MobsimTimer timer, DrtTaskFactory taskFactory,
			ChargingInfrastructure chargingInfrastructure) {
		this.timer = timer;
		this.taskFactory = (EDrtTaskFactoryImpl)taskFactory;
		linkToChargerMap = chargingInfrastructure.getChargers().values().stream()
				.collect(Collectors.toMap(c -> c.getLink().getId(), c -> c));
	}

	public void chargeVehicle(DvrpVehicle vehicle) {
		DrtStayTask currentTask = (DrtStayTask)vehicle.getSchedule().getCurrentTask();
		Link currentLink = currentTask.getLink();
		Charger charger = linkToChargerMap.get(currentLink.getId());
		if (charger != null) {
			ElectricVehicle ev = ((EvDvrpVehicle)vehicle).getElectricVehicle();
			if (!charger.getLogic().getChargingStrategy().isChargingCompleted(ev)) {
				chargeVehicleImpl(vehicle, charger);
			}
		}
	}

	private void chargeVehicleImpl(DvrpVehicle vehicle, Charger charger) {
		Schedule schedule = vehicle.getSchedule();
		DrtStayTask stayTask = (DrtStayTask)schedule.getCurrentTask();
		if (stayTask.getTaskIdx() != schedule.getTaskCount() - 1) {
			throw new IllegalStateException("The current STAY task is not last. Not possible without prebooking");
		}
		stayTask.setEndTime(timer.getTimeOfDay()); // finish STAY

		// add CHARGING TASK
		double beginTime = stayTask.getEndTime();
		ChargingStrategy strategy = charger.getLogic().getChargingStrategy();
		ElectricVehicle ev = ((EvDvrpVehicle)vehicle).getElectricVehicle();
		double totalEnergy = -strategy.calcRemainingEnergyToCharge(ev);

		double chargingDuration = Math.min(strategy.calcRemainingTimeToCharge(ev),
				vehicle.getServiceEndTime() - beginTime);
		if (chargingDuration <= 0) {
			return;// no charging
		}
		double endTime = beginTime + chargingDuration;

		schedule.addTask(taskFactory.createChargingTask(vehicle, beginTime, endTime, charger, totalEnergy));
		((ChargingWithQueueingAndAssignmentLogic)charger.getLogic()).assignVehicle(ev);

		// append STAY
		schedule.addTask(taskFactory.createStayTask(vehicle, endTime, vehicle.getServiceEndTime(), charger.getLink()));
	}
}
