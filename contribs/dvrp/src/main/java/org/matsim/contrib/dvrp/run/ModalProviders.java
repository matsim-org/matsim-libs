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

import javax.inject.Inject;

import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.Provider;
import com.google.inject.name.Names;

/**
 * @author Michal Maciejewski (michalm)
 */
public class ModalProviders {
	public static <T> Provider<T> createProvider(Function<Injector, T> delegate) {
		return new Provider<T>() {
			@Inject
			private Injector injector;

			@Override
			public T get() {
				return delegate.apply(injector);
			}
		};
	}

	public static <T> Provider<T> createProvider(String mode, Function<InstanceGetter, T> delegate) {
		return new Provider<T>() {
			@Inject
			private Injector injector;

			@Override
			public T get() {
				return delegate.apply(new InstanceGetter(mode, injector));
			}
		};
	}

	public static final class InstanceGetter {
		private final String mode;
		private final Injector injector;

		private InstanceGetter(String mode, Injector injector) {
			this.mode = mode;
			this.injector = injector;
		}

		public <T> T get(Class<T> type) {
			return injector.getInstance(type);
		}

		public <T> T get(Key<T> key) {
			return injector.getInstance(key);
		}

		public <T> T getModal(Class<T> type) {
			return injector.getInstance(DvrpModes.key(type, mode));
		}

		public <T> T getNamed(Class<T> type, String name) {
			return injector.getInstance(Key.get(type, Names.named(name)));
		}
	}

	public static abstract class AbstractProvider<T> implements Provider<T> {
		private final String mode;

		@Inject
		private Injector injector;

		protected AbstractProvider(String mode) {
			this.mode = mode;
		}

		protected <I> I getModalInstance(Class<I> type) {
			return injector.getInstance(DvrpModes.key(type, mode));
		}
	}
}
