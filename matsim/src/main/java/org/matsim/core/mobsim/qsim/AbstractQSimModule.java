
/* *********************************************************************** *
 * project: org.matsim.*
 * AbstractQSimModule.java
 *                                                                         *
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
 * *********************************************************************** */

 package org.matsim.core.mobsim.qsim;

import com.google.inject.Module;
import com.google.inject.binder.LinkedBindingBuilder;
import com.google.inject.multibindings.Multibinder;
import com.google.inject.name.Names;
import com.google.inject.util.Modules;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.IterationScoped;
import org.matsim.core.mobsim.qsim.components.QSimComponent;

import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.List;

public abstract class AbstractQSimModule extends AbstractModule {

	protected final void configureMobsim() {
		configureQSim();
	}

	@Deprecated // for experts only
	protected final LinkedBindingBuilder<QSimComponent> addQSimComponentBinding(Annotation annotation) {
		Multibinder<QSimComponent> multibinder = Multibinder.newSetBinder(binder(), QSimComponent.class, annotation);
		multibinder.permitDuplicates();
		return multibinder.addBinding();
	}

	@Deprecated // for experts only
	protected LinkedBindingBuilder<QSimComponent> addQSimComponentBinding(Class<? extends Annotation> annotationClass) {
		Multibinder<QSimComponent> multibinder = Multibinder.newSetBinder(binder(), QSimComponent.class, annotationClass);
		multibinder.permitDuplicates();
		return multibinder.addBinding();
	}
	
	protected LinkedBindingBuilder<QSimComponent> addQSimComponentBinding(String name) {
		return addQSimComponentBinding(Names.named(name));
	}

	// Use methods above
	@Deprecated
	protected <T extends QSimComponent> void addNamedComponent(Class<T> componentClass, String name) {
		addQSimComponentBinding(name).to(componentClass).in(IterationScoped.class);
	}

	protected abstract void configureQSim();
	
	protected void install(AbstractQSimModule module) {
		super.install(module);
	}

	public final void install() {
		configureMobsim();
	}
}
