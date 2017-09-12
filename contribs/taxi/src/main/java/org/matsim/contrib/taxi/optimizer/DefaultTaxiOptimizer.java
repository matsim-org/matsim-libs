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

import java.util.Collection;
import java.util.List;

import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.dvrp.data.Fleet;
import org.matsim.contrib.dvrp.data.Request;
import org.matsim.contrib.dvrp.data.Vehicle;
import org.matsim.contrib.dvrp.schedule.Task;
import org.matsim.contrib.taxi.data.TaxiRequest;
import org.matsim.contrib.taxi.run.TaxiConfigGroup;
import org.matsim.contrib.taxi.schedule.TaxiTask;
import org.matsim.contrib.taxi.schedule.TaxiTask.TaxiTaskType;
import org.matsim.contrib.taxi.scheduler.TaxiScheduler;
import org.matsim.core.mobsim.framework.events.MobsimBeforeSimStepEvent;

/**
 * @author michalm
 */
public class DefaultTaxiOptimizer implements TaxiOptimizer {
	private final Fleet fleet;
	private final TaxiScheduler scheduler;

	private final Collection<TaxiRequest> unplannedRequests;
	private final UnplannedRequestInserter requestInserter;

	private final boolean doUnscheduleAwaitingRequests;// PLANNED
	private final boolean doUpdateTimelines;// PLANNED
	private final boolean destinationKnown;
	private final boolean vehicleDiversion;
	private final int reoptimizationTimeStep;

	private boolean requiresReoptimization = false;

	public DefaultTaxiOptimizer(TaxiConfigGroup taxiCfg, Fleet fleet, TaxiScheduler scheduler,
			AbstractTaxiOptimizerParams params, UnplannedRequestInserter requestInserter,
			Collection<TaxiRequest> unplannedRequests, boolean doUnscheduleAwaitingRequests,
			boolean doUpdateTimelines) {
		this.fleet = fleet;
		this.scheduler = scheduler;

		this.unplannedRequests = unplannedRequests;
		this.requestInserter = requestInserter;

		this.doUnscheduleAwaitingRequests = doUnscheduleAwaitingRequests;
		this.doUpdateTimelines = doUpdateTimelines;

		destinationKnown = taxiCfg.isDestinationKnown();
		vehicleDiversion = taxiCfg.isVehicleDiversion();
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
				for (Vehicle v : fleet.getVehicles().values()) {
					scheduler.updateTimeline(v);
				}
			}

			scheduleUnplannedRequests();

			if (doUnscheduleAwaitingRequests && vehicleDiversion) {
				handleAimlessDriveTasks();
			}

			requiresReoptimization = false;
		}
	}

	public static boolean isNewDecisionEpoch(@SuppressWarnings("rawtypes") MobsimBeforeSimStepEvent e,
			int epochLength) {
		return e.getSimulationTime() % epochLength == 0;
	}

	protected void unscheduleAwaitingRequests() {
		List<TaxiRequest> removedRequests = scheduler.removeAwaitingRequestsFromAllSchedules();
		unplannedRequests.addAll(removedRequests);
	}

	protected void scheduleUnplannedRequests() {
		requestInserter.scheduleUnplannedRequests(unplannedRequests);
	}

	protected void handleAimlessDriveTasks() {
		scheduler.stopAllAimlessDriveTasks();
	}

	@Override
	public void requestSubmitted(Request request) {
		unplannedRequests.add((TaxiRequest)request);
		requiresReoptimization = true;
	}

	@Override
	public void nextTask(Vehicle vehicle) {
		scheduler.updateBeforeNextTask(vehicle);

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
		scheduler.updateTimeline(vehicle);// TODO comment this out...

		// TODO we may here possibly decide whether or not to reoptimize
		// if (delays/speedups encountered) {requiresReoptimization = true;}
	}

	protected void setRequiresReoptimization(boolean requiresReoptimization) {
		this.requiresReoptimization = requiresReoptimization;
	}
}
