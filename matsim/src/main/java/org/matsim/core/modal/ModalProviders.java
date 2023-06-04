/*
 * *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2021 by the members listed in the COPYING,        *
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

package org.matsim.core.modal;

import java.lang.annotation.Annotation;
import java.util.function.Function;

import jakarta.inject.Inject;

import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.Provider;
import com.google.inject.TypeLiteral;
import com.google.inject.name.Names;

/**
 * @author Michal Maciejewski (michalm)
 */
public class ModalProviders {
	public static <M extends Annotation, T> Provider<T> createProvider(String mode,
			ModalAnnotationCreator<M> modalAnnotationCreator, Function<InstanceGetter<M>, T> delegate) {
		return new Provider<>() {
			@Inject
			private Injector injector;

			@Override
			public T get() {
				return delegate.apply(new InstanceGetter<>(mode, modalAnnotationCreator, injector));
			}
		};
	}

	public static final class InstanceGetter<M extends Annotation> {
		private final String mode;
		private final Injector injector;
		private final ModalAnnotationCreator<M> modalAnnotationCreator;

		private InstanceGetter(String mode, ModalAnnotationCreator<M> modalAnnotationCreator, Injector injector) {
			this.mode = mode;
			this.injector = injector;
			this.modalAnnotationCreator = modalAnnotationCreator;
		}

		public <T> T get(Class<T> type) {
			return injector.getInstance(type);
		}

		public <T> T get(Key<T> key) {
			return injector.getInstance(key);
		}

		public <T> T get(TypeLiteral<T> typeLiteral) {
			return injector.getInstance(Key.get(typeLiteral));
		}

		public <T> T getModal(Class<T> type) {
			return injector.getInstance(modalAnnotationCreator.key(type, mode));
		}

		public <T> T getModal(TypeLiteral<T> typeLiteral) {
			return injector.getInstance(modalAnnotationCreator.key(typeLiteral, mode));
		}

		public <T> T getNamed(Class<T> type, String name) {
			return injector.getInstance(Key.get(type, Names.named(name)));
		}

		public <T> T getNamed(TypeLiteral<T> typeLiteral, String name) {
			return injector.getInstance(Key.get(typeLiteral, Names.named(name)));
		}
	}

	public static abstract class AbstractProvider<M extends Annotation, T> implements Provider<T> {
		private final String mode;
		private final ModalAnnotationCreator<M> modalAnnotationCreator;

		@Inject
		private Injector injector;

		protected AbstractProvider(String mode, ModalAnnotationCreator<M> modalAnnotationCreator) {
			this.mode = mode;
			this.modalAnnotationCreator = modalAnnotationCreator;
		}

		protected <I> I getModalInstance(Class<I> type) {
			return injector.getInstance(modalAnnotationCreator.key(type, mode));
		}

		protected <I> I getModalInstance(TypeLiteral<I> typeLiteral) {
			return injector.getInstance(modalAnnotationCreator.key(typeLiteral, mode));
		}

		protected <I> Provider<I> getModalProvider(Class<I> type) {
			return injector.getProvider(modalAnnotationCreator.key(type, mode));
		}

		protected String getMode() {
			return mode;
		}
	}
}
