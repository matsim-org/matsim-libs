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

import jakarta.inject.Inject;
import java.util.Set;

/**
 *
 * An attempt at making analysis modules reusable by emulating parts of the ControllerListener
 * protocol while reading an events file.
 *
 * @author michaz
 */
public final class ReplayEvents {

    public interface Results {
        <T> T get(Class<? extends T> type);
    }

    @Inject
    Set<ControllerListener> controllerListenersDeclaredByModules;

    @Inject
		ControllerListenerManager controllerListenerManager;

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
        instance.playEventsFile(eventsFilename, 1, false);

        return new Results() {
            @Override
            public <T> T get(Class<? extends T> type) {
                return injector.getInstance(type);
            }
        };
    }

    public void playEventsFile(String eventsFilename, int iterationNumber, boolean isLastIteration) {
        ((ControllerListenerManagerImpl) controllerListenerManager).fireControllerStartupEvent();
        for (ControllerListener controllerListener : controllerListenersDeclaredByModules) {
            if (controllerListener instanceof StartupListener) {
                ((StartupListener) controllerListener).notifyStartup(new StartupEvent(null));
            }
        }
        ((ControllerListenerManagerImpl) controllerListenerManager).fireControllerIterationStartsEvent(iterationNumber, isLastIteration);
        for (ControllerListener controllerListener : controllerListenersDeclaredByModules) {
            if (controllerListener instanceof IterationStartsListener) {
                ((IterationStartsListener) controllerListener).notifyIterationStarts(new IterationStartsEvent(null, iterationNumber, isLastIteration));
            }
        }
        ((ControllerListenerManagerImpl) controllerListenerManager).fireControllerBeforeMobsimEvent(iterationNumber, isLastIteration);
        for (ControllerListener controllerListener : controllerListenersDeclaredByModules) {
            if (controllerListener instanceof BeforeMobsimListener) {
                ((BeforeMobsimListener) controllerListener).notifyBeforeMobsim(new BeforeMobsimEvent(null, iterationNumber, isLastIteration));
            }
        }
        new MatsimEventsReader(eventsManager).readFile(eventsFilename);
        ((ControllerListenerManagerImpl) controllerListenerManager).fireControllerAfterMobsimEvent(iterationNumber, isLastIteration);
        for (ControllerListener controllerListener : controllerListenersDeclaredByModules) {
            if (controllerListener instanceof AfterMobsimListener) {
                ((AfterMobsimListener) controllerListener).notifyAfterMobsim(new AfterMobsimEvent(null, iterationNumber, isLastIteration));
            }
        }
        ((ControllerListenerManagerImpl) controllerListenerManager).fireControllerIterationEndsEvent(iterationNumber, isLastIteration);
        for (ControllerListener controllerListener : controllerListenersDeclaredByModules) {
            if (controllerListener instanceof IterationEndsListener) {
                ((IterationEndsListener) controllerListener).notifyIterationEnds(new IterationEndsEvent(null, iterationNumber, isLastIteration));
            }
        }
        ((ControllerListenerManagerImpl) controllerListenerManager).fireControllerShutdownEvent(false, iterationNumber);
        for (ControllerListener controllerListener : controllerListenersDeclaredByModules) {
            if (controllerListener instanceof ShutdownListener) {
                ((ShutdownListener) controllerListener).notifyShutdown(new ShutdownEvent(null, false, iterationNumber, null));
            }
        }
    }

    public static class Module extends AbstractModule {
        @Override
		public void install() {
			bind(ReplayEvents.class).asEagerSingleton();
            bind(ControllerListenerManager.class).to(ControllerListenerManagerImpl.class).asEagerSingleton();
		}
    }
}
