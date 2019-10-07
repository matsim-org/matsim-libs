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

package playground.vsp.analysis.modules.modalAnalyses.modalShare;

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
import org.matsim.core.router.StageActivityTypes;

/**
 * If someone starts with transit_walk leg, and do not use pt before starting a regular act (home/work/leis/shop); it is walk
 * else it is pt mode.
 * @author amit
 */
public class ModalShareEventHandler implements PersonDepartureEventHandler, TransitDriverStartsEventHandler, ActivityStartEventHandler, PersonStuckEventHandler, StageActivityTypes {

	private final SortedMap<String, Integer> mode2numberOflegs = new TreeMap<>();
	private final List<Id<Person>> transitDriverPersons = new ArrayList<>();
	// agents who first departs with transitWalk and their subsequent modes are stored here until it starts a regular act (home/work/leis/shop)
	private final Map<Id<Person>, List<String>> person2Modes = new HashMap<>();

	@Override
	public void reset(int iteration) {
		this.mode2numberOflegs.clear();
		this.transitDriverPersons.clear();
		this.person2Modes.clear();
	}

	@Override
	public void handleEvent(PersonDepartureEvent event) {
		String legMode = event.getLegMode();
		Id<Person> personId = event.getPersonId();

		if( transitDriverPersons.remove(personId) ) {
			// transit driver drives "car" which should not be counted in the modal share.
			return;
		}

		//at this point, it could be main leg (e.g. car/bike) or start of a stage activity (e.g. car/pt interaction)
		List<String> usedModes = person2Modes.getOrDefault(event.getPersonId(), new ArrayList<>());
		usedModes.add(event.getLegMode());
		person2Modes.put(event.getPersonId(), usedModes);
	}

	@Override
	public void handleEvent(TransitDriverStartsEvent event) {
		transitDriverPersons.add(event.getDriverId());
	}

	@Override
	public void handleEvent(ActivityStartEvent event) {
		if( person2Modes.containsKey(event.getPersonId()) ) {
			if( ! isStageActivity(event.getActType()) ) {
				String legMode = getMainMode(person2Modes.remove(event.getPersonId()));
				storeMode(legMode);
			} else { 
				// else continue storing leg modes
			}
		} else {
			throw new RuntimeException("Person "+event.getPersonId()+" is not registered.");
		}
	}

	@Override
	public void handleEvent(PersonStuckEvent event) {
		if( person2Modes.containsKey( event.getPersonId()) ) {
			// since mode for transit users is determined at activity start, so storing mode for stuck agents so that, these can be handeled later.
			List<String> modes = person2Modes.get(event.getPersonId());
			modes.add(event.getLegMode());
		}
	}

	private void storeMode(String legMode) {
		mode2numberOflegs.merge(legMode, 1, (a, b) -> a + b);
	}

	private void handleRemainingTransitUsers(){
		Logger.getLogger(ModalShareEventHandler.class).warn("A few transit users are not handle due to stuckAndAbort. Handling them now.");
		for(Id<Person> pId : person2Modes.keySet()){
			String legMode = getMainMode(person2Modes.get(pId));
			storeMode(legMode);
		}
		person2Modes.clear();
	}

	public SortedMap<String, Integer> getMode2numberOflegs() {
		if (!person2Modes.isEmpty()) {
			handleRemainingTransitUsers();
		}
		return mode2numberOflegs;
	}

	@Override
	public boolean isStageActivity(String actType){
		return actType.endsWith("interaction");
	}

	private String getMainMode(List<String> modes){
		if (modes.size()==1) return modes.get(0).equals(TransportMode.transit_walk) ? TransportMode.walk: modes.get(0);
		
		if (modes.contains(TransportMode.pt)) return TransportMode.pt;
		if (modes.contains(TransportMode.car)) return TransportMode.car;
		if (modes.contains(TransportMode.bike)) return TransportMode.bike;
		if (modes.contains(TransportMode.walk)) return TransportMode.walk;
		if (modes.contains(TransportMode.ride)) return TransportMode.ride;
		
		if (modes.contains(TransportMode.transit_walk) || modes.contains(TransportMode.access_walk) || modes.contains(TransportMode.egress_walk)) {
			return TransportMode.walk;
		} 
		
		throw new RuntimeException("Unknown mode(s) "+ modes.toString());
	}
}