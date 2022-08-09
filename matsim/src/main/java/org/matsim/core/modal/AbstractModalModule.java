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

import org.matsim.core.controler.AbstractModule;

import com.google.inject.Key;
import com.google.inject.Provider;
import com.google.inject.TypeLiteral;
import com.google.inject.binder.LinkedBindingBuilder;
import com.google.inject.multibindings.MapBinder;

/**
 * @author Michal Maciejewski (michalm)
 */
public abstract class AbstractModalModule<M extends Annotation> extends AbstractModule {
	private final String mode;
	private final ModalAnnotationCreator<M> modalAnnotationCreator;

	protected AbstractModalModule(String mode, ModalAnnotationCreator<M> modalAnnotationCreator) {
		this.mode = mode;
		this.modalAnnotationCreator = modalAnnotationCreator;
	}

	protected String getMode() {
		return mode;
	}

	protected <T> Key<T> modalKey(Class<T> type) {
		return modalAnnotationCreator.key(type, mode);
	}

	protected <T> Key<T> modalKey(TypeLiteral<T> typeLiteral) {
		return modalAnnotationCreator.key(typeLiteral, mode);
	}

	protected <T> LinkedBindingBuilder<T> bindModal(Class<T> type) {
		return bind(modalKey(type));
	}

	protected <T> LinkedBindingBuilder<T> bindModal(TypeLiteral<T> typeLiteral) {
		return bind(modalKey(typeLiteral));
	}

	protected <K, V> MapBinder<K, V> modalMapBinder(Class<K> keyType, Class<V> valueType) {
		return MapBinder.newMapBinder(binder(), keyType, valueType, modalAnnotationCreator.mode(getMode()));
	}

	protected <K, V> MapBinder<K, V> modalMapBinder(TypeLiteral<K> keyType, TypeLiteral<V> valueType) {
		return MapBinder.newMapBinder(binder(), keyType, valueType, modalAnnotationCreator.mode(getMode()));
	}

	protected <T> Provider<T> modalProvider(Function<ModalProviders.InstanceGetter<M>, T> delegate) {
		return ModalProviders.createProvider(mode, modalAnnotationCreator, delegate);
	}
}
