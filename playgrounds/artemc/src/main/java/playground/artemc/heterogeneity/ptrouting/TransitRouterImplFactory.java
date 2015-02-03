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

package playground.artemc.heterogeneity.ptrouting;

import org.matsim.pt.router.PreparedTransitSchedule;
import org.matsim.pt.router.TransitRouter;
import org.matsim.pt.router.TransitRouterConfig;
import org.matsim.pt.router.TransitRouterFactory;
import org.matsim.pt.transitSchedule.api.TransitSchedule;

/**
 * @author mrieser
 */
public class TransitRouterImplFactory implements TransitRouterFactory {

	private final TransitSchedule schedule;
	private final TransitRouterConfig config;
	private final TransitRouterNetwork routerNetwork;
	private final PreparedTransitSchedule preparedTransitSchedule;

	public TransitRouterImplFactory(final TransitSchedule schedule, final TransitRouterConfig config) {
		this.schedule = schedule;
		this.config = config;
		this.routerNetwork = TransitRouterNetwork.createFromSchedule(this.schedule, this.config.beelineWalkConnectionDistance);
		this.preparedTransitSchedule = new PreparedTransitSchedule(schedule);
	}

	@Override
	public TransitRouter createTransitRouter() {
		TransitRouterNetworkTravelTimeAndDisutilityHetero ttCalculator = new TransitRouterNetworkTravelTimeAndDisutilityHetero(this.config, this.preparedTransitSchedule);
		return new TransitRouterImpl(this.config, this.preparedTransitSchedule, this.routerNetwork, ttCalculator, ttCalculator);
	}
	
}
