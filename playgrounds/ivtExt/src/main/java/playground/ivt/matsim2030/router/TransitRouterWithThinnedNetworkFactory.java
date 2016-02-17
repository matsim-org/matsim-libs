/* *********************************************************************** *
 * project: org.matsim.*
 * TransitRouterWithThinnedNetworkFactory.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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
package playground.ivt.matsim2030.router;

import org.matsim.pt.router.*;
import org.matsim.pt.transitSchedule.api.TransitSchedule;

import javax.inject.Provider;
import javax.inject.Singleton;

/**
 * based on Alex's code based on Christoph's code...
 * @author thibautd
 */
@Singleton
public class TransitRouterWithThinnedNetworkFactory  implements Provider<TransitRouter> {
	private final TransitRouterConfig configTransit;
	private final TransitRouterNetwork routerNetwork;
	private final PreparedTransitSchedule preparedTransitSchedule;

	public TransitRouterWithThinnedNetworkFactory(
			final TransitSchedule schedule,
			final TransitRouterConfig configTransit,
			final TransitRouterNetwork transitRouterNetwork) {
		this.configTransit = configTransit;
		this.routerNetwork = transitRouterNetwork;
		this.preparedTransitSchedule = new PreparedTransitSchedule(schedule);
	}

	@Override
	public TransitRouter get() {
		TransitRouterNetworkTravelTimeAndDisutility ttCalculator = new TransitRouterNetworkTravelTimeAndDisutility(this.configTransit, this.preparedTransitSchedule);
		return new TransitRouterImpl(this.configTransit, preparedTransitSchedule , this.routerNetwork, ttCalculator, ttCalculator);
	}
	
}
