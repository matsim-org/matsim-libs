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

package org.matsim.contrib.taxi.optimizer;

import java.net.URL;

import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.dvrp.fleet.Fleet;
import org.matsim.contrib.dvrp.trafficmonitoring.DvrpTravelTimeModule;
import org.matsim.contrib.taxi.optimizer.assignment.AssignmentTaxiOptimizer;
import org.matsim.contrib.taxi.optimizer.assignment.AssignmentTaxiOptimizerParams;
import org.matsim.contrib.taxi.optimizer.fifo.FifoTaxiOptimizer;
import org.matsim.contrib.taxi.optimizer.fifo.FifoTaxiOptimizerParams;
import org.matsim.contrib.taxi.optimizer.rules.RuleBasedTaxiOptimizer;
import org.matsim.contrib.taxi.optimizer.rules.RuleBasedTaxiOptimizerParams;
import org.matsim.contrib.taxi.optimizer.zonal.ZonalTaxiOptimizer;
import org.matsim.contrib.taxi.optimizer.zonal.ZonalTaxiOptimizerParams;
import org.matsim.contrib.taxi.run.TaxiConfigGroup;
import org.matsim.contrib.taxi.scheduler.TaxiScheduler;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.mobsim.framework.MobsimTimer;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;

import com.google.inject.Provider;
import com.google.inject.name.Named;

public class DefaultTaxiOptimizerProvider implements Provider<TaxiOptimizer> {
	private final EventsManager eventsManager;
	private final TaxiConfigGroup taxiCfg;
	private final Fleet fleet;
	private final Network network;
	private final MobsimTimer timer;
	private final TravelTime travelTime;
	private final TravelDisutility travelDisutility;
	private final TaxiScheduler scheduler;
	private final URL context;

	public DefaultTaxiOptimizerProvider(EventsManager eventsManager, TaxiConfigGroup taxiCfg, Fleet fleet,
			Network network, MobsimTimer timer, @Named(DvrpTravelTimeModule.DVRP_ESTIMATED) TravelTime travelTime,
			TravelDisutility travelDisutility, TaxiScheduler scheduler, URL context) {
		this.eventsManager = eventsManager;
		this.taxiCfg = taxiCfg;
		this.fleet = fleet;
		this.network = network;
		this.timer = timer;
		this.travelTime = travelTime;
		this.travelDisutility = travelDisutility;
		this.scheduler = scheduler;
		this.context = context;
	}

	@Override
	public TaxiOptimizer get() {
		switch (taxiCfg.getTaxiOptimizerParams().getName()) {
			case AssignmentTaxiOptimizerParams.SET_NAME:
				return new AssignmentTaxiOptimizer(eventsManager, taxiCfg, fleet, network, timer, travelTime,
						travelDisutility, scheduler);
			case FifoTaxiOptimizerParams.SET_NAME:
				return new FifoTaxiOptimizer(eventsManager, taxiCfg, fleet, network, timer, travelTime,
						travelDisutility, scheduler);
			case RuleBasedTaxiOptimizerParams.SET_NAME:
				return RuleBasedTaxiOptimizer.create(eventsManager, taxiCfg, fleet, scheduler, network, timer,
						travelTime, travelDisutility);
			case ZonalTaxiOptimizerParams.SET_NAME:
				return ZonalTaxiOptimizer.create(eventsManager, taxiCfg, fleet, scheduler, network, timer, travelTime,
						travelDisutility, context);
		}
		throw new RuntimeException("Unsupported taxi optimizer type: " + taxiCfg.getTaxiOptimizerParams().getName());
	}

	public static AbstractTaxiOptimizerParams createParameterSet(String type) {
		switch (type) {
			case AssignmentTaxiOptimizerParams.SET_NAME:
				return new AssignmentTaxiOptimizerParams();
			case FifoTaxiOptimizerParams.SET_NAME:
				return new FifoTaxiOptimizerParams();
			case RuleBasedTaxiOptimizerParams.SET_NAME:
				return new RuleBasedTaxiOptimizerParams();
			case ZonalTaxiOptimizerParams.SET_NAME:
				return new ZonalTaxiOptimizerParams();
			default:
				return null;
		}
	}
}
