/*
 * *********************************************************************** *
 * project: org.matsim.*
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
 * *********************************************************************** *
 */

package org.matsim.contrib.dvrp.passenger;

import java.util.HashMap;
import java.util.Map;

import com.google.common.base.Preconditions;

/**
 * This class has a multi-iteration scope, whereas {@link PassengerEngine} has only a single-iteration scope.
 * <p>
 * PassengerRequestEventToPassengerEngineForwarder is registered at {@link org.matsim.core.api.experimental.events.EventsManager}
 * and forwards events to PassengerEngine
 *
 * @author Michal Maciejewski (michalm)
 */
public class PassengerRequestEventForwarder
		implements PassengerRequestRejectedEventHandler, PassengerRequestScheduledEventHandler {

	interface PassengerRequestEventListener {
		void notifyPassengerRequestRejected(PassengerRequestRejectedEvent event);

		void notifyPassengerRequestScheduled(PassengerRequestScheduledEvent event);
	}

	private final Map<String, PassengerRequestEventListener> listeners = new HashMap<>();

	@Override
	public void handleEvent(PassengerRequestRejectedEvent event) {
		listeners.get(event.getMode()).notifyPassengerRequestRejected(event);
	}

	@Override
	public void handleEvent(PassengerRequestScheduledEvent event) {
		listeners.get(event.getMode()).notifyPassengerRequestScheduled(event);
	}

	void registerListenerForMode(String mode, PassengerRequestEventListener listener) {
		Preconditions.checkState(listeners.put(mode, listener) == null, "Listener for mode: %s already registered");
	}

	@Override
	public void reset(int iteration) {
		listeners.clear();
	}
}
