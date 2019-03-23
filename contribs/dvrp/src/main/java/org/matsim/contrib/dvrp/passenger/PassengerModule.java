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

package org.matsim.contrib.dvrp.passenger;

import org.matsim.core.controler.AbstractModule;
import org.matsim.core.mobsim.qsim.AbstractQSimModule;

import com.google.inject.multibindings.Multibinder;

/**
 * This module initialises generic (i.e. not taxi or drt-specific) AND global (not mode-specific) dvrp objects.
 * <p>
 * Some of the initialised objects will become modal at some point in the future. E.g. VehicleType or TravelTime
 * are likely to be provided separately per each mode in the future.
 *
 * @author michalm
 */
public final class PassengerModule extends AbstractModule {
	public final static String BOOKING_ENGINE_COMPONENT_NAME = "BookingEngine";

	@Override
	public void install() {
		bind(PassengerRequestEventToPassengerEngineForwarder.class).asEagerSingleton();
		addEventHandlerBinding().to(PassengerRequestEventToPassengerEngineForwarder.class);

		installQSimModule(new AbstractQSimModule() {
			@Override
			protected void configureQSim() {
				bind(BookingEngine.class).asEagerSingleton();
				addQSimComponentBinding(BOOKING_ENGINE_COMPONENT_NAME).to(BookingEngine.class);
				Multibinder.newSetBinder(this.binder(), WakeupGenerator.class)
						.addBinding()
						.to(DrtTaxiPrebookingWakeupGenerator.class)
						.asEagerSingleton();
			}
		});
	}
}
