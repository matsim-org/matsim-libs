/* *********************************************************************** *
 * project: org.matsim.*
 * MyTransitRouterImplFactory.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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

package org.matsim.pt.router;

import org.matsim.pt.transitSchedule.api.TransitSchedule;

/**
 * @author cdobler
 */
public class MyTransitRouterImplFactory implements TransitRouterFactory {

	private final TransitSchedule schedule;
	private final TransitRouterConfig config;
	private final TransitRouterNetwork routerNetwork;
	private final PreparedTransitSchedule preparedTransitSchedule;

	public MyTransitRouterImplFactory(final TransitSchedule schedule, final TransitRouterConfig config) {
		this.schedule = schedule;
		this.config = config;
		this.routerNetwork = TransitRouterNetwork.createFromSchedule(this.schedule, this.config.beelineWalkConnectionDistance);
		this.preparedTransitSchedule = new PreparedTransitSchedule(schedule);
	}

	@Override
	public TransitRouter createTransitRouter() {
		MyTransitRouterNetworkTravelTimeAndDisutility ttCalculator = new MyTransitRouterNetworkTravelTimeAndDisutility(this.config, this.preparedTransitSchedule);
		return new MyTransitRouterImpl(this.config, new PreparedTransitSchedule(schedule), this.routerNetwork, ttCalculator, ttCalculator);
	}
	
}
