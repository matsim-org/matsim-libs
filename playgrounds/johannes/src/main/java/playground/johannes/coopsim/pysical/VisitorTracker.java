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
package playground.johannes.coopsim.pysical;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

	private Map<Id, PersonData> personData;
	
	private Map<Id, List<Visitor>> facilityData;
	
	private String ignoreType = "home";
	
	public void setIgnoreType(String type) {
		this.ignoreType = type;
	}
	
	@Override
	public void handleEvent(ActivityStartEvent event) {
		if (!event.getActType().equals(ignoreType)) {
			Visitor visitor = new Visitor();
			visitor.personId = event.getPersonId();
			visitor.startEvent = event;
			visitor.endEvent = null;

			PersonData data = personData.get(event.getPersonId());
			if(data == null) {
				data = new PersonData();
				data.visits = new ArrayList<VisitorTracker.Visit>();
				personData.put(event.getPersonId(), data);
			}
			data.currentVisitor = visitor;
			/*
			 * add visitor to facility
			 */
			List<Visitor> visitors = facilityData.get(event.getFacilityId());
			if (visitors == null) {
				visitors = new ArrayList<Visitor>(100);
				facilityData.put(event.getFacilityId(), visitors);
			}
			visitors.add(visitor);
		}
	}

	@Override
	public void reset(int iteration) {
		personData = new HashMap<Id, VisitorTracker.PersonData>();
		facilityData = new HashMap<Id, List<Visitor>>();

	}

	@Override
	public void handleEvent(ActivityEndEvent event) {
		if (!event.getActType().equals(ignoreType)) {
			PersonData data = personData.get(event.getPersonId());
			if (data != null) {
				data.currentVisitor.endEvent = event;
				/*
				 * add visit to person
				 */
				Visit visit = new Visit();
				visit.facilityId = event.getFacilityId();
				visit.startEvent = data.currentVisitor.startEvent;
				visit.endEvent = event;

				data.visits.add(visit);
			} else {
				logger.warn(String.format("No visitor found for person %1$s.", event.getPersonId()));
			}
		}

	}
	
	public double timeOverlap(Person person, Person alter) {
		PersonData data = personData.get(person.getId());
		if(data == null)
			return 0;
		
		double sum = 0;
		for (Visit visit : data.visits) {
			List<Visitor> visitors = facilityData.get(visit.facilityId);
			for (Visitor visitor : visitors) {
				if (alter.getId().equals(visitor.personId)) {
					double start = Math.max(visit.startEvent.getTime(), visitor.startEvent.getTime());
					double end = Math.min(visit.endEvent.getTime(), visitor.endEvent.getTime());
					double delta = Math.max(0.0, end - start);
					sum += delta;
					break;
				}

			}
		}

		return sum;
	}
	
	public double timeOverlap(Person person, Collection<Person> alters) {
		PersonData data = personData.get(person.getId());
		if(data == null)
			return 0;

		double sum = 0;
		for (Visit visit : data.visits) {
			List<Visitor> visitors = facilityData.get(visit.facilityId);
			for (Visitor visitor : visitors) {
				for (Person alter : alters) {
					if (alter.getId().equals(visitor.personId)) {
						double start = Math.max(visit.startEvent.getTime(), visitor.startEvent.getTime());
						double end = Math.min(visit.endEvent.getTime(), visitor.endEvent.getTime());
						double delta = Math.max(0.0, end - start);
						sum += delta;
						break;
					}
				}

			}
		}

		return sum;
	}
	
	private class PersonData {
		
		private Visitor currentVisitor;
		
		private List<Visit> visits;
		
	}
	
	private class Visit {
		
		private Id facilityId;
		
		private ActivityStartEvent startEvent;
		
		private ActivityEndEvent endEvent;
	}
	
	private class Visitor {
		
		private Id personId;
		
		private ActivityStartEvent startEvent;
		
		private ActivityEndEvent endEvent;
	}

}
