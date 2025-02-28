/* ********************************************************************** *
 * project: org.matsim.*
 * ProfilingEventsModule.java
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

import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.events.*;
import org.matsim.core.controler.listener.*;
import org.matsim.core.mobsim.framework.events.MobsimBeforeCleanupEvent;
import org.matsim.core.mobsim.framework.events.MobsimInitializedEvent;
import org.matsim.core.mobsim.framework.listeners.MobsimBeforeCleanupListener;
import org.matsim.core.mobsim.framework.listeners.MobsimInitializedListener;

/**
 * Hook into MATSim Listeners to create JFR profiling events at different phases within MATSim.
 */
public class ProfilingEventsModule extends AbstractModule {

	@Override
	public void install() {
		var iterationTimer = new JFRIterationTimer();
		addControlerListenerBinding().toInstance(iterationTimer.startListener);
		addControlerListenerBinding().toInstance(iterationTimer);
		addControlerListenerBinding().to(MatsimEvents.class);
		addMobsimListenerBinding().to(MobsimTimer.class);
	}

	static class JFRIterationTimer implements IterationEndsListener {

		private final StartListener startListener = new StartListener();

		static class StartListener implements IterationStartsListener {

			private JFRIterationEvent event = null;

			/**
			 * @return Almost highest possible priority to start before most other listeners.
			 * 		   Run only after instrumentation start listener.
			 */
			@Override
			public double priority() {
				return Double.MAX_VALUE - 100;
			}

			@Override
			public void notifyIterationStarts(IterationStartsEvent iterationStartsEvent) {
				if (event != null) {
					event.commit();
				}
				event = new JFRIterationEvent(iterationStartsEvent.getIteration());
				event.begin();
			}

		}

		/**
		 * @return Almost lowest possible priority to end after most other listeners.
		 * 		   Only run before instrumentation end listener.
		 */
		@Override
		public double priority() {
			return Double.MIN_VALUE + 100;
		}

		@Override
		public void notifyIterationEnds(IterationEndsEvent iterationEndsEvent) {
			if (startListener.event != null) {
				startListener.event.commit();
				startListener.event = null;
			}
		}

	}

	private static final class MobsimTimer implements MobsimInitializedListener, MobsimBeforeCleanupListener {

		private JFRMobsimEvent event = null;

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
			event = new JFRMobsimEvent();
			event.begin();
		}
	}

	private static final class MatsimEvents implements StartupListener, ShutdownListener, ReplanningListener, ScoringListener {

		@Override
		public void notifyReplanning(ReplanningEvent replanningEvent) {
			JFRMatsimEvent.create("replanning").commit();
		}

		@Override
		public void notifyScoring(ScoringEvent scoringEvent) {
			JFRMatsimEvent.create("scoring").commit();
		}

		@Override
		public void notifyShutdown(ShutdownEvent shutdownEvent) {
			JFRMatsimEvent.create("shutdown").commit();
		}

		@Override
		public void notifyStartup(StartupEvent startupEvent) {
			JFRMatsimEvent.create("startup").commit();
		}
	}
}
