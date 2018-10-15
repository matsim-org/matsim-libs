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
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.events.PersonStuckEvent;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.dvrp.data.Fleet;
import org.matsim.contrib.dvrp.data.Request;
import org.matsim.contrib.dvrp.data.Vehicle;
import org.matsim.contrib.dvrp.passenger.PassengerRequests;
import org.matsim.contrib.dvrp.schedule.Task;
import org.matsim.contrib.taxi.data.TaxiRequest;
import org.matsim.contrib.taxi.data.validator.TaxiRequestValidator;
import org.matsim.contrib.taxi.passenger.TaxiRequestRejectedEvent;
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
	private static final Logger log = Logger.getLogger(DefaultTaxiOptimizer.class);

	private final Fleet fleet;
	private final TaxiScheduler scheduler;

	private final Collection<TaxiRequest> unplannedRequests = new TreeSet<TaxiRequest>(
			PassengerRequests.ABSOLUTE_COMPARATOR);
	private final UnplannedRequestInserter requestInserter;

	private final boolean destinationKnown;
	private final boolean vehicleDiversion;
	private final DefaultTaxiOptimizerParams params;
	
	private final EventsManager eventsManager;
	private final TaxiRequestValidator requestValidator;
	private final boolean printDetailedWarnings;

	private boolean requiresReoptimization = false;

	public DefaultTaxiOptimizer(TaxiConfigGroup taxiCfg, Fleet fleet, TaxiScheduler scheduler,
			DefaultTaxiOptimizerParams params, UnplannedRequestInserter requestInserter,
			TaxiRequestValidator requestValidator, EventsManager eventsManager) {
		this.fleet = fleet;
		this.scheduler = scheduler;
		this.requestInserter = requestInserter;
		this.params = params;
		this.requestValidator = requestValidator;
		this.eventsManager = eventsManager;

		destinationKnown = taxiCfg.isDestinationKnown();
		vehicleDiversion = taxiCfg.isVehicleDiversion();
		this.printDetailedWarnings = taxiCfg.isPrintDetailedWarnings();
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
		TaxiRequest taxiRequest = (TaxiRequest)request;
		Set<String> violations = requestValidator.validateTaxiRequest(taxiRequest);
		
		if (!violations.isEmpty()) {
			String causes = violations.stream().collect(Collectors.joining(", "));
			if (printDetailedWarnings) log.warn("Request " + request.getId() + " will not be served. The agent will get stuck. Causes: " + causes);
			taxiRequest.setRejected(true);
			eventsManager.processEvent(
					new TaxiRequestRejectedEvent(taxiRequest.getSubmissionTime(), taxiRequest.getId(), causes));
			eventsManager.processEvent(
					new PersonStuckEvent(taxiRequest.getSubmissionTime(), taxiRequest.getPassenger().getId(),
							taxiRequest.getFromLink().getId(), taxiRequest.getPassenger().getMode()));
			return;
		}

		unplannedRequests.add(taxiRequest);
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
