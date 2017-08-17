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

import java.util.*;

import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.drt.data.DrtRequest;
import org.matsim.contrib.drt.optimizer.depot.Depots;
import org.matsim.contrib.drt.optimizer.rebalancing.RebalancingStrategy.Relocation;
import org.matsim.contrib.drt.passenger.events.DrtRequestRejectedEvent;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.drt.schedule.DrtStayTask;
import org.matsim.contrib.drt.scheduler.EmptyVehicleRelocator;
import org.matsim.contrib.dvrp.data.*;
import org.matsim.core.mobsim.framework.events.MobsimBeforeSimStepEvent;

import com.google.common.collect.Iterables;

/**
 * @author michalm
 */
public abstract class AbstractDrtOptimizer implements DrtOptimizer {
	private final DrtOptimizerContext optimContext;
	private final Collection<DrtRequest> unplannedRequests;
	private final EmptyVehicleRelocator relocator;
	private final int rebalancingInterval;

	private boolean requiresReoptimization = false;

	public AbstractDrtOptimizer(DrtOptimizerContext optimContext, DrtConfigGroup drtCfg,
			Collection<DrtRequest> unplannedRequests) {
		this.optimContext = optimContext;
		this.unplannedRequests = unplannedRequests;
		this.rebalancingInterval = drtCfg.getRebalancingInterval();

		relocator = new EmptyVehicleRelocator(optimContext.network, optimContext.travelTime,
				optimContext.travelDisutility, optimContext.scheduler);
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

		if (optimContext.rebalancingStrategy != null && e.getSimulationTime() % rebalancingInterval == 0) {
			rebalanceFleet();
		}
	}

	private void rebalanceFleet() {
		// right now we relocate only idle vehicles (vehicles that are being relocated cannot be relocated)
		Iterable<? extends Vehicle> rebalancableVehicles = Iterables
				.filter(getOptimContext().fleet.getVehicles().values(), optimContext.scheduler::isIdle);
		List<Relocation> relocations = optimContext.rebalancingStrategy.calcRelocations(rebalancableVehicles,getOptimContext().timer.getTimeOfDay());

		for (Relocation r : relocations) {
			Link currentLink = ((DrtStayTask)r.vehicle.getSchedule().getCurrentTask()).getLink();
			if (currentLink != r.link) {
				relocator.relocateVehicle(r.vehicle, r.link, optimContext.timer.getTimeOfDay());
			}
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

		vehicle.getSchedule().nextTask();

		// if STOP->STAY then choose the best depot
		if (optimContext.depotFinder != null && Depots.isSwitchingFromStopToStay(vehicle)) {
			Link depotLink = optimContext.depotFinder.findDepot(vehicle);
			if (depotLink != null) {
				relocator.relocateVehicle(vehicle, depotLink, optimContext.timer.getTimeOfDay());
			}
		}
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
