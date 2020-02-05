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

import java.util.List;

import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
import org.matsim.contrib.dvrp.fleet.Fleet;
import org.matsim.contrib.dvrp.optimizer.Request;
import org.matsim.contrib.dvrp.passenger.RequestQueue;
import org.matsim.contrib.dvrp.schedule.Task;
import org.matsim.contrib.taxi.passenger.TaxiRequest;
import org.matsim.contrib.taxi.run.TaxiConfigGroup;
import org.matsim.contrib.taxi.schedule.TaxiTask;
import org.matsim.contrib.taxi.schedule.TaxiTask.TaxiTaskType;
import org.matsim.contrib.taxi.scheduler.TaxiScheduler;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.mobsim.framework.events.MobsimBeforeSimStepEvent;

/**
 * @author michalm
 */
public class DefaultTaxiOptimizer implements TaxiOptimizer {
	private final Fleet fleet;
	private final TaxiScheduler scheduler;

	private final RequestQueue<TaxiRequest> unplannedRequests = RequestQueue.withNoAdvanceRequestPlanningHorizon();

	private final UnplannedRequestInserter requestInserter;

	private final TaxiConfigGroup taxiCfg;
	private final AbstractTaxiOptimizerParams params;

	private boolean requiresReoptimization = false;

	public DefaultTaxiOptimizer(EventsManager eventsManager, TaxiConfigGroup taxiCfg, Fleet fleet,
			TaxiScheduler scheduler, UnplannedRequestInserter requestInserter) {
		this.fleet = fleet;
		this.scheduler = scheduler;
		this.requestInserter = requestInserter;
		this.taxiCfg = taxiCfg;
		params = taxiCfg.getTaxiOptimizerParams();
	}

	@Override
	public void notifyMobsimBeforeSimStep(@SuppressWarnings("rawtypes") MobsimBeforeSimStepEvent e) {
		unplannedRequests.updateQueuesOnNextTimeSteps(e.getSimulationTime());
		requiresReoptimization |= !unplannedRequests.getSchedulableRequests().isEmpty();

		if (requiresReoptimization && isNewDecisionEpoch(e, params.getReoptimizationTimeStep())) {
			if (params.doUnscheduleAwaitingRequests) {
				unscheduleAwaitingRequests();
			}

			// TODO update timeline only if the algo really wants to reschedule in this time step,
			// perhaps by checking if there are any unplanned requests??
			if (params.doUpdateTimelines) {
				for (DvrpVehicle v : fleet.getVehicles().values()) {
					scheduler.updateTimeline(v);
				}
			}

			requestInserter.scheduleUnplannedRequests(unplannedRequests.getSchedulableRequests());

			if (params.doUnscheduleAwaitingRequests && taxiCfg.isVehicleDiversion()) {
				scheduler.stopAllAimlessDriveTasks();
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
		removedRequests.forEach(unplannedRequests::addRequest);
	}

	@Override
	public void requestSubmitted(Request request) {
		unplannedRequests.addRequest((TaxiRequest)request);
	}

	@Override
	public void nextTask(DvrpVehicle vehicle) {
		scheduler.updateBeforeNextTask(vehicle);

		Task newCurrentTask = vehicle.getSchedule().nextTask();
		if (!requiresReoptimization && newCurrentTask != null) {// schedule != COMPLETED
			requiresReoptimization = doReoptimizeAfterNextTask((TaxiTask)newCurrentTask);
		}
	}

	protected boolean doReoptimizeAfterNextTask(TaxiTask newCurrentTask) {
		return !taxiCfg.isDestinationKnown() && newCurrentTask.getTaxiTaskType() == TaxiTaskType.OCCUPIED_DRIVE;
	}

	protected void setRequiresReoptimization(boolean requiresReoptimization) {
		this.requiresReoptimization = requiresReoptimization;
	}
}
