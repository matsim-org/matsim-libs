/* *********************************************************************** *
 * project: org.matsim.*
 * TransitRouterImplFactory.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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

package playground.jbischoff.pt;

import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.Config;
import org.matsim.pt.router.PreparedTransitSchedule;
import org.matsim.pt.router.TransitRouter;
import org.matsim.pt.router.TransitRouterConfig;
import org.matsim.pt.router.TransitRouterImpl;
import org.matsim.pt.router.TransitRouterNetwork;
import org.matsim.pt.router.TransitRouterNetworkTravelTimeAndDisutility;
import org.matsim.pt.transitSchedule.api.TransitSchedule;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

/**
 * @author mrieser
 */
@Singleton
public class VariableAccessTransitRouterImplFactory implements Provider<TransitRouter> {

	private final TransitRouterConfig transitRouterconfig;
	private final TransitRouterNetwork routerNetwork;
	private final PreparedTransitSchedule preparedTransitSchedule;
	private final Config config;
	private final Network network;

	@Inject
	VariableAccessTransitRouterImplFactory(final TransitSchedule schedule, final Config config, final Network network) {
		this.config = config;

		this.transitRouterconfig = new TransitRouterConfig(config.planCalcScore(),config.plansCalcRoute(),config.transitRouter(),config.vspExperimental());
		this.routerNetwork = TransitRouterNetwork.createFromSchedule(schedule, this.transitRouterconfig.getBeelineWalkConnectionDistance());
		this.preparedTransitSchedule = new PreparedTransitSchedule(schedule);
		this.network = network;
	}

	

	@Override
	public TransitRouter get() {
		VariableAccessEgressTravelDisutility variableAccessEgressTravelDisutility = new DistancebasedVariableAccessModule(network,config);
		TransitRouterNetworkTravelTimeAndDisutility ttCalculator = new TransitRouterNetworkTravelTimeAndDisutility(this.transitRouterconfig, this.preparedTransitSchedule);
		return new VariableAccessTransitRouterImpl(this.transitRouterconfig, this.preparedTransitSchedule, this.routerNetwork, ttCalculator, ttCalculator, variableAccessEgressTravelDisutility, network);
	}
	
}
