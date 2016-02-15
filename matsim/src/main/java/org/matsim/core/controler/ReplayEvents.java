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

import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.controler.events.*;
import org.matsim.core.controler.listener.*;
import org.matsim.core.events.MatsimEventsReader;

import javax.inject.Inject;
import java.util.Set;

/**
 *
 * An attempt at making analysis modules reusable by emulating parts of the ControlerListener
 * protocol while reading an events file.
 *
 * @author michaz
 */
public class ReplayEvents {

    public interface Results {
        <T> T get(Class<? extends T> type);
    }

    @Inject
    Set<ControlerListener> controlerListenersDeclaredByModules;

    @Inject
    EventsManager eventsManager;

    public static Results run(final Config config, final String eventsFilename, final AbstractModule... modules) {
        final com.google.inject.Injector injector = Injector.createInjector(
                config,
                new Module(),
                new AbstractModule() {
                    @Override
                    public void install() {
                        for (AbstractModule module : modules) {
                            install(module);
                        }
                    }
                });
        ReplayEvents instance = injector.getInstance(ReplayEvents.class);
        instance.playEventsFile(eventsFilename, 1);

        return new Results() {
            @Override
            public <T> T get(Class<? extends T> type) {
                return injector.getInstance(type);
            }
        };
    }

    public void playEventsFile(String eventsFilename, int iterationNumber) {
        for (ControlerListener controlerListener : controlerListenersDeclaredByModules) {
            if (controlerListener instanceof StartupListener) {
                ((StartupListener) controlerListener).notifyStartup(new StartupEvent(null));
            }
        }
        for (ControlerListener controlerListener : controlerListenersDeclaredByModules) {
            if (controlerListener instanceof IterationStartsListener) {
                ((IterationStartsListener) controlerListener).notifyIterationStarts(new IterationStartsEvent(null, iterationNumber));
            }
        }
        for (ControlerListener controlerListener : controlerListenersDeclaredByModules) {
            if (controlerListener instanceof BeforeMobsimListener) {
                ((BeforeMobsimListener) controlerListener).notifyBeforeMobsim(new BeforeMobsimEvent(null, iterationNumber));
            }
        }
        new MatsimEventsReader(eventsManager).readFile(eventsFilename);
        for (ControlerListener controlerListener : controlerListenersDeclaredByModules) {
            if (controlerListener instanceof AfterMobsimListener) {
                ((AfterMobsimListener) controlerListener).notifyAfterMobsim(new AfterMobsimEvent(null, iterationNumber));
            }
        }
        for (ControlerListener controlerListener : controlerListenersDeclaredByModules) {
            if (controlerListener instanceof IterationEndsListener) {
                ((IterationEndsListener) controlerListener).notifyIterationEnds(new IterationEndsEvent(null, iterationNumber));
            }
        }
        for (ControlerListener controlerListener : controlerListenersDeclaredByModules) {
            if (controlerListener instanceof ShutdownListener) {
                ((ShutdownListener) controlerListener).notifyShutdown(new ShutdownEvent(null, false));
            }
        }
    }

    public static class Module extends AbstractModule {
        @Override
		public void install() {
			bind(ReplayEvents.class).asEagerSingleton();
		}
    }
}
