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

import com.google.inject.Binder;
import com.google.inject.Singleton;
import com.google.inject.multibindings.Multibinder;
import org.matsim.core.events.handler.EventHandler;

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
        this.install();
    }

    public abstract void install();

    protected final void include(AbstractModule module) {
        binder.install(toGuiceModule(module));
    }

    protected final <T> void bindToInstance(Class<T> type, T instance) {
        binder.bind(type).toInstance(instance);
    }

    protected final <T> void bindAsSingleton(Class<T> type) {
        binder.bind(type).in(Singleton.class);
    }

    protected final <T> void bindToProviderAsSingleton(Class<T> type, Class<? extends javax.inject.Provider<? extends T>> providerType) {
        binder.bind(type).toProvider(providerType).in(Singleton.class);
    }

    protected final <T> void bindToProvider(Class<T> type, Class<? extends javax.inject.Provider<? extends T>> providerType) {
        binder.bind(type).toProvider(providerType);
    }

    protected final void addEventHandler(Class<? extends EventHandler> type) {
        Multibinder<EventHandler> eventHandlerMultibinder = Multibinder.newSetBinder(this.binder, EventHandler.class);
        eventHandlerMultibinder.addBinding().to(type);
    }

    protected final void addEventHandler(EventHandler instance) {
        Multibinder<EventHandler> eventHandlerMultibinder = Multibinder.newSetBinder(this.binder, EventHandler.class);
        eventHandlerMultibinder.addBinding().toInstance(instance);
    }

}
