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

package org.matsim.contrib.dvrp.run;

import java.util.function.Function;

import org.matsim.core.controler.listener.ControlerListener;
import org.matsim.core.mobsim.framework.listeners.MobsimInitializedListener;
import org.matsim.core.mobsim.framework.listeners.MobsimListener;

import com.google.inject.Provider;

/**
 * Typical usecase: binding multi-iteration object stats calculators to overcome the limitation of the QSim scope of Fleet.
 * Notifies objectListener to that the object has been created.
 * <p>
 * If objectListener is also ControllerListener and/or MobsimListener, which is quite typical,
 * addControlerListenerBinding() and/or addModalComponent() will be called, respectively.
 *
 * @author Michal Maciejewski (michalm)
 */
public final class QSimScopeObjectListenerModule<T, L extends QSimScopeObjectListener<T>>
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
		if (ControlerListener.class.isAssignableFrom(listenerClass)) {
			addControlerListenerBinding().to(modalKey((Class<? extends ControlerListener>)listenerClass));
		}

		installQSimModule(new AbstractDvrpModeQSimModule(getMode()) {
			@Override
			protected void configureQSim() {
				if (MobsimListener.class.isAssignableFrom(listenerClass)) {
					addModalQSimComponentBinding().to(modalKey((Class<? extends MobsimListener>)listenerClass));
				}

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

	public static <T, L extends QSimScopeObjectListener<T>> Builder<T, L> builder(Class<L> listenerClass) {
		return new Builder<>(listenerClass);
	}

	public static final class Builder<T, L extends QSimScopeObjectListener<T>> {
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

		public AbstractDvrpModeModule build() {
			return new QSimScopeObjectListenerModule<>(this);
		}
	}
}
