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
package org.matsim.contrib.drt.extension.services.schedule;

import com.google.common.base.Verify;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.IdMap;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.drt.extension.edrt.schedule.EDrtChargingTask;
import org.matsim.contrib.drt.extension.services.events.DrtServiceEndedEvent;
import org.matsim.contrib.drt.extension.services.events.DrtServiceScheduledEvent;
import org.matsim.contrib.drt.extension.services.events.DrtServiceStartedEvent;
import org.matsim.contrib.drt.extension.services.services.params.DrtServiceParams;
import org.matsim.contrib.drt.extension.services.tasks.DrtServiceTask;
import org.matsim.contrib.drt.extension.services.tasks.ServiceTaskFactory;
import org.matsim.contrib.drt.extension.services.tasks.StackableTasks;
import org.matsim.contrib.drt.extension.operations.operationFacilities.OperationFacility;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.drt.schedule.DrtTaskFactory;
import org.matsim.contrib.drt.scheduler.EmptyVehicleRelocator;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
import org.matsim.contrib.dvrp.path.VrpPathWithTravelData;
import org.matsim.contrib.dvrp.path.VrpPaths;
import org.matsim.contrib.dvrp.schedule.*;
import org.matsim.contrib.dvrp.tracker.OnlineDriveTaskTracker;
import org.matsim.contrib.dvrp.util.LinkTimePair;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.mobsim.framework.MobsimTimer;
import org.matsim.core.router.speedy.SpeedyALTFactory;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;

import java.util.*;

/**
 * @author steffenaxer
 */
public class ServiceTaskSchedulerImpl implements ServiceTaskScheduler {
	private static final Logger LOG = LogManager.getLogger(ServiceTaskSchedulerImpl.class);
	private final StackableTasks stackableTasks;
	private final DrtConfigGroup drtConfigGroup;
	private final Network network;
	private final TravelTime travelTime;
	private final MobsimTimer timer;
	private final ServiceTaskFactory taskFactory;
	private final LeastCostPathCalculator router;
	private final EventsManager eventsManager;
	private final MobsimTimer mobsimTimer;
	private final Map<Id<DvrpVehicle>, HashMap<Id<DrtService>, DrtService>> scheduledServices = new IdMap<>(DvrpVehicle.class);
	private final Map<Id<DvrpVehicle>, HashMap<Id<DrtService>, DrtService>> startedServices = new IdMap<>(DvrpVehicle.class);

	public ServiceTaskSchedulerImpl(final DrtConfigGroup drtConfigGroup,
									final Network network,
									final TravelTime travelTime,
									final TravelDisutility travelDisutility,
									final MobsimTimer timer,
									final DrtTaskFactory taskFactory,
									final StackableTasks stackableTasks,
									final EventsManager eventsManager,
									final MobsimTimer mobsimTimer
	) {
		this.drtConfigGroup = drtConfigGroup;
		this.network = network;
		this.travelTime = travelTime;
		this.timer = timer;
		this.taskFactory = (ServiceTaskFactory) taskFactory;
		this.stackableTasks = stackableTasks;
		this.eventsManager = eventsManager;
		this.mobsimTimer = mobsimTimer;
		this.router = new SpeedyALTFactory().createPathCalculator(network, travelDisutility, travelTime);
	}

	@Override
	public void scheduleServiceTask(DvrpVehicle vehicle, OperationFacility serviceFacility, DrtServiceParams drtServiceParams, boolean enableTaskStacking) {
		double duration = drtServiceParams.duration;

		final Schedule schedule = vehicle.getSchedule();

		final Task currentTask = schedule.getCurrentTask();

		Link toLink = network.getLinks().get(serviceFacility.getLinkId());

		if (currentTask instanceof DriveTask
			&& currentTask.getTaskType().equals(EmptyVehicleRelocator.RELOCATE_VEHICLE_TASK_TYPE)
			&& currentTask.equals(schedule.getTasks().get(schedule.getTaskCount() - 2))) {
			//try to divert/cancel relocation
			LinkTimePair start = ((OnlineDriveTaskTracker) currentTask.getTaskTracker()).getDiversionPoint();

			VrpPathWithTravelData path;
			if (start != null) {
				toLink = network.getLinks().get(serviceFacility.getLinkId());
				path = VrpPaths.calcAndCreatePath(start.link, toLink, start.time, router, travelTime);
				((OnlineDriveTaskTracker) currentTask.getTaskTracker()).divertPath(path);

				// remove STAY
				schedule.removeLastTask();
			} else {
				start = new LinkTimePair(((DriveTask) currentTask).getPath().getToLink(), currentTask.getEndTime());
				path = VrpPaths.calcAndCreatePath(start.link, toLink, start.time, router, travelTime);

				// remove STAY
				schedule.removeLastTask();

				//add drive to maintenance location
				schedule.addTask(taskFactory.createDriveTask(vehicle, path, RELOCATE_SERVICE_TASK_TYPE)); // add RELOCATE
			}

			double startTime = path.getArrivalTime();
			double endTime = startTime + duration;

			addServiceTask(vehicle, drtServiceParams, startTime, endTime, toLink, serviceFacility);

		}
		// Append new task to existing charging task
		else if (currentTask instanceof EDrtChargingTask chargingTask &&
			Schedules.getLastTask(schedule) != currentTask &&
			!(Schedules.getNextTask(schedule) instanceof DrtServiceTask)) {

			double compensatedDuration = compensateDuration(currentTask, duration, drtServiceParams.name, enableTaskStacking);
			schedule.removeLastTask(); //Remove stay
			double startTime = currentTask.getEndTime();
			double endTime = startTime + compensatedDuration;
			addServiceTask(vehicle, drtServiceParams, startTime, endTime, chargingTask.getLink(), serviceFacility);
		}
		// Append new task to existing service task
		else if (currentTask instanceof EDrtChargingTask chargingTask &&
			Schedules.getLastTask(schedule) != currentTask &&
			(Schedules.getNextTask(schedule) instanceof DrtServiceTask)) {

			double compensatedDuration = compensateDuration(currentTask, duration, drtServiceParams.name, enableTaskStacking);
			schedule.removeLastTask(); //Remove stay

			//Append to end
			double startTime = Schedules.getLastTask(schedule).getEndTime();
			double endTime = startTime + compensatedDuration;
			addServiceTask(vehicle, drtServiceParams, startTime, endTime, chargingTask.getLink(), serviceFacility);
		} else {
			double compensatedDuration = compensateDuration(currentTask, duration, drtServiceParams.name, enableTaskStacking);
			final Task task = schedule.getTasks().get(schedule.getTaskCount() - 1);
			final Link lastLink = ((StayTask) task).getLink();

			if (lastLink.getId() != serviceFacility.getLinkId()) {
				double departureTime = task.getBeginTime();

				if (schedule.getCurrentTask() == task) {
					departureTime = Math.max(task.getBeginTime(), timer.getTimeOfDay());
				}

				VrpPathWithTravelData path = VrpPaths.calcAndCreatePath(lastLink, toLink,
					departureTime, router,
					travelTime);
				if (path.getArrivalTime() < vehicle.getServiceEndTime()) {

					if (schedule.getCurrentTask() == task) {
						task.setEndTime(timer.getTimeOfDay());
					} else {
						// remove STAY
						schedule.removeLastTask();
					}

					//add drive to service location
					schedule.addTask(taskFactory.createDriveTask(vehicle, path, RELOCATE_SERVICE_TASK_TYPE)); // add RELOCATE
					double startTime = path.getArrivalTime();
					double endTime = startTime + compensatedDuration;

					addServiceTask(vehicle, drtServiceParams, startTime, endTime, toLink, serviceFacility);
				}
			} else {
				double startTime;
				if (schedule.getCurrentTask() == task) {
					task.setEndTime(timer.getTimeOfDay());
					startTime = timer.getTimeOfDay();
				} else {
					// remove STAY
					startTime = task.getBeginTime();
					schedule.removeLastTask();
				}
				double endTime = startTime + compensatedDuration;

				addServiceTask(vehicle, drtServiceParams, startTime, endTime, toLink, serviceFacility);
			}
		}
	}

	private void addServiceTask(DvrpVehicle vehicle, DrtServiceParams drtServiceParams, double startTime, double endTime, Link link, OperationFacility operationFacility) {

		Verify.verify(link.getId().equals(operationFacility.getLinkId()));

		if (startTime > vehicle.getServiceEndTime() || endTime > vehicle.getServiceEndTime()) {
			// Do not schedule behind service time
			return;
		}

		Schedule schedule = vehicle.getSchedule();
		// append DrtServiceTask
		Id<DrtService> drtServiceId = Id.create(UUID.randomUUID().toString(), DrtService.class);
		schedule.addTask(taskFactory.createServiceTask(drtServiceId, startTime, endTime, link, operationFacility));
		this.eventsManager.processEvent(new DrtServiceScheduledEvent(drtServiceId, mobsimTimer.getTimeOfDay(), startTime, endTime, drtConfigGroup.mode, drtServiceParams.name, vehicle.getId(), link.getId(), operationFacility.getId()));
		this.scheduledServices.computeIfAbsent(vehicle.getId(), k -> new HashMap<>()).put(drtServiceId, new DrtService(drtServiceId, startTime, endTime, drtServiceParams.name, link.getId(), operationFacility.getId()));

		// append DrtStayTask
		schedule.addTask(taskFactory.createStayTask(vehicle, endTime, Math.max(vehicle.getServiceEndTime(), endTime), link));
	}

	@Override
	public Map<Id<DrtService>, DrtService> getScheduledServices(Id<DvrpVehicle> dvrpVehicleId) {
		return this.scheduledServices.get(dvrpVehicleId);
	}

	@Override
	public Map<Id<DrtService>, DrtService> getStartedServices(Id<DvrpVehicle> dvrpVehicleId) {
		return this.startedServices.get(dvrpVehicleId);
	}

	@Override
	public void startService(Id<DvrpVehicle> dvrpVehicleId, Id<DrtService> drtServiceId) {
		DrtService drtService = this.scheduledServices.get(dvrpVehicleId).remove(drtServiceId);
		Verify.verify(drtService != null);
		drtService.start();
		this.startedServices.computeIfAbsent(dvrpVehicleId, k -> new HashMap<>()).put(drtService.drtServiceId, drtService);
		this.eventsManager.processEvent(new DrtServiceStartedEvent(drtService.drtServiceId, mobsimTimer.getTimeOfDay(), drtConfigGroup.mode, drtService.serviceType, dvrpVehicleId, drtService.linkId, drtService.operationFacilityId));
	}

	@Override
	public void stopService(Id<DvrpVehicle> dvrpVehicleId, Id<DrtService> drtServiceId) {
		DrtService drtService = this.startedServices.get(dvrpVehicleId).remove(drtServiceId);
		Verify.verify(drtService != null);
		drtService.end();
		this.eventsManager.processEvent(new DrtServiceEndedEvent(drtService.drtServiceId, mobsimTimer.getTimeOfDay(), drtConfigGroup.mode, drtService.serviceType, dvrpVehicleId, drtService.linkId, drtService.operationFacilityId));
	}

	double compensateDuration(Task currentTask, double requestedDuration, String serviceName, boolean enableTaskStacking) {
		if (stackableTasks.isStackableTask(currentTask) && enableTaskStacking) {
			double currentTaskDuration = currentTask.getEndTime() - currentTask.getBeginTime();
			if (requestedDuration <= currentTaskDuration) {
				LOG.debug("Service {} with requested {} seconds takes place while {}"
					, serviceName, requestedDuration, currentTask.getTaskType().name());
				return 1.;
			} else {
				double compensatedDuration = Math.max(1, requestedDuration - currentTaskDuration);
				LOG.debug("Service {} with requested {} seconds takes partially place while {}. Remaining {} seconds"
					, serviceName, requestedDuration, currentTask.getTaskType().name(), compensatedDuration);
				return compensatedDuration;
			}
		}
		return requestedDuration;
	}


}
