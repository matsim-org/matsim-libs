/* *********************************************************************** *
 * project: org.matsim.*
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
package playground.dgrether.events.filters;

import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.PersonStuckEvent;
import org.matsim.api.core.v01.events.VehicleEntersTrafficEvent;
import org.matsim.api.core.v01.population.Person;


/**
 * @author dgrether
 *
 */
public class PersonEventFilter implements EventFilter {

	private Set<Id<Person>> personIds;

	public PersonEventFilter(final Set<Id<Person>> personIDs) {
		this.personIds = personIDs;
	}
	
	
	@Override
	public boolean doProcessEvent(Event event) {
		if (event instanceof VehicleEntersTrafficEvent) {
			VehicleEntersTrafficEvent e = (VehicleEntersTrafficEvent) event;
			Id<Person> personId = e.getPersonId();
			this.personIds.contains(personId);
		} else if (event instanceof PersonDepartureEvent) {
			PersonDepartureEvent e = (PersonDepartureEvent) event;
			Id<Person> personId = e.getPersonId();
			this.personIds.contains(personId);
		} else if (event instanceof PersonArrivalEvent) {
			PersonArrivalEvent e = (PersonArrivalEvent) event;
			Id<Person> personId = e.getPersonId();
			this.personIds.contains(personId);
		} else if (event instanceof ActivityStartEvent) {
			ActivityStartEvent e = (ActivityStartEvent) event;
			Id<Person> personId = e.getPersonId();
			this.personIds.contains(personId);
		} else if (event instanceof ActivityEndEvent) {
			ActivityEndEvent e = (ActivityEndEvent) event;
			Id<Person> personId = e.getPersonId();
			this.personIds.contains(personId);
		} else if (event instanceof PersonStuckEvent) {
			PersonStuckEvent e = (PersonStuckEvent) event;
			Id<Person> personId = e.getPersonId();
			this.personIds.contains(personId);
		/* the following was here until we removed the person from link enter and leave events.
		   seems that it works without... Theresa oct'2015 */
//		} else if (event instanceof LinkEnterEvent) {
//			LinkEnterEvent e = (LinkEnterEvent) event;
//			Id<Person> personId = e.getDriverId();
//			this.personIds.contains(personId);
//		} else if (event instanceof LinkLeaveEvent) {
//			LinkLeaveEvent e = (LinkLeaveEvent) event;
//			Id<Person> personId = e.getDriverId();
//			this.personIds.contains(personId);
		} else {
			return false;
		}
		return false;
	}
	
	
}
