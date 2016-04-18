/* *********************************************************************** *
 * project: org.matsim.*
 * HitchHikingTripRouterFactory.java
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
import org.matsim.core.controler.MatsimServices;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.population.PopulationFactoryImpl;
import org.matsim.core.router.TripRouter;
import playground.thibautd.hitchiking.HitchHikingConfigGroup;
import playground.thibautd.hitchiking.HitchHikingConstants;
import playground.thibautd.hitchiking.HitchHikingSpots;
import playground.thibautd.hitchiking.spotweights.SpotWeighter;

/**
 * @author thibautd
 */
public class HitchHikingTripRouterFactory /* extends DefaultTripRouterFactoryImpl */ {
	private final HitchHikingSpots spots;
	private final MatsimServices controler;
	private final SpotWeighter spotWeighter;
	private final HitchHikingConfigGroup config;

	public HitchHikingTripRouterFactory(
			final MatsimServices controler,
			final HitchHikingSpots spots,
			final SpotWeighter spotWeighter,
			final HitchHikingConfigGroup config) {
		//super(controler.getScenario(), null, null);
		this.controler = controler;
		this.spotWeighter = spotWeighter;
		this.spots = spots;
		this.config = config;
	}

	// @Override
	public TripRouter instantiateAndConfigureTripRouter() {
		if (true) 
			throw new UnsupportedOperationException( "TODO: replace MainModeIdentifier in PlanRouter" );
		//TripRouter instance = super.get(iterationContext);
		TripRouter instance = null;
        instance.setRoutingModule(
				HitchHikingConstants.PASSENGER_MODE,
				new HitchHikingPassengerRoutingModule(
					instance.getRoutingModule( TransportMode.pt ),
					spots,
					((PopulationFactoryImpl) controler.getScenario().getPopulation().getFactory()).getRouteFactory(),
					spotWeighter,
					config,
					// XXX here or higher level?
					MatsimRandom.getLocalInstance()));

		instance.setRoutingModule(
				HitchHikingConstants.DRIVER_MODE,
				new HitchHikingDriverRoutingModule(
					spotWeighter,
					instance.getRoutingModule( TransportMode.car ),
					spots,
					config,
					// XXX here or even at a higher level?
					MatsimRandom.getLocalInstance()));

		return instance;
	}

		//@Override
		//protected String identifyMainMode( final List<PlanElement> trip ) {
		//	for (PlanElement pe : trip) {
		//		if (pe instanceof Leg) {
		//			String mode = ((Leg) pe).getMode();

		//			if ( mode.equals( HitchHikingConstants.DRIVER_MODE ) ) {
		//				return HitchHikingConstants.DRIVER_MODE;
		//			}
		//			else if ( mode.equals( HitchHikingConstants.PASSENGER_MODE ) ) {
		//				return HitchHikingConstants.PASSENGER_MODE;
		//			}
		//		}
		//	}

		//	return super.identifyMainMode( trip );
		//}
}

