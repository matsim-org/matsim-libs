/*
 * *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2020 by the members listed in the COPYING,        *
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
 * *********************************************************************** *
 */

package org.matsim.core.events;

import java.util.Collection;
import java.util.concurrent.ConcurrentLinkedQueue;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.controler.events.AfterMobsimEvent;
import org.matsim.core.controler.listener.AfterMobsimListener;

/**
 * Meant for event handlers that are created anew in each iteration and should operate only until the end of the current
 * mobsim. Typically, these are event handlers created in AbstractQSimModules.
 *
 * @author Michal Maciejewski (michalm)
 */
@Singleton
public class MobsimScopeEventHandling implements AfterMobsimListener {
	private final Collection<MobsimScopeEventHandler> eventHandlers = new ConcurrentLinkedQueue<>();
	private final EventsManager eventsManager;

	@Inject
	public MobsimScopeEventHandling(EventsManager eventsManager) {
		this.eventsManager = eventsManager;
	}

	public void addMobsimScopeHandler(MobsimScopeEventHandler handler) {
		eventHandlers.add(handler);
		eventsManager.addHandler(handler);
	}

	@Override
	public void notifyAfterMobsim(AfterMobsimEvent event) {
		eventHandlers.forEach(eventsManager::removeHandler);
		eventHandlers.forEach(eventHandler -> eventHandler.cleanupAfterMobsim(event.getIteration()));
		eventHandlers.clear();
	}
}
