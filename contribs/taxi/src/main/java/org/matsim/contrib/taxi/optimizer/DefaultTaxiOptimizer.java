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
import java.util.TreeSet;

import org.apache.log4j.Logger;
import org.matsim.contrib.dvrp.data.Fleet;
import org.matsim.contrib.dvrp.data.Request;
import org.matsim.contrib.dvrp.data.Vehicle;
import org.matsim.contrib.dvrp.passenger.PassengerRequests;
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
	private static final Logger log = Logger.getLogger(DefaultTaxiOptimizer.class);

	private final Fleet fleet;
	private final TaxiScheduler scheduler;

	private final Collection<TaxiRequest> unplannedRequests = new TreeSet<TaxiRequest>(
			PassengerRequests.ABSOLUTE_COMPARATOR);
	private final UnplannedRequestInserter requestInserter;

	private final boolean destinationKnown;
	private final boolean vehicleDiversion;
	private final DefaultTaxiOptimizerParams params;

	private boolean requiresReoptimization = false;

	public DefaultTaxiOptimizer(TaxiConfigGroup taxiCfg, Fleet fleet, TaxiScheduler scheduler,
			DefaultTaxiOptimizerParams params, UnplannedRequestInserter requestInserter) {
		this.fleet = fleet;
		this.scheduler = scheduler;
		this.requestInserter = requestInserter;
		this.params = params;

		destinationKnown = taxiCfg.isDestinationKnown();
		vehicleDiversion = taxiCfg.isVehicleDiversion();
	}

	@Override
	public void notifyMobsimBeforeSimStep(@SuppressWarnings("rawtypes") MobsimBeforeSimStepEvent e) {
		if (requiresReoptimization && isNewDecisionEpoch(e, params.reoptimizationTimeStep)) {
			if (params.doUnscheduleAwaitingRequests) {
				unscheduleAwaitingRequests();
			}

			// TODO update timeline only if the algo really wants to reschedule in this time step,
			// perhaps by checking if there are any unplanned requests??
			if (params.doUpdateTimelines) {
				for (Vehicle v : fleet.getVehicles().values()) {
					scheduler.updateTimeline(v);
				}
			}

			scheduleUnplannedRequests();

			if (params.doUnscheduleAwaitingRequests && vehicleDiversion) {
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

	protected void setRequiresReoptimization(boolean requiresReoptimization) {
		this.requiresReoptimization = requiresReoptimization;
	}
}
