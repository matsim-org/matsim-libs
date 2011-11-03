/* *********************************************************************** *
 * project: michalm
 * MyEventsReporter.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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
import org.matsim.core.api.experimental.events.Event;
import org.matsim.core.api.experimental.events.PersonEvent;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.events.handler.BasicEventHandler;

/**
 * @author nagel
 *
 */
public class MyEventsReporter implements BasicEventHandler {

	@Override
	public void handleEvent(Event event) {
		if ( event instanceof PersonEvent ) {
			if ( ((PersonEvent)event).getPersonId().equals( new IdImpl("3") ) ) {
				Logger.getLogger("").warn( event ) ;
			}
		}
	}

	@Override
	public void reset(int iteration) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException();
	}

}
