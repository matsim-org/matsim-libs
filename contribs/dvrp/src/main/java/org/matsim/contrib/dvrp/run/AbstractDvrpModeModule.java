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

import org.matsim.core.controler.AbstractModule;

import com.google.inject.Key;
import com.google.inject.Provider;
import com.google.inject.binder.LinkedBindingBuilder;

/**
 * @author Michal Maciejewski (michalm)
 */
public abstract class AbstractDvrpModeModule extends AbstractModule {
	private final String mode;

	protected AbstractDvrpModeModule(String mode) {
		this.mode = mode;
	}

	protected String getMode() {
		return mode;
	}

	protected <T> Key<T> modalKey(Class<T> type) {
		return Key.get(type, DvrpModes.mode(mode));
	}

	protected <T> LinkedBindingBuilder<T> bindModal(Class<T> type) {
		return bind(modalKey(type));
	}

	protected <T> Provider<T> modalProvider(Function<ModalProviders.InstanceGetter, T> delegate) {
		return ModalProviders.createProvider(mode, delegate);
	}
}
