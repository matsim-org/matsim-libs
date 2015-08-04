/* *********************************************************************** *
 * project: org.matsim.*
 * DgAverageTravelTimeSpeed
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
package playground.dgrether.koehlerstrehlersignal.analysis;

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.PersonStuckEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.events.handler.PersonStuckEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;


/**
 * @author dgrether
 * @author tthunig
 * test
 *
 */
public class TtTotalDelay implements LinkEnterEventHandler, LinkLeaveEventHandler, PersonDepartureEventHandler, PersonArrivalEventHandler, PersonStuckEventHandler{

	private static final Logger log = Logger
			.getLogger(TtTotalDelay.class);
	
	private Network network;
	private Map<Id<Person>, Double> earliestLinkExitTimeByPerson;
	private double totalDelay;

	public TtTotalDelay(Network network) {
		this.network = network;
		this.reset(0);
	}

	@Override
	public void reset(int iteration) {
		this.earliestLinkExitTimeByPerson = new HashMap<>();
		this.totalDelay = 0.0;
	}

	@Override
	public void handleEvent(LinkLeaveEvent event) {
		if (this.network.getLinks().containsKey(event.getLinkId())) {
			Double earliestLinkExitTime = this.earliestLinkExitTimeByPerson.remove(event.getPersonId());
			if (earliestLinkExitTime != null) {
				// add the number of seconds the agent is later as the earliest link exit time as delay
				this.totalDelay += event.getTime() - earliestLinkExitTime;
			}
		}
	}

	@Override
	public void handleEvent(LinkEnterEvent event) {
		if (this.network.getLinks().containsKey(event.getLinkId())) {
			Link link = this.network.getLinks().get(event.getLinkId());
			double freespeedTT = link.getLength()/link.getFreespeed();
			// this is the earliest time where matsim sets the agent to the next link
			double matsimFreespeedTT = Math.floor(freespeedTT + 1);
			this.earliestLinkExitTimeByPerson.put(event.getPersonId(), event.getTime() + matsimFreespeedTT);
		}
	}

	@Override
	public void handleEvent(PersonStuckEvent event) {
		this.earliestLinkExitTimeByPerson.remove(event.getPersonId());
		log.warn("Agent " + event.getPersonId() + " got stucked at link "
				+ event.getLinkId() + ". His delay at this link is not considered in the total delay.");
	}

	@Override
	public void handleEvent(PersonArrivalEvent event) {
		this.earliestLinkExitTimeByPerson.remove(event.getPersonId());		
	}

	
	public double getTotalDelay() {
		return totalDelay;
	}

	@Override
	public void handleEvent(PersonDepartureEvent event) {
		if (this.network.getLinks().containsKey(event.getLinkId())){
			// for the first link every agent needs one second
			this.earliestLinkExitTimeByPerson.put(event.getPersonId(), event.getTime() + 1);
		}
	}

}
