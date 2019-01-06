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

import org.matsim.contrib.dvrp.router.TimeAsTravelDisutility;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.mobsim.qsim.AbstractQSimModule;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;

import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.name.Named;

/**
 * @author michalm
 */
public class DvrpTravelDisutilityModule extends AbstractModule {
	public static AbstractModule createWithTimeAsTravelDisutility(Class<? extends Annotation> annotationType) {
		return new DvrpTravelDisutilityModule(annotationType, TimeAsTravelDisutility::new);
	}

	private final Class<? extends Annotation> annotationType;
	private final TravelDisutilityFactory travelDisutilityFactory;

	public DvrpTravelDisutilityModule(Class<? extends Annotation> annotationType,
			TravelDisutilityFactory travelDisutilityFactory) {
		this.annotationType = annotationType;
		this.travelDisutilityFactory = travelDisutilityFactory;
	}

	@Override
	public void install() {
		bind(TravelDisutilityFactory.class).annotatedWith(annotationType).toInstance(travelDisutilityFactory);

		installQSimModule(new AbstractQSimModule() {
			@Override
			protected void configureQSim() {
				bind(TravelDisutility.class).annotatedWith(annotationType).toProvider(new Provider<TravelDisutility>() {
					@Inject
					private Injector injector;

					@Inject
					@Named(DvrpTravelTimeModule.DVRP_ESTIMATED)
					private TravelTime travelTime;

					@Override
					public TravelDisutility get() {
						return injector.getInstance(Key.get(TravelDisutilityFactory.class, annotationType))
								.createTravelDisutility(travelTime);
					}
				}).asEagerSingleton();
			}
		});
	}
}
