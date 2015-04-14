/*
 *  *********************************************************************** *
 *  * project: org.matsim.*
 *  * FromEvents.java
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

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.controler.events.ShutdownEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.ControlerListener;
import org.matsim.core.controler.listener.ShutdownListener;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.events.handler.EventHandler;

import java.util.Set;

/**
 *
 * An attempt at making analysis modules reusable by emulating parts of the ControlerListener
 * protocol while reading an events file.
 *
 * @author michaz
 */
public class ReplayEvents {

    public static interface Results {
        <T> T get(Class<? extends T> type);
    }

    public static Results run(final Scenario scenario, final String eventsFilename, final AbstractModule... modules) {
        final Injector injector = Injector.createInjector(
                scenario.getConfig(),
                new AbstractModule() {
                    @Override
                    public void install() {
                        for (AbstractModule module : modules) {
                            install(module);
                        }
                        bind(Scenario.class).toInstance(scenario);
                    }
                });
        final EventsManager eventsManager = EventsUtils.createEventsManager(scenario.getConfig());
        for (EventHandler eventHandler : injector.getEventHandlersDeclaredByModules()) {
            eventsManager.addHandler(eventHandler);
        }
        Set<ControlerListener> controlerListenersDeclaredByModules = injector.getControlerListenersDeclaredByModules();
        for (ControlerListener controlerListener : controlerListenersDeclaredByModules) {
            if (controlerListener instanceof StartupListener) {
                ((StartupListener) controlerListener).notifyStartup(new StartupEvent(null));
            }
        }
        new MatsimEventsReader(eventsManager).readFile(eventsFilename);
        for (ControlerListener controlerListener : controlerListenersDeclaredByModules) {
            if (controlerListener instanceof ShutdownListener) {
                ((ShutdownListener) controlerListener).notifyShutdown(new ShutdownEvent(null, false));
            }
        }
        return new Results() {
            @Override
            public <T> T get(Class<? extends T> type) {
                return injector.getInstance(type);
            }
        };
    }

}
