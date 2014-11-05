/*
 *  *********************************************************************** *
 *  * project: org.matsim.*
 *  * Injector.java
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
import org.matsim.core.config.Config;
import org.matsim.core.controler.listener.ControlerListener;
import org.matsim.core.events.handler.EventHandler;
import org.matsim.core.scoring.ScoringFunctionFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

class Injector {

    private com.google.inject.Injector injector;

    private Injector(com.google.inject.Injector injector) {
        this.injector = injector;
    }

    static Injector createInjector(final Config config, AbstractModule... modules) {
        com.google.inject.Injector bootstrapInjector = Guice.createInjector(new Module() {
            @Override
            public void configure(Binder binder) {
                binder.requireExplicitBindings(); // For now, we are conservative
                binder.disableCircularProxies(); // and disable any kind of magic.
                binder.bind(Config.class).toInstance(config);
            }
        });
        // A MATSim module needs the config at configuration time in order to decide what
        // features to provide. So we create a bootstrapInjector which already has the config
        // and provides it to the MATSim modules.
        List<com.google.inject.Module> guiceModules = new ArrayList<>();
        for (AbstractModule module : modules) {
            bootstrapInjector.injectMembers(module);
            guiceModules.add(AbstractModule.toGuiceModule(module));
        }
        return new Injector(bootstrapInjector.createChildInjector(guiceModules));
    }

    <T> T getInstance(Class<T> type) {
        return injector.getInstance(type);
    }

    Set<EventHandler> getEventHandlersDeclaredByModules() {
        return injector.getInstance(Key.get(
                new TypeLiteral<Set<EventHandler>>() {
                }
        ));
    }

    Set<ControlerListener> getControlerListenersDeclaredByModules() {
        return injector.getInstance(Key.get(
                new TypeLiteral<Set<ControlerListener>>() {
                }
        ));
    }

    public void retrofitScoringFunctionFactory(ScoringFunctionFactory scoringFunctionFactory) {
        injector.injectMembers(scoringFunctionFactory);
    }

}
