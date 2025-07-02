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

import static org.matsim.contrib.taxi.schedule.TaxiTaskBaseType.EMPTY_DRIVE;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
import org.matsim.contrib.dvrp.fleet.Fleet;
import org.matsim.contrib.dvrp.path.VrpPathWithTravelData;
import org.matsim.contrib.dvrp.schedule.Schedule;
import org.matsim.contrib.dvrp.schedule.Schedule.ScheduleStatus;
import org.matsim.contrib.dvrp.schedule.Schedules;
import org.matsim.contrib.dvrp.schedule.Task;
import org.matsim.contrib.ev.charging.ChargingEstimations;
import org.matsim.contrib.ev.charging.ChargingStrategy;
import org.matsim.contrib.ev.charging.ChargingWithAssignmentLogic;
import org.matsim.contrib.ev.fleet.ElectricVehicle;
import org.matsim.contrib.ev.infrastructure.Charger;
import org.matsim.contrib.taxi.run.TaxiConfigGroup;
import org.matsim.contrib.taxi.schedule.TaxiStayTask;
import org.matsim.contrib.taxi.schedule.TaxiTaskType;
import org.matsim.contrib.taxi.scheduler.TaxiScheduleInquiry;
import org.matsim.contrib.taxi.scheduler.TaxiScheduler;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.mobsim.framework.MobsimTimer;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.TravelTime;

public class ETaxiScheduler extends TaxiScheduler {
	public static final TaxiTaskType DRIVE_TO_CHARGER = new TaxiTaskType("DRIVE_TO_CHARGER", EMPTY_DRIVE);

	private final ChargingStrategy.Factory chargingStrategyFactory;

	public ETaxiScheduler(TaxiConfigGroup taxiCfg, Fleet fleet, TaxiScheduleInquiry taxiScheduleInquiry,
			TravelTime travelTime, Supplier<LeastCostPathCalculator> routerCreator, EventsManager eventsManager,
			MobsimTimer mobsimTimer, ChargingStrategy.Factory chargingStrategyFactory) {
		super(taxiCfg, fleet, taxiScheduleInquiry, travelTime, routerCreator, eventsManager, mobsimTimer);
		this.chargingStrategyFactory = chargingStrategyFactory;
	}

	// FIXME underestimated due to the ongoing AUX/drive consumption
	// not a big issue for e-rule-based dispatching (no look ahead)
	// more problematic for e-assignment dispatching
	public void scheduleCharging(DvrpVehicle vehicle, ElectricVehicle ev, Charger charger,
			VrpPathWithTravelData vrpPath) {
		Schedule schedule = vehicle.getSchedule();
		divertOrAppendDrive(schedule, vrpPath, DRIVE_TO_CHARGER);

		ChargingWithAssignmentLogic logic = (ChargingWithAssignmentLogic)charger.getLogic();
		ChargingStrategy strategy = chargingStrategyFactory.createStrategy(charger.getSpecification(), ev);
		double chargingEndTime = vrpPath.getArrivalTime() + ChargingEstimations.estimateMaxWaitTimeForNextVehicle(
				charger)// TODO not precise!!!
				+ strategy.calcRemainingTimeToCharge();// TODO not precise !!! (SOC will be lower)
		schedule.addTask(new ETaxiChargingTask(vrpPath.getArrivalTime(), chargingEndTime, charger, ev,
				-strategy.calcRemainingEnergyToCharge(), strategy));// TODO not precise !!! (ditto)
		logic.assignVehicle(ev, strategy);

		appendStayTask(vehicle);
	}

	// =========================================================================================

	private boolean chargingTasksRemovalMode = false;
	private List<DvrpVehicle> vehiclesWithUnscheduledCharging;

	public void beginChargingTaskRemoval() {
		chargingTasksRemovalMode = true;
		vehiclesWithUnscheduledCharging = new ArrayList<>();
	}

	public List<DvrpVehicle> endChargingTaskRemoval() {
		chargingTasksRemovalMode = false;
		return vehiclesWithUnscheduledCharging;
	}

	// Drives-to-chargers can be diverted if diversion is on.
	// Otherwise, we do not remove stays-at-chargers from schedules.
	@Override
	protected Integer countUnremovablePlannedTasks(Schedule schedule) {
		TaxiTaskType currentTaskType = (TaxiTaskType)schedule.getCurrentTask().getTaskType();

		if (currentTaskType.equals(ETaxiChargingTask.TYPE)) {
			return 0;
		} else if (currentTaskType.baseType().get() == EMPTY_DRIVE //
				&& Schedules.getNextTask(schedule).getTaskType().equals(ETaxiChargingTask.TYPE)) {
			//drive task to charging station
			if (taxiCfg.vehicleDiversion) {
				return 0;// though questionable
			}

			// if no diversion and driving to a charger then keep 'charge'
			return 1;
		}

		return super.countUnremovablePlannedTasks(schedule);
	}

	// A vehicle doing 'charge' is not considered idle.
	// This is ensured by having at least one task (e.g. 'wait') following the 'charge'.
	//
	// Maybe for a more complex algos we would like to interrupt charging tasks as well.
	@Override
	protected void removePlannedTasks(DvrpVehicle vehicle, int newLastTaskIdx) {
		Schedule schedule = vehicle.getSchedule();
		List<? extends Task> tasks = schedule.getTasks();

		for (int i = schedule.getTaskCount() - 1; i > newLastTaskIdx; i--) {
			Task task = tasks.get(i);

			if (!chargingTasksRemovalMode && task.getTaskType().equals(ETaxiChargingTask.TYPE)) {
				break;// cannot remove -> stop removing
			}

			schedule.removeTask(task);
			taskRemovedFromSchedule(vehicle, task);
		}

		if (schedule.getStatus() == ScheduleStatus.UNPLANNED) {
			return;
		}

		Task lastTask = Schedules.getLastTask(schedule);
		if (lastTask.getTaskType().equals(ETaxiChargingTask.TYPE)) {
			// we must append 'wait' because both 'charge' and 'wait' are 'STAY' tasks,
			// so the standard TaxiScheduler cannot distinguish them and would treat 'charge' as 'wait'

			// we can use chargeEndTime for both begin and end times,
			// the right endTime is set in TaxiScheduler.removeAwaitingRequestsImpl()
			double chargeEndTime = lastTask.getEndTime();
			Link chargeLink = ((ETaxiChargingTask)lastTask).getLink();
			schedule.addTask(new TaxiStayTask(chargeEndTime, chargeEndTime, chargeLink));
		}
	}

	@Override
	protected void taskRemovedFromSchedule(DvrpVehicle vehicle, Task task) {
		if (task.getTaskType().equals(ETaxiChargingTask.TYPE)) {
			ETaxiChargingTask chargingTask = ((ETaxiChargingTask)task);
			chargingTask.getChargingLogic().unassignVehicle(chargingTask.getElectricVehicle());
			vehiclesWithUnscheduledCharging.add(vehicle);
		} else {
			super.taskRemovedFromSchedule(vehicle, task);
		}
	}
}
