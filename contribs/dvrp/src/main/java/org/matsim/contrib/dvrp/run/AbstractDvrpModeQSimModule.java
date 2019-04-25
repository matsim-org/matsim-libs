/*
 * *********************************************************************** *
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
 * *********************************************************************** *
 */

package org.matsim.contrib.dvrp.run;

import java.util.function.Function;

import org.matsim.core.mobsim.framework.listeners.MobsimListener;
import org.matsim.core.mobsim.qsim.AbstractQSimModule;
import org.matsim.core.mobsim.qsim.components.QSimComponent;

import com.google.inject.Key;
import com.google.inject.Provider;
import com.google.inject.binder.LinkedBindingBuilder;

/**
 * @author Michal Maciejewski (michalm)
 */
public abstract class AbstractDvrpModeQSimModule extends AbstractQSimModule {
	private final String mode;

	protected AbstractDvrpModeQSimModule(String mode) {
		this.mode = mode;
	}

	protected final String getMode() {
		return mode;
	}

	protected final DvrpMode getDvrpMode() {
		return DvrpModes.mode(mode);
	}

	protected final <T> Key<T> modalKey(Class<T> type) {
		return DvrpModes.key(type, mode);
	}

	protected final <T> LinkedBindingBuilder<T> bindModal(Class<T> type) {
		return bind(modalKey(type));
	}

	/**
	 * Adding this method to AbstractDvrpModeQSimModule prevents accidentally calling
	 * AbstractModule.addMobsimListenerBinding() from the AbstractDvrpModeQSimModule.configureQSim() method.
	 * (which has happened to me at least twice, michal.mac, feb'19)
	 * <p>
	 * Normally, if an AbstractQSimModule class is an inner class of an outer AbstractModule class,
	 * addMobsimListenerBinding() will call AbstractModule.addMobsimListenerBinding(), which will have no effect (
	 * too late for adding a listener with the controller scope).
	 * <p>
	 * However, quite likely, the programmer intention is to add a listener with the mobsim/QSim scope, as if
	 * addQSimComponentBinding() or addModalQSimComponentBinding() were called.
	 * <p>
	 * Although less likely, other methods of AbstractModule could also be accidentally called. Therefore, one should
	 * consider prefixing calls with "this" (e.g. this.addModalQSimComponentBinding()) when an AbstractQSimModule
	 * is inside an AbstractModule.
	 *
	 * @throws RuntimeException
	 */
	@Deprecated
	protected final LinkedBindingBuilder<MobsimListener> addMobsimListenerBinding() {
		throw new UnsupportedOperationException(
				"Very likely you wanted to call addQSimComponentBinding() or addModalQSimComponentBinding()");
	}

	protected final LinkedBindingBuilder<QSimComponent> addModalQSimComponentBinding() {
		return addQSimComponentBinding(getDvrpMode());
	}

	protected final <T extends QSimComponent> void addModalComponent(Class<T> componentClass, Key<? extends T> key) {
		bind(componentClass).annotatedWith(getDvrpMode()).to(key).asEagerSingleton();
		addQSimComponentBinding(getDvrpMode()).to(Key.get(componentClass, getDvrpMode()));
	}

	protected final <T extends QSimComponent> void addModalComponent(Class<T> componentClass,
			Provider<T> componentProvider) {
		bind(componentClass).annotatedWith(getDvrpMode()).toProvider(componentProvider).asEagerSingleton();
		addQSimComponentBinding(getDvrpMode()).to(Key.get(componentClass, getDvrpMode()));
	}

	protected final <T extends QSimComponent> void addModalComponent(Class<T> componentClass) {
		bind(componentClass).annotatedWith(getDvrpMode()).to(componentClass).asEagerSingleton();
		addQSimComponentBinding(getDvrpMode()).to(Key.get(componentClass, getDvrpMode()));
	}

	protected final <T> Provider<T> modalProvider(Function<ModalProviders.InstanceGetter, T> delegate) {
		return ModalProviders.createProvider(mode, delegate);
	}
}
