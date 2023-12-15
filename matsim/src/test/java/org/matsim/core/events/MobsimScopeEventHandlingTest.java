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

import static org.mockito.Mockito.*;

import org.junit.jupiter.api.Test;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.controler.events.AfterMobsimEvent;

/**
 * @author Michal Maciejewski (michalm)
 */
public class MobsimScopeEventHandlingTest {
	private final EventsManager eventsManager = mock(EventsManager.class);
	private final MobsimScopeEventHandling eventHandling = new MobsimScopeEventHandling(eventsManager);
	private final MobsimScopeEventHandler handler = mock(MobsimScopeEventHandler.class);

	@Test
	void test_addMobsimScopeHandler() {
		eventHandling.addMobsimScopeHandler(handler);

		verify(eventsManager, times(1)).addHandler(argThat(arg -> arg == handler));
	}

	@Test
	void test_notifyAfterMobsim_oneHandler() {
		eventHandling.addMobsimScopeHandler(handler);
		eventHandling.notifyAfterMobsim(new AfterMobsimEvent(null, 99, false));

		verify(eventsManager, times(1)).removeHandler(argThat(arg -> arg == handler));
		verify(handler, times(1)).cleanupAfterMobsim(intThat(arg -> arg == 99));
	}

	@Test
	void test_notifyAfterMobsim_noHandlersAfterRemoval() {
		eventHandling.addMobsimScopeHandler(handler);
		eventHandling.notifyAfterMobsim(new AfterMobsimEvent(null, 99, false));

		verify(eventsManager, times(1)).removeHandler(any());
		verify(handler, times(1)).cleanupAfterMobsim(anyInt());

		//no handlers in this iteration, so no new calls to removeHandler() and cleanupAfterMobsim()
		eventHandling.notifyAfterMobsim(new AfterMobsimEvent(null, 100, false));

		verify(eventsManager, times(1)).removeHandler(any());
		verify(handler, times(1)).cleanupAfterMobsim(anyInt());
	}
}