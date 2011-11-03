/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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

package playground.taxicab;

import org.matsim.core.api.experimental.events.Event;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.handler.EventHandler;

/**
 * @author nagel
 *
 */
public class Dispatcher implements PassengerTaxiRequestEventHandler {

	private EventsManager eventsManager;

	public Dispatcher( EventsManager evm ) {
		this.eventsManager = evm ;
	}

	@Override
	public void reset(int iteration) {
	}

	public void handleEvent( PassengerTaxiRequestEvent ev2 ) {
		// do something
		// ...
		// eventually recruit a taxi:
		this.eventsManager.processEvent( new DispatcherTaxiRequestEvent(
				ev2.getTime(),ev2.getLinkId(), ev2.getPersonId()
		) ) ;
	}
}

