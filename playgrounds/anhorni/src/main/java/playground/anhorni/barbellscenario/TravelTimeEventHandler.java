/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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
package playground.anhorni.barbellscenario;


import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;


public class TravelTimeEventHandler implements PersonDepartureEventHandler, PersonArrivalEventHandler, LinkEnterEventHandler, LinkLeaveEventHandler {
	private final Map<Id<Person>, PersonDepartureEvent> pendantDepartureEvent = new HashMap<>();
	private final Map<Id<Person>, LinkEnterEvent> pendantLinkEnterEvent = new HashMap<>();
	
	private List<Double> netTTs = new Vector<Double>();
	private List<Double> linkTTs = new Vector<Double>();
	
	@Override
	public void reset(final int iteration) {
		this.pendantDepartureEvent.clear();
		this.pendantLinkEnterEvent.clear();
	}
	
	@Override
	public void handleEvent(PersonArrivalEvent event) {
		double netTT = event.getTime() - this.pendantDepartureEvent.get(event.getPersonId()).getTime();
		this.netTTs.add(netTT);
	}

	@Override
	public void handleEvent(final PersonDepartureEvent event) {
		this.pendantDepartureEvent.put(event.getPersonId(), event);
	}
	
	@Override
	public void handleEvent(LinkEnterEvent event) {
		if (event.getLinkId().compareTo(Id.create(3, Link.class)) == 0) {
			this.pendantLinkEnterEvent.put(event.getDriverId(), event);
		}
	}

	@Override
	public void handleEvent(LinkLeaveEvent event) {
		if (event.getLinkId().compareTo(Id.create(3, Link.class)) == 0) {
			double linkTT = event.getTime() - this.pendantLinkEnterEvent.get(event.getDriverId()).getTime();
			this.linkTTs.add(linkTT);
		}
	}

	public List<Double> getNetTTs() {
		return netTTs;
	}

	public List<Double> getLinkTTs() {
		return linkTTs;
	}	
}

