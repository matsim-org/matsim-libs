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

import org.matsim.core.controler.AbstractModule;
import org.matsim.pt.router.TransitRouterFactory;
import playground.artemc.transitRouter.stopStopTimes.StopStopTime;
import playground.artemc.transitRouter.waitTimes.WaitTime;


public class TransitRouterEventsHeteroWSModule extends AbstractModule {

    private WaitTime waitTimes;
	private StopStopTime stopStopTimes;

	public TransitRouterEventsHeteroWSModule(WaitTime waitTimes, StopStopTime stopStopTimes) {
		this.waitTimes = waitTimes;
		this.stopStopTimes = stopStopTimes;
	}

	@Override
    public void install() {
		bindToInstance(WaitTime.class, waitTimes);
		bindToInstance(StopStopTime.class, stopStopTimes);
        bindTo(TransitRouterFactory.class, TransitRouterEventsHeteroWSFactory.class);
    }
}
