/* *********************************************************************** *
 * project: org.matsim.*
 * VisitorTracker.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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
package playground.johannes.socialnetworks.sim.interaction;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.ActivityEndEvent;
import org.matsim.core.api.experimental.events.ActivityStartEvent;
import org.matsim.core.api.experimental.events.handler.ActivityEndEventHandler;
import org.matsim.core.api.experimental.events.handler.ActivityStartEventHandler;

/**
 * @author illenberger
 *
 */
public class VisitorTracker implements ActivityStartEventHandler, ActivityEndEventHandler {
	
	private static final Logger logger = Logger.getLogger(VisitorTracker.class);

	private Map<Id, Visitor> startEvents;
	
	private Map<Id, Set<Visit>> visits;
	
	private Map<Id, Set<Visitor>> visitors;
	
	@Override
	public void handleEvent(ActivityStartEvent event) {
		Visitor visitor = new Visitor();
		visitor.person = event.getPersonId();
		visitor.startEvent = event;
		visitor.endEvent = null;
		
		startEvents.put(event.getPersonId(), visitor);
		/*
		 * add visitor to facility
		 */
		Set<Visitor> facilityVisitors = visitors.get(event.getFacilityId());
		if(facilityVisitors == null) {
			facilityVisitors = new HashSet<Visitor>();
			visitors.put(event.getFacilityId(), facilityVisitors);
		}
		facilityVisitors.add(visitor);
	}

	@Override
	public void reset(int iteration) {
		startEvents = new HashMap<Id, Visitor>();
		visits = new HashMap<Id, Set<Visit>>();
		visitors = new HashMap<Id, Set<Visitor>>();

	}

	@Override
	public void handleEvent(ActivityEndEvent event) {
		Visitor visitor = startEvents.get(event.getPersonId());
		if(visitor != null) {
			visitor.endEvent = event;
			/*
			 * add visit to person
			 */
			Set<Visit> facilityVisits = visits.get(event.getPersonId());
			if(facilityVisits == null) {
				facilityVisits = new HashSet<Visit>();
				visits.put(event.getPersonId(), facilityVisits);
			}
			
			Visit visit = new Visit();
			visit.facility = event.getFacilityId();
			visit.startEvent = visitor.startEvent;
			visit.endEvent = event;
			
			facilityVisits.add(visit);
		} else {
			logger.warn(String.format("No visitor found for person %1$s.", event.getPersonId()));
		}

	}
	
	public double timeOverlap(Person person, Set<Person> friends) {
		Set<Visit> facilityVisits = visits.get(person.getId());
		double sum = 0;
		for (Visit visit : facilityVisits) {
			if (visit.startEvent.getActType().equalsIgnoreCase("leisure")) {

				Set<Visitor> facilityVisitors = visitors.get(visit.facility);
				for (Visitor visitor : facilityVisitors) {
					if (visitor.startEvent.getActType().equalsIgnoreCase("leisure")) {

						for (Person friend : friends) {
							if (friend.getId().equals(visitor.person)) {
								double start = Math.max(visit.startEvent.getTime(), visitor.startEvent.getTime());
								double end = Math.min(visit.endEvent.getTime(), visitor.endEvent.getTime());
								double delta = Math.max(0.0, end - start);
								sum += delta;
								break;
							}
						}
					}
				}
			}
		}
		
		return sum;
	}
	
	private class Visit {
		
		private Id facility;
		
		private ActivityStartEvent startEvent;
		
		private ActivityEndEvent endEvent;
	}
	
	private class Visitor {
		
		private Id person;
		
		private ActivityStartEvent startEvent;
		
		private ActivityEndEvent endEvent;
	}

}
