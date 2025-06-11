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

import java.util.ArrayList;
import java.util.List;

import com.google.inject.multibindings.MapBinder;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.controler.listener.ControlerListener;
import org.matsim.core.events.handler.EventHandler;
import org.matsim.core.mobsim.framework.Mobsim;
import org.matsim.core.mobsim.framework.listeners.MobsimListener;
import org.matsim.core.mobsim.qsim.AbstractQSimModule;
import org.matsim.core.replanning.PlanStrategy;
import org.matsim.core.replanning.StrategyManagerModule;
import org.matsim.core.replanning.selectors.PlanSelector;
import org.matsim.core.router.RoutingModule;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.util.LeastCostPathCalculatorFactory;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.utils.objectattributes.AttributeConverter;
import org.matsim.vis.snapshotwriters.SnapshotWriter;

import com.google.inject.Binder;
import com.google.inject.Inject;
import com.google.inject.Key;
import com.google.inject.Module;
import com.google.inject.TypeLiteral;
import com.google.inject.binder.AnnotatedBindingBuilder;
import com.google.inject.binder.LinkedBindingBuilder;
import com.google.inject.multibindings.Multibinder;
import com.google.inject.name.Names;
import com.google.inject.util.Modules;

/**
 * "Designed for inheritance."
 * Extend this class, overwrite configure, and use the methods of this class to
 * install your module.
 * <p></p>
 * See comments in subclasses.
 * <p></p>
 * AbstractModule is a custom version of com.google.inject.Module, with the following differences:<ul>
 * <li> AbstractModule is a bit more restrictive.
 * <li> AbstractModule already has the config (as getConfig() ), which is helpful since the config contains material that
 * has to be used to configure the services.
 * </ul>
 *
 * @author michaz
 */
public abstract class AbstractModule implements Module {

	private Binder binder;

	@Inject
	com.google.inject.Injector bootstrapInjector;
	private Config config;

	public AbstractModule() {
		// config will be injected later
	}

	public AbstractModule(Config config) {
		this.config = config;
	}

	@Override
	public final void configure(Binder binder) {
		if (this.config == null) {
			this.config = bootstrapInjector.getInstance(Config.class);
		}
		// Guice error messages should give the code location of the error in the user's module,
		// not in this class.
		this.binder = binder.skipSources(AbstractModule.class);

		initializeMultibinders();

		this.install();
	}

	private void initializeMultibinders() {
		// We do need to make these calls here in order to register the multi binders. Otherwise, guice doesn't know that they exist. In particular,
		// if none of the corresponding addXXXBinding methods was called, the set binder would not be registered, and guice would complain.
		Multibinder.newSetBinder(this.binder, MobsimListener.class);
		Multibinder.newSetBinder(this.binder, SnapshotWriter.class);
		Multibinder.newSetBinder(this.binder, EventHandler.class);
		Multibinder.newSetBinder(this.binder, ControlerListener.class);
		MapBinder.newMapBinder(this.binder, new TypeLiteral<Class<?>>(){}, new TypeLiteral<AttributeConverter<?>>() {} );
		Multibinder.newSetBinder(this.binder, AbstractQSimModule.class);
		Multibinder.newSetBinder( this.binder, AbstractQSimModule.class, Names.named( "overridesFromAbstractModule" ) );
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
		return Multibinder.newSetBinder(this.binder, EventHandler.class).addBinding();
	}

	protected final void installQSimModule(AbstractQSimModule qsimModule) {
		Multibinder.newSetBinder(this.binder, AbstractQSimModule.class).addBinding().toInstance(qsimModule);
	}
	protected final void installOverridingQSimModule(AbstractQSimModule qsimModule) {
		Multibinder.newSetBinder( this.binder, AbstractQSimModule.class, Names.named( "overridesFromAbstractModule" ) ).addBinding().toInstance(qsimModule);
	}

	/**
	 * @see ControlerListener
	 */
	protected final LinkedBindingBuilder<ControlerListener> addControlerListenerBinding() {
		return Multibinder.newSetBinder(this.binder, ControlerListener.class).addBinding();
	}

	/**
	 * @see StrategyManagerModule
	 */
	protected final com.google.inject.binder.LinkedBindingBuilder<PlanSelector<Plan, Person>> bindPlanSelectorForRemoval() {
		return bind(new TypeLiteral<PlanSelector<Plan, Person>>(){});
	}

	protected final com.google.inject.binder.LinkedBindingBuilder<PlanStrategy> addPlanStrategyBinding(String selectorName) {
		return binder().bind(PlanStrategy.class).annotatedWith(Names.named(selectorName));
	}

	protected final com.google.inject.binder.LinkedBindingBuilder<Mobsim> bindMobsim() {
		return bind(Mobsim.class);
	}

	protected final com.google.inject.binder.LinkedBindingBuilder<ScoringFunctionFactory> bindScoringFunctionFactory() {
		return bind(ScoringFunctionFactory.class);
	}

	/**
	 * Contents retrieved (I think) by injected method QSim#addQueueSimulationListeners(...).  Is not public, and therefore cannot be referenced from here.
	 * <br/>
	 * I think that that method will be called every time the mobsim will be constructed.  If the injected classes are singletons, they will
	 * presumably be re-used, otherwise they will be newly constructed.
	 */
	protected final com.google.inject.binder.LinkedBindingBuilder<MobsimListener> addMobsimListenerBinding() {
		return Multibinder.newSetBinder(this.binder, MobsimListener.class).addBinding();
	}

	protected final com.google.inject.binder.LinkedBindingBuilder<SnapshotWriter> addSnapshotWriterBinding() {
		return Multibinder.newSetBinder(this.binder, SnapshotWriter.class).addBinding();
	}

	protected final LinkedBindingBuilder<AttributeConverter<?>> addAttributeConverterBinding(final Class<?> clazz ) {
		return MapBinder.newMapBinder(this.binder, new TypeLiteral<Class<?>>(){}, new TypeLiteral<AttributeConverter<?>>() {} ).addBinding( clazz );
	}
	/**
	 * @deprecated better use {@link #addTravelDisutilityFactoryBinding(String)}.
	 */
	@Deprecated
	protected final com.google.inject.binder.LinkedBindingBuilder<TravelDisutilityFactory> bindCarTravelDisutilityFactory() {
		return addTravelDisutilityFactoryBinding( TransportMode.car );
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

	protected final LinkedBindingBuilder<PersonPrepareForSimAlgorithm> addPersonPrepareForSimAlgorithm() {
		return Multibinder.newSetBinder(binder(), PersonPrepareForSimAlgorithm.class).addBinding();
	}

	protected final com.google.inject.binder.LinkedBindingBuilder<EventsManager> bindEventsManager() {
		return binder().bind(EventsManager.class);
	}

	/**
	 * @deprecated besser use {@link #addTravelTimeBinding(String)}.
	 */
	@Deprecated
	protected final LinkedBindingBuilder<TravelTime> bindNetworkTravelTime() {
		return bind( carTravelTime() );
	}

	@Deprecated // please inline
	protected final Key<TravelTime> networkTravelTime() { return carTravelTime(); }

	@SuppressWarnings("static-method")
	protected final Key<TravelTime> carTravelTime() {
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

	protected final <T> jakarta.inject.Provider<T> getProvider(TypeLiteral<T> typeLiteral) {
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
