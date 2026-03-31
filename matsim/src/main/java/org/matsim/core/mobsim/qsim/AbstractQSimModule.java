
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
import org.matsim.core.events.MobsimScopeEventHandler;
import org.matsim.core.mobsim.framework.AbstractMobsimModule;
import org.matsim.core.mobsim.qsim.components.QSimComponent;
import org.matsim.core.mobsim.qsim.qnetsimengine.linkspeedcalculator.LinkSpeedCalculator;
import org.matsim.core.mobsim.qsim.qnetsimengine.parking.ParkingSearchTimeCalculator;
import org.matsim.core.mobsim.qsim.qnetsimengine.vehicle_handler.VehicleHandler;

import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.List;

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

	protected LinkedBindingBuilder<MobsimScopeEventHandler> addMobsimScopeEventHandlerBinding(){
		return Multibinder.newSetBinder( binder(), MobsimScopeEventHandler.class ).addBinding();
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

	// the methods below are strictly speaking not necessary.  But help with detectability.  A bit similar to the general matsim AbstractModule.

	/**
	 * <p>This is deliberately ``add'' and not ``set'' since multiple such speed calculators can be added, as long as they do not answer to the same
	 * (vehicle, link, time) combination.</p>
	 *
	 * <p>This is plugged together in {@link org.matsim.core.mobsim.qsim.qnetsimengine.DefaultQNetworkFactory}.
	 * Should presumably be done similarly
	 * for the other ways to configure {@link org.matsim.core.mobsim.qsim.qnetsimengine.ConfigurableQNetworkFactory},
	 * and then the latter could be deprecated.</p>
	 */
	protected LinkedBindingBuilder<LinkSpeedCalculator> addLinkSpeedCalculatorBinding() {
		return Multibinder.newSetBinder(this.binder(), LinkSpeedCalculator.class).addBinding();
	}

	protected LinkedBindingBuilder<VehicleHandler> addVehicleHandlerBinding() {
		return Multibinder.newSetBinder(this.binder(), VehicleHandler.class).addBinding();
	}

	protected LinkedBindingBuilder<ParkingSearchTimeCalculator> addParkingSearchTimeCalculatorBinding() {
		return Multibinder.newSetBinder(this.binder(), ParkingSearchTimeCalculator.class).addBinding();
	}
}
