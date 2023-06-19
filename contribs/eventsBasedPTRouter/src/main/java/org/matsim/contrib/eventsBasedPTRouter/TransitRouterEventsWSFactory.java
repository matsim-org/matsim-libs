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

import com.google.inject.Inject;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.eventsBasedPTRouter.stopStopTimes.StopStopTime;
import org.matsim.contrib.eventsBasedPTRouter.waitTimes.WaitTime;
import org.matsim.pt.router.PreparedTransitSchedule;
import org.matsim.pt.router.TransitRouter;
import org.matsim.pt.router.TransitRouterConfig;

import jakarta.inject.Provider;
import jakarta.inject.Singleton;

/**
 * Factory for the variable transit router
 *
 * @author sergioo
 */
@Singleton
public class TransitRouterEventsWSFactory implements Provider<TransitRouter> {

	private final TransitRouterConfig config;
	private final TransitRouterNetworkWW routerNetwork;
	private final Scenario scenario;
	private WaitTime waitTime;

    public void setStopStopTime(StopStopTime stopStopTime) {
        this.stopStopTime = stopStopTime;
    }

    public void setWaitTime(WaitTime waitTime) {
        this.waitTime = waitTime;
    }

    private StopStopTime stopStopTime;

	@Inject
    public TransitRouterEventsWSFactory(final Scenario scenario, final WaitTime waitTime, final StopStopTime stopStopTime) {
		this.config = new TransitRouterConfig(scenario.getConfig().planCalcScore(),
				scenario.getConfig().plansCalcRoute(), scenario.getConfig().transitRouter(),
				scenario.getConfig().vspExperimental());
		routerNetwork = TransitRouterNetworkWW.createFromSchedule(scenario.getNetwork(), scenario.getTransitSchedule(), this.config.getBeelineWalkConnectionDistance());
		this.scenario = scenario;
		this.waitTime = waitTime;
		this.stopStopTime = stopStopTime;
	}
	@Override
	public TransitRouter get() {
		return new TransitRouterVariableImpl(config, new TransitRouterNetworkTravelTimeAndDisutilityWS(config, routerNetwork, waitTime, stopStopTime, scenario.getConfig().travelTimeCalculator(), scenario.getConfig().qsim(), new PreparedTransitSchedule(scenario.getTransitSchedule())), routerNetwork);
	}

}
