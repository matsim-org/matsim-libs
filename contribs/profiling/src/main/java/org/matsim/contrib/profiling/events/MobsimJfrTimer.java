/* ********************************************************************** *
 * project: org.matsim.*
 * MobsimJfrTimer.java
 *                                                                        *
 * ********************************************************************** *
 *                                                                        *
 * copyright       : (C) 2025 by the members listed in the COPYING,       *
 *                   LICENSE and WARRANTY file.                           *
 * email           : info at matsim dot org                               *
 *                                                                        *
 * ********************************************************************** *
 *                                                                        *
 *   This program is free software; you can redistribute it and/or modify *
 *   it under the terms of the GNU General Public License as published by *
 *   the Free Software Foundation; either version 2 of the License, or    *
 *   (at your option) any later version.                                  *
 *   See also COPYING, LICENSE and WARRANTY file                          *
 *                                                                        *
 * ********************************************************************** */

package org.matsim.contrib.profiling.events;

import org.matsim.core.controler.listener.ControlerListener;
import org.matsim.core.mobsim.framework.events.MobsimBeforeCleanupEvent;
import org.matsim.core.mobsim.framework.events.MobsimInitializedEvent;
import org.matsim.core.mobsim.framework.listeners.MobsimBeforeCleanupListener;
import org.matsim.core.mobsim.framework.listeners.MobsimInitializedListener;

/**
 * Fire a {@link MobsimJfrEvent} at the start and end of mobsim.
 * <p>Caveat: Since the {@link MobsimInitializedListener} and {@link MobsimBeforeCleanupListener} are no {@link ControlerListener}s,
 * it is not possible to use the {@link ControlerListener#priority() priority} to let them run before / after all other listeners.
 */
class MobsimJfrTimer implements MobsimInitializedListener, MobsimBeforeCleanupListener {

	private MobsimJfrEvent event = null;

	@Override
	public void notifyMobsimBeforeCleanup(MobsimBeforeCleanupEvent mobsimBeforeCleanupEvent) {
		if (event != null) {
			event.commit();
			event = null;
		}
	}

	@Override
	public void notifyMobsimInitialized(MobsimInitializedEvent mobsimInitializedEvent) {
		if (event != null) {
			event.commit();
		}
		event = new MobsimJfrEvent();
		event.begin();
	}
}
