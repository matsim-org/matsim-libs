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

package org.matsim.contrib.ev.charging;

import org.matsim.contrib.ev.EvConfigGroup;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.controler.AbstractModule;

import com.google.inject.Inject;
import com.google.inject.Provider;

/**
 * @author Michal Maciejewski (michalm)
 */
public class ChargingModule extends AbstractModule {
	@Inject
	private EvConfigGroup evCfg;

	@Override
	public void install() {
		bind(ChargingLogic.Factory.class).toProvider(new Provider<ChargingLogic.Factory>() {
			@Inject
			private EventsManager eventsManager;

			@Override
			public ChargingLogic.Factory get() {
				return charger -> new ChargingWithQueueingLogic(charger,
						new FixedSpeedChargingStrategy(charger.getPower()), eventsManager);
			}
		});


		bind(ChargingHandler.class).asEagerSingleton();
		addMobsimListenerBinding().to(ChargingHandler.class);
	}
}
