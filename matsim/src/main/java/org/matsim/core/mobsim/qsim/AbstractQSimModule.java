
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

import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.List;

import com.google.inject.multibindings.Multibinder;
import org.matsim.core.mobsim.framework.AbstractMobsimModule;
import org.matsim.core.mobsim.qsim.components.QSimComponent;

import com.google.inject.Module;
import com.google.inject.binder.LinkedBindingBuilder;
import com.google.inject.name.Names;
import com.google.inject.util.Modules;

public abstract class AbstractQSimModule extends AbstractMobsimModule {
	@Override
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
		addQSimComponentBinding(name).to(componentClass);
	}

	protected abstract void configureQSim();
	
	protected void install(AbstractQSimModule module) {
		module.setParent(this);
		super.install(module);
	}

	public static AbstractQSimModule overrideQSimModules(Collection<AbstractQSimModule> base,
			List<AbstractQSimModule> overrides) {
		Module composite = Modules.override(base).with(overrides);

		AbstractQSimModule wrapper = new AbstractQSimModule() {
			@Override
			protected void configureQSim() {
				install(composite);
			}
		};

		base.forEach(m -> m.setParent(wrapper));
		overrides.forEach(m -> m.setParent(wrapper));

		return wrapper;
	}
}
