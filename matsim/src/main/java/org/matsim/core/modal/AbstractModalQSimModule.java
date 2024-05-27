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

import jakarta.inject.Provider;

import org.matsim.core.mobsim.framework.listeners.MobsimListener;
import org.matsim.core.mobsim.qsim.AbstractQSimModule;
import org.matsim.core.mobsim.qsim.components.QSimComponent;

import com.google.inject.Key;
import com.google.inject.TypeLiteral;
import com.google.inject.binder.LinkedBindingBuilder;

/**
 * @author Michal Maciejewski (michalm)
 */
public abstract class AbstractModalQSimModule<M extends Annotation> extends AbstractQSimModule {
	private final String mode;
	private final ModalAnnotationCreator<M> modalAnnotationCreator;

	protected AbstractModalQSimModule(String mode, ModalAnnotationCreator<M> modalAnnotationCreator) {
		this.mode = mode;
		this.modalAnnotationCreator = modalAnnotationCreator;
	}

	protected final String getMode() {
		return mode;
	}

	protected final M getModalAnnotation() {
		return modalAnnotationCreator.mode(mode);
	}

	protected final <T> Key<T> modalKey(Class<T> type) {
		return modalAnnotationCreator.key(type, mode);
	}

	protected final <T> Key<T> modalKey(TypeLiteral<T> typeLiteral) {
		return modalAnnotationCreator.key(typeLiteral, mode);
	}

	protected final <T> LinkedBindingBuilder<T> bindModal(Class<T> type) {
		return bind(modalKey(type));
	}

	protected final <T> LinkedBindingBuilder<T> bindModal(TypeLiteral<T> typeLiteral) {
		return bind(modalKey(typeLiteral));
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
		return addQSimComponentBinding(getModalAnnotation());
	}

	protected final <T extends QSimComponent> void addModalComponent(Class<T> componentClass, Key<? extends T> key) {
		bindModal(componentClass).to(key).asEagerSingleton();
		addModalQSimComponentBinding().to(modalKey(componentClass));
	}

	protected final <T extends QSimComponent> void addModalComponent(Class<T> componentClass,
			Provider<? extends T> componentProvider) {
		bindModal(componentClass).toProvider(componentProvider).asEagerSingleton();
		addModalQSimComponentBinding().to(modalKey(componentClass));
	}

	protected final <T extends QSimComponent> void addModalComponent(Class<T> componentClass,
			Class<? extends T> implementation) {
		bindModal(componentClass).to(implementation).asEagerSingleton();
		addModalQSimComponentBinding().to(modalKey(componentClass));
	}

	protected final <T extends QSimComponent> void addModalComponent(Class<T> componentClass) {
		addModalComponent(componentClass, componentClass);
	}

	protected final <T> Provider<T> modalProvider(Function<ModalProviders.InstanceGetter<M>, T> delegate) {
		return ModalProviders.createProvider(mode, modalAnnotationCreator, delegate);
	}
}
