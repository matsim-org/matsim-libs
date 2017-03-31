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

package playground.michalm.drt.optimizer;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.dvrp.data.Fleet;
import org.matsim.contrib.dvrp.router.TimeAsTravelDisutility;
import org.matsim.contrib.dvrp.trafficmonitoring.VrpTravelTimeModules;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.filter.NetworkFilterManager;
import org.matsim.core.network.filter.NetworkLinkFilter;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.util.*;

import com.google.inject.*;
import com.google.inject.name.Named;

import playground.michalm.drt.optimizer.insertion.*;
import playground.michalm.drt.run.DrtConfigGroup;
import playground.michalm.drt.scheduler.*;

/**
 * @author michalm
 */
public class DefaultDrtOptimizerProvider implements Provider<DrtOptimizer> {
	public static final String DRT_OPTIMIZER = "drt_optimizer";

	private final Scenario scenario;
	private final DrtConfigGroup drtCfg;
	private final Fleet fleet;
	private final TravelTime travelTime;
	private final QSim qSim;
	private final Network drtNetwork;

	@Inject(optional = true)
	private @Named(DRT_OPTIMIZER) TravelDisutilityFactory travelDisutilityFactory;

	@Inject
	public DefaultDrtOptimizerProvider(Scenario scenario, DrtConfigGroup drtCfg, Fleet fleet,
			@Named(VrpTravelTimeModules.DVRP_ESTIMATED) TravelTime travelTime, QSim qSim, @Named(DrtConfigGroup.GROUP_NAME) Network drtNetwork) {
		this.scenario = scenario;
		this.drtCfg = drtCfg;
		this.fleet = fleet;
		this.travelTime = travelTime;
		this.qSim = qSim;
		this.drtNetwork = drtNetwork;
	}


	@Override
	public DrtOptimizer get() {
		DrtSchedulerParams schedulerParams = new DrtSchedulerParams(60);

		TravelDisutility travelDisutility = travelDisutilityFactory == null ? new TimeAsTravelDisutility(travelTime)
				: travelDisutilityFactory.createTravelDisutility(travelTime);

		DrtScheduler scheduler = new DrtScheduler(scenario, fleet, qSim.getSimTimer(), schedulerParams, travelTime);
		
		
		DrtOptimizerContext optimContext = new DrtOptimizerContext(fleet, drtNetwork, qSim.getSimTimer(),
				travelTime, travelDisutility, scheduler);

		return new InsertionDrtOptimizer(optimContext, drtCfg, new InsertionDrtOptimizerParams(null));
	}
	



	
	
}
