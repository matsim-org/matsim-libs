/* *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2018 by the members listed in the COPYING,        *
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

package org.matsim.contrib.dvrp.trafficmonitoring;

import javax.inject.Inject;
import javax.inject.Provider;

import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;

import com.google.inject.Binder;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.name.Named;
import com.google.inject.name.Names;

/**
 * @author michalm
 */
public class DvrpTravelDisutilityProvider implements Provider<TravelDisutility> {
	public static void bindTravelDisutilityForOptimizer(Binder binder, String annotation) {
		binder.bind(TravelDisutility.class).annotatedWith(Names.named(annotation))
				.toProvider(new DvrpTravelDisutilityProvider(annotation)).asEagerSingleton();
	}

	@Inject
	private Injector injector;

	@Inject
	@Named(DvrpTravelTimeModule.DVRP_ESTIMATED)
	private TravelTime travelTime;

	private final String annotation;

	public DvrpTravelDisutilityProvider(String annotation) {
		this.annotation = annotation;
	}

	@Override
	public TravelDisutility get() {
		return injector.getInstance(Key.get(TravelDisutilityFactory.class, Names.named(annotation)))
				.createTravelDisutility(travelTime);
	}
}
