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

package playground.michalm.taxi.optimizer.privateAV;

import org.apache.commons.configuration.*;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.dvrp.data.Fleet;
import org.matsim.contrib.dvrp.router.TimeAsTravelDisutility;
import org.matsim.contrib.dvrp.trafficmonitoring.VrpTravelTimeModules;
import org.matsim.contrib.taxi.optimizer.*;
import org.matsim.contrib.taxi.optimizer.rules.*;
import org.matsim.contrib.taxi.run.TaxiConfigGroup;
import org.matsim.contrib.taxi.scheduler.*;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.util.*;

import com.google.inject.*;
import com.google.inject.name.Named;

public class PrivateATOptimizerProvider implements Provider<TaxiOptimizer> {
	public static final String TAXI_OPTIMIZER = "taxi_optimizer";
	public static final String TYPE = "type";

	public enum OptimizerType {
		ASSIGNMENT, FIFO, RULE_BASED, ZONAL;
	}

	private final TaxiConfigGroup taxiCfg;
	private final Scenario scenario;
	private final Fleet fleet;
	private final TravelTime travelTime;
	private final QSim qSim;

	@Inject(optional = true)
	private @Named(TAXI_OPTIMIZER) TravelDisutilityFactory travelDisutilityFactory;

	@Inject
	public PrivateATOptimizerProvider(TaxiConfigGroup taxiCfg, Scenario scenario, Fleet fleet,
			@Named(VrpTravelTimeModules.DVRP_ESTIMATED) TravelTime travelTime, QSim qSim) {
		this.taxiCfg = taxiCfg;
		this.scenario = scenario;
		this.fleet = fleet;
		this.travelTime = travelTime;
		this.qSim = qSim;
	}

	@Override
	public TaxiOptimizer get() {
		TaxiSchedulerParams schedulerParams = new TaxiSchedulerParams(taxiCfg);

		TravelDisutility travelDisutility = travelDisutilityFactory == null ? new TimeAsTravelDisutility(travelTime)
				: travelDisutilityFactory.createTravelDisutility(travelTime);

		TaxiScheduler scheduler = new TaxiScheduler(scenario, fleet, qSim.getSimTimer(), schedulerParams, travelTime,
				travelDisutility);

		TaxiOptimizerContext optimContext = new TaxiOptimizerContext(fleet, scenario.getNetwork(), qSim.getSimTimer(),
				travelTime, travelDisutility, scheduler);

		Configuration optimizerConfig = new MapConfiguration(taxiCfg.getOptimizerConfigGroup().getParams());
		return new RuleBasedTaxiOptimizer(optimContext, new RuleBasedTaxiOptimizerParams(optimizerConfig),
				new PrivateAVDispatchFinder(optimContext));
	}
}
