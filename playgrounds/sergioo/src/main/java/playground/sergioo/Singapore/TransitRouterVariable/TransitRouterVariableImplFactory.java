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

package playground.sergioo.Singapore.TransitRouterVariable;

import org.matsim.core.controler.Controler;
import org.matsim.pt.router.TransitRouter;
import org.matsim.pt.router.TransitRouterConfig;
import org.matsim.pt.router.TransitRouterFactory;

/**
 * Factory for the variable transit router
 * 
 * @author sergioo
 */
public class TransitRouterVariableImplFactory implements TransitRouterFactory {

	private final TransitRouterConfig config;
	private final TransitRouterNetworkWW routerNetwork;
	private TransitRouterNetworkTravelTimeAndDisutilityVariableWW transitRouterNetworkTravelTimeCostVariable;
	private Controler controler;
	private WaitTime waitTime;
	
	public TransitRouterVariableImplFactory(final Controler controler, final TransitRouterConfig config, WaitTime waitTime) {
		this.controler = controler;
		this.config = config;
		this.waitTime = waitTime;
		routerNetwork = TransitRouterNetworkWW.createFromSchedule(controler.getNetwork(), controler.getScenario().getTransitSchedule(), this.config.beelineWalkConnectionDistance);
	}
	@Override
	public TransitRouter createTransitRouter() {
		transitRouterNetworkTravelTimeCostVariable = new TransitRouterNetworkTravelTimeAndDisutilityVariableWW(config, controler.getNetwork(), routerNetwork, controler.getTravelTimeCalculator(), waitTime);
		controler.addControlerListener(transitRouterNetworkTravelTimeCostVariable);
		return new TransitRouterVariableImpl(config, transitRouterNetworkTravelTimeCostVariable, routerNetwork, controler.getNetwork());
	}

}
