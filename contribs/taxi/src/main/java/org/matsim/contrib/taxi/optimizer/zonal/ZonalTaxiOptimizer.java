/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
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

package org.matsim.contrib.taxi.optimizer.zonal;

import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.dvrp.fleet.Fleet;
import org.matsim.contrib.taxi.optimizer.rules.IdleTaxiZonalRegistry;
import org.matsim.contrib.taxi.optimizer.rules.RuleBasedTaxiOptimizer;
import org.matsim.contrib.taxi.optimizer.rules.UnplannedRequestZonalRegistry;
import org.matsim.contrib.taxi.run.TaxiConfigGroup;
import org.matsim.contrib.taxi.scheduler.TaxiScheduler;
import org.matsim.contrib.zone.SquareGridSystem;
import org.matsim.contrib.zone.ZonalSystem;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.mobsim.framework.MobsimTimer;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;

public class ZonalTaxiOptimizer extends RuleBasedTaxiOptimizer {
	public static ZonalTaxiOptimizer create(EventsManager eventsManager, TaxiConfigGroup taxiCfg, Fleet fleet,
			TaxiScheduler scheduler, Network network, MobsimTimer timer, TravelTime travelTime,
			TravelDisutility travelDisutility) {
		double cellSize = ((ZonalTaxiOptimizerParams)taxiCfg.getTaxiOptimizerParams()).getCellSize();
		return create(eventsManager, taxiCfg, fleet, scheduler, network, timer, travelTime, travelDisutility,
				new SquareGridSystem(network, cellSize));
	}

	public static ZonalTaxiOptimizer create(EventsManager eventsManager, TaxiConfigGroup taxiCfg, Fleet fleet,
			TaxiScheduler scheduler, Network network, MobsimTimer timer, TravelTime travelTime,
			TravelDisutility travelDisutility, ZonalSystem zonalSystem) {
		IdleTaxiZonalRegistry idleTaxiRegistry = new IdleTaxiZonalRegistry(zonalSystem, scheduler);
		UnplannedRequestZonalRegistry unplannedRequestRegistry = new UnplannedRequestZonalRegistry(zonalSystem);
		ZonalRequestInserter requestInserter = new ZonalRequestInserter(fleet, scheduler, timer, network, travelTime,
				travelDisutility, ((ZonalTaxiOptimizerParams)taxiCfg.getTaxiOptimizerParams()), idleTaxiRegistry,
				unplannedRequestRegistry);
		return new ZonalTaxiOptimizer(eventsManager, taxiCfg, fleet, scheduler, idleTaxiRegistry,
				unplannedRequestRegistry, requestInserter);
	}

	public ZonalTaxiOptimizer(EventsManager eventsManager, TaxiConfigGroup taxiCfg, Fleet fleet,
			TaxiScheduler scheduler, IdleTaxiZonalRegistry idleTaxiRegistry,
			UnplannedRequestZonalRegistry unplannedRequestRegistry, ZonalRequestInserter requestInserter) {
		super(eventsManager, taxiCfg, fleet, scheduler, idleTaxiRegistry, unplannedRequestRegistry, requestInserter);
	}
}
