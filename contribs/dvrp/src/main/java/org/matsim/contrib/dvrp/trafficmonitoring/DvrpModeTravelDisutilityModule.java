/*
 * *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2019 by the members listed in the COPYING,        *
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
 * *********************************************************************** *
 */

package org.matsim.contrib.dvrp.trafficmonitoring;

import javax.inject.Inject;

import org.matsim.contrib.dvrp.router.TimeAsTravelDisutility;
import org.matsim.contrib.dvrp.run.AbstractDvrpModeModule;
import org.matsim.contrib.dvrp.run.AbstractDvrpModeQSimModule;
import org.matsim.contrib.dvrp.run.ModalProviders;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;

import com.google.inject.name.Named;

/**
 * @author michalm
 */
public class DvrpModeTravelDisutilityModule extends AbstractDvrpModeModule {
	public static AbstractModule createWithTimeAsTravelDisutility(String mode) {
		return new DvrpModeTravelDisutilityModule(mode, TimeAsTravelDisutility::new);
	}

	private final TravelDisutilityFactory travelDisutilityFactory;

	public DvrpModeTravelDisutilityModule(String mode, TravelDisutilityFactory travelDisutilityFactory) {
		super(mode);
		this.travelDisutilityFactory = travelDisutilityFactory;
	}

	@Override
	public void install() {
		bindModal(TravelDisutilityFactory.class).toInstance(travelDisutilityFactory);

		installQSimModule(new AbstractDvrpModeQSimModule(getMode()) {
			@Override
			protected void configureQSim() {
				bindModal(TravelDisutility.class).toProvider(
						new ModalProviders.AbstractProvider<TravelDisutility>(getMode()) {

							@Inject
							@Named(DvrpTravelTimeModule.DVRP_ESTIMATED)
							private TravelTime travelTime;

							@Override
							public TravelDisutility get() {
								return getModalInstance(TravelDisutilityFactory.class).createTravelDisutility(
										travelTime);
							}
						}).asEagerSingleton();
			}
		});
	}
}
