/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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

package org.matsim.contrib.drt.taxibus.algorithm.optimizer;

import java.util.*;

import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.drt.DrtRequest;
import org.matsim.contrib.drt.tasks.DrtTask;
import org.matsim.contrib.drt.tasks.DrtTask.DrtTaskType;
import org.matsim.contrib.dvrp.data.*;
import org.matsim.contrib.dvrp.schedule.*;
import org.matsim.core.mobsim.framework.events.MobsimBeforeSimStepEvent;

public abstract class AbstractTaxibusOptimizer implements TaxibusOptimizer {
	private final TaxibusOptimizerContext optimContext;
	private final Collection<DrtRequest> unplannedRequests;

	private final boolean doUnscheduleAwaitingRequests;// PLANNED or TAXI_DISPATCHED
	private final boolean destinationKnown;
	private final boolean vehicleDiversion;

	private boolean requiresReoptimization = false;

	public AbstractTaxibusOptimizer(TaxibusOptimizerContext optimContext, boolean doUnscheduleAwaitingRequests) {
		this.optimContext = optimContext;
		this.unplannedRequests = new TreeSet<>(Requests.ABSOLUTE_COMPARATOR);
		this.doUnscheduleAwaitingRequests = doUnscheduleAwaitingRequests;

		destinationKnown = optimContext.scheduler.getParams().destinationKnown;
		vehicleDiversion = optimContext.scheduler.getParams().vehicleDiversion;
	}

	@Override
	public void notifyMobsimBeforeSimStep(@SuppressWarnings("rawtypes") MobsimBeforeSimStepEvent e) {
		if (!unplannedRequests.isEmpty()) {
			requiresReoptimization = true;
		}

		if (requiresReoptimization) {

			for (Vehicle v : optimContext.vrpData.getVehicles().values()) {
				optimContext.scheduler.updateTimeline(v.getSchedule());
			}
			if (e.getSimulationTime() % 60 == 0) {
				scheduleUnplannedRequests();
			}
			if (doUnscheduleAwaitingRequests && vehicleDiversion) {
				handleAimlessDriveTasks();
			}

			requiresReoptimization = false;
		}
	}

	protected abstract void scheduleUnplannedRequests();

	protected void handleAimlessDriveTasks() {
		optimContext.scheduler.stopAllAimlessDriveTasks();
	}

	@Override
	public void requestSubmitted(Request request) {
		unplannedRequests.add((DrtRequest)request);
		requiresReoptimization = true;
	}

	@Override
	public void nextTask(Vehicle vehicle) {
		Schedule schedule = vehicle.getSchedule();
		optimContext.scheduler.updateBeforeNextTask(schedule);

		Task newCurrentTask = schedule.nextTask();

		if (!requiresReoptimization && newCurrentTask != null) {// schedule != COMPLETED
			requiresReoptimization = doReoptimizeAfterNextTask((DrtTask)newCurrentTask);
		}
	}

	protected boolean doReoptimizeAfterNextTask(DrtTask newCurrentTask) {
		return !destinationKnown && newCurrentTask.getDrtTaskType() == DrtTaskType.DRIVE_WITH_PASSENGERS;
	}

	@Override
	public void vehicleEnteredNextLink(Vehicle vehicle, Link nextLink) {
		optimContext.scheduler.updateTimeline(vehicle.getSchedule());

		// TODO we may here possibly decide whether or not to reoptimize
		// if (delays/speedups encountered) {requiresReoptimization = true;}
	}

	protected TaxibusOptimizerContext getOptimContext() {
		return optimContext;
	}

	protected Collection<DrtRequest> getUnplannedRequests() {
		return unplannedRequests;
	}

	protected boolean isRequiresReoptimization() {
		return requiresReoptimization;
	}

	protected void setRequiresReoptimization(boolean requiresReoptimization) {
		this.requiresReoptimization = requiresReoptimization;
	}
}
