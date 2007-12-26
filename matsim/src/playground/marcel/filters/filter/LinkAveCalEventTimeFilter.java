/* *********************************************************************** *
 * project: org.matsim.*
 * LinkAveCalEventTimeFilter.java
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
import org.matsim.events.EventLinkEnter;
import org.matsim.events.EventLinkLeave;
import org.matsim.gbl.Gbl;

/**
 * @author ychen
 * 
 */
public class LinkAveCalEventTimeFilter extends EventFilterA {
	private static final double criterionMAX = Gbl.parseTime("08:00");

	private static final double criterionMIN = Gbl.parseTime("06:00");

	private boolean judgeTime(double time) {
		return (time < criterionMAX) && (time > criterionMIN);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.matsim.playground.filters.EventFilter#judge(int,
	 *      org.matsim.demandmodeling.events.Event.EnterLinkEventData)
	 */
	@Override
	public boolean judge(BasicEvent event) {
		if ((event.getClass()==(EventLinkEnter.class))
				|| (event.getClass()==(EventLinkLeave.class))) {
			return judgeTime(event.time);
		}
		return isResult();
	}
}
