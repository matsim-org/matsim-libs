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

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.pt.router.PreparedTransitSchedule;
import org.matsim.pt.router.TransitRouter;
import org.matsim.pt.router.TransitRouterConfig;
import org.matsim.pt.router.TransitRouterFactory;

/**
 * Factory for the variable transit router
 * 
 * @author sergioo
 */
public class TransitRouterWSImplFactory implements TransitRouterFactory {

	private final TransitRouterConfig config;
	private final TransitRouterNetworkWW routerNetwork;
	private final Network network;
	private TransitRouterNetworkTravelTimeAndDisutilityWS transitRouterNetworkTravelTimeAndDisutilityWS;
	
	public TransitRouterWSImplFactory(final TransitRouterConfig config, final Scenario scenario, final WaitTime waitTime, final StopStopTime stopStopTime) {
		this.config = config;
		this.network = scenario.getNetwork();
		routerNetwork = TransitRouterNetworkWW.createFromSchedule(network, scenario.getTransitSchedule(), this.config.beelineWalkConnectionDistance);
		transitRouterNetworkTravelTimeAndDisutilityWS = new TransitRouterNetworkTravelTimeAndDisutilityWS(config, network, routerNetwork, waitTime, stopStopTime, scenario.getConfig().travelTimeCalculator(), scenario.getConfig().getQSimConfigGroup(), new PreparedTransitSchedule(scenario.getTransitSchedule()));
	}
	@Override
	public TransitRouter createTransitRouter() {
		return new TransitRouterVariableImpl(config, transitRouterNetworkTravelTimeAndDisutilityWS, routerNetwork, network);
	}

}
