/* *********************************************************************** *
 * project: org.matsim.*
 * EventDepTimeFilter.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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
package playground.yu.visum.filter;

import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.Event;
import org.matsim.core.utils.misc.Time;

/**
 * @author ychen
 * 
 */
public class EventDepTimeFilter extends EventFilterA {
	private static double criterionMAX = Time.parseTime("08:00");

	private static double criterionMIN = Time.parseTime("06:00");

	@Override
	public boolean judge(Event event) {
		if (event.getClass().equals(PersonDepartureEvent.class)) {
			return event.getTime() < criterionMAX
					&& event.getTime() > criterionMIN;
		}
		return isResult();
	}

}
