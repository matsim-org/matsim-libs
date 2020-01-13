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

import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.dvrp.fleet.Fleet;
import org.matsim.contrib.dvrp.trafficmonitoring.DvrpTravelTimeModule;
import org.matsim.contrib.etaxi.ETaxiScheduler;
import org.matsim.contrib.etaxi.optimizer.assignment.AssignmentETaxiOptimizer;
import org.matsim.contrib.etaxi.optimizer.assignment.AssignmentETaxiOptimizerParams;
import org.matsim.contrib.etaxi.optimizer.rules.RuleBasedETaxiOptimizer;
import org.matsim.contrib.etaxi.optimizer.rules.RuleBasedETaxiOptimizerParams;
import org.matsim.contrib.ev.infrastructure.ChargingInfrastructure;
import org.matsim.contrib.taxi.optimizer.AbstractTaxiOptimizerParams;
import org.matsim.contrib.taxi.optimizer.TaxiOptimizer;
import org.matsim.contrib.taxi.run.TaxiConfigGroup;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.mobsim.framework.MobsimTimer;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;

import com.google.inject.Provider;
import com.google.inject.name.Named;

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

	public ETaxiOptimizerProvider(EventsManager eventsManager, TaxiConfigGroup taxiCfg, Fleet fleet, Network network,
			MobsimTimer timer, @Named(DvrpTravelTimeModule.DVRP_ESTIMATED) TravelTime travelTime,
			TravelDisutility travelDisutility, ETaxiScheduler eScheduler,
			ChargingInfrastructure chargingInfrastructure) {
		this.eventsManager = eventsManager;
		this.taxiCfg = taxiCfg;
		this.fleet = fleet;
		this.network = network;
		this.timer = timer;
		this.travelTime = travelTime;
		this.travelDisutility = travelDisutility;
		this.eScheduler = eScheduler;
		this.chargingInfrastructure = chargingInfrastructure;
	}

	@Override
	public TaxiOptimizer get() {
		switch (taxiCfg.getTaxiOptimizerParams().getName()) {
			case RuleBasedETaxiOptimizerParams.SET_NAME:
				return RuleBasedETaxiOptimizer.create(eventsManager, taxiCfg, fleet, eScheduler, network, timer,
						travelTime, travelDisutility, chargingInfrastructure);
			case AssignmentETaxiOptimizerParams.SET_NAME:
				return AssignmentETaxiOptimizer.create(eventsManager, taxiCfg, fleet, network, timer, travelTime,
						travelDisutility, eScheduler, chargingInfrastructure);
		}
		throw new RuntimeException("Unsupported taxi optimizer type: " + taxiCfg.getTaxiOptimizerParams().getName());
	}

	public static AbstractTaxiOptimizerParams createParameterSet(String type) {
		switch (type) {
			case AssignmentETaxiOptimizerParams.SET_NAME:
				return new AssignmentETaxiOptimizerParams();
			case RuleBasedETaxiOptimizerParams.SET_NAME:
				return new RuleBasedETaxiOptimizerParams();
			default:
				return null;
		}
	}
}
