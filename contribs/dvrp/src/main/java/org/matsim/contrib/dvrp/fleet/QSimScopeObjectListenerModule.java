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

package org.matsim.contrib.dvrp.fleet;

import java.util.function.Function;

import org.matsim.contrib.dvrp.run.AbstractDvrpModeModule;
import org.matsim.contrib.dvrp.run.AbstractDvrpModeQSimModule;
import org.matsim.contrib.dvrp.run.ModalProviders;
import org.matsim.core.controler.listener.ControlerListener;
import org.matsim.core.mobsim.framework.events.MobsimBeforeCleanupEvent;
import org.matsim.core.mobsim.framework.listeners.MobsimBeforeCleanupListener;

import com.google.inject.Provider;

/**
 * Typical usecase: binding multi-iteration object stats calculators to overcome the limitation of the QSim scope of Fleet.
 * Notifies objectListener (also being a ControlerListener) to that the object has been created.
 *
 * @author Michal Maciejewski (michalm)
 */
public final class QSimScopeObjectListenerModule<T, L extends QSimScopeObjectListener<T> & ControlerListener>
		extends AbstractDvrpModeModule {
	public static <T, L extends QSimScopeObjectListener<T> & ControlerListener> QSimScopeObjectListenerModule createModule(
			String mode, Class<T> objectClass, Class<L> listenerClass,
			Function<ModalProviders.InstanceGetter, L> listenerProvider) {
		return new QSimScopeObjectListenerModule<>(mode, objectClass, listenerClass,
				ModalProviders.createProvider(mode, listenerProvider));
	}

	public static <T, L extends QSimScopeObjectListener<T> & ControlerListener> QSimScopeObjectListenerModule createModule(
			String mode, Class<T> objectClass, Class<L> listenerClass, Provider<L> listenerProvider) {
		return new QSimScopeObjectListenerModule<>(mode, objectClass, listenerClass, listenerProvider);
	}

	private final Class<T> objectClass;
	private final Class<L> listenerClass;
	private final Provider<L> listenerProvider;

	private QSimScopeObjectListenerModule(String mode, Class<T> objectClass, Class<L> listenerClass,
			Provider<L> listenerProvider) {
		super(mode);
		this.objectClass = objectClass;
		this.listenerClass = listenerClass;
		this.listenerProvider = listenerProvider;
	}

	@Override
	public void install() {
		bindModal(listenerClass).toProvider(listenerProvider).asEagerSingleton();
		addControlerListenerBinding().to(modalKey(listenerClass));

		installQSimModule(new AbstractDvrpModeQSimModule(getMode()) {
			@Override
			protected void configureQSim() {
				addModalQSimComponentBinding().toProvider(modalProvider(
						getter -> new MobsimBeforeCleanupNotifier<>(getter.getModal(listenerClass),
								getter.getModal(objectClass))));
			}
		});
	}

	//TODO this should be MobsimInitializedListener (MobsimBeforeCleanupListener is sometimes too late)
	private static class MobsimBeforeCleanupNotifier<T> implements MobsimBeforeCleanupListener {
		private final QSimScopeObjectListener<T> objectListener;
		private final T object;

		public MobsimBeforeCleanupNotifier(QSimScopeObjectListener<T> objectListener, T object) {
			this.objectListener = objectListener;
			this.object = object;
		}

		@Override
		public void notifyMobsimBeforeCleanup(MobsimBeforeCleanupEvent e) {
			objectListener.objectCreated(object);
		}
	}
}
