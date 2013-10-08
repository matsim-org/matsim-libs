/* *********************************************************************** *
 * project: org.matsim.*
 * PluginEventHandler.java
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

package playground.mrieser.template;

import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;

public class TemplateEventHandler implements PersonDepartureEventHandler {

	private int counter = 0;
	
	public void handleEvent(final PersonDepartureEvent event) {
		this.counter++;
	}

	public void reset(final int iteration) {
		this.counter = 0;
	}
	
	public int getCount() {
		return this.counter;
	}

}
