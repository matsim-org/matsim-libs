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
public class Dispatcher implements EventHandler {

	private EventsManager eventsManager;

	/**
	 * 
	 */
	public Dispatcher( EventsManager evm ) {
		this.eventsManager = evm ;
	}

	/* (non-Javadoc)
	 * @see org.matsim.core.events.handler.EventHandler#reset(int)
	 */
	@Override
	public void reset(int iteration) {
		// yyyy Auto-generated method stub
		throw new UnsupportedOperationException();
	}
	
	public void handleEvent( Event ev ) {
		if ( ev instanceof PassengerTaxiRequestEvent ) {
			// do something

			// eventually recruit a taxi:
			this.eventsManager.processEvent( new DispatcherTaxiRequestEvent(ev.getTime()) ) ;
		}
		
//		else ignore the event
	}
	

}
