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
package org.matsim.contrib.drt.extension.services.dispatcher;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.drt.extension.services.schedule.DrtService;
import org.matsim.contrib.drt.extension.services.services.ServiceTriggerFactory;
import org.matsim.contrib.drt.extension.services.services.params.AbstractServiceTriggerParam;
import org.matsim.contrib.drt.extension.services.services.params.DrtServiceParams;
import org.matsim.contrib.drt.extension.services.schedule.ServiceTaskScheduler;
import org.matsim.contrib.drt.extension.services.services.params.DrtServicesParams;
import org.matsim.contrib.drt.extension.services.services.tracker.ServiceExecutionTracker;
import org.matsim.contrib.drt.extension.services.services.tracker.ServiceExecutionTrackers;
import org.matsim.contrib.drt.extension.services.services.triggers.ServiceExecutionTrigger;
import org.matsim.contrib.drt.extension.operations.operationFacilities.OperationFacility;
import org.matsim.contrib.drt.extension.operations.operationFacilities.OperationFacilityFinder;
import org.matsim.contrib.drt.schedule.DrtStayTask;
import org.matsim.contrib.drt.scheduler.EmptyVehicleRelocator;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
import org.matsim.contrib.dvrp.fleet.Fleet;
import org.matsim.contrib.dvrp.schedule.DriveTask;
import org.matsim.contrib.dvrp.schedule.Schedule;
import org.matsim.contrib.dvrp.schedule.Task;
import org.matsim.contrib.dvrp.tracker.OnlineDriveTaskTracker;
import org.matsim.contrib.dvrp.util.LinkTimePair;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.mobsim.framework.MobsimTimer;

import java.util.*;
import java.util.stream.Stream;

/**
 * @author steffenaxer
 */
public class ServiceTaskDispatcherImpl implements ServiceTaskDispatcher {
	private static final Logger LOG = LogManager.getLogger(ServiceTaskDispatcherImpl.class);
	private final DrtServicesParams drtServicesParams;
	private final Fleet fleet;
	private final ServiceTaskScheduler serviceTaskScheduler;
	private final OperationFacilityFinder operationFacilityFinder;
	private final ServiceTriggerFactory serviceTriggerFactory;
	private final ServiceExecutionTrackers executionTrackers;

	public ServiceTaskDispatcherImpl(final DrtServicesParams drtServicesParams,
									 final Fleet fleet,
									 final ServiceTaskScheduler serviceTaskScheduler,
									 final OperationFacilityFinder operationFacilityFinder,
									 final ServiceTriggerFactory serviceTriggerFactory,
									 final ServiceExecutionTrackers executionTrackers) {
		this.drtServicesParams = drtServicesParams;
		this.fleet = fleet;
		this.serviceTaskScheduler = serviceTaskScheduler;
		this.operationFacilityFinder = operationFacilityFinder;
		this.serviceTriggerFactory = serviceTriggerFactory;
		this.executionTrackers = executionTrackers;
		this.installTriggers();
	}

	record ServiceScheduleEntry(DvrpVehicle dvrpVehicle, OperationFacility operationFacility, DrtServiceParams drtServiceParams, Boolean stackable) {
	}

	@Override
	public void dispatch(double timeStep) {
		Stream<ServiceScheduleEntry> serviceEntries = this.fleet.getVehicles().values().stream()
			.flatMap(v -> checkVehicleForService(v, timeStep).stream());
		dispatch(serviceEntries);
	}

	@Override
	public void startService(DvrpVehicle dvrpVehicle, Id<DrtService> drtServiceId) {
		this.serviceTaskScheduler.startService(dvrpVehicle.getId(), drtServiceId);
	}

	@Override
	public void stopService(DvrpVehicle dvrpVehicle, Id<DrtService> drtServiceId) {
		this.serviceTaskScheduler.stopService(dvrpVehicle.getId(), drtServiceId);
	}

	void dispatch(Stream<ServiceScheduleEntry> entries) {
		entries.forEach(e -> this.serviceTaskScheduler.scheduleServiceTask(e.dvrpVehicle, e.operationFacility, e.drtServiceParams, e.stackable));
	}

	List<ServiceScheduleEntry> checkVehicleForService(DvrpVehicle dvrpVehicle, double timeStep) {
		List<ServiceScheduleEntry> servicesToBeScheduled = new ArrayList<>();
		ServiceExecutionTracker serviceExecutionTracker = this.executionTrackers.getTrackers().get(dvrpVehicle.getId());
		for (DrtServiceParams drtServiceParams : serviceExecutionTracker.getServices()) {
			int executionLimit = drtServiceParams.executionLimit;
			int currentExecutions = serviceExecutionTracker.getScheduledCounter(drtServiceParams.name);
			boolean stackable = drtServiceParams.enableTaskStacking;

			if (currentExecutions == executionLimit) {
				LOG.debug("Execution limit for vehicle {} and service {} reached.", drtServiceParams.name, dvrpVehicle.getId());
				continue;
			}

			for (ServiceExecutionTrigger serviceExecutionTrigger : serviceExecutionTracker.getTriggers(drtServiceParams)) {
				if (serviceExecutionTrigger.requiresService(dvrpVehicle, timeStep)) {
					LOG.debug("{} scheduled service {} for vehicle {} at {}.", serviceExecutionTrigger.getName(), drtServiceParams.name, dvrpVehicle.getId(), timeStep);
					servicesToBeScheduled.add(new ServiceScheduleEntry(dvrpVehicle, findServiceFacility(dvrpVehicle), drtServiceParams, stackable));
				}
			}
		}
		return servicesToBeScheduled;
	}

	private void installTriggers() {
		for (DvrpVehicle vehicle : this.fleet.getVehicles().values()) {
			ServiceExecutionTracker serviceExecutionTracker = this.executionTrackers.getTrackers().get(vehicle.getId());
			for (DrtServiceParams drtServiceParams : this.drtServicesParams.getServices()) {
				Collection<? extends Collection<? extends ConfigGroup>> triggerSets = drtServiceParams.getParameterSets().values();
				for (Collection<? extends ConfigGroup> triggerSet : triggerSets) {
					for (var trigger : triggerSet) {
						// There could be multiple triggers of the same type
						var triggerParam = (AbstractServiceTriggerParam) trigger;
						serviceExecutionTracker.addTrigger(drtServiceParams, this.serviceTriggerFactory.get(vehicle.getId(), triggerParam));
					}
				}
			}
		}
	}

	private OperationFacility findServiceFacility(DvrpVehicle dvrpVehicle) {
		final Schedule schedule = dvrpVehicle.getSchedule();
		Task currentTask = schedule.getCurrentTask();
		Link lastLink;
		if (currentTask instanceof DriveTask
			&& currentTask.getTaskType().equals(EmptyVehicleRelocator.RELOCATE_VEHICLE_TASK_TYPE)
			&& currentTask.equals(schedule.getTasks().get(schedule.getTaskCount() - 2))) {
			LinkTimePair start = ((OnlineDriveTaskTracker) currentTask.getTaskTracker()).getDiversionPoint();
			if (start != null) {
				lastLink = start.link;
			} else {
				lastLink = ((DriveTask) currentTask).getPath().getToLink();
			}
		} else {
			lastLink = ((DrtStayTask) schedule.getTasks()
				.get(schedule.getTaskCount() - 1)).getLink();
		}
		return operationFacilityFinder.findFacility(lastLink.getCoord()).orElseThrow();
	}

}
