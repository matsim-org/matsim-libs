/*
 *  *********************************************************************** *
 *  * project: org.matsim.*
 *  * RandomizedTransitRouterModule.java
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  * copyright       : (C) 2015 by the members listed in the COPYING, *
 *  *                   LICENSE and WARRANTY file.                            *
 *  * email           : info at matsim dot org                                *
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  *   This program is free software; you can redistribute it and/or modify  *
 *  *   it under the terms of the GNU General Public License as published by  *
 *  *   the Free Software Foundation; either version 2 of the License, or     *
 *  *   (at your option) any later version.                                   *
 *  *   See also COPYING, LICENSE and WARRANTY file                           *
 *  *                                                                         *
 *  * ***********************************************************************
 */

package playground.artemc.transitRouter;

import org.matsim.contrib.eventsBasedPTRouter.stopStopTimes.StopStopTime;
import org.matsim.contrib.eventsBasedPTRouter.waitTimes.WaitTime;
import org.matsim.core.controler.AbstractModule;
import org.matsim.pt.router.TransitRouterFactory;


public class TransitRouterEventsHeteroWSModule extends AbstractModule {

    private WaitTime waitTimes;
	private StopStopTime stopStopTimes;

	public TransitRouterEventsHeteroWSModule(WaitTime waitTimes, StopStopTime stopStopTimes) {
		this.waitTimes = waitTimes;
		this.stopStopTimes = stopStopTimes;
	}

	@Override
    public void install() {
		bind(WaitTime.class).toInstance(waitTimes);
		bind(StopStopTime.class).toInstance(stopStopTimes);
		bind(TransitRouterFactory.class).to(TransitRouterEventsHeteroWSFactory.class);
	}
}
