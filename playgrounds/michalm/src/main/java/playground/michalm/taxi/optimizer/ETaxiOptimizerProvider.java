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

package playground.michalm.taxi.optimizer;

import org.apache.commons.configuration.*;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.dvrp.data.Fleet;
import org.matsim.contrib.dvrp.router.TimeAsTravelDisutility;
import org.matsim.contrib.dvrp.run.DvrpModule;
import org.matsim.contrib.dvrp.trafficmonitoring.DvrpTravelTimeModule;
import org.matsim.contrib.taxi.optimizer.*;
import org.matsim.contrib.taxi.run.TaxiConfigGroup;
import org.matsim.contrib.taxi.scheduler.TaxiSchedulerParams;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.util.*;

import com.google.inject.*;
import com.google.inject.name.Named;

import playground.michalm.ev.data.EvData;
import playground.michalm.taxi.optimizer.assignment.*;
import playground.michalm.taxi.optimizer.rules.*;
import playground.michalm.taxi.scheduler.ETaxiScheduler;

public class ETaxiOptimizerProvider implements Provider<TaxiOptimizer> {
	public static final String TYPE = "type";

	public enum EOptimizerType {
		E_RULE_BASED, E_ASSIGNMENT;
	}

	private final TaxiConfigGroup taxiCfg;
	private final Network network;
	private final Fleet fleet;
	private final TravelTime travelTime;
	private final QSim qSim;
	private final EvData evData;

	@Inject(optional = true)
	private @Named(DefaultTaxiOptimizerProvider.TAXI_OPTIMIZER) TravelDisutilityFactory travelDisutilityFactory;

	@Inject
	public ETaxiOptimizerProvider(TaxiConfigGroup taxiCfg, @Named(DvrpModule.DVRP_ROUTING) Network network, Fleet fleet,
			@Named(DvrpTravelTimeModule.DVRP_ESTIMATED) TravelTime travelTime, QSim qSim, EvData evData) {
		this.taxiCfg = taxiCfg;
		this.network = network;
		this.fleet = fleet;
		this.travelTime = travelTime;
		this.qSim = qSim;
		this.evData = evData;
	}

	@Override
	public TaxiOptimizer get() {
		TaxiSchedulerParams schedulerParams = new TaxiSchedulerParams(taxiCfg);

		TravelDisutility travelDisutility = travelDisutilityFactory == null ? new TimeAsTravelDisutility(travelTime)
				: travelDisutilityFactory.createTravelDisutility(travelTime);

		ETaxiScheduler scheduler = new ETaxiScheduler(taxiCfg, network, fleet, qSim.getSimTimer(), schedulerParams,
				travelTime, travelDisutility);

		ETaxiOptimizerContext optimContext = new ETaxiOptimizerContext(fleet, network, qSim.getSimTimer(), travelTime,
				travelDisutility, scheduler, evData);

		Configuration optimizerConfig = new MapConfiguration(taxiCfg.getOptimizerConfigGroup().getParams());

		EOptimizerType type = EOptimizerType.valueOf(optimizerConfig.getString(TYPE));
		switch (type) {
			case E_RULE_BASED:
				return new RuleBasedETaxiOptimizer(optimContext, new RuleBasedETaxiOptimizerParams(optimizerConfig));

			case E_ASSIGNMENT:
				return new AssignmentETaxiOptimizer(optimContext, new AssignmentETaxiOptimizerParams(optimizerConfig));

			default:
				throw new RuntimeException();
		}
	}
}
