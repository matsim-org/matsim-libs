/* ********************************************************************** *
 * project: org.matsim.*
 * OperationsJfrTimer.java
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

import org.matsim.core.controler.events.ReplanningEvent;
import org.matsim.core.controler.events.ScoringEvent;
import org.matsim.core.controler.events.ShutdownEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.ReplanningListener;
import org.matsim.core.controler.listener.ScoringListener;
import org.matsim.core.controler.listener.ShutdownListener;
import org.matsim.core.controler.listener.StartupListener;

class OperationsJfrTimer implements StartupListener, ShutdownListener, ReplanningListener, ScoringListener {

	final StartListener startListener = new StartListener();

	static class StartListener implements StartupListener, ShutdownListener, ReplanningListener, ScoringListener {

		private ReplanningListenersJfrEvent replanningListenersJfrEvent = null;
		private ScoringListenersJfrEvent scoringListenersJfrEvent = null;
		private MatsimStartupListenersJfrEvent startupJfrEvent = null;
		private MatsimShutdownListenersJfrEvent shutdownJfrEvent = null;

		/**
		 * @return Highest possible priority to start before most other listeners.
		 */
		@Override
		public double priority() {
			return Double.POSITIVE_INFINITY;
		}

		@Override
		public void notifyReplanning(ReplanningEvent replanningEvent) {
			if (replanningListenersJfrEvent != null) {
				throw new IllegalStateException("Another replanning started, while still waiting for the end of a previous one. Are the listeners registered properly?");
			}
			replanningListenersJfrEvent = new ReplanningListenersJfrEvent();
			replanningListenersJfrEvent.begin();
		}

		@Override
		public void notifyScoring(ScoringEvent scoringEvent) {
			if (scoringListenersJfrEvent != null) {
				throw new IllegalStateException("Another scoring started, while still waiting for the end of a previous one. Are the listeners registered properly?");
			}
			scoringListenersJfrEvent = new ScoringListenersJfrEvent();
			scoringListenersJfrEvent.begin();
		}

		@Override
		public void notifyShutdown(ShutdownEvent shutdownEvent) {
			if (shutdownJfrEvent != null) {
				throw new IllegalStateException("Another shutdown started, shouldn't there only be one?");
			}
			shutdownJfrEvent = new MatsimShutdownListenersJfrEvent();
			shutdownJfrEvent.begin();
		}

		@Override
		public void notifyStartup(StartupEvent startupEvent) {
			if (startupJfrEvent != null) {
				throw new IllegalStateException("Another startup started, shouldn't there only be one?");
			}
			startupJfrEvent = new MatsimStartupListenersJfrEvent();
			startupJfrEvent.begin();
		}
	}

	/**
	 * @return Lowest possible priority to end after most other listeners.
	 */
	@Override
	public double priority() {
		return Double.NEGATIVE_INFINITY;
	}

	@Override
	public void notifyReplanning(ReplanningEvent replanningEvent) {
		if (startListener.replanningListenersJfrEvent == null) {
			throw new IllegalStateException("Replanning ended, before its start was noticed. Are the listeners registered properly?");
		}
		startListener.replanningListenersJfrEvent.commit();
		startListener.replanningListenersJfrEvent = null;
	}

	@Override
	public void notifyScoring(ScoringEvent scoringEvent) {
		if (startListener.scoringListenersJfrEvent == null) {
			throw new IllegalStateException("Scoring ended, before its start was noticed. Are the listeners registered properly?");
		}
		startListener.scoringListenersJfrEvent.commit();
		startListener.scoringListenersJfrEvent = null;
	}

	@Override
	public void notifyShutdown(ShutdownEvent shutdownEvent) {
		if (startListener.shutdownJfrEvent == null) {
			throw new IllegalStateException("Shutdown ended, before its start was noticed. Are the listeners registered properly?");
		}
		startListener.shutdownJfrEvent.commit();
		startListener.shutdownJfrEvent = null;
	}

	@Override
	public void notifyStartup(StartupEvent startupEvent) {
		if (startListener.startupJfrEvent == null) {
			throw new IllegalStateException("Startup ended, before its start was noticed. Are the listeners registered properly?");
		}
		startListener.startupJfrEvent.commit();
		startListener.startupJfrEvent = null;
	}

}
