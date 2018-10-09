/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package org.matsim.contrib.eventsBasedPTRouter;

import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.eventsBasedPTRouter.waitTimes.WaitTime;
import org.matsim.core.controler.MatsimServices;
import org.matsim.pt.router.PreparedTransitSchedule;
import org.matsim.pt.router.TransitRouter;
import org.matsim.pt.router.TransitRouterConfig;

import javax.inject.Provider;
import javax.inject.Singleton;


/**
 * Factory for the variable transit router
 * 
 * @author sergioo
 */
@Singleton
public class TransitRouterEventsWLFactory implements Provider<TransitRouterVariableImpl> {

	private final TransitRouterConfig config;
	private final TransitRouterNetworkWW routerNetwork;
	private final Network network;
	private MatsimServices controler;
	private final WaitTime waitTime;
	
	public TransitRouterEventsWLFactory(final MatsimServices controler, final WaitTime waitTime) {
		this.config = new TransitRouterConfig(controler.getScenario().getConfig().planCalcScore(),
				controler.getScenario().getConfig().plansCalcRoute(), controler.getScenario().getConfig().transitRouter(),
				controler.getScenario().getConfig().vspExperimental());
        this.network = controler.getScenario().getNetwork();
		this.controler = controler;
		this.waitTime = waitTime;
		routerNetwork = TransitRouterNetworkWW.createFromSchedule(network, controler.getScenario().getTransitSchedule(), this.config.getBeelineWalkConnectionDistance());
	}
	@Override
	public TransitRouterVariableImpl get() {
		return new TransitRouterVariableImpl(config, new TransitRouterNetworkTravelTimeAndDisutilityWW(config, network, routerNetwork, controler.getLinkTravelTimes(), waitTime, controler.getConfig().travelTimeCalculator(), controler.getConfig().qsim(), new PreparedTransitSchedule(controler.getScenario().getTransitSchedule())), routerNetwork);
	}

}
