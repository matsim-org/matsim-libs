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

package playground.jbischoff.taxi.setup;

import javax.inject.Inject;

import org.apache.commons.configuration.*;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.dvrp.data.Fleet;
import org.matsim.contrib.dvrp.router.TimeAsTravelDisutility;
import org.matsim.contrib.dvrp.run.DvrpModule;
import org.matsim.contrib.dvrp.trafficmonitoring.DvrpTravelTimeModule;
import org.matsim.contrib.taxi.optimizer.*;
import org.matsim.contrib.taxi.run.TaxiConfigGroup;
import org.matsim.contrib.taxi.scheduler.*;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.router.util.*;

import com.google.inject.Provider;
import com.google.inject.name.Named;

import playground.jbischoff.taxi.inclusion.optimizer.*;

public class JbTaxiOptimizerProvider implements Provider<TaxiOptimizer> {
	public static final String TYPE = "type";

	private final TaxiConfigGroup taxiCfg;
	private final Network network;
	private final Fleet fleet;
	private final TravelTime travelTime;
	private final QSim qSim;

	@Inject
	public JbTaxiOptimizerProvider(TaxiConfigGroup taxiCfg, @Named(DvrpModule.DVRP_ROUTING) Network network,
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
		TravelDisutility travelDisutility = new TimeAsTravelDisutility(travelTime);
		TaxiScheduler scheduler = new TaxiScheduler(taxiCfg, network, fleet, qSim.getSimTimer(), schedulerParams,
				travelTime, travelDisutility);

		TaxiOptimizerContext optimContext = new TaxiOptimizerContext(fleet, network, qSim.getSimTimer(), travelTime,
				travelDisutility, scheduler);

		Configuration optimizerConfig = new MapConfiguration(taxiCfg.getOptimizerConfigGroup().getParams());
		return new InclusionRuleBasedTaxiOptimizer(optimContext,
				new InclusionRuleBasedTaxiOptimizerParams(optimizerConfig));
	}
}
