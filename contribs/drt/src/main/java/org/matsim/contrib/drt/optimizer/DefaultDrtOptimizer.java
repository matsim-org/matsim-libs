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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.drt.optimizer.depot.DepotFinder;
import org.matsim.contrib.drt.optimizer.insertion.UnplannedRequestInserter;
import org.matsim.contrib.drt.optimizer.rebalancing.RebalancingParams;
import org.matsim.contrib.drt.optimizer.rebalancing.RebalancingStrategy;
import org.matsim.contrib.drt.optimizer.rebalancing.RebalancingStrategy.Relocation;
import org.matsim.contrib.drt.passenger.DrtRequest;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.drt.schedule.DrtStayTask;
import org.matsim.contrib.drt.scheduler.EmptyVehicleRelocator;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
import org.matsim.contrib.dvrp.fleet.Fleet;
import org.matsim.contrib.dvrp.optimizer.Request;
import org.matsim.contrib.dvrp.schedule.ScheduleInquiry;
import org.matsim.contrib.dvrp.schedule.ScheduleTimingUpdater;
import org.matsim.core.mobsim.framework.MobsimTimer;
import org.matsim.core.mobsim.framework.events.MobsimBeforeSimStepEvent;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ForkJoinPool;
import java.util.stream.Stream;

/**
 * @author michalm
 */
public class DefaultDrtOptimizer implements DrtOptimizer {

    private static final Logger log = LogManager.getLogger(DefaultDrtOptimizer.class);

    private final ForkJoinPool qsimScopeForkJoinPool;
    private final DrtConfigGroup drtCfg;
    private final Integer rebalancingInterval;
    private final Fleet fleet;
    private final ScheduleInquiry scheduleInquiry;
    private final ScheduleTimingUpdater scheduleTimingUpdater;
    private final RebalancingStrategy rebalancingStrategy;
    private final MobsimTimer mobsimTimer;
    private final DepotFinder depotFinder;
    private final EmptyVehicleRelocator relocator;
    private final UnplannedRequestInserter requestInserter;
    private final DrtRequestInsertionRetryQueue insertionRetryQueue;

    private final Queue<DrtRequest> unplannedRequests = new LinkedList<>();

    private final ScheduleInquiry.IdleCriteria rebalancingCriteria;

    private final ScheduleInquiry.IdleCriteria returnToDepotCriteria;

    public DefaultDrtOptimizer(QsimScopeForkJoinPool qsimScopeForkJoinPool, DrtConfigGroup drtCfg, Fleet fleet, MobsimTimer mobsimTimer, DepotFinder depotFinder,
                               RebalancingStrategy rebalancingStrategy, ScheduleInquiry scheduleInquiry, ScheduleTimingUpdater scheduleTimingUpdater,
                               EmptyVehicleRelocator relocator, UnplannedRequestInserter requestInserter, DrtRequestInsertionRetryQueue insertionRetryQueue) {
        this.qsimScopeForkJoinPool = qsimScopeForkJoinPool.getPool();
        this.drtCfg = drtCfg;
        this.fleet = fleet;
        this.mobsimTimer = mobsimTimer;
        this.depotFinder = depotFinder;
        this.rebalancingStrategy = rebalancingStrategy;
        this.scheduleInquiry = scheduleInquiry;
        this.scheduleTimingUpdater = scheduleTimingUpdater;
        this.relocator = relocator;
        this.requestInserter = requestInserter;
        this.insertionRetryQueue = insertionRetryQueue;

        Optional<RebalancingParams> rebalancingParams = drtCfg.getRebalancingParams();
        this.rebalancingInterval = rebalancingParams.map(RebalancingParams::getInterval).orElse(null);
        this.rebalancingCriteria = rebalancingParams.map(params -> new ScheduleInquiry.IdleCriteria(params.getRebalancingTimeout(), params.getRebalancingMinIdleGap())).orElse(null);
        this.returnToDepotCriteria = new ScheduleInquiry.IdleCriteria(drtCfg.getReturnToDepotTimeout(), drtCfg.getReturnToDepotMinIdleGap());
    }

	@Override
	public void notifyMobsimBeforeSimStep(@SuppressWarnings("rawtypes") MobsimBeforeSimStepEvent e) {
		boolean scheduleTimingUpdated = false;

		if (!unplannedRequests.isEmpty() || insertionRetryQueue.hasRequestsToRetryNow(e.getSimulationTime())) {
			waitForAllTimingUpdates(); // Concurrent timing updates
			scheduleTimingUpdated = true;
			requestInserter.scheduleUnplannedRequests(unplannedRequests);
		}

		relocateVehiclesToDepot(drtCfg.getReturnToDepotEvaluationInterval(), drtCfg.getReturnToDepotTimeout());

		if (rebalancingInterval != null && e.getSimulationTime() % rebalancingInterval == 0) {
			if (!scheduleTimingUpdated) {
				waitForAllTimingUpdates(); // Concurrent timing updates
			}
			rebalanceFleet();
		}
	}

	private void waitForAllTimingUpdates() {
		CompletableFuture
			.allOf(fleet.getVehicles().values().stream()
				.map(v -> CompletableFuture.runAsync(() -> scheduleTimingUpdater.updateTimings(v), qsimScopeForkJoinPool))
				.toArray(CompletableFuture[]::new))
			.join();
	}

    private void rebalanceFleet() {
        // right now we relocate only idle vehicles (vehicles that are being relocated cannot be relocated)
        Stream<? extends DvrpVehicle> rebalancableVehicles = fleet.getVehicles().values()
                .stream()
                .filter(vehicle -> scheduleInquiry.isIdle(vehicle, rebalancingCriteria));

        List<Relocation> relocations = rebalancingStrategy.calcRelocations(rebalancableVehicles, mobsimTimer.getTimeOfDay());

		if (!relocations.isEmpty()) {
			log.debug("Fleet rebalancing: #relocations=" + relocations.size());
			for (Relocation r : relocations) {
				Link currentLink = ((DrtStayTask)r.vehicle.getSchedule().getCurrentTask()).getLink();
				if (currentLink != r.link) {
					relocator.relocateVehicle(r.vehicle, r.link, EmptyVehicleRelocator.RELOCATE_VEHICLE_TASK_TYPE);
				}
			}
		}
	}

	@Override
	public void requestSubmitted(Request request) {
		unplannedRequests.add((DrtRequest)request);
	}

	@Override
	public void nextTask(DvrpVehicle vehicle) {
		scheduleTimingUpdater.updateBeforeNextTask(vehicle);
		vehicle.getSchedule().nextTask();
	}

    private void relocateVehiclesToDepot(double evaluationInterval, double timeout) {
        if (drtCfg.isIdleVehiclesReturnToDepots() && mobsimTimer.getTimeOfDay() % evaluationInterval == 0) {
            fleet.getVehicles().values()
                    .stream()
                    .filter(vehicle -> scheduleInquiry.isIdle(vehicle, returnToDepotCriteria))
                    .forEach(v -> {
                                Link depotLink = depotFinder.findDepot(v);
                                if (depotLink != null) {
                                    relocator.relocateVehicle(v, depotLink, EmptyVehicleRelocator.RELOCATE_VEHICLE_TO_DEPOT_TASK_TYPE);
                                }
                            }
                    );
        }
    }
}
