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

import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.internal.HasPersonId;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;



/**
 * @author johannes
 *
 */
public abstract class TransitLineEvent extends Event implements HasPersonId {
	
	public static final String PERSON_KEY = "person";
	
	public static final String LINE_KEY = "line";
	
	public static final String ROUTE_KEY = "route";
	
	public static final String STOP_KEY = "stop";
	
	private final Person person;
	
	private final TransitLine line;
	
	private final TransitRoute route;
	
	private final TransitStopFacility stop;
	
	public TransitLineEvent(double time, Person person, TransitLine line, TransitRoute route, TransitStopFacility stop) {
		super(time);
		this.person = person;
		this.line = line;
		this.route = route;
		this.stop = stop;
	}
	
	@Override
	public Map<String, String> getAttributes() {
		Map<String, String> attr = super.getAttributes();
		attr.put(PERSON_KEY, this.getPersonId().toString());
		attr.put(LINE_KEY, line.getId().toString());
		attr.put(ROUTE_KEY, route.getId().toString());
		attr.put(STOP_KEY, stop.getId().toString());
		return attr;
	}
	/* (non-Javadoc)
	 * @see org.matsim.core.api.internal.HasPersonId#getDriverId()
	 */
	@Override
	public Id getPersonId() {
		return person.getId();
	}

	/**
	 * @return the line
	 */
	public TransitLine getLine() {
		return line;
	}

	/**
	 * @return the route
	 */
	public TransitRoute getRoute() {
		return route;
	}

	/**
	 * @return the stop
	 */
	public TransitStopFacility getStop() {
		return stop;
	}
}
