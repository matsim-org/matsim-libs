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

package playground.marcel.filters.filter;

import org.matsim.events.BasicEvent;
import org.matsim.events.EventAgentDeparture;
import org.matsim.gbl.Gbl;

/**
 * @author ychen
 * 
 */
public class EventDepTimeFilter extends EventFilterA {
	private static double criterionMAX = Gbl.parseTime("08:00");

	private static double criterionMIN = Gbl.parseTime("06:00");

	/* (non-Javadoc)
	 * @see org.matsim.playground.filters.filter.EventFilter#judge(org.matsim.demandmodeling.events.BasicEvent)
	 */
	@Override
	public boolean judge(BasicEvent event) {
		if (event.getClass().equals(EventAgentDeparture.class)) {
			return (event.time<criterionMAX)&&(event.time>criterionMIN);
		}
		return isResult();
	}

}
