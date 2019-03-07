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
import org.matsim.core.mobsim.framework.listeners.MobsimInitializedListener;

import com.google.inject.Provider;

/**
 * Typical usecase: binding multi-iteration object stats calculators to overcome the limitation of the QSim scope of Fleet.
 * Notifies objectListener (also being a ControlerListener) to that the object has been created.
 *
 * @author Michal Maciejewski (michalm)
 */
public final class QSimScopeObjectListenerModule<T, L extends QSimScopeObjectListener<T> & ControlerListener>
		extends AbstractDvrpModeModule {

	private final Class<T> objectClass;
	private final Class<L> listenerClass;
	private final Provider<L> listenerProvider;

	private QSimScopeObjectListenerModule(Builder<T, L> builder) {
		super(builder.mode);
		objectClass = builder.objectClass;
		listenerClass = builder.listenerClass;
		listenerProvider = builder.listenerProvider;
	}

	@Override
	public void install() {
		bindModal(listenerClass).toProvider(listenerProvider).asEagerSingleton();
		addControlerListenerBinding().to(modalKey(listenerClass));

		installQSimModule(new AbstractDvrpModeQSimModule(getMode()) {
			@Override
			protected void configureQSim() {
				addModalQSimComponentBinding().toProvider(modalProvider(
						getter -> mobsimInitializedListener(getter.getModal(listenerClass),
								getter.getModal(objectClass))));
			}
		});
	}

	private static <T> MobsimInitializedListener mobsimInitializedListener(QSimScopeObjectListener<T> objectListener,
			T object) {
		return e -> objectListener.objectCreated(object);
	}

	public static <T, L extends QSimScopeObjectListener<T> & ControlerListener> Builder<T, L> builder(
			Class<L> listenerClass) {
		return new Builder<>(listenerClass);
	}

	public static final class Builder<T, L extends QSimScopeObjectListener<T> & ControlerListener> {
		private final Class<L> listenerClass;
		private String mode;
		private Class<T> objectClass;
		private Provider<L> listenerProvider;

		private Builder(Class<L> listenerClass) {
			this.listenerClass = listenerClass;
		}

		public Builder<T, L> mode(String val) {
			mode = val;
			return this;
		}

		public Builder<T, L> objectClass(Class<T> val) {
			objectClass = val;
			return this;
		}

		public Builder<T, L> listenerCreator(Function<ModalProviders.InstanceGetter, L> val) {
			listenerProvider = ModalProviders.createProvider(mode, val);
			return this;
		}

		public Builder<T, L> listenerProvider(Provider<L> val) {
			listenerProvider = val;
			return this;
		}

		public QSimScopeObjectListenerModule<T, L> build() {
			return new QSimScopeObjectListenerModule<>(this);
		}
	}
}
