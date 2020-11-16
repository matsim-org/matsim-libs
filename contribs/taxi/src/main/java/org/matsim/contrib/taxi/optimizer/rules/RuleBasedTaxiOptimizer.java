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

import static org.matsim.contrib.taxi.schedule.TaxiTaskBaseType.STAY;

import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
import org.matsim.contrib.dvrp.fleet.Fleet;
import org.matsim.contrib.dvrp.optimizer.Request;
import org.matsim.contrib.dvrp.schedule.Schedule;
import org.matsim.contrib.dvrp.schedule.Schedule.ScheduleStatus;
import org.matsim.contrib.dvrp.schedule.ScheduleTimingUpdater;
import org.matsim.contrib.dvrp.schedule.Schedules;
import org.matsim.contrib.dvrp.schedule.Task;
import org.matsim.contrib.taxi.optimizer.DefaultTaxiOptimizer;
import org.matsim.contrib.taxi.optimizer.UnplannedRequestInserter;
import org.matsim.contrib.taxi.passenger.TaxiRequest;
import org.matsim.contrib.taxi.run.TaxiConfigGroup;
import org.matsim.contrib.taxi.schedule.TaxiStayTask;
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
			TaxiScheduler scheduler, ScheduleTimingUpdater scheduleTimingUpdater, Network network, MobsimTimer timer,
			TravelTime travelTime, TravelDisutility travelDisutility) {
		double cellSize = ((RuleBasedTaxiOptimizerParams)taxiCfg.getTaxiOptimizerParams()).getCellSize();
		return create(eventsManager, taxiCfg, fleet, scheduler, scheduleTimingUpdater, network, timer, travelTime,
				travelDisutility, new SquareGridSystem(network.getNodes().values(), cellSize));
	}

	public static RuleBasedTaxiOptimizer create(EventsManager eventsManager, TaxiConfigGroup taxiCfg, Fleet fleet,
			TaxiScheduler scheduler, ScheduleTimingUpdater scheduleTimingUpdater, Network network, MobsimTimer timer,
			TravelTime travelTime, TravelDisutility travelDisutility, ZonalSystem zonalSystem) {
		IdleTaxiZonalRegistry idleTaxiRegistry = new IdleTaxiZonalRegistry(zonalSystem, scheduler.getScheduleInquiry());
		UnplannedRequestZonalRegistry unplannedRequestRegistry = new UnplannedRequestZonalRegistry(zonalSystem);
		RuleBasedRequestInserter requestInserter = new RuleBasedRequestInserter(scheduler, timer, network, travelTime,
				travelDisutility, ((RuleBasedTaxiOptimizerParams)taxiCfg.getTaxiOptimizerParams()), idleTaxiRegistry,
				unplannedRequestRegistry);
		return new RuleBasedTaxiOptimizer(eventsManager, taxiCfg, fleet, scheduler, scheduleTimingUpdater,
				idleTaxiRegistry, unplannedRequestRegistry, requestInserter);
	}

	private final TaxiScheduler scheduler;
	private final IdleTaxiZonalRegistry idleTaxiRegistry;
	private final UnplannedRequestZonalRegistry unplannedRequestRegistry;

	public RuleBasedTaxiOptimizer(EventsManager eventsManager, TaxiConfigGroup taxiCfg, Fleet fleet,
			TaxiScheduler scheduler, ScheduleTimingUpdater scheduleTimingUpdater,
			IdleTaxiZonalRegistry idleTaxiRegistry, UnplannedRequestZonalRegistry unplannedRequestRegistry,
			UnplannedRequestInserter requestInserter) {
		super(eventsManager, taxiCfg, fleet, scheduler, scheduleTimingUpdater, requestInserter);

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
		} else if (scheduler.getScheduleInquiry().isIdle(vehicle)) {
			idleTaxiRegistry.addVehicle(vehicle);
		} else {
			if (schedule.getCurrentTask().getTaskIdx() != 0) {// not first task
				Task previousTask = Schedules.getPreviousTask(schedule);
				if (STAY.isBaseTypeOf(previousTask)) {
					idleTaxiRegistry.removeVehicle(vehicle);
				}
			}
		}
	}

	@Override
	protected boolean doReoptimizeAfterNextTask(Task newCurrentTask) {
		return STAY.isBaseTypeOf(newCurrentTask);
	}
}
