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
import com.google.inject.multibindings.MapBinder;
import com.google.inject.multibindings.Multibinder;
import com.google.inject.util.Modules;
import org.matsim.core.config.Config;
import org.matsim.core.controler.listener.ControlerListener;
import org.matsim.core.events.handler.EventHandler;
import org.matsim.core.replanning.PlanStrategy;

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
        this.binder = binder;
        this.eventHandlerMultibinder = Multibinder.newSetBinder(this.binder, EventHandler.class);
        this.controlerListenerMultibinder = Multibinder.newSetBinder(this.binder, ControlerListener.class);
        this.planStrategyMultibinder = MapBinder.newMapBinder(this.binder, String.class, PlanStrategy.class);
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
        });
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

    protected final void addControlerListenerByProvider(Class<? extends javax.inject.Provider<? extends ControlerListener>> providerType) {
        controlerListenerMultibinder.addBinding().toProvider(providerType);
    }

    protected final void addControlerListener(Class<? extends ControlerListener> type) {
        controlerListenerMultibinder.addBinding().to(type);
    }

    protected final void addControlerListener(ControlerListener instance) {
        controlerListenerMultibinder.addBinding().toInstance(instance);
    }

    protected final void addPlanStrategyByProvider(String strategyName, Class<? extends javax.inject.Provider<? extends PlanStrategy>> providerType) {
        planStrategyMultibinder.addBinding(strategyName).toProvider(providerType);
    }

    protected final Object getDelegate() {
        return binder;
    }

    protected final <T> javax.inject.Provider<T> getProvider(TypeLiteral<T> typeLiteral) {
        return binder.getProvider(Key.get(typeLiteral));
    }

    public static AbstractModule override(final Iterable<AbstractModule> modules, final AbstractModule abstractModule) {
        return new AbstractModule() {
            @Override
            public void install() {
                final List<com.google.inject.Module> guiceModules = new ArrayList<>();
                for (AbstractModule module : modules) {
                    bootstrapInjector.injectMembers(module);
                    guiceModules.add(AbstractModule.toGuiceModule(module));
                }
                bootstrapInjector.injectMembers(abstractModule);
                ((Binder) getDelegate()).install(Modules.override(guiceModules).with(AbstractModule.toGuiceModule(abstractModule)));
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
