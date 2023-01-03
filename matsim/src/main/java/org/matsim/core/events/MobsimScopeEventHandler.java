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

import org.matsim.core.controler.events.AfterMobsimEvent;
import org.matsim.core.events.handler.EventHandler;

/**
 * Event handlers implementing this class are instantiated shortly before the mobsim is started, and removed as one of the {@link AfterMobsimEvent}s.
 *
 * @author Michal Maciejewski (michalm)
 */
public interface MobsimScopeEventHandler extends EventHandler {
	/**
	 * Under normal circumstances, this method will never be called. All mobsim-scoped event handlers are removed
	 * on AfterMobsimEvent, so before the new mobsim initialisation phase.
	 *
	 * @param iteration the up-coming iteration from which up-coming events will be from.
	 */
	@Deprecated //made deprecated to minimise chances someone overrides this method (cannot be final)
	@Override
	default void reset(int iteration) {
		throw new IllegalStateException("This handler should have been unregistered on AfterMobsimEvent");
	}

	/**
	 * Gives the event handler the possibility to clean up its internal state.
	 * This method is called directly after the handler is removed from event handlers (on {@link AfterMobsimEvent} ).
	 *
	 * @param iteration the iteration in which the handler was instantiated
	 */
	default void cleanupAfterMobsim(int iteration) {
	}
}
