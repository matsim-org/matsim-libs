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
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.events.PersonStuckEvent;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.drt.data.DrtRequest;
import org.matsim.contrib.drt.optimizer.depot.DepotFinder;
import org.matsim.contrib.drt.optimizer.depot.Depots;
import org.matsim.contrib.drt.optimizer.insertion.UnplannedRequestInserter;
import org.matsim.contrib.drt.optimizer.rebalancing.RebalancingStrategy;
import org.matsim.contrib.drt.optimizer.rebalancing.RebalancingStrategy.Relocation;
import org.matsim.contrib.drt.optimizer.rebalancing.mincostflow.MinCostFlowRebalancingParams;
import org.matsim.contrib.drt.passenger.events.DrtRequestRejectedEvent;
import org.matsim.contrib.drt.run.Drt;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.drt.schedule.DrtStayTask;
import org.matsim.contrib.drt.scheduler.DrtScheduleInquiry;
import org.matsim.contrib.drt.scheduler.DrtScheduleTimingUpdater;
import org.matsim.contrib.drt.scheduler.EmptyVehicleRelocator;
import org.matsim.contrib.dvrp.data.Fleet;
import org.matsim.contrib.dvrp.data.Request;
import org.matsim.contrib.dvrp.data.Vehicle;
import org.matsim.contrib.dvrp.passenger.PassengerRequestValidator;
import org.matsim.contrib.dvrp.passenger.PassengerRequests;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.mobsim.framework.MobsimTimer;
import org.matsim.core.mobsim.framework.events.MobsimBeforeSimStepEvent;

import com.google.inject.Inject;

/**
 * @author michalm
 */
public class DefaultDrtOptimizer implements DrtOptimizer {
	private static final Logger log = Logger.getLogger(DefaultDrtOptimizer.class);

	private final DrtConfigGroup drtCfg;
	private final MinCostFlowRebalancingParams rebalancingParams;
	private final boolean rebalancingEnabled;
	private final Fleet fleet;
	private final DrtScheduleInquiry scheduleInquiry;
	private final DrtScheduleTimingUpdater scheduleTimingUpdater;
	private final RebalancingStrategy rebalancingStrategy;
	private final MobsimTimer mobsimTimer;
	private final EventsManager eventsManager;
	private final PassengerRequestValidator requestValidator;
	private final DepotFinder depotFinder;
	private final EmptyVehicleRelocator relocator;
	private final UnplannedRequestInserter requestInserter;

	private final Collection<DrtRequest> unplannedRequests = new TreeSet<DrtRequest>(
			PassengerRequests.ABSOLUTE_COMPARATOR);
	private boolean requiresReoptimization = false;

	@Inject
	public DefaultDrtOptimizer(DrtConfigGroup drtCfg, @Drt Fleet fleet, MobsimTimer mobsimTimer,
			EventsManager eventsManager, @Drt PassengerRequestValidator requestValidator, DepotFinder depotFinder,
			RebalancingStrategy rebalancingStrategy, DrtScheduleInquiry scheduleInquiry,
			DrtScheduleTimingUpdater scheduleTimingUpdater, EmptyVehicleRelocator relocator,
			UnplannedRequestInserter requestInserter) {
		this.drtCfg = drtCfg;
		this.fleet = fleet;
		this.mobsimTimer = mobsimTimer;
		this.eventsManager = eventsManager;
		this.requestValidator = requestValidator;
		this.depotFinder = depotFinder;
		this.rebalancingStrategy = rebalancingStrategy;
		this.scheduleInquiry = scheduleInquiry;
		this.scheduleTimingUpdater = scheduleTimingUpdater;
		this.relocator = relocator;
		this.requestInserter = requestInserter;
		rebalancingParams = drtCfg.getMinCostFlowRebalancing();
		rebalancingEnabled = MinCostFlowRebalancingParams.isRebalancingEnabled(rebalancingParams);
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

		if (rebalancingEnabled && e.getSimulationTime() % rebalancingParams.getInterval() == 0) {
			rebalanceFleet();
		}
	}

	private void rebalanceFleet() {
		// right now we relocate only idle vehicles (vehicles that are being relocated cannot be relocated)
		Stream<? extends Vehicle> rebalancableVehicles = fleet.getVehicles()
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
		DrtRequest drtRequest = (DrtRequest)request;
		Set<String> violations = requestValidator.validateRequest(drtRequest);

		if (!violations.isEmpty()) {
			String causes = violations.stream().collect(Collectors.joining(", "));
			log.warn("Request " + request.getId() + " will not be served. The agent will get stuck. Causes: " + causes);
			drtRequest.setRejected(true);
			eventsManager.processEvent(
					new DrtRequestRejectedEvent(mobsimTimer.getTimeOfDay(), drtRequest.getId(), causes));
			eventsManager.processEvent(
					new PersonStuckEvent(mobsimTimer.getTimeOfDay(), drtRequest.getPassenger().getId(),
							drtRequest.getFromLink().getId(), drtRequest.getPassenger().getMode()));
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
		if (drtCfg.getIdleVehiclesReturnToDepots() && Depots.isSwitchingFromStopToStay(vehicle)) {
			Link depotLink = depotFinder.findDepot(vehicle);
			if (depotLink != null) {
				relocator.relocateVehicle(vehicle, depotLink);
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
