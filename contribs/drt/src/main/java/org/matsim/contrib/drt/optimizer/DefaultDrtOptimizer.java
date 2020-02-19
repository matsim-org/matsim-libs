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

import java.util.List;
import java.util.stream.Stream;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.drt.optimizer.depot.DepotFinder;
import org.matsim.contrib.drt.optimizer.depot.Depots;
import org.matsim.contrib.drt.optimizer.insertion.UnplannedRequestInserter;
import org.matsim.contrib.drt.optimizer.rebalancing.RebalancingStrategy;
import org.matsim.contrib.drt.optimizer.rebalancing.RebalancingStrategy.Relocation;
import org.matsim.contrib.drt.optimizer.rebalancing.mincostflow.MinCostFlowRebalancingParams;
import org.matsim.contrib.drt.passenger.DrtRequest;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.drt.schedule.DrtStayTask;
import org.matsim.contrib.drt.scheduler.DrtScheduleInquiry;
import org.matsim.contrib.drt.scheduler.DrtScheduleTimingUpdater;
import org.matsim.contrib.drt.scheduler.EmptyVehicleRelocator;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
import org.matsim.contrib.dvrp.fleet.Fleet;
import org.matsim.contrib.dvrp.optimizer.Request;
import org.matsim.contrib.dvrp.passenger.RequestQueue;
import org.matsim.core.mobsim.framework.MobsimTimer;
import org.matsim.core.mobsim.framework.events.MobsimBeforeSimStepEvent;

/**
 * @author michalm
 */
public class DefaultDrtOptimizer implements DrtOptimizer {
	private static final Logger log = Logger.getLogger(DefaultDrtOptimizer.class);

	private final DrtConfigGroup drtCfg;
	private final Integer rebalancingInterval;
	private final Fleet fleet;
	private final DrtScheduleInquiry scheduleInquiry;
	private final DrtScheduleTimingUpdater scheduleTimingUpdater;
	private final RebalancingStrategy rebalancingStrategy;
	private final MobsimTimer mobsimTimer;
	private final DepotFinder depotFinder;
	private final EmptyVehicleRelocator relocator;
	private final UnplannedRequestInserter requestInserter;

	private final RequestQueue<DrtRequest> unplannedRequests;

	public DefaultDrtOptimizer(DrtConfigGroup drtCfg, Fleet fleet, MobsimTimer mobsimTimer, DepotFinder depotFinder,
			RebalancingStrategy rebalancingStrategy, DrtScheduleInquiry scheduleInquiry,
			DrtScheduleTimingUpdater scheduleTimingUpdater, EmptyVehicleRelocator relocator,
			UnplannedRequestInserter requestInserter) {
		this.drtCfg = drtCfg;
		this.fleet = fleet;
		this.mobsimTimer = mobsimTimer;
		this.depotFinder = depotFinder;
		this.rebalancingStrategy = rebalancingStrategy;
		this.scheduleInquiry = scheduleInquiry;
		this.scheduleTimingUpdater = scheduleTimingUpdater;
		this.relocator = relocator;
		this.requestInserter = requestInserter;
		rebalancingInterval = drtCfg.getMinCostFlowRebalancing()
				.map(MinCostFlowRebalancingParams::getInterval)
				.orElse(null);
		unplannedRequests = RequestQueue.withLimitedAdvanceRequestPlanningHorizon(
				drtCfg.getAdvanceRequestPlanningHorizon());
	}

	@Override
	public void notifyMobsimBeforeSimStep(@SuppressWarnings("rawtypes") MobsimBeforeSimStepEvent e) {
		unplannedRequests.updateQueuesOnNextTimeSteps(e.getSimulationTime());

		if (!unplannedRequests.getSchedulableRequests().isEmpty()) {
			for (DvrpVehicle v : fleet.getVehicles().values()) {
				scheduleTimingUpdater.updateTimings(v);
			}

			requestInserter.scheduleUnplannedRequests(unplannedRequests.getSchedulableRequests());
		}

		if (rebalancingInterval != null && e.getSimulationTime() % rebalancingInterval == 0) {
			rebalanceFleet();
		}
	}

	private void rebalanceFleet() {
		// right now we relocate only idle vehicles (vehicles that are being relocated cannot be relocated)
		Stream<? extends DvrpVehicle> rebalancableVehicles = fleet.getVehicles()
				.values()
				.stream()
				.filter(scheduleInquiry::isIdle);
		List<Relocation> relocations = rebalancingStrategy.calcRelocations(rebalancableVehicles,
				mobsimTimer.getTimeOfDay());

		if (!relocations.isEmpty()) {
			log.debug("Fleet rebalancing: #relocations=" + relocations.size());
			for (Relocation r : relocations) {
				Link currentLink = ((DrtStayTask)r.vehicle.getSchedule().getCurrentTask()).getLink();
				if (currentLink != r.link) {
					relocator.relocateVehicle(r.vehicle, r.link);
				}
			}
		}
	}

	@Override
	public void requestSubmitted(Request request) {
		unplannedRequests.addRequest((DrtRequest)request);
	}

	@Override
	public void nextTask(DvrpVehicle vehicle) {
		scheduleTimingUpdater.updateBeforeNextTask(vehicle);

		vehicle.getSchedule().nextTask();

		// if STOP->STAY then choose the best depot
		if (drtCfg.getIdleVehiclesReturnToDepots() && Depots.isSwitchingFromStopToStay(vehicle)) {
			Link depotLink = depotFinder.findDepot(vehicle);
			if (depotLink != null) {
				relocator.relocateVehicle(vehicle, depotLink);
			}
		}
	}
}
