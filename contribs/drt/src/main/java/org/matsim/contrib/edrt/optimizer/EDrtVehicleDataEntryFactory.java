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

import java.util.List;

import org.matsim.contrib.drt.optimizer.VehicleEntry;
import org.matsim.contrib.drt.optimizer.VehicleDataEntryFactoryImpl;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
import org.matsim.contrib.dvrp.schedule.Schedule;
import org.matsim.contrib.dvrp.schedule.Schedule.ScheduleStatus;
import org.matsim.contrib.dvrp.schedule.Task;
import org.matsim.contrib.dvrp.schedule.Task.TaskStatus;
import org.matsim.contrib.edrt.schedule.EDrtChargingTask;
import org.matsim.contrib.ev.dvrp.ETask;
import org.matsim.contrib.ev.dvrp.EvDvrpVehicle;
import org.matsim.contrib.ev.dvrp.tracker.ETaskTracker;
import org.matsim.contrib.ev.fleet.Battery;

import com.google.inject.Provider;

/**
 * @author michalm
 */
public class EDrtVehicleDataEntryFactory implements VehicleEntry.EntryFactory {
	public static class EVehicleEntry extends VehicleEntry {
		public final double socBeforeFinalStay;

		public EVehicleEntry(VehicleEntry entry, double socBeforeFinalStay) {
			super(entry.vehicle, entry.start, entry.stops);
			this.socBeforeFinalStay = socBeforeFinalStay;
		}
	}

	private final double minimumRelativeSoc;
	private final VehicleDataEntryFactoryImpl entryFactory;

	public EDrtVehicleDataEntryFactory(DrtConfigGroup drtCfg, double minimumRelativeSoc) {
		this.minimumRelativeSoc = minimumRelativeSoc;
		entryFactory = new VehicleDataEntryFactoryImpl(drtCfg);
	}

	@Override
	public VehicleEntry create(DvrpVehicle vehicle, double currentTime) {
		if (!entryFactory.isEligibleForRequestInsertion(vehicle, currentTime)) {
			return null;
		}

		Schedule schedule = vehicle.getSchedule();
		int taskCount = schedule.getTaskCount();
		if (taskCount > 1) {
			Task oneBeforeLast = schedule.getTasks().get(taskCount - 2);
			if (oneBeforeLast.getStatus() != TaskStatus.PERFORMED && oneBeforeLast.getTaskType()
					.equals(EDrtChargingTask.TYPE)) {
				return null;
			}
		}

		Battery battery = ((EvDvrpVehicle)vehicle).getElectricVehicle().getBattery();
		int nextTaskIdx;
		double socBeforeNextTask;
		if (schedule.getStatus() == ScheduleStatus.PLANNED) {
			nextTaskIdx = 0;
			socBeforeNextTask = battery.getSoc();
		} else { // STARTED
			Task currentTask = schedule.getCurrentTask();
			ETaskTracker eTracker = (ETaskTracker)currentTask.getTaskTracker();
			socBeforeNextTask = eTracker.predictSocAtEnd();
			nextTaskIdx = currentTask.getTaskIdx() + 1;
		}

		List<? extends Task> tasks = schedule.getTasks();
		for (int i = nextTaskIdx; i < tasks.size() - 1; i++) {
			socBeforeNextTask -= ((ETask)tasks.get(i)).getTotalEnergy();
		}

		if (socBeforeNextTask < minimumRelativeSoc * battery.getCapacity()) {
			return null;// skip undercharged vehicles
		}

		VehicleEntry entry = entryFactory.create(vehicle, currentTime);
		return entry == null ? null : new EVehicleEntry(entry, socBeforeNextTask);
	}

	public static class EDrtVehicleDataEntryFactoryProvider implements Provider<VehicleEntry.EntryFactory> {
		private final DrtConfigGroup drtCfg;
		private final double minimumRelativeSoc;

		public EDrtVehicleDataEntryFactoryProvider(DrtConfigGroup drtCfg, double minimumRelativeSoc) {
			this.drtCfg = drtCfg;
			this.minimumRelativeSoc = minimumRelativeSoc;
		}

		@Override
		public EDrtVehicleDataEntryFactory get() {
			return new EDrtVehicleDataEntryFactory(drtCfg, minimumRelativeSoc);
		}
	}
}
