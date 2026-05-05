
/* *********************************************************************** *
 * project: org.matsim.*
 * EventsManagerModule.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2019 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

package org.matsim.core.events;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import org.apache.commons.lang3.BooleanUtils;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.groups.ControllerConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.events.handler.EventHandler;

import java.util.Collection;

public final class EventsManagerModule extends AbstractModule {

	@Override
	public void install() {

		//noinspection StatementWithEmptyBody
		if (getConfig().controller().getMobsim().equals(ControllerConfigGroup.MobsimType.dsim.toString())) {
			// Bind nothing, the distributed sim will bind its own events manager
		} else if (BooleanUtils.isTrue(getConfig().eventsManager().getOneThreadPerHandler())) {
			bindEventsManager().to(ParallelEventsManager.class).in(Singleton.class);
		} else if (getConfig().eventsManager().getNumberOfThreads() != null) {
			if (BooleanUtils.isTrue(getConfig().eventsManager().getSynchronizeOnSimSteps())) {
				bindEventsManager().to(SimStepParallelEventsManagerImpl.class).in(Singleton.class);
			} else {
				bindEventsManager().to(ParallelEventsManagerImpl.class).in(Singleton.class);
			}
		} else {
			bindEventsManager().to(SimStepParallelEventsManagerImpl.class).in(Singleton.class);
		}
		bind(EventHandlerRegistrator.class).asEagerSingleton();
	}

	public static class EventHandlerRegistrator {
		@Inject
		EventHandlerRegistrator(EventsManager eventsManager, Collection<Provider<EventHandler>> eventHandlersDeclaredByModules) {
			for (Provider<EventHandler> eventHandler : eventHandlersDeclaredByModules) {
				eventsManager.addHandler(eventHandler);
			}
		}
	}
}
