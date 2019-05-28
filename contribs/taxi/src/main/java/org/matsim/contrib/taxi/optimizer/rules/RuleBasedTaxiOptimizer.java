/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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

package org.matsim.contrib.taxi.optimizer.rules;

import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
import org.matsim.contrib.dvrp.fleet.Fleet;
import org.matsim.contrib.dvrp.optimizer.Request;
import org.matsim.contrib.dvrp.schedule.Schedule;
import org.matsim.contrib.dvrp.schedule.Schedule.ScheduleStatus;
import org.matsim.contrib.dvrp.schedule.Schedules;
import org.matsim.contrib.taxi.optimizer.DefaultTaxiOptimizer;
import org.matsim.contrib.taxi.optimizer.UnplannedRequestInserter;
import org.matsim.contrib.taxi.passenger.TaxiRequest;
import org.matsim.contrib.taxi.run.TaxiConfigGroup;
import org.matsim.contrib.taxi.schedule.TaxiStayTask;
import org.matsim.contrib.taxi.schedule.TaxiTask;
import org.matsim.contrib.taxi.schedule.TaxiTask.TaxiTaskType;
import org.matsim.contrib.taxi.scheduler.TaxiScheduler;
import org.matsim.contrib.zone.SquareGridSystem;
import org.matsim.contrib.zone.ZonalSystem;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.mobsim.framework.MobsimTimer;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;

/**
 * @author michalm
 */
public class RuleBasedTaxiOptimizer extends DefaultTaxiOptimizer {
	public static RuleBasedTaxiOptimizer create(EventsManager eventsManager, TaxiConfigGroup taxiCfg, Fleet fleet,
			TaxiScheduler scheduler, Network network, MobsimTimer timer, TravelTime travelTime,
			TravelDisutility travelDisutility, RuleBasedTaxiOptimizerParams params) {
		return create(eventsManager, taxiCfg, fleet, scheduler, network, timer, travelTime, travelDisutility, params,
				new SquareGridSystem(network, params.getCellSize()));
	}

	public static RuleBasedTaxiOptimizer create(EventsManager eventsManager, TaxiConfigGroup taxiCfg, Fleet fleet,
			TaxiScheduler scheduler, Network network, MobsimTimer timer, TravelTime travelTime,
			TravelDisutility travelDisutility, RuleBasedTaxiOptimizerParams params, ZonalSystem zonalSystem) {
		IdleTaxiZonalRegistry idleTaxiRegistry = new IdleTaxiZonalRegistry(zonalSystem, scheduler);
		UnplannedRequestZonalRegistry unplannedRequestRegistry = new UnplannedRequestZonalRegistry(zonalSystem);
		RuleBasedRequestInserter requestInserter = new RuleBasedRequestInserter(scheduler, timer, network, travelTime,
				travelDisutility, params, idleTaxiRegistry, unplannedRequestRegistry);
		return new RuleBasedTaxiOptimizer(eventsManager, taxiCfg, fleet, scheduler, params, idleTaxiRegistry,
				unplannedRequestRegistry, requestInserter);
	}

	private final TaxiScheduler scheduler;
	private final IdleTaxiZonalRegistry idleTaxiRegistry;
	private final UnplannedRequestZonalRegistry unplannedRequestRegistry;

	public RuleBasedTaxiOptimizer(EventsManager eventsManager, TaxiConfigGroup taxiCfg, Fleet fleet,
			TaxiScheduler scheduler, RuleBasedTaxiOptimizerParams params, IdleTaxiZonalRegistry idleTaxiRegistry,
			UnplannedRequestZonalRegistry unplannedRequestRegistry, UnplannedRequestInserter requestInserter) {
		super(eventsManager, taxiCfg, fleet, scheduler, params, requestInserter);

		this.scheduler = scheduler;
		this.idleTaxiRegistry = idleTaxiRegistry;
		this.unplannedRequestRegistry = unplannedRequestRegistry;

		if (taxiCfg.isVehicleDiversion()) {
			// hmmmm, change into warning?? or even allow it (e.g. for empty taxi relocaton)??
			throw new RuntimeException("Diversion is not supported by RuleBasedTaxiOptimizer");
		}
	}

	@Override
	public void requestSubmitted(Request request) {
		super.requestSubmitted(request);
		unplannedRequestRegistry.addRequest((TaxiRequest)request);
	}

	@Override
	public void nextTask(DvrpVehicle vehicle) {
		super.nextTask(vehicle);

		Schedule schedule = vehicle.getSchedule();
		if (schedule.getStatus() == ScheduleStatus.COMPLETED) {
			TaxiStayTask lastTask = (TaxiStayTask)Schedules.getLastTask(schedule);
			if (lastTask.getBeginTime() < vehicle.getServiceEndTime()) {
				idleTaxiRegistry.removeVehicle(vehicle);
			}
		} else if (scheduler.isIdle(vehicle)) {
			idleTaxiRegistry.addVehicle(vehicle);
		} else {
			if (schedule.getCurrentTask().getTaskIdx() != 0) {// not first task
				TaxiTask previousTask = (TaxiTask)Schedules.getPreviousTask(schedule);
				if (isWaitStay(previousTask)) {
					idleTaxiRegistry.removeVehicle(vehicle);
				}
			}
		}
	}

	@Override
	protected boolean doReoptimizeAfterNextTask(TaxiTask newCurrentTask) {
		return isWaitStay(newCurrentTask);
	}

	protected boolean isWaitStay(TaxiTask task) {
		return task.getTaxiTaskType() == TaxiTaskType.STAY;
	}
}
