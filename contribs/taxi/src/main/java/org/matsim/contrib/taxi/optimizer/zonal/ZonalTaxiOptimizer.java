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
import org.matsim.contrib.dvrp.data.Fleet;
import org.matsim.contrib.taxi.optimizer.rules.IdleTaxiZonalRegistry;
import org.matsim.contrib.taxi.optimizer.rules.RuleBasedTaxiOptimizer;
import org.matsim.contrib.taxi.optimizer.rules.UnplannedRequestZonalRegistry;
import org.matsim.contrib.taxi.run.TaxiConfigGroup;
import org.matsim.contrib.taxi.scheduler.TaxiScheduler;
import org.matsim.contrib.zone.SquareGridSystem;
import org.matsim.contrib.zone.ZonalSystem;
import org.matsim.core.mobsim.framework.MobsimTimer;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;

public class ZonalTaxiOptimizer extends RuleBasedTaxiOptimizer {
	public static ZonalTaxiOptimizer create(TaxiConfigGroup taxiCfg, Fleet fleet, TaxiScheduler scheduler,
			Network network, MobsimTimer timer, TravelTime travelTime, TravelDisutility travelDisutility,
			ZonalTaxiOptimizerParams params) {
		return create(taxiCfg, fleet, scheduler, network, timer, travelTime, travelDisutility, params,
				new SquareGridSystem(network, params.cellSize));
	}

	public static ZonalTaxiOptimizer create(TaxiConfigGroup taxiCfg, Fleet fleet, TaxiScheduler scheduler,
			Network network, MobsimTimer timer, TravelTime travelTime, TravelDisutility travelDisutility,
			ZonalTaxiOptimizerParams params, ZonalSystem zonalSystem) {
		IdleTaxiZonalRegistry idleTaxiRegistry = new IdleTaxiZonalRegistry(zonalSystem, scheduler);
		UnplannedRequestZonalRegistry unplannedRequestRegistry = new UnplannedRequestZonalRegistry(zonalSystem);
		ZonalRequestInserter requestInserter = new ZonalRequestInserter(fleet, scheduler, timer, network, travelTime,
				travelDisutility, params, idleTaxiRegistry, unplannedRequestRegistry);
		return new ZonalTaxiOptimizer(taxiCfg, fleet, scheduler, network, params, idleTaxiRegistry,
				unplannedRequestRegistry, requestInserter);
	}

	public ZonalTaxiOptimizer(TaxiConfigGroup taxiCfg, Fleet fleet, TaxiScheduler scheduler, Network network,
			ZonalTaxiOptimizerParams params, IdleTaxiZonalRegistry idleTaxiRegistry,
			UnplannedRequestZonalRegistry unplannedRequestRegistry, ZonalRequestInserter requestInserter) {
		super(taxiCfg, fleet, scheduler, params, idleTaxiRegistry, unplannedRequestRegistry, requestInserter);
	}
}
