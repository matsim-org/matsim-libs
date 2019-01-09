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

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.MapConfiguration;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.dvrp.data.Fleet;
import org.matsim.contrib.dvrp.router.DvrpRoutingNetworkProvider;
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
import org.matsim.core.mobsim.framework.MobsimTimer;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;

import com.google.inject.Provider;
import com.google.inject.name.Named;

public class DefaultTaxiOptimizerProvider implements Provider<TaxiOptimizer> {
	public static final String TYPE = "type";

	public enum OptimizerType {
		ASSIGNMENT, FIFO, RULE_BASED, ZONAL;
	}

	private final TaxiConfigGroup taxiCfg;
	private final Fleet fleet;
	private final Network network;
	private final MobsimTimer timer;
	private final TravelTime travelTime;
	private final TravelDisutility travelDisutility;
	private final TaxiScheduler scheduler;

	public DefaultTaxiOptimizerProvider(TaxiConfigGroup taxiCfg, Fleet fleet,
			@Named(DvrpRoutingNetworkProvider.DVRP_ROUTING) Network network, MobsimTimer timer,
			@Named(DvrpTravelTimeModule.DVRP_ESTIMATED) TravelTime travelTime, TravelDisutility travelDisutility,
			TaxiScheduler scheduler) {
		this.taxiCfg = taxiCfg;
		this.fleet = fleet;
		this.network = network;
		this.timer = timer;
		this.travelTime = travelTime;
		this.travelDisutility = travelDisutility;
		this.scheduler = scheduler;
	}

	@Override
	public TaxiOptimizer get() {
		Configuration optimizerConfig = new MapConfiguration(taxiCfg.getOptimizerConfigGroup().getParams());
		OptimizerType type = OptimizerType.valueOf(optimizerConfig.getString(TYPE));

		switch (type) {
			case ASSIGNMENT:
				return new AssignmentTaxiOptimizer(taxiCfg, fleet, network, timer, travelTime, travelDisutility,
						scheduler, new AssignmentTaxiOptimizerParams(optimizerConfig));

			case FIFO:
				return new FifoTaxiOptimizer(taxiCfg, fleet, network, timer, travelTime, travelDisutility, scheduler,
						new FifoTaxiOptimizerParams(optimizerConfig));

			case RULE_BASED:
				return RuleBasedTaxiOptimizer.create(taxiCfg, fleet, scheduler, network, timer, travelTime,
						travelDisutility, new RuleBasedTaxiOptimizerParams(optimizerConfig));

			case ZONAL:
				return ZonalTaxiOptimizer.create(taxiCfg, fleet, scheduler, network, timer, travelTime,
						travelDisutility, new ZonalTaxiOptimizerParams(optimizerConfig));

			default:
				throw new IllegalStateException();
		}
	}
}
