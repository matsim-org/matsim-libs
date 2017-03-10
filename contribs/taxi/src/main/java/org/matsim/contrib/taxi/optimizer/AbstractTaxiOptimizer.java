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

package org.matsim.contrib.taxi.optimizer;

import java.util.*;

import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.dvrp.data.*;
import org.matsim.contrib.dvrp.schedule.Task;
import org.matsim.contrib.taxi.data.TaxiRequest;
import org.matsim.contrib.taxi.schedule.TaxiTask;
import org.matsim.contrib.taxi.schedule.TaxiTask.TaxiTaskType;
import org.matsim.core.mobsim.framework.events.MobsimBeforeSimStepEvent;

/**
 * @author michalm
 */
public abstract class AbstractTaxiOptimizer implements TaxiOptimizer {
	private final TaxiOptimizerContext optimContext;
	private final Collection<TaxiRequest> unplannedRequests;

	private final boolean doUnscheduleAwaitingRequests;// PLANNED
	private final boolean doUpdateTimelines;// PLANNED
	private final boolean destinationKnown;
	private final boolean vehicleDiversion;
	private final int reoptimizationTimeStep;

	private boolean requiresReoptimization = false;

	public AbstractTaxiOptimizer(TaxiOptimizerContext optimContext, AbstractTaxiOptimizerParams params,
			Collection<TaxiRequest> unplannedRequests, boolean doUnscheduleAwaitingRequests,
			boolean doUpdateTimelines) {
		this.optimContext = optimContext;
		this.unplannedRequests = unplannedRequests;
		this.doUnscheduleAwaitingRequests = doUnscheduleAwaitingRequests;
		this.doUpdateTimelines = doUpdateTimelines;

		destinationKnown = optimContext.scheduler.getParams().destinationKnown;
		vehicleDiversion = optimContext.scheduler.getParams().vehicleDiversion;
		reoptimizationTimeStep = params.reoptimizationTimeStep;
	}

	@Override
	public void notifyMobsimBeforeSimStep(@SuppressWarnings("rawtypes") MobsimBeforeSimStepEvent e) {
		if (requiresReoptimization && isNewDecisionEpoch(e, reoptimizationTimeStep)) {
			if (doUnscheduleAwaitingRequests) {
				unscheduleAwaitingRequests();
			}

			// TODO update timeline only if the algo really wants to reschedule in this time step,
			// perhaps by checking if there are any unplanned requests??
			if (doUpdateTimelines) {
				for (Vehicle v : optimContext.fleet.getVehicles().values()) {
					optimContext.scheduler.updateTimeline(v);
				}
			}

			scheduleUnplannedRequests();

			if (doUnscheduleAwaitingRequests && vehicleDiversion) {
				handleAimlessDriveTasks();
			}

			requiresReoptimization = false;
		}
	}

	protected boolean isNewDecisionEpoch(@SuppressWarnings("rawtypes") MobsimBeforeSimStepEvent e, int epochLength) {
		return e.getSimulationTime() % epochLength == 0;
	}

	protected void unscheduleAwaitingRequests() {
		List<TaxiRequest> removedRequests = optimContext.scheduler.removeAwaitingRequestsFromAllSchedules();
		unplannedRequests.addAll(removedRequests);
	}

	protected abstract void scheduleUnplannedRequests();

	protected void handleAimlessDriveTasks() {
		optimContext.scheduler.stopAllAimlessDriveTasks();
	}

	@Override
	public void requestSubmitted(Request request) {
		unplannedRequests.add((TaxiRequest)request);
		requiresReoptimization = true;
	}

	@Override
	public void nextTask(Vehicle vehicle) {
		optimContext.scheduler.updateBeforeNextTask(vehicle);

		Task newCurrentTask = vehicle.getSchedule().nextTask();

		if (!requiresReoptimization && newCurrentTask != null) {// schedule != COMPLETED
			requiresReoptimization = doReoptimizeAfterNextTask((TaxiTask)newCurrentTask);
		}
	}

	protected boolean doReoptimizeAfterNextTask(TaxiTask newCurrentTask) {
		return !destinationKnown && newCurrentTask.getTaxiTaskType() == TaxiTaskType.OCCUPIED_DRIVE;
	}

	@Override
	public void vehicleEnteredNextLink(Vehicle vehicle, Link nextLink) {
		// TODO do we really need this??? timeline is updated always before reoptimisation
		optimContext.scheduler.updateTimeline(vehicle);// TODO comment this out...

		// TODO we may here possibly decide whether or not to reoptimize
		// if (delays/speedups encountered) {requiresReoptimization = true;}
	}

	protected Collection<TaxiRequest> getUnplannedRequests() {
		return unplannedRequests;
	}

	protected TaxiOptimizerContext getOptimContext() {
		return optimContext;
	}

	protected boolean isRequiresReoptimization() {
		return requiresReoptimization;
	}

	protected void setRequiresReoptimization(boolean requiresReoptimization) {
		this.requiresReoptimization = requiresReoptimization;
	}
}
