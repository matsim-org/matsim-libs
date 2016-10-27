/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
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

package playground.agarwalamit.analysis.modalShare;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.PersonStuckEvent;
import org.matsim.api.core.v01.events.TransitDriverStartsEvent;
import org.matsim.api.core.v01.events.handler.ActivityStartEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.events.handler.PersonStuckEventHandler;
import org.matsim.api.core.v01.events.handler.TransitDriverStartsEventHandler;
import org.matsim.api.core.v01.population.Person;
import org.matsim.pt.PtConstants;

/**
 * If someone starts with transit_walk leg, and do not use pt before starting a regular act (home/work/leis/shop); it is walk
 * else it is pt mode.
 * @author amit
 */
public class ModalShareEventHandler implements PersonDepartureEventHandler, TransitDriverStartsEventHandler, ActivityStartEventHandler, PersonStuckEventHandler {

	private final SortedMap<String, Integer> mode2numberOflegs = new TreeMap<>();
	private final List<Id<Person>> transitDriverPersons = new ArrayList<>();
	// agents who first departs with transitWalk and their subsequent modes are stored here until it starts a regular act (home/work/leis/shop)
	private final Map<Id<Person>, List<String>> modesForTransitUsers = new HashMap<>(); 

	@Override
	public void reset(int iteration) {
		this.mode2numberOflegs.clear();
		this.transitDriverPersons.clear();
		this.modesForTransitUsers.clear();
	}

	@Override
	public void handleEvent(PersonDepartureEvent event) {
		String legMode = event.getLegMode();
		Id<Person> personId = event.getPersonId();

		if( transitDriverPersons.remove(personId) ) {
			// transit driver drives "car" which should not be counted in the modal share.
		} else {
			if(legMode.equals(TransportMode.transit_walk) || legMode.equals(TransportMode.pt) ) { 
				// transit_walk - transit_walk || transit_walk - pt
				if(modesForTransitUsers.containsKey(personId)) {
					List<String> modes = modesForTransitUsers.get(personId);
					modes.add(legMode);
				} else {
					List<String> modes = new ArrayList<>();
					modes.add(legMode);
					modesForTransitUsers.put(personId, modes);
				}
			} else {
				storeMode(legMode);
			}
		}
	}

	@Override
	public void handleEvent(TransitDriverStartsEvent event) {
		transitDriverPersons.add(event.getDriverId());
	}

	@Override
	public void handleEvent(ActivityStartEvent event) {
		if( modesForTransitUsers.containsKey(event.getPersonId()) ) {
			if(! event.getActType().equals(PtConstants.TRANSIT_ACTIVITY_TYPE) ) { 
				List<String> modes = modesForTransitUsers.remove(event.getPersonId());
				String legMode = modes.contains(TransportMode.pt) ? TransportMode.pt : TransportMode.walk;
				storeMode(legMode);
			} else { 
				// else continue
			}
		} else {
			// nothing to do
		}
	}

	@Override
	public void handleEvent(PersonStuckEvent event) {
		if( modesForTransitUsers.containsKey( event.getPersonId()) ) { 
			// since mode for transit users is determined at activity start, so storing mode for stuck agents so that, these can be handeled later.
			List<String> modes = modesForTransitUsers.get(event.getPersonId());
			modes.add(event.getLegMode());
		}
	}

	private void storeMode(String legMode) {
		if(mode2numberOflegs.containsKey(legMode)){
			mode2numberOflegs.put(legMode, mode2numberOflegs.get(legMode) + 1 );
		} else {
			mode2numberOflegs.put(legMode, 1);
		}
	}

	public void handleRemainingTransitUsers(){
		if(!modesForTransitUsers.isEmpty()) {
			Logger.getLogger(ModalShareFromEvents.class).warn("A few transit users are not handle due to stuckAndAbort. Handling them now.");
			for(Id<Person> pId : modesForTransitUsers.keySet()){
				List<String> modes = modesForTransitUsers.get(pId);
				String legMode = modes.contains(TransportMode.pt) ? TransportMode.pt : TransportMode.walk;
				storeMode(legMode);
			}
			modesForTransitUsers.clear();
		}
	}

	public SortedMap<String, Integer> getMode2numberOflegs() {
		return mode2numberOflegs;
	}
}