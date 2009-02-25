/* *********************************************************************** *
 * project: org.matsim.*
 * InteractionHandler.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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
package playground.johannes.interaction;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.matsim.events.ActEndEvent;
import org.matsim.events.ActStartEvent;
import org.matsim.events.handler.ActEndEventHandler;
import org.matsim.events.handler.ActStartEventHandler;
import org.matsim.facilities.Facility;
import org.matsim.interfaces.core.v01.Person;

/**
 * @author illenberger
 *
 */
public class InteractionHandler implements ActStartEventHandler,
		ActEndEventHandler {

	private InteractionSelector selector;
	
	private Interactor interactor;
	
	private Map<Facility, PhysicalFacility> facilities;
	
	public InteractionHandler(InteractionSelector selector, Interactor interactor) {
		this.selector = selector;
		this.interactor = interactor;
		this.facilities = new HashMap<Facility, PhysicalFacility>();
	}
	
	public void handleEvent(ActStartEvent event) {
		Facility f = event.act.getFacility();
		PhysicalFacility pf = facilities.get(f);
		if(pf == null) {
			pf = new PhysicalFacility();
			facilities.put(f, pf);
		}
		
		pf.enterPerson(event.agent, event.time);
	}

	public void reset(int iteration) {
		for(PhysicalFacility pf : facilities.values())
			pf.reset();

	}

	public void handleEvent(ActEndEvent event) {
		Facility f = event.act.getFacility();
		PhysicalFacility pf = facilities.get(f);
		
		if(pf == null)
			throw new RuntimeException("Tried to remove a visitor from a non-existing physical facility!");
		else {
			pf.leavePerson(event.agent, event.time);
		}

	}

	private class PhysicalFacility {
		
		private Map<Person, Visitor> visitors = new HashMap<Person, Visitor>();
		
		private void enterPerson(Person p, double time) {
			visitors.put(p, new Visitor(p, time));
		}
		
		private void leavePerson(Person p, double time) {
			Visitor v = visitors.remove(p);
			if(v == null)
				throw new RuntimeException("Tried to remove a visitor that did not enter this facility!");
			else {
				Collection<Visitor> targets = selector.select(v, visitors.values());
				for(Visitor target : targets) {
					double startTime = Math.max(v.getEnterTime(), target.getEnterTime());
					interactor.interact(v.getPerson(), target.getPerson(), startTime, time);
				}
			}
		}
		
		private void reset() {
			visitors.clear();
		}
	}
}
