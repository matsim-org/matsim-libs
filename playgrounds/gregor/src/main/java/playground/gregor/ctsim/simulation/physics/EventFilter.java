package playground.gregor.ctsim.simulation.physics;
/* *********************************************************************** *
 * project: org.matsim.*
 *
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

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.events.EventsReaderXMLv1;
import org.matsim.core.events.algorithms.EventWriterXML;
import org.matsim.core.events.handler.EventHandler;

import java.util.HashSet;
import java.util.Set;

/**
 * Created by laemmel on 16/11/15.
 */
public class EventFilter implements EventHandler, PersonDepartureEventHandler, PersonArrivalEventHandler, LinkEnterEventHandler, LinkLeaveEventHandler {

	private boolean it1 = true;

	private Set<Id<Person>> pers = new HashSet<>();

	private EventsManager em;


	public static void main(String[] args) {
		String events = "/Users/laemmel/devel/padang/output/ITERS/it.100/100.events.xml.gz";

		EventFilter filter = new EventFilter();

		{
			EventsManager em = new EventsManagerImpl();
			em.addHandler(filter);
			EventsReaderXMLv1 r = new EventsReaderXMLv1(em);
			r.parse(events);
		}
		filter.it1 = false;
		EventsManager em = new EventsManagerImpl();
		em.addHandler(filter);
		EventsReaderXMLv1 r = new EventsReaderXMLv1(em);
		EventsManager em2 = new EventsManagerImpl();
		filter.em = em2;
		EventWriterXML w = new EventWriterXML("/Users/laemmel/devel/padang/debug/events.xml.gz");
		em2.addHandler(w);
		r.parse(events);
	}

	@Override
	public void reset(int iteration) {

	}

	@Override
	public void handleEvent(LinkEnterEvent event) {
		Id<Person> p = Id.createPersonId(event.getVehicleId());
		if (!it1 && pers.contains(p)) {
			em.processEvent(event);
		}

	}

	@Override
	public void handleEvent(LinkLeaveEvent event) {
		Id<Person> p = Id.createPersonId(event.getVehicleId());
		if (!it1 && pers.contains(p)) {
			em.processEvent(event);
		}
	}

	@Override
	public void handleEvent(PersonArrivalEvent event) {
		if (!it1 && pers.contains(event.getPersonId())) {
			em.processEvent(event);
		}
		if (it1) {
			this.pers.remove(event.getPersonId());
		}
	}

	@Override
	public void handleEvent(PersonDepartureEvent event) {
		if (!it1 && pers.contains(event.getPersonId())) {
			em.processEvent(event);
		}
		if (it1) {
			this.pers.add(event.getPersonId());
		}
	}
}
