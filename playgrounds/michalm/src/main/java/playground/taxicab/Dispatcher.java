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

import org.apache.log4j.Logger;
import org.matsim.core.api.experimental.events.EventsManager;

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

	@Override
	public void handleEvent( PassengerTaxiRequestEvent ev ) {
		Logger.getLogger("").warn(" entering handleEvent ...") ;
		// do something
		// ...
		// eventually recruit a taxi:
		this.eventsManager.processEvent( new DispatcherTaxiRequestEvent( ev.getTime(),ev.getLinkId(), ev.getPersonId() ) ) ;
	}
}

