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

import com.google.inject.*;
import com.google.inject.binder.AnnotatedBindingBuilder;
import com.google.inject.binder.LinkedBindingBuilder;
import com.google.inject.multibindings.MapBinder;
import com.google.inject.multibindings.Multibinder;
import com.google.inject.util.Modules;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.config.Config;
import org.matsim.core.controler.listener.ControlerListener;
import org.matsim.core.events.handler.EventHandler;
import org.matsim.core.mobsim.framework.Mobsim;
import org.matsim.core.replanning.PlanStrategy;
import org.matsim.core.replanning.selectors.GenericPlanSelector;

import java.util.ArrayList;
import java.util.List;

/**
 * "Designed for inheritance."
 * Extend this class, overwrite configure, and use the methods of this class to
 * install your module.
 *
 * See comments in subclasses.
 *
 * @author michaz
 */
public abstract class AbstractModule {

    private Binder binder;
    private Multibinder<EventHandler> eventHandlerMultibinder;
    private Multibinder<ControlerListener> controlerListenerMultibinder;
    private MapBinder<String, GenericPlanSelector<Plan, Person>> planSelectorForRemovalMultibinder;
    private MapBinder<String, PlanStrategy> planStrategyMultibinder;

    @Inject
    com.google.inject.Injector bootstrapInjector;

    static com.google.inject.Module toGuiceModule(final AbstractModule module) {
        return new com.google.inject.Module() {
            @Override
            public void configure(Binder binder) {
                module.configure(binder);
            }
        };
    }

    final void configure(Binder binder) {
        // Guice error messages should give the code location of the error in the user's module,
        // not in this class.
        this.binder = binder.skipSources(AbstractModule.class);

        this.eventHandlerMultibinder = Multibinder.newSetBinder(this.binder, EventHandler.class);
        this.controlerListenerMultibinder = Multibinder.newSetBinder(this.binder, ControlerListener.class);
        this.planStrategyMultibinder = MapBinder.newMapBinder(this.binder, String.class, PlanStrategy.class);
        this.planSelectorForRemovalMultibinder = MapBinder.newMapBinder(this.binder, new TypeLiteral<String>(){}, new TypeLiteral<GenericPlanSelector<Plan, Person>>(){});
        this.install();
    }

    public abstract void install();

    protected final Config getConfig() {
        return bootstrapInjector.getInstance(Config.class);
    }

    protected final void include(AbstractModule module) {
        bootstrapInjector.injectMembers(module);
        Module guiceModule = toGuiceModule(module);
        binder.install(guiceModule);
    }

    protected final <T> void bindTo(Class<T> type, Class<? extends T> implementation) {
        binder.bind(type).to(implementation);
    }

    protected final <T> void bindToInstance(Class<T> type, T instance) {
        binder.bind(type).toInstance(instance);
    }

    protected final <T> void bindAsSingleton(Class<T> type) {
        binder.bind(type).in(Singleton.class);
    }

    protected final <T> void bindAsSingleton(Class<T> type, Class<? extends T> implementation) {
        binder.bind(type).to(implementation).in(Singleton.class);
    }

    protected final <T> void bindToProviderAsSingleton(Class<T> type, Class<? extends javax.inject.Provider<? extends T>> providerType) {
        binder.bind(type).toProvider(providerType).in(Singleton.class);
    }

    protected final <T> void bindToProviderAsSingleton(Class<T> type, final javax.inject.Provider<? extends T> provider) {
        binder.bind(type).toProvider(new com.google.inject.Provider<T>() {
            @Override
            public T get() {
                return provider.get();
            }
        }).in(Singleton.class);
    }

    protected final <T> void bindToProvider(Class<T> type, Class<? extends javax.inject.Provider<? extends T>> providerType) {
        binder.bind(type).toProvider(providerType);
    }

    protected final <T> void bindToProvider(Class<T> type, final javax.inject.Provider<? extends T> provider) {
        binder.bind(type).toProvider(new com.google.inject.Provider<T>() {
            @Override
            public T get() {
                return provider.get();
            }
        });
    }

    protected final void addEventHandler(Class<? extends EventHandler> type) {
        eventHandlerMultibinder.addBinding().to(type);
    }

    protected final void addEventHandler(EventHandler instance) {
        eventHandlerMultibinder.addBinding().toInstance(instance);
    }

    protected LinkedBindingBuilder<ControlerListener> addControlerListenerBinding() {
        return controlerListenerMultibinder.addBinding();
    }

    protected final com.google.inject.binder.LinkedBindingBuilder<GenericPlanSelector<Plan, Person>> addPlanSelectorForRemovalBinding(String selectorName) {
        return planSelectorForRemovalMultibinder.addBinding(selectorName);
    }

    protected final com.google.inject.binder.LinkedBindingBuilder<PlanStrategy> addPlanStrategyBinding(String selectorName) {
        return planStrategyMultibinder.addBinding(selectorName);
    }

    protected final com.google.inject.binder.LinkedBindingBuilder<Mobsim> bindMobsim() {
        return binder().bind(Mobsim.class);
    }

    public <T> AnnotatedBindingBuilder<T> bind(Class<T> aClass) {
        return binder.bind(aClass);
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
                    guiceModules.add(AbstractModule.toGuiceModule(module));
                }
                bootstrapInjector.injectMembers(abstractModule);
                binder().install(Modules.override(guiceModules).with(AbstractModule.toGuiceModule(abstractModule)));
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
