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

import com.google.inject.Guice;
import com.google.inject.Key;
import com.google.inject.TypeLiteral;
import org.matsim.core.events.handler.EventHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

class Injector {

    private com.google.inject.Injector injector;

    private Injector(com.google.inject.Injector injector) {
        this.injector = injector;
    }

    static Injector createInjector(AbstractModule... modules) {
        List<com.google.inject.Module> guiceModules = new ArrayList<>();
        for (AbstractModule module : modules) {
            guiceModules.add(AbstractModule.toGuiceModule(module));
        }
        return new Injector(Guice.createInjector(guiceModules));
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

}
