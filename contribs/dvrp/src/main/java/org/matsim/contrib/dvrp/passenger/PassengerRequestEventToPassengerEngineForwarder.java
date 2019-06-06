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

/**
 * This class has a multi-iteration scope, whereas {@link PassengerEngine} has only a single-iteration scope.
 * <p>
 * PassengerRequestEventToPassengerEngineForwarder is registered at {@link org.matsim.core.api.experimental.events.EventsManager}
 * and forwards events to PassengerEngine
 *
 * @author Michal Maciejewski (michalm)
 */
public class PassengerRequestEventToPassengerEngineForwarder
		implements PassengerRequestRejectedEventHandler, PassengerRequestScheduledEventHandler {
	private final Map<String, PassengerEngine> passengerEngines = new HashMap<>();

	@Override
	public void handleEvent(PassengerRequestRejectedEvent event) {
		passengerEngines.get(event.getMode()).notifyPassengerRequestRejected(event);
	}

	@Override
	public void handleEvent(PassengerRequestScheduledEvent event) {
		passengerEngines.get(event.getMode()).notifyPassengerRequestScheduled(event);
	}

	void registerPassengerEngineEventsHandler(PassengerEngine passengerEngine) {
		passengerEngines.put(passengerEngine.getMode(), passengerEngine);
	}

	@Override
	public void reset(int iteration) {
		passengerEngines.clear();
	}
}
