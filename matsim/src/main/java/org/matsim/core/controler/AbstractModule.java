/*
 *  *********************************************************************** *
 *  * project: org.matsim.*
 *  * Module.java
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  * copyright       : (C) 2014 by the members listed in the COPYING, *
 *  *                   LICENSE and WARRANTY file.                            *
 *  * email           : info at matsim dot org                                *
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  *   This program is free software; you can redistribute it and/or modify  *
 *  *   it under the terms of the GNU General Public License as published by  *
 *  *   the Free Software Foundation; either version 2 of the License, or     *
 *  *   (at your option) any later version.                                   *
 *  *   See also COPYING, LICENSE and WARRANTY file                           *
 *  *                                                                         *
 *  * ***********************************************************************
 */

package org.matsim.core.controler;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.List;

import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.controler.listener.ControlerListener;
import org.matsim.core.events.handler.EventHandler;
import org.matsim.core.mobsim.framework.Mobsim;
import org.matsim.core.mobsim.framework.listeners.MobsimListener;
import org.matsim.core.replanning.PlanStrategy;
import org.matsim.core.replanning.StrategyManagerConfigLoader;
import org.matsim.core.replanning.StrategyManagerModule;
import org.matsim.core.replanning.selectors.GenericPlanSelector;
import org.matsim.core.router.RoutingModule;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.util.LeastCostPathCalculatorFactory;
import org.matsim.core.router.util.TravelTime;
import org.matsim.vis.snapshotwriters.SnapshotWriter;

import com.google.inject.Binder;
import com.google.inject.BindingAnnotation;
import com.google.inject.Inject;
import com.google.inject.Key;
import com.google.inject.Module;
import com.google.inject.TypeLiteral;
import com.google.inject.binder.AnnotatedBindingBuilder;
import com.google.inject.binder.LinkedBindingBuilder;
import com.google.inject.multibindings.MapBinder;
import com.google.inject.multibindings.Multibinder;
import com.google.inject.name.Names;
import com.google.inject.util.Modules;

import tutorial.programming.planStrategyForRemoval.RunPlanSelectorForRemovalExample;

/**
 * "Designed for inheritance."
 * Extend this class, overwrite configure, and use the methods of this class to
 * install your module.
 * <p/>
 * See comments in subclasses.
 * <p/>
 * AbstractModule is a custom version of com.google.inject.Module, with the following differences:<ul>
 * <li> AbstractModule is a bit more restrictive.
 * <li> AbstractModule already has the config (as getConfig() ), which is helpful since the config contains material that
 * has to be used to configure the controler.
 * </ul>
 *
 * @author michaz
 */
public abstract class AbstractModule implements Module {

	private Binder binder;
	private Multibinder<EventHandler> eventHandlerMultibinder;
	private Multibinder<ControlerListener> controlerListenerMultibinder;
	private Multibinder<MobsimListener> mobsimListenerMultibinder;
	private Multibinder<SnapshotWriter> snapshotWriterMultibinder;
	private MapBinder<String, GenericPlanSelector<Plan, Person>> planSelectorForRemovalMultibinder;
	private MapBinder<String, PlanStrategy> planStrategyMultibinder;

	@Inject
	com.google.inject.Injector bootstrapInjector;
	private Config config;

	public AbstractModule() {
		// config will be injected later
	}

	public AbstractModule(Config config) {
		this.config = config;
	}

	public final void configure(Binder binder) {
		if (this.config == null) {
			this.config = bootstrapInjector.getInstance(Config.class);
		}
		// Guice error messages should give the code location of the error in the user's module,
		// not in this class.
		this.binder = binder.skipSources(AbstractModule.class);
		this.mobsimListenerMultibinder = Multibinder.newSetBinder(this.binder, MobsimListener.class);
		this.snapshotWriterMultibinder = Multibinder.newSetBinder(this.binder, SnapshotWriter.class);
		this.eventHandlerMultibinder = Multibinder.newSetBinder(this.binder, EventHandler.class);
		this.controlerListenerMultibinder = Multibinder.newSetBinder(this.binder, ControlerListener.class);
		this.planStrategyMultibinder = MapBinder.newMapBinder(this.binder, String.class, PlanStrategy.class);
		this.planSelectorForRemovalMultibinder = MapBinder.newMapBinder(this.binder, new TypeLiteral<String>(){}, new TypeLiteral<GenericPlanSelector<Plan, Person>>(){});
		this.install();
	}

	public abstract void install();

	protected final Config getConfig() {
		return config;
	}

	protected final void install(Module module) {
		bootstrapInjector.injectMembers(module);
		binder.install(module);
	}

	protected final LinkedBindingBuilder<EventHandler> addEventHandlerBinding() {
		return eventHandlerMultibinder.addBinding();
	}

	/**
	 * See {@link RunControlerListenerExample} for an example.
	 * 
	 * @see {@link ControlerListener}
	 */
	protected final LinkedBindingBuilder<ControlerListener> addControlerListenerBinding() {
		return controlerListenerMultibinder.addBinding();
	}

	/**
	 * See {@link RunPlanSelectorForRemovalExample} for an example.
	 * 
	 * @see {@link StrategyManagerModule}, {@link StrategyManagerConfigLoader}
	 */
	protected final com.google.inject.binder.LinkedBindingBuilder<GenericPlanSelector<Plan, Person>> addPlanSelectorForRemovalBinding(String selectorName) {
		return planSelectorForRemovalMultibinder.addBinding(selectorName);
	}

	protected final com.google.inject.binder.LinkedBindingBuilder<PlanStrategy> addPlanStrategyBinding(String selectorName) {
		return planStrategyMultibinder.addBinding(selectorName);
	}

	protected final com.google.inject.binder.LinkedBindingBuilder<Mobsim> bindMobsim() {
		return bind(Mobsim.class);
	}

	protected final com.google.inject.binder.LinkedBindingBuilder<MobsimListener> addMobsimListenerBinding() {
		return mobsimListenerMultibinder.addBinding();
	}

	protected final com.google.inject.binder.LinkedBindingBuilder<SnapshotWriter> addSnapshotWriterBinding() {
		return snapshotWriterMultibinder.addBinding();
	}

	protected final com.google.inject.binder.LinkedBindingBuilder<TravelDisutilityFactory> bindCarTravelDisutilityFactory() {
		return bind(carTravelDisutilityFactoryKey());
	}

	@SuppressWarnings("static-method")
	protected final Key<TravelDisutilityFactory> carTravelDisutilityFactoryKey() {
		return Key.get(TravelDisutilityFactory.class, Names.named(TransportMode.car));
	}

	protected final com.google.inject.binder.LinkedBindingBuilder<TravelDisutilityFactory> addTravelDisutilityFactoryBinding(String mode) {
		return binder().bind(TravelDisutilityFactory.class).annotatedWith(Names.named(mode));
	}

	protected final com.google.inject.binder.LinkedBindingBuilder<LeastCostPathCalculatorFactory> bindLeastCostPathCalculatorFactory() {
		return bind(LeastCostPathCalculatorFactory.class);
	}

	protected final com.google.inject.binder.LinkedBindingBuilder<TravelTime> addTravelTimeBinding(String mode) {
		return binder().bind(TravelTime.class).annotatedWith(Names.named(mode));
	}

	protected final LinkedBindingBuilder<RoutingModule> addRoutingModuleBinding(String mode) {
		return binder().bind(RoutingModule.class).annotatedWith(Names.named(mode));
	}

	protected final com.google.inject.binder.LinkedBindingBuilder<EventsManager> bindEventsManager() {
		return binder().bind(EventsManager.class);
	}

	protected final LinkedBindingBuilder<TravelTime> bindNetworkTravelTime() {
		return bind(networkTravelTime());
	}

	@SuppressWarnings("static-method")
	protected final Key<TravelTime> networkTravelTime() {
		return Key.get(TravelTime.class, Names.named(TransportMode.car));
	}

	protected <T> AnnotatedBindingBuilder<T> bind(Class<T> aClass) {
		return binder.bind(aClass);
	}

	protected <T> AnnotatedBindingBuilder<T> bind(TypeLiteral<T> typeLiteral) {
		return binder.bind(typeLiteral);
	}

	protected <T> LinkedBindingBuilder<T> bind(Key<T> key) {
		return binder.bind(key);
	}

	protected final Binder binder() {
		return binder;
	}

	protected final <T> javax.inject.Provider<T> getProvider(TypeLiteral<T> typeLiteral) {
		return binder.getProvider(Key.get(typeLiteral));
	}

	public static AbstractModule override(final Iterable<? extends AbstractModule> modules, final AbstractModule abstractModule) {
		return new AbstractModule() {
			@Override
			public void install() {
				final List<com.google.inject.Module> guiceModules = new ArrayList<>();
				for (AbstractModule module : modules) {
					bootstrapInjector.injectMembers(module);
					guiceModules.add(module);
				}
				bootstrapInjector.injectMembers(abstractModule);
				binder().install(Modules.override(guiceModules).with(abstractModule));
			}
		};
	}

	public static AbstractModule emptyModule() {
		return new AbstractModule() {
			@Override
			public void install() {}
		};
	}
}
