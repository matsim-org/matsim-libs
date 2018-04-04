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
import java.util.List;
import java.util.TreeSet;
import java.util.stream.Stream;

import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.drt.data.DrtRequest;
import org.matsim.contrib.drt.data.validator.DrtRequestValidator;
import org.matsim.contrib.drt.optimizer.depot.DepotFinder;
import org.matsim.contrib.drt.optimizer.depot.Depots;
import org.matsim.contrib.drt.optimizer.insertion.UnplannedRequestInserter;
import org.matsim.contrib.drt.optimizer.rebalancing.RebalancingStrategy;
import org.matsim.contrib.drt.optimizer.rebalancing.RebalancingStrategy.Relocation;
import org.matsim.contrib.drt.passenger.events.DrtRequestRejectedEvent;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.drt.schedule.DrtStayTask;
import org.matsim.contrib.drt.scheduler.DrtScheduleTimingUpdater;
import org.matsim.contrib.drt.scheduler.DrtScheduler;
import org.matsim.contrib.drt.scheduler.EmptyVehicleRelocator;
import org.matsim.contrib.dvrp.data.Fleet;
import org.matsim.contrib.dvrp.data.Request;
import org.matsim.contrib.dvrp.data.Vehicle;
import org.matsim.contrib.dvrp.passenger.PassengerRequests;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.mobsim.framework.MobsimTimer;
import org.matsim.core.mobsim.framework.events.MobsimBeforeSimStepEvent;

import com.google.inject.Inject;

/**
 * @author michalm
 */
public class DefaultDrtOptimizer implements DrtOptimizer {
	public static final String DRT_OPTIMIZER = "drt_optimizer";

	private final DrtConfigGroup drtCfg;
	private final Fleet fleet;
	private final DrtScheduler scheduler;
	private final DrtScheduleTimingUpdater scheduleTimingUpdater;
	private final RebalancingStrategy rebalancingStrategy;
	private final MobsimTimer mobsimTimer;
	private final EventsManager eventsManager;
	private final DrtRequestValidator requestValidator;
	private final DepotFinder depotFinder;
	private final EmptyVehicleRelocator relocator;
	private final UnplannedRequestInserter requestInserter;

	private final Collection<DrtRequest> unplannedRequests = new TreeSet<DrtRequest>(
			PassengerRequests.ABSOLUTE_COMPARATOR);
	private boolean requiresReoptimization = false;

	@Inject
	public DefaultDrtOptimizer(DrtConfigGroup drtCfg, Fleet fleet, MobsimTimer mobsimTimer, EventsManager eventsManager,
			DrtRequestValidator requestValidator, DepotFinder depotFinder, RebalancingStrategy rebalancingStrategy,
			DrtScheduler scheduler, DrtScheduleTimingUpdater scheduleTimingUpdater, EmptyVehicleRelocator relocator,
			UnplannedRequestInserter requestInserter) {
		this.drtCfg = drtCfg;
		this.fleet = fleet;
		this.mobsimTimer = mobsimTimer;
		this.eventsManager = eventsManager;
		this.requestValidator = requestValidator;
		this.depotFinder = drtCfg.getIdleVehiclesReturnToDepots() ? depotFinder : null;
		this.rebalancingStrategy = drtCfg.getRebalancingInterval() != 0 ? rebalancingStrategy : null;
		this.scheduler = scheduler;
		this.scheduleTimingUpdater = scheduleTimingUpdater;
		this.relocator = relocator;
		this.requestInserter = requestInserter;
	}

	@Override
	public void notifyMobsimBeforeSimStep(@SuppressWarnings("rawtypes") MobsimBeforeSimStepEvent e) {
		if (requiresReoptimization) {
			for (Vehicle v : fleet.getVehicles().values()) {
				scheduleTimingUpdater.updateTimings(v);
			}

			requestInserter.scheduleUnplannedRequests(unplannedRequests);
			requiresReoptimization = false;
		}

		if (rebalancingStrategy != null && e.getSimulationTime() % drtCfg.getRebalancingInterval() == 0) {
			rebalanceFleet();
		}
	}

	private void rebalanceFleet() {
		// right now we relocate only idle vehicles (vehicles that are being relocated cannot be relocated)
		Stream<? extends Vehicle> rebalancableVehicles = fleet.getVehicles().values().stream()
				.filter(scheduler::isIdle);

		List<Relocation> relocations = rebalancingStrategy.calcRelocations(rebalancableVehicles,
				mobsimTimer.getTimeOfDay());

		for (Relocation r : relocations) {
			Link currentLink = ((DrtStayTask)r.vehicle.getSchedule().getCurrentTask()).getLink();
			if (currentLink != r.link) {
				relocator.relocateVehicle(r.vehicle, r.link, mobsimTimer.getTimeOfDay());
			}
		}
	}

	@Override
	public void requestSubmitted(Request request) {
		DrtRequest drtRequest = (DrtRequest)request;
		if (!requestValidator.validateDrtRequest(drtRequest)) {
			drtRequest.setRejected(true);
			eventsManager.processEvent(new DrtRequestRejectedEvent(mobsimTimer.getTimeOfDay(), drtRequest.getId()));
			return;
		}

		unplannedRequests.add(drtRequest);
		requiresReoptimization = true;
	}

	@Override
	public void nextTask(Vehicle vehicle) {
		scheduleTimingUpdater.updateBeforeNextTask(vehicle);

		vehicle.getSchedule().nextTask();

		// if STOP->STAY then choose the best depot
		if (depotFinder != null && Depots.isSwitchingFromStopToStay(vehicle)) {
			Link depotLink = depotFinder.findDepot(vehicle);
			if (depotLink != null) {
				relocator.relocateVehicle(vehicle, depotLink, mobsimTimer.getTimeOfDay());
			}
		}
	}

	@Override
	public void vehicleEnteredNextLink(Vehicle vehicle, Link nextLink) {
		// scheduler.updateTimeline(vehicle);

		// TODO we may here possibly decide whether or not to reoptimize
		// if (delays/speedups encountered) {requiresReoptimization = true;}
	}
}
