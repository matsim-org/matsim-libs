/* *********************************************************************** *
 * project: org.matsim.*
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

package playground.toronto.router;

import org.matsim.api.core.v01.network.Network;
import org.matsim.pt.router.PreparedTransitSchedule;
import org.matsim.pt.router.TransitRouter;
import org.matsim.pt.router.TransitRouterConfig;
import org.matsim.pt.router.TransitRouterFactory;
import org.matsim.pt.router.TransitRouterImpl;
import org.matsim.pt.router.TransitRouterNetwork;
import org.matsim.pt.transitSchedule.api.TransitSchedule;

import playground.toronto.exceptions.NetworkFormattingException;
import playground.toronto.router.calculators.TorontoTransitRouterNetworkTravelTimeAndDisutility;
import playground.toronto.router.calculators.TransitDataCache;
import playground.toronto.router.routernetwork.TorontoTransitRouterNetworkImprovedEfficiency;

/**
 * @author pkucirek
 */
public class TorontoTransitRouterImplFactory implements TransitRouterFactory {

	private final TransitSchedule schedule;
	private final TransitRouterConfig config;
	private final TransitRouterNetwork routerNetwork;
	
	private final double busPenalty;
	private final double subwayPenalty;
	private final double streetcarPenalty;
	
	private final TransitDataCache cache;

	public TorontoTransitRouterImplFactory(final Network network, final TransitSchedule schedule, final TransitRouterConfig config, TransitDataCache cache, double bus,
			double streetcar, double subway) throws NetworkFormattingException {
		this.busPenalty = bus;
		this.streetcarPenalty = streetcar;
		this.subwayPenalty = subway;
		this.cache = cache;
		
		this.schedule = schedule;
		this.config = config;
		this.routerNetwork = TorontoTransitRouterNetworkImprovedEfficiency.createTorontoTransitRouterNetwork(network, schedule, 0.0); 
				//TransitRouterNetwork.createFromSchedule(this.schedule, this.config.beelineWalkConnectionDistance);
	}

	@Override
	public TransitRouter get() {
		TorontoTransitRouterNetworkTravelTimeAndDisutility ttCalculator = new TorontoTransitRouterNetworkTravelTimeAndDisutility(
				config, cache, busPenalty, subwayPenalty, streetcarPenalty);
		return new TransitRouterImpl(this.config, new PreparedTransitSchedule(schedule), this.routerNetwork, ttCalculator, ttCalculator);
	}
}
