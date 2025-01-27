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

import org.matsim.contrib.ev.EvModule;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.mobsim.qsim.AbstractQSimModule;

import com.google.inject.Provides;
import com.google.inject.Singleton;

/**
 * @author Michal Maciejewski (michalm)
 */
public class ChargingModule extends AbstractModule {
	@Override
	public void install() {
		// By default, charging logic with queue is used
		bind(ChargingLogic.Factory.class).to(ChargingWithQueueingLogic.Factory.class);

		// By default, charging strategy that chargers to 100% is used
		bind(ChargingStrategy.Factory.class).toInstance(new ChargeUpToMaxSocStrategy.Factory(1.0));

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

		// standard charging priority for all chargers
		bind(ChargingPriority.Factory.class).toInstance(ChargingPriority.FIFO);
	}

	@Provides @Singleton
	ChargingWithQueueingLogic.Factory provideChargingWithQueueingLogicFactory(EventsManager eventsManager, ChargingPriority.Factory chargingPriorityFactory) {
		return new ChargingWithQueueingLogic.Factory(eventsManager, chargingPriorityFactory);
	}

	@Provides @Singleton
	ChargingWithQueueingAndAssignmentLogic.Factory provideChargingWithQueueingAndAssignmentLogicFactory(EventsManager eventsManager, ChargingPriority.Factory chargingPriorityFactory) {
		return new ChargingWithQueueingAndAssignmentLogic.Factory(eventsManager, chargingPriorityFactory);
	}
}
