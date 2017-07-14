/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2017 by the members listed in the COPYING,        *
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

package org.matsim.contrib.drt.optimizer;

import java.util.Collection;

import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.drt.data.DrtRequest;
import org.matsim.contrib.drt.passenger.events.DrtRequestRejectedEvent;
import org.matsim.contrib.drt.schedule.DrtTask;
import org.matsim.contrib.dvrp.data.*;
import org.matsim.contrib.dvrp.schedule.Task;
import org.matsim.core.mobsim.framework.events.MobsimBeforeSimStepEvent;

/**
 * @author michalm
 */
public abstract class AbstractDrtOptimizer implements DrtOptimizer {
	private final DrtOptimizerContext optimContext;
	private final Collection<DrtRequest> unplannedRequests;

	private boolean requiresReoptimization = false;

	public AbstractDrtOptimizer(DrtOptimizerContext optimContext, Collection<DrtRequest> unplannedRequests) {
		this.optimContext = optimContext;
		this.unplannedRequests = unplannedRequests;
	}

	@Override
	public void notifyMobsimBeforeSimStep(@SuppressWarnings("rawtypes") MobsimBeforeSimStepEvent e) {
		if (requiresReoptimization) {
			for (Vehicle v : optimContext.fleet.getVehicles().values()) {
				optimContext.scheduler.updateTimeline(v);
			}

			scheduleUnplannedRequests();
			requiresReoptimization = false;
		}
	}

	protected abstract void scheduleUnplannedRequests();

	@Override
	public void requestSubmitted(Request request) {
		DrtRequest drtRequest = (DrtRequest)request;
		if (!optimContext.requestValidator.validateDrtRequest(drtRequest)) {
			optimContext.eventsManager.processEvent(
					new DrtRequestRejectedEvent(getOptimContext().timer.getTimeOfDay(), drtRequest.getId()));
			return;
		}

		unplannedRequests.add(drtRequest);
		requiresReoptimization = true;
	}

	@Override
	public void nextTask(Vehicle vehicle) {
		optimContext.scheduler.updateBeforeNextTask(vehicle);
		
		Task newCurrentTask = vehicle.getSchedule().nextTask();
		if (!requiresReoptimization && newCurrentTask != null) {// schedule != COMPLETED
			requiresReoptimization = doReoptimizeAfterNextTask((DrtTask)newCurrentTask);
		}
	}

	protected boolean doReoptimizeAfterNextTask(DrtTask newCurrentTask) {
		return false;
	}

	@Override
	public void vehicleEnteredNextLink(Vehicle vehicle, Link nextLink) {
		// optimContext.scheduler.updateTimeline(vehicle);

		// TODO we may here possibly decide whether or not to reoptimize
		// if (delays/speedups encountered) {requiresReoptimization = true;}
	}

	protected Collection<DrtRequest> getUnplannedRequests() {
		return unplannedRequests;
	}

	protected DrtOptimizerContext getOptimContext() {
		return optimContext;
	}
}
