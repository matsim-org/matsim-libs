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

package playground.sergioo.singapore2012.transitRouterVariable;

import com.google.inject.Provider;

import org.matsim.api.core.v01.network.Network;
import org.matsim.core.controler.MatsimServices;
import org.matsim.pt.router.PreparedTransitSchedule;
import org.matsim.pt.router.TransitRouter;
import org.matsim.pt.router.TransitRouterConfig;
import playground.sergioo.singapore2012.transitRouterVariable.waitTimes.WaitTime;


/**
 * Factory for the variable transit router
 * 
 * @author sergioo
 */
public class TransitRouterWWImplFactory implements Provider<TransitRouter> {

	private final TransitRouterConfig config;
	private final TransitRouterNetworkWW routerNetwork;
	private final Network network;
	private MatsimServices controler;
	private final WaitTime waitTime;
	
	public TransitRouterWWImplFactory(final MatsimServices controler, final WaitTime waitTime) {
		this.config = new TransitRouterConfig(controler.getScenario().getConfig().planCalcScore(),
				controler.getScenario().getConfig().plansCalcRoute(), controler.getScenario().getConfig().transitRouter(),
				controler.getScenario().getConfig().vspExperimental());
        this.network = controler.getScenario().getNetwork();
		this.controler = controler;
		this.waitTime = waitTime;
		routerNetwork = TransitRouterNetworkWW.createFromSchedule(network, controler.getScenario().getTransitSchedule(), this.config.getBeelineWalkConnectionDistance());
	}
	@Override
	public TransitRouter get() {
		return new TransitRouterVariableImpl(config, new TransitRouterNetworkTravelTimeAndDisutilityWW(config, network, routerNetwork, controler.getLinkTravelTimes(), waitTime, controler.getConfig().travelTimeCalculator(), controler.getConfig().qsim(), new PreparedTransitSchedule(controler.getScenario().getTransitSchedule())), routerNetwork);
	}

}
