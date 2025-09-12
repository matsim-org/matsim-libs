/* ********************************************************************** *
 * project: org.matsim.*
 * IterationJfrTimer.java
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

import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.IterationStartsListener;

class IterationJfrTimer implements IterationEndsListener, IterationStartsListener {

	final StartListener startListener = new StartListener();

	static class StartListener implements IterationStartsListener, IterationEndsListener {

		private IterationJfrEvent event = null;
		private IterationStartsListenersJfrEvent startsListenersEvent = null;
		private IterationEndsListenersJfrEvent endsListenersEvent = null;

		/**
		 * @return Almost highest possible priority to start before most other listeners.
		 * Run only after {@link org.matsim.contrib.profiling.instrument.EnableProfilingModule#ProfilingStartListener}.
		 */
		@Override
		public double priority() {
			return Double.MAX_VALUE;
		}

		/**
		 * Start of IterationStartsListeners
		 */
		@Override
		public void notifyIterationStarts(IterationStartsEvent iterationStartsEvent) {
			if (event != null || startsListenersEvent != null) {
				throw new IllegalStateException("Another iteration started, before an ends listener was called. Are the listeners registered properly?");
			}
			event = new IterationJfrEvent(iterationStartsEvent.getIteration());
			event.begin();
			startsListenersEvent = new IterationStartsListenersJfrEvent();
			startsListenersEvent.begin();
		}

		/**
		 * Start of IterationEndsListeners
		 */
		@Override
		public void notifyIterationEnds(IterationEndsEvent iterationEndsEvent) {
			if (endsListenersEvent != null) {
				throw new IllegalStateException("Another batch of IterationEndsListeners called, while still waiting for the previous IterationEndsListeners to end. Are the listeners registered properly?");
			}
			endsListenersEvent = new IterationEndsListenersJfrEvent();
			endsListenersEvent.begin();
		}
	}


	/**
	 * @return Almost lowest possible priority to end after most other listeners.
	 * Only run before {@link org.matsim.contrib.profiling.instrument.EnableProfilingModule#ProfilingEndListener}.
	 */
	@Override
	public double priority() {
		return -Double.MAX_VALUE;
	}

	/**
	 * End of IterationEndsListeners
	 */
	@Override
	public void notifyIterationEnds(IterationEndsEvent iterationEndsEvent) {
		if (startListener.event == null || startListener.endsListenersEvent == null) {
			throw new IllegalStateException("Iteration ended, before a start listener was called. Are the listeners registered properly?");
		}
		startListener.endsListenersEvent.commit();
		startListener.event.commit();
		startListener.endsListenersEvent = null;
		startListener.event = null;
	}

	/**
	 * End of IterationStartsListeners
	 */
	@Override
	public void notifyIterationStarts(IterationStartsEvent iterationStartsEvent) {
		if (startListener.startsListenersEvent == null) {
			throw new IllegalStateException("End of StartsListeners called, before the starts listeners were started. Are the listeners registered properly?");
		}

		startListener.startsListenersEvent.commit();
		startListener.startsListenersEvent = null;
	}

}
