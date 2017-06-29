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

import org.apache.commons.configuration.*;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.dvrp.data.Fleet;
import org.matsim.contrib.dvrp.router.TimeAsTravelDisutility;
import org.matsim.contrib.dvrp.run.DvrpModule;
import org.matsim.contrib.dvrp.trafficmonitoring.DvrpTravelTimeModule;
import org.matsim.contrib.taxi.optimizer.assignment.*;
import org.matsim.contrib.taxi.optimizer.fifo.*;
import org.matsim.contrib.taxi.optimizer.rules.*;
import org.matsim.contrib.taxi.run.TaxiConfigGroup;
import org.matsim.contrib.taxi.scheduler.*;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.util.*;

import com.google.inject.*;
import com.google.inject.name.Named;

public class DefaultTaxiOptimizerProvider implements Provider<TaxiOptimizer> {
	public static final String TAXI_OPTIMIZER = "taxi_optimizer";
	public static final String TYPE = "type";

	public enum OptimizerType {
		ASSIGNMENT, FIFO, RULE_BASED, ZONAL;
	}

	private final TaxiConfigGroup taxiCfg;
	private final Network network;
	private final Fleet fleet;
	private final TravelTime travelTime;
	private final QSim qSim;

	@Inject(optional = true)
	private @Named(TAXI_OPTIMIZER) TravelDisutilityFactory travelDisutilityFactory;

	@Inject
	public DefaultTaxiOptimizerProvider(TaxiConfigGroup taxiCfg, @Named(DvrpModule.DVRP_ROUTING) Network network,
			Fleet fleet, @Named(DvrpTravelTimeModule.DVRP_ESTIMATED) TravelTime travelTime, QSim qSim) {
		this.taxiCfg = taxiCfg;
		this.network = network;
		this.fleet = fleet;
		this.travelTime = travelTime;
		this.qSim = qSim;
	}

	@Override
	public TaxiOptimizer get() {
		TaxiSchedulerParams schedulerParams = new TaxiSchedulerParams(taxiCfg);

		TravelDisutility travelDisutility = travelDisutilityFactory == null ? new TimeAsTravelDisutility(travelTime)
				: travelDisutilityFactory.createTravelDisutility(travelTime);

		TaxiScheduler scheduler = new TaxiScheduler(taxiCfg, network, fleet, qSim.getSimTimer(), schedulerParams,
				travelTime, travelDisutility);

		TaxiOptimizerContext optimContext = new TaxiOptimizerContext(fleet, network, qSim.getSimTimer(), travelTime,
				travelDisutility, scheduler);

		Configuration optimizerConfig = new MapConfiguration(taxiCfg.getOptimizerConfigGroup().getParams());
		OptimizerType type = OptimizerType.valueOf(optimizerConfig.getString(TYPE));

		switch (type) {
			case ASSIGNMENT:
				return new AssignmentTaxiOptimizer(optimContext, new AssignmentTaxiOptimizerParams(optimizerConfig));

			case FIFO:
				return new FifoTaxiOptimizer(optimContext, new FifoTaxiOptimizerParams(optimizerConfig));

			case RULE_BASED:
				return new RuleBasedTaxiOptimizer(optimContext, new RuleBasedTaxiOptimizerParams(optimizerConfig));

			case ZONAL:
				return new RuleBasedTaxiOptimizer(optimContext, new RuleBasedTaxiOptimizerParams(optimizerConfig));

			default:
				throw new IllegalStateException();
		}
	}
}
