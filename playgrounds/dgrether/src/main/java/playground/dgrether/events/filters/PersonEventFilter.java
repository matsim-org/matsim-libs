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
import org.matsim.core.api.experimental.events.Event;
import org.matsim.core.api.experimental.events.PersonEvent;


/**
 * @author dgrether
 *
 */
public class PersonEventFilter implements EventFilter {

	private Set<Id> personIds;

	public PersonEventFilter(final Set<Id> personIDs) {
		this.personIds = personIDs;
	}
	
	
	@Override
	public boolean doProcessEvent(Event event) {
		if (event instanceof PersonEvent) {
			return this.personIds.contains(((PersonEvent)event).getPersonId());
		}
		return false;
	}
	
	
}
