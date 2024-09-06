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

import com.google.inject.Singleton;
import org.matsim.contrib.ev.EvModule;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.mobsim.qsim.AbstractQSimModule;

import com.google.inject.Inject;
import com.google.inject.Provider;

/**
 * @author Michal Maciejewski (michalm)
 */
public class ChargingModule extends AbstractModule {
	@Override
	public void install() {
		// The following returns a charging logic for a given charger specification.  Needs to be a provider, since the eventsManager needs to be inserted.
		bind(ChargingLogic.Factory.class).toProvider(new Provider<>() {
			@Inject private EventsManager eventsManager;
			@Override public ChargingLogic.Factory get() {
				return charger -> new ChargingWithQueueingLogic(charger, new ChargeUpToMaxSocStrategy(charger, 1.), eventsManager);
			}
		});

		// The following returns the charging power/speed for a vehicle:
		bind(ChargingPower.Factory.class).toInstance(ev -> new FixedSpeedCharging(ev, 1));

		installQSimModule(new AbstractQSimModule() {
			@Override protected void configureQSim() {
				// The following binds the ChargingHandler as MobsimAfterSimstepListener.  That is what goes through the chargers and calls the charging logic.
				this.addQSimComponentBinding(EvModule.EV_COMPONENT).to(ChargingHandler.class).asEagerSingleton();
			}
		});
		// One could instead have said addMobsimListenerBinding()... .  This would have been more expressive.  But it would no longer be
		// possible to switch this on and off via EvModule.EV_COMPONENT.

//		this.addMobsimListenerBinding().to( ChargingHandler.class ).in( Singleton.class );
		// does not work since ChargingInfrastructure is not available.
	}
}
