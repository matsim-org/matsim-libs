/* *********************************************************************** *
 * project: org.matsim.*
 * FacilityVisitors.java
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

package playground.marcel.pt.utils;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.matsim.events.ActEndEvent;
import org.matsim.events.ActStartEvent;
import org.matsim.events.handler.ActEndEventHandler;
import org.matsim.events.handler.ActStartEventHandler;
import org.matsim.facilities.Facility;
import org.matsim.population.Person;

/**
 * Keeps track of all persons that are currently executing an activity at the facility.
 *
 * @author mrieser
 */
public class FacilityVisitors implements ActEndEventHandler, ActStartEventHandler {

	private static final List<Person> EMPTY_LIST= Collections.unmodifiableList(new LinkedList<Person>());

	/** for each facility and for each activity type being performed at a facility the list of persons performing that activity type. */
	private final Map<Facility, Map<String, List<Person>>> facilities = new HashMap<Facility, Map<String, List<Person>>>();

	public void handleEvent(final ActStartEvent event) {
		Facility facility = event.act.getFacility();
		if (facility != null) {
			Map<String, List<Person>> actTypes = this.facilities.get(facility);
			if (actTypes == null) {
				actTypes = new HashMap<String, List<Person>>();
				this.facilities.put(facility, actTypes);
			}
			List<Person> persons = actTypes.get(event.act.getType());
			if (persons == null) {
				persons = new LinkedList<Person>();
				actTypes.put(event.act.getType(), persons);
			}
			persons.add(event.agent);
		}
	}

	public void handleEvent(final ActEndEvent event) {
		Facility facility = event.act.getFacility();
		if (facility != null) {
			List<Person> persons = this.facilities.get(facility).get(event.act.getType());
			if (persons != null) {
				persons.remove(event.agent);
			}
		}
	}

	/**
	 * @param facility
	 * @param actType
	 * @return the list of persons currently executing an activity of the specified type at the given facility, ordered by their activity start time
	 */
	public List<Person> getVisitors(final Facility facility, final String actType) {
		Map<String, List<Person>> actTypes = this.facilities.get(facility);
		if (actTypes == null) {
			return EMPTY_LIST;
		}
		List<Person> persons = actTypes.get(actType);
		if (persons == null) {
			return EMPTY_LIST;
		}
		return Collections.unmodifiableList(persons);
	}

	public void reset(final int iteration) {
		this.facilities.clear();
	}

}
