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
package playground.thibautd.hitchiking.run;

import java.util.List;

import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.population.routes.ModeRouteFactory;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.util.LeastCostPathCalculatorFactory;
import org.matsim.core.router.util.PersonalizableTravelTimeFactory;

import playground.thibautd.hitchiking.HitchHikingConstants;
import playground.thibautd.router.TripRouter;
import playground.thibautd.router.TripRouterFactory;

/**
 * @author thibautd
 */
public class HitchHikingTripRouterFactory extends TripRouterFactory {

	public HitchHikingTripRouterFactory(
			final Network network,
			final TravelDisutilityFactory travelCostCalculatorFactory,
			final PersonalizableTravelTimeFactory travelTimeFactory,
			final LeastCostPathCalculatorFactory leastCostPathAlgoFactory,
			final ModeRouteFactory modeRouteFactory) {
		super(network, travelCostCalculatorFactory, travelTimeFactory,
				leastCostPathAlgoFactory, modeRouteFactory);
	}

	@Override
	protected TripRouter initRouter() {
		return new HhTripRouter();
	}

	private static class HhTripRouter extends TripRouter {
		@Override
		protected String identifyMainMode( final List<PlanElement> trip ) {
			for (PlanElement pe : trip) {
				if (pe instanceof Leg) {
					String mode = ((Leg) pe).getMode();

					if ( mode.equals( HitchHikingConstants.DRIVER_MODE ) ) {
						return HitchHikingConstants.DRIVER_MODE;
					}
					else if ( mode.equals( HitchHikingConstants.PASSENGER_MODE ) ) {
						return HitchHikingConstants.PASSENGER_MODE;
					}
				}
			}

			return super.identifyMainMode( trip );
		}
	}
}

