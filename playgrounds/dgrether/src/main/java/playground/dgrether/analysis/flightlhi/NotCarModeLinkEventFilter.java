/* *********************************************************************** *
 * project: org.matsim.*
 * NotCarModeLinkEventHandler
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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
package playground.dgrether.analysis.flightlhi;

import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.PersonStuckEvent;
import org.matsim.api.core.v01.events.Event;

import playground.dgrether.events.filters.EventFilter;

/**
 * @author dgrether
 * 
 */
public class NotCarModeLinkEventFilter implements EventFilter {

	@Override
	public boolean doProcessEvent(Event event) {
		if (event instanceof PersonDepartureEvent) {
			PersonDepartureEvent e = (PersonDepartureEvent) event;
			if (e.getLegMode().equals(TransportMode.car))
				return false;
		}
		else if (event instanceof PersonArrivalEvent) {
			PersonArrivalEvent e = (PersonArrivalEvent) event;
			if (e.getLegMode().equals(TransportMode.car))
				return false;
		}
		else if (event instanceof PersonStuckEvent) {
			PersonStuckEvent e = (PersonStuckEvent) event;
			if (e.getLegMode().equals(TransportMode.car))
				return false;
		}
		return true;
	}

}
