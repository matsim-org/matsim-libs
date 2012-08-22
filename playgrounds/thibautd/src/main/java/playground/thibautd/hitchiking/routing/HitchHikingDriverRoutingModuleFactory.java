/* *********************************************************************** *
 * project: org.matsim.*
 * HitchHikingDriverRoutingModuleFactory.java
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
package playground.thibautd.hitchiking.routing;

import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.gbl.MatsimRandom;

import playground.thibautd.hitchiking.HitchHikingConfigGroup;
import playground.thibautd.hitchiking.HitchHikingSpots;
import playground.thibautd.hitchiking.spotweights.SpotWeighter;
import playground.thibautd.router.RoutingModule;
import playground.thibautd.router.RoutingModuleFactory;
import playground.thibautd.router.TripRouterFactory;

/**
 * @author thibautd
 */
public class HitchHikingDriverRoutingModuleFactory implements RoutingModuleFactory {
	private final HitchHikingSpots spots;
	private final HitchHikingConfigGroup configGroup;
	private final SpotWeighter spotsWeighter;

	public HitchHikingDriverRoutingModuleFactory(
			final HitchHikingSpots spots,
			final SpotWeighter spotsWeighter,
			final HitchHikingConfigGroup configGroup) {
		this.spots = spots;
		this.configGroup = configGroup;
		this.spotsWeighter = spotsWeighter;
	}

	@Override
	public RoutingModule createModule(final String mainMode, final TripRouterFactory factory) {
		return new HitchHikingDriverRoutingModule(
				spotsWeighter,
				factory.getRoutingModuleFactories().get( TransportMode.car ).createModule( TransportMode.car , factory ),
				spots,
				configGroup,
				// XXX here or even at a higher level?
				MatsimRandom.getLocalInstance());
	}
}

