/*
 * *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2024 by the members listed in the COPYING,        *
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
 * *********************************************************************** *
 */
package org.matsim.contrib.drt.extension.services.optimizer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.contrib.drt.extension.services.dispatcher.ServiceTaskDispatcher;
import org.matsim.contrib.drt.extension.operations.shifts.optimizer.ShiftDrtOptimizer;
import org.matsim.contrib.drt.extension.services.tasks.DrtServiceTask;
import org.matsim.contrib.drt.optimizer.DrtOptimizer;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
import org.matsim.contrib.dvrp.optimizer.Request;
import org.matsim.contrib.dvrp.schedule.Schedule;
import org.matsim.contrib.dvrp.schedule.ScheduleTimingUpdater;
import org.matsim.contrib.dvrp.schedule.Schedules;
import org.matsim.contrib.dvrp.schedule.Task;
import org.matsim.core.mobsim.framework.MobsimTimer;
import org.matsim.core.mobsim.framework.events.MobsimBeforeSimStepEvent;
import org.matsim.core.mobsim.framework.events.MobsimInitializedEvent;
import org.matsim.core.mobsim.framework.listeners.MobsimInitializedListener;

/**
 * @author steffenaxer
 */
public class DrtServiceTaskOptimizer implements DrtOptimizer, MobsimInitializedListener {
	private static final Logger LOG = LogManager.getLogger(DrtServiceTaskOptimizer.class);
	private final DrtOptimizer delegate;
	private final ScheduleTimingUpdater scheduleTimingUpdater;
	private final ServiceTaskDispatcher serviceTaskDispatcher;

	public DrtServiceTaskOptimizer(ServiceTaskDispatcher serviceTaskDispatcher, DrtOptimizer delegate, ScheduleTimingUpdater scheduleTimingUpdater, MobsimTimer mobsimTimer) {
		this.delegate = delegate;
		this.scheduleTimingUpdater = scheduleTimingUpdater;
		this.serviceTaskDispatcher = serviceTaskDispatcher;
	}

	@Override
	public void requestSubmitted(Request request) {
		this.delegate.requestSubmitted(request);
	}

	@Override
	public void nextTask(DvrpVehicle vehicle) {
		scheduleTimingUpdater.updateBeforeNextTask(vehicle);
		updateServiceEnds(vehicle);

		final Task previousTask = getTaskOrNull(vehicle);
		delegate.nextTask(vehicle);

		final Task successivTask = getTaskOrNull(vehicle);

		observeServiceExecution(vehicle, previousTask, successivTask);
	}

	private void updateServiceEnds(DvrpVehicle vehicle) {
		Schedule schedule = vehicle.getSchedule();

		if (schedule.getStatus() != Schedule.ScheduleStatus.STARTED ||
			schedule.getCurrentTask() == Schedules.getLastTask(schedule)) {
			return;
		}

		int currentTaskIdx = vehicle.getSchedule().getCurrentTask().getTaskIdx();
		var serviceTasks = vehicle.getSchedule().getTasks().stream()
			.filter(t -> t.getTaskIdx() >= currentTaskIdx)
			.filter(t -> t instanceof DrtServiceTask).toList();

		serviceTasks.forEach(t -> enforceIntendedDuration(vehicle, (DrtServiceTask) t));
	}

	private void enforceIntendedDuration(DvrpVehicle dvrpVehicle, DrtServiceTask drtServiceTask) {
		double intendedDuration = drtServiceTask.getIntendedDuration();
		double currentDuration = drtServiceTask.getEndTime() - drtServiceTask.getBeginTime();

		if (currentDuration < intendedDuration) {
			double endTime = drtServiceTask.getBeginTime() + intendedDuration;
			drtServiceTask.setEndTime(endTime);
			this.scheduleTimingUpdater.updateTimingsStartingFromTaskIdx(dvrpVehicle,drtServiceTask.getTaskIdx()+1, endTime);
		}
	}


	private void observeServiceExecution(DvrpVehicle dvrpVehicle, Task previousTask, Task successivTask) {
		if (previousTask != successivTask) {
			if (successivTask instanceof DrtServiceTask drtServiceTask) {
				this.serviceTaskDispatcher.startService(dvrpVehicle, drtServiceTask.getDrtServiceId());
			}

			if (previousTask instanceof DrtServiceTask drtServiceTask) {
				this.serviceTaskDispatcher.stopService(dvrpVehicle, drtServiceTask.getDrtServiceId());
			}
		}
	}

	private Task getTaskOrNull(DvrpVehicle vehicle) {
		var schedule = vehicle.getSchedule();
		if (schedule.getStatus() == Schedule.ScheduleStatus.STARTED) {
			return schedule.getCurrentTask();
		}
		return null;
	}

	@Override
	public void notifyMobsimBeforeSimStep(MobsimBeforeSimStepEvent e) {
		this.serviceTaskDispatcher.dispatch(e.getSimulationTime());
		this.delegate.notifyMobsimBeforeSimStep(e);
	}

	@Override
	public void notifyMobsimInitialized(MobsimInitializedEvent e) {
		if (this.delegate instanceof ShiftDrtOptimizer shiftDrtOptimizer) {
			shiftDrtOptimizer.notifyMobsimInitialized(e);
		}
	}
}
