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

package org.matsim.contrib.drt.extension.edrt.scheduler;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.drt.extension.edrt.schedule.EDrtTaskFactoryImpl;
import org.matsim.contrib.drt.schedule.DrtStayTask;
import org.matsim.contrib.drt.schedule.DrtTaskFactory;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
import org.matsim.contrib.dvrp.schedule.Schedule;
import org.matsim.contrib.ev.charging.ChargingStrategy;
import org.matsim.contrib.ev.charging.ChargingWithAssignmentLogic;
import org.matsim.contrib.ev.fleet.ElectricVehicle;
import org.matsim.contrib.ev.infrastructure.Charger;
import org.matsim.contrib.ev.infrastructure.ChargingInfrastructure;
import org.matsim.contrib.evrp.EvDvrpVehicle;
import org.matsim.core.mobsim.framework.MobsimTimer;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * @author michalm
 */
public class EmptyVehicleChargingScheduler {
	private final MobsimTimer timer;
	private final EDrtTaskFactoryImpl taskFactory;
	private final Map<Id<Link>, List<Charger>> linkToChargersMap;
	private final ChargingStrategy.Factory chargingStrategyFactory;

	public EmptyVehicleChargingScheduler(MobsimTimer timer, DrtTaskFactory taskFactory,
			ChargingInfrastructure chargingInfrastructure, ChargingStrategy.Factory chargingStrategyFactory) {
		this.timer = timer;
		this.chargingStrategyFactory = chargingStrategyFactory;
		this.taskFactory = (EDrtTaskFactoryImpl)taskFactory;
		linkToChargersMap = chargingInfrastructure.getChargers()
				.values()
				.stream()
				.collect(Collectors.groupingBy(c -> c.getLink().getId()));
	}

	public void chargeVehicle(DvrpVehicle vehicle) {
		DrtStayTask currentTask = (DrtStayTask)vehicle.getSchedule().getCurrentTask();
		Link currentLink = currentTask.getLink();
		List<Charger> chargers = linkToChargersMap.get(currentLink.getId());
		if (chargers != null) {
			Optional<Charger> freeCharger = chargers.stream().filter(c -> c.getLogic().getPluggedVehicles().isEmpty()).findFirst();

			// Empty charger or at least smallest queue charger
			Charger charger = freeCharger.orElseGet(() -> chargers.stream().min(Comparator.comparingInt(e -> e.getLogic().getQueuedVehicles().size())).orElseThrow());
			ElectricVehicle ev = ((EvDvrpVehicle)vehicle).getElectricVehicle();
			ChargingStrategy strategy = chargingStrategyFactory.createStrategy(charger.getSpecification(), ev);
			if (!strategy.isChargingCompleted()) {
				chargeVehicleImpl(vehicle, ev, charger, strategy);
			}
		}
	}



	private void chargeVehicleImpl(DvrpVehicle vehicle, ElectricVehicle ev, Charger charger, ChargingStrategy strategy) {
		Schedule schedule = vehicle.getSchedule();
		DrtStayTask stayTask = (DrtStayTask)schedule.getCurrentTask();
		if (stayTask.getTaskIdx() != schedule.getTaskCount() - 1) {
			throw new IllegalStateException("The current STAY task is not last. Not possible without prebooking");
		}
		stayTask.setEndTime(timer.getTimeOfDay()); // finish STAY

		// add CHARGING TASK
		double beginTime = stayTask.getEndTime();
		double totalEnergy = -strategy.calcRemainingEnergyToCharge();

		double chargingDuration = Math.min(strategy.calcRemainingTimeToCharge(),
				vehicle.getServiceEndTime() - beginTime);
		if (chargingDuration <= 0) {
			return;// no charging
		}
		double endTime = beginTime + chargingDuration;

		schedule.addTask(taskFactory.createChargingTask(vehicle, beginTime, endTime, charger, totalEnergy, strategy));
		((ChargingWithAssignmentLogic)charger.getLogic()).assignVehicle(ev, strategy);

		// append STAY
		schedule.addTask(taskFactory.createStayTask(vehicle, endTime, vehicle.getServiceEndTime(), charger.getLink()));
	}
}
