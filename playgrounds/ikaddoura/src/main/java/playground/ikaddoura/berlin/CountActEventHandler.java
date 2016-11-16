/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
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

package playground.ikaddoura.berlin;

import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.handler.ActivityStartEventHandler;
import org.matsim.api.core.v01.population.Person;

/**
* @author ikaddoura
*/

public class CountActEventHandler implements ActivityStartEventHandler {
	
	private Map<Id<Person>, Integer> personId2activityCounter = new HashMap<>();
	
	@Override
	public void reset(int iteration) {
		this.personId2activityCounter.clear();
	}

	@Override
	public void handleEvent(ActivityStartEvent event) {
				
		if (personId2activityCounter.containsKey(event.getPersonId())) {
			personId2activityCounter.put(event.getPersonId(), personId2activityCounter.get(event.getPersonId()) + 1);
		} else {
			personId2activityCounter.put(event.getPersonId(), 1); // whenever an activity start event is thrown, it is already the second (#1) activity
		}			
	}

	public int getActivityCounter(Id<Person> personId) {
		return personId2activityCounter.get(personId);
	}

	public Map<Id<Person>, Integer> getActivityCounter() {
		return personId2activityCounter;
	}

}

