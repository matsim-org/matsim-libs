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

import static org.matsim.contrib.taxi.schedule.TaxiTaskBaseType.OCCUPIED_DRIVE;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.TreeSet;

import org.matsim.contrib.drt.passenger.DrtRequest;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
import org.matsim.contrib.dvrp.fleet.Fleet;
import org.matsim.contrib.dvrp.optimizer.Request;
import org.matsim.contrib.dvrp.passenger.PassengerRequest;
import org.matsim.contrib.dvrp.schedule.ScheduleTimingUpdater;
import org.matsim.contrib.dvrp.schedule.Task;
import org.matsim.contrib.taxi.run.TaxiConfigGroup;
import org.matsim.contrib.taxi.scheduler.TaxiScheduler;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.mobsim.framework.events.MobsimBeforeSimStepEvent;

/**
 * @author michalm
 */
public class DefaultTaxiOptimizer implements TaxiOptimizer {
	private final Fleet fleet;
	private final TaxiScheduler scheduler;

	private final Collection<DrtRequest> unplannedRequests = new TreeSet<>(
			Comparator.comparing(PassengerRequest::getEarliestStartTime) //
					.thenComparing(PassengerRequest::getLatestStartTime) //
					.thenComparing(Request::getSubmissionTime) //
					.thenComparing(Request::getId));

	private final UnplannedRequestInserter requestInserter;

	private final TaxiConfigGroup taxiCfg;
	private final AbstractTaxiOptimizerParams params;

	private boolean requiresReoptimization = false;
	private final ScheduleTimingUpdater scheduleTimingUpdater;

	public DefaultTaxiOptimizer(EventsManager eventsManager, TaxiConfigGroup taxiCfg, Fleet fleet,
			TaxiScheduler scheduler, ScheduleTimingUpdater scheduleTimingUpdater,
			UnplannedRequestInserter requestInserter) {
		this.fleet = fleet;
		this.scheduler = scheduler;
		this.scheduleTimingUpdater = scheduleTimingUpdater;
		this.requestInserter = requestInserter;
		this.taxiCfg = taxiCfg;
		params = taxiCfg.getTaxiOptimizerParams();
	}

	@Override
	public void notifyMobsimBeforeSimStep(@SuppressWarnings("rawtypes") MobsimBeforeSimStepEvent e) {
		requiresReoptimization |= !unplannedRequests.isEmpty();

		if (requiresReoptimization && isNewDecisionEpoch(e, params.getReoptimizationTimeStep())) {
			if (params.doUnscheduleAwaitingRequests) {
				unscheduleAwaitingRequests();
			}

			// TODO update timeline only if the algo really wants to reschedule in this time step,
			// perhaps by checking if there are any unplanned requests??
			if (params.doUpdateTimelines) {
				for (DvrpVehicle v : fleet.getVehicles().values()) {
					scheduleTimingUpdater.updateTimings(v);
				}
			}

			requestInserter.scheduleUnplannedRequests(unplannedRequests);

			if (params.doUnscheduleAwaitingRequests && taxiCfg.vehicleDiversion) {
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
		List<DrtRequest> removedRequests = scheduler.removeAwaitingRequestsFromAllSchedules();
		removedRequests.forEach(unplannedRequests::add);
	}

	@Override
	public void requestSubmitted(Request request) {
		unplannedRequests.add((DrtRequest)request);
	}

	@Override
	public void nextTask(DvrpVehicle vehicle) {
		scheduleTimingUpdater.updateBeforeNextTask(vehicle);
		scheduler.updateBeforeNextTask(vehicle);

		Task newCurrentTask = vehicle.getSchedule().nextTask();
		if (!requiresReoptimization && newCurrentTask != null) {// schedule != COMPLETED
			requiresReoptimization = doReoptimizeAfterNextTask(newCurrentTask);
		}
	}

	protected boolean doReoptimizeAfterNextTask(Task newCurrentTask) {
		return !taxiCfg.destinationKnown && OCCUPIED_DRIVE.isBaseTypeOf(newCurrentTask);
	}

	protected void setRequiresReoptimization(boolean requiresReoptimization) {
		this.requiresReoptimization = requiresReoptimization;
	}
}
