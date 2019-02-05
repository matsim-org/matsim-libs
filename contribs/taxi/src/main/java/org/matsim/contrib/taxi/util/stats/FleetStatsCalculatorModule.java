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

package org.matsim.contrib.taxi.util.stats;

import java.util.function.Function;

import org.matsim.contrib.dvrp.data.Fleet;
import org.matsim.contrib.dvrp.run.AbstractDvrpModeModule;
import org.matsim.contrib.dvrp.run.AbstractDvrpModeQSimModule;
import org.matsim.contrib.dvrp.run.ModalProviders;
import org.matsim.core.controler.listener.ControlerListener;
import org.matsim.core.mobsim.framework.events.MobsimBeforeCleanupEvent;
import org.matsim.core.mobsim.framework.listeners.MobsimBeforeCleanupListener;

import com.google.inject.Provider;

/**
 * Useful for binding multi-iteration fleet stats calculators to overcome the limitation of the QSim scope of Fleet.
 * Notifies FleetStatsCalculator (also being a controler listener) to update stats at the end of each QSim.
 *
 * @author Michal Maciejewski (michalm)
 */
public final class FleetStatsCalculatorModule<T extends FleetStatsCalculator & ControlerListener>
		extends AbstractDvrpModeModule {
	public static <T extends FleetStatsCalculator & ControlerListener> FleetStatsCalculatorModule createModule(
			String mode, Class<T> clazz, Function<ModalProviders.InstanceGetter, T> delegate) {
		return new FleetStatsCalculatorModule(mode, clazz, ModalProviders.createProvider(mode, delegate));
	}

	public static <T extends FleetStatsCalculator & ControlerListener> FleetStatsCalculatorModule createModule(
			String mode, Class<T> clazz, Provider<T> provider) {
		return new FleetStatsCalculatorModule(mode, clazz, provider);
	}

	private final Class<T> clazz;
	private final Provider<T> provider;

	private FleetStatsCalculatorModule(String mode, Class<T> clazz, Provider<T> provider) {
		super(mode);
		this.clazz = clazz;
		this.provider = provider;
	}

	@Override
	public void install() {
		bindModal(clazz).toProvider(provider).asEagerSingleton();
		addControlerListenerBinding().to(modalKey(clazz));

		installQSimModule(new AbstractDvrpModeQSimModule(getMode()) {
			@Override
			protected void configureQSim() {
				addModalQSimComponentBinding().toProvider(modalProvider(
						getter -> new MobsimBeforeCleanupNotifier(getter.getModal(clazz),
								getter.getModal(Fleet.class))));
			}
		});
	}

	private static class MobsimBeforeCleanupNotifier implements MobsimBeforeCleanupListener {
		private final FleetStatsCalculator fleetStatsCalculator;
		private final Fleet fleet;

		public MobsimBeforeCleanupNotifier(FleetStatsCalculator fleetStatsCalculator, Fleet fleet) {
			this.fleetStatsCalculator = fleetStatsCalculator;
			this.fleet = fleet;
		}

		@Override
		public void notifyMobsimBeforeCleanup(MobsimBeforeCleanupEvent e) {
			fleetStatsCalculator.updateStats(fleet);
		}
	}
}
