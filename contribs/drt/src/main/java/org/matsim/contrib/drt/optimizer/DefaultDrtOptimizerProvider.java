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

package org.matsim.contrib.drt.optimizer;

import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.drt.data.validator.DrtRequestValidator;
import org.matsim.contrib.drt.optimizer.insertion.*;
import org.matsim.contrib.drt.optimizer.insertion.filter.*;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.drt.scheduler.*;
import org.matsim.contrib.dvrp.data.Fleet;
import org.matsim.contrib.dvrp.router.TimeAsTravelDisutility;
import org.matsim.contrib.dvrp.run.DvrpModule;
import org.matsim.contrib.dvrp.trafficmonitoring.DvrpTravelTimeModule;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.util.*;

import com.google.inject.*;
import com.google.inject.name.Named;

/**
 * @author michalm
 */
public class DefaultDrtOptimizerProvider implements Provider<DrtOptimizer> {
	public static final String DRT_OPTIMIZER = "drt_optimizer";

	private final DrtConfigGroup drtCfg;
	private final Network network;
	private final Fleet fleet;
	private final TravelTime travelTime;
	private final QSim qSim;
	private final DrtRequestValidator requestValidator;

	@Inject(optional = true)
	private @Named(DRT_OPTIMIZER) TravelDisutilityFactory travelDisutilityFactory;

	@Inject
	public DefaultDrtOptimizerProvider(DrtConfigGroup drtCfg, @Named(DvrpModule.DVRP_ROUTING) Network network,
			Fleet fleet, @Named(DvrpTravelTimeModule.DVRP_ESTIMATED) TravelTime travelTime, QSim qSim,
			DrtRequestValidator requestValidator) {
		this.drtCfg = drtCfg;
		this.network = network;
		this.fleet = fleet;
		this.travelTime = travelTime;
		this.qSim = qSim;
		this.requestValidator = requestValidator;
	}

	@Override
	public DrtOptimizer get() {
		DrtSchedulerParams schedulerParams = new DrtSchedulerParams(drtCfg.getStopDuration());
		DrtScheduler scheduler = new DrtScheduler(drtCfg, fleet, qSim.getSimTimer(), schedulerParams, travelTime);

		DrtVehicleFilter filter = drtCfg.getkNearestVehicles() > 0
				? new KNearestVehicleFilter(drtCfg.getkNearestVehicles()) : new NoFilter();

		TravelDisutility travelDisutility = travelDisutilityFactory == null ? new TimeAsTravelDisutility(travelTime)
				: travelDisutilityFactory.createTravelDisutility(travelTime);

		DrtOptimizerContext optimContext = new DrtOptimizerContext(fleet, network, qSim.getSimTimer(), travelTime,
				travelDisutility, scheduler, qSim.getEventsManager(), filter, requestValidator);

		return drtCfg.getIdleVehiclesReturnToDepots() ? new InsertionDrtOptimizerWithDepots(optimContext, drtCfg)
				: new InsertionDrtOptimizer(optimContext, drtCfg);
	}
}
