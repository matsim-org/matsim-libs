/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
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

package org.matsim.contrib.etaxi.optimizer;

import com.google.inject.Provider;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.common.zones.ZoneSystem;
import org.matsim.contrib.common.zones.ZoneSystemUtils;
import org.matsim.contrib.dvrp.fleet.Fleet;
import org.matsim.contrib.dvrp.schedule.ScheduleTimingUpdater;
import org.matsim.contrib.etaxi.ETaxiScheduler;
import org.matsim.contrib.etaxi.optimizer.assignment.AssignmentETaxiOptimizer;
import org.matsim.contrib.etaxi.optimizer.assignment.AssignmentETaxiOptimizerParams;
import org.matsim.contrib.etaxi.optimizer.rules.RuleBasedETaxiOptimizer;
import org.matsim.contrib.etaxi.optimizer.rules.RuleBasedETaxiOptimizerParams;
import org.matsim.contrib.ev.infrastructure.ChargingInfrastructure;
import org.matsim.contrib.taxi.optimizer.BestDispatchFinder;
import org.matsim.contrib.taxi.optimizer.TaxiOptimizer;
import org.matsim.contrib.taxi.optimizer.rules.*;
import org.matsim.contrib.taxi.run.TaxiConfigGroup;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.mobsim.framework.MobsimTimer;
import org.matsim.core.router.speedy.SpeedyALTFactory;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;

public class ETaxiOptimizerProvider implements Provider<TaxiOptimizer> {
	private final EventsManager eventsManager;
	private final TaxiConfigGroup taxiCfg;
	private final Fleet fleet;
	private final Network network;
	private final MobsimTimer timer;
	private final TravelTime travelTime;
	private final TravelDisutility travelDisutility;
	private final ETaxiScheduler eScheduler;
	private final ChargingInfrastructure chargingInfrastructure;
	private final Config config;
	private final ScheduleTimingUpdater scheduleTimingUpdater;

	public ETaxiOptimizerProvider(EventsManager eventsManager, TaxiConfigGroup taxiCfg, Fleet fleet, Network network,
			MobsimTimer timer, TravelTime travelTime, TravelDisutility travelDisutility, ETaxiScheduler eScheduler,
			ScheduleTimingUpdater scheduleTimingUpdater, ChargingInfrastructure chargingInfrastructure, Config config) {
		this.eventsManager = eventsManager;
		this.taxiCfg = taxiCfg;
		this.fleet = fleet;
		this.network = network;
		this.timer = timer;
		this.travelTime = travelTime;
		this.travelDisutility = travelDisutility;
		this.eScheduler = eScheduler;
		this.scheduleTimingUpdater = scheduleTimingUpdater;
		this.chargingInfrastructure = chargingInfrastructure;
		this.config = config;
	}

	@Override
	public TaxiOptimizer get() {
		String type = taxiCfg.getTaxiOptimizerParams().getName();
		if (type.equals(RuleBasedETaxiOptimizerParams.SET_NAME)) {
			ZonalRegisters zonalRegisters = createZonalRegisters(
					((RuleBasedETaxiOptimizerParams)taxiCfg.getTaxiOptimizerParams()).getRuleBasedTaxiOptimizerParams(), config);
			BestDispatchFinder dispatchFinder = new BestDispatchFinder(eScheduler.getScheduleInquiry(), network, timer,
					travelTime, travelDisutility);
			RuleBasedRequestInserter requestInserter = new RuleBasedRequestInserter(eScheduler, timer, dispatchFinder,
					((RuleBasedETaxiOptimizerParams)taxiCfg.getTaxiOptimizerParams()).getRuleBasedTaxiOptimizerParams(),
					zonalRegisters);

			return new RuleBasedETaxiOptimizer(eventsManager, taxiCfg, fleet, eScheduler, scheduleTimingUpdater,
					chargingInfrastructure, zonalRegisters, dispatchFinder, requestInserter);
		} else if (type.equals(AssignmentETaxiOptimizerParams.SET_NAME)) {
			LeastCostPathCalculator router = new SpeedyALTFactory().createPathCalculator(network, travelDisutility,
					travelTime);
			return new AssignmentETaxiOptimizer(eventsManager, taxiCfg, fleet, timer, network, travelTime,
					travelDisutility, eScheduler, scheduleTimingUpdater, chargingInfrastructure, router);
		} else {
			throw new RuntimeException(
					"Unsupported taxi optimizer type: " + taxiCfg.getTaxiOptimizerParams().getName());
		}
	}

	private ZonalRegisters createZonalRegisters(RuleBasedTaxiOptimizerParams params, Config config) {
		ZoneSystem zoneSystem = ZoneSystemUtils.createZoneSystem(config.getContext(), network,
			params.getZoneSystemParams(), config.global().getCoordinateSystem(), zone -> true);
		IdleTaxiZonalRegistry idleTaxiRegistry = new IdleTaxiZonalRegistry(zoneSystem,
				eScheduler.getScheduleInquiry());
		UnplannedRequestZonalRegistry unplannedRequestRegistry = new UnplannedRequestZonalRegistry(zoneSystem);
		return new ZonalRegisters(idleTaxiRegistry, unplannedRequestRegistry);
	}
}
