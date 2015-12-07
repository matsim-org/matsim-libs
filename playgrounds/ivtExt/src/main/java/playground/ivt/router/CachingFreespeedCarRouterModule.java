/* *********************************************************************** *
 * project: org.matsim.*
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
package playground.ivt.router;

import com.google.inject.Inject;
import com.google.inject.Provider;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.router.RoutingModule;
import org.matsim.core.router.TripRouter;
import org.matsim.core.router.TripRouterFactoryBuilderWithDefaults;
import org.matsim.core.router.costcalculators.FreespeedTravelTimeAndDisutility;

/**
 * @author thibautd
 */
public class CachingFreespeedCarRouterModule extends AbstractModule {
	private final TripSoftCache cache = new TripSoftCache( false , TripSoftCache.LocationType.link );

	@Override
	public void install() {
		addRoutingModuleBinding( TransportMode.car ).toProvider(
				new Provider<RoutingModule>() {
					@Inject
					Scenario sc = null;
					@Override
					public RoutingModule get() {
						final FreespeedTravelTimeAndDisutility tt = new FreespeedTravelTimeAndDisutility(sc.getConfig().planCalcScore());

						final TripRouterFactoryBuilderWithDefaults b = new TripRouterFactoryBuilderWithDefaults();
						b.setTravelTime( tt );
						b.setTravelDisutility( tt );
						final TripRouter tripRouter = b.build(sc).get();

						return new CachingRoutingModuleWrapper(
										cache,
										tripRouter.getRoutingModule(
												TransportMode.car));
					}
				} );
	}
}
