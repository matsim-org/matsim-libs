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

package org.matsim.contrib.drt.extension.edrt.optimizer;

import java.util.List;

import org.matsim.contrib.drt.extension.edrt.schedule.EDrtChargingTask;
import org.matsim.contrib.drt.optimizer.VehicleDataEntryFactoryImpl;
import org.matsim.contrib.drt.optimizer.VehicleEntry;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
import org.matsim.contrib.dvrp.schedule.Schedule;
import org.matsim.contrib.dvrp.schedule.Schedule.ScheduleStatus;
import org.matsim.contrib.dvrp.schedule.Task;
import org.matsim.contrib.dvrp.schedule.Task.TaskStatus;
import org.matsim.contrib.ev.fleet.Battery;
import org.matsim.contrib.evrp.ETask;
import org.matsim.contrib.evrp.EvDvrpVehicle;
import org.matsim.contrib.evrp.tracker.ETaskTracker;

import com.google.inject.Provider;

/**
 * @author michalm
 */
public class EDrtVehicleDataEntryFactory implements VehicleEntry.EntryFactory {
	public static class EVehicleEntry extends VehicleEntry {
		public final double socBeforeFinalStay;

		public EVehicleEntry(VehicleEntry entry, double socBeforeFinalStay) {
			super(entry);
			this.socBeforeFinalStay = socBeforeFinalStay;
		}
	}

	private final double minimumRelativeSoc;
	private final VehicleDataEntryFactoryImpl entryFactory;

	public EDrtVehicleDataEntryFactory(double minimumRelativeSoc) {
		this.minimumRelativeSoc = minimumRelativeSoc;
		entryFactory = new VehicleDataEntryFactoryImpl();
	}

	@Override
	public VehicleEntry create(DvrpVehicle vehicle, double currentTime) {
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
		double chargeBeforeNextTask;

		switch (schedule.getStatus()) {
			case PLANNED:
				nextTaskIdx = 0;
				chargeBeforeNextTask = battery.getCharge();
				break;
			case STARTED:
				Task currentTask = schedule.getCurrentTask();
				ETaskTracker eTracker = (ETaskTracker) currentTask.getTaskTracker();
				chargeBeforeNextTask = eTracker.predictChargeAtEnd();
				nextTaskIdx = currentTask.getTaskIdx() + 1;
				break;
			default:
				return null;
		}


		List<? extends Task> tasks = schedule.getTasks();
		for (int i = nextTaskIdx; i < tasks.size() - 1; i++) {
			chargeBeforeNextTask -= ((ETask)tasks.get(i)).getTotalEnergy();
		}

		if (chargeBeforeNextTask < minimumRelativeSoc * battery.getCapacity()) {
			return null;// skip undercharged vehicles
		}

		VehicleEntry entry = entryFactory.create(vehicle, currentTime);
		return entry == null ? null : new EVehicleEntry(entry, chargeBeforeNextTask);
	}

	public static class EDrtVehicleDataEntryFactoryProvider implements Provider<VehicleEntry.EntryFactory> {
		private final double minimumRelativeSoc;

		public EDrtVehicleDataEntryFactoryProvider(double minimumRelativeSoc) {
			this.minimumRelativeSoc = minimumRelativeSoc;
		}

		@Override
		public EDrtVehicleDataEntryFactory get() {
			return new EDrtVehicleDataEntryFactory(minimumRelativeSoc);
		}
	}
}
