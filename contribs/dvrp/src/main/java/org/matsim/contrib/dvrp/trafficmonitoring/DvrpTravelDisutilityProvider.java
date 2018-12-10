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

import java.lang.annotation.Annotation;

import javax.inject.Inject;
import javax.inject.Provider;

import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;

import com.google.inject.Binder;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.name.Named;

/**
 * @author michalm
 */
public class DvrpTravelDisutilityProvider implements Provider<TravelDisutility> {
	public static void bindTravelDisutilityForOptimizer(Binder binder, Class<? extends Annotation> annotationType) {
		binder.bind(TravelDisutility.class)
				.annotatedWith(annotationType)
				.toProvider(new DvrpTravelDisutilityProvider(annotationType))
				.asEagerSingleton();
	}

	@Inject
	private Injector injector;

	@Inject
	@Named(DvrpTravelTimeModule.DVRP_ESTIMATED)
	private TravelTime travelTime;

	private final Class<? extends Annotation> annotationType;

	public DvrpTravelDisutilityProvider(Class<? extends Annotation> annotationType) {
		this.annotationType = annotationType;
	}

	@Override
	public TravelDisutility get() {
		return injector.getInstance(Key.get(TravelDisutilityFactory.class, annotationType))
				.createTravelDisutility(travelTime);
	}
}
