/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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

/**
 * 
 */
package playground.johannes.gsv.sim;

import org.matsim.api.core.v01.population.Person;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

/**
 * @author johannes
 *
 */
public class TransitAlightEvent extends TransitLineEvent {

	public static final String TYPE = "personalight";

	public TransitAlightEvent(double time, Person person, TransitLine line,
			TransitRoute route, TransitStopFacility stop) {
		super(time, person, line, route, stop);
		// TODO Auto-generated constructor stub
	}
	
	/* (non-Javadoc)
	 * @see org.matsim.api.core.v01.events.Event#getEventType()
	 */
	@Override
	public String getEventType() {
		return TYPE;
	}

}
