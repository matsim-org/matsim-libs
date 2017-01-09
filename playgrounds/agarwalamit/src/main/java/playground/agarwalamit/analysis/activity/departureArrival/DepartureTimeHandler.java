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

package playground.agarwalamit.analysis.activity.departureArrival;

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
 * (1) This excludes the departure of transit drivers.
 * (2) See followings
 * transit_walk - transit_walk --> walk &
 * transit_walk - pt - transit_walk --> pt &
 * transit_walk - pt - transit_walk - pt - transit_walk --> pt 
 * @author amit
 */

public class DepartureTimeHandler implements PersonDepartureEventHandler, TransitDriverStartsEventHandler, ActivityStartEventHandler, PersonStuckEventHandler {

	private final double timeBinSize;
	private final Map<String, SortedMap<Double, Integer> > mode2TimeBin2Count = new HashMap<>();
	private final List<Id<Person>> transitDriverPersons = new ArrayList<>();
	// agents who first departs with transitWalk and their subsequent modes are stored here until it starts a regular act (home/work/leis/shop)
	private final Map<Id<Person>, List<String>> modesForTransitUsers = new HashMap<>();
	private final Map<Id<Person>, Double> transitUserDepartureTime = new HashMap<>(); 

	public DepartureTimeHandler(final double timebinsize) {
		this.timeBinSize = timebinsize;
	}

	public DepartureTimeHandler(final double simulationEndTime, final int noOfTimeBins) {
		this(  simulationEndTime/noOfTimeBins );
	}

	@Override
	public void reset(int iteration) {
		this.mode2TimeBin2Count.clear();
		this.modesForTransitUsers.clear();
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
					transitUserDepartureTime.put(personId, event.getTime()); // store the departure time, when agent is left after a regular activity
				}
			} else {
				storeMode(event.getTime(), legMode);
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
				double eventTime = transitUserDepartureTime.remove(event.getPersonId());
				storeMode(eventTime, legMode);
			} else { 
				// else continue
			}
		} else {
			// nothing to do
		}
	}
	
	@Override
	public void handleEvent(PersonStuckEvent event) {
		List<String> modes = modesForTransitUsers.get(event.getPersonId());
		modes.add(event.getLegMode());
	}
	
	private void storeMode(final double eventTime, final String legMode) {
		double time = Math.max(1, Math.ceil( eventTime/this.timeBinSize) ) * this.timeBinSize;

		if(this.mode2TimeBin2Count.containsKey(legMode)){
			SortedMap<Double, Integer> timebin2count = mode2TimeBin2Count.get(legMode);
			if(timebin2count.containsKey(time)) {
				timebin2count.put(time, timebin2count.get(time) + 1 );
			} else {
				timebin2count.put(time,   1 );	
			}

		} else {
			SortedMap<Double, Integer> timebin2count = new TreeMap<>();
			timebin2count.put(time, 1);
			mode2TimeBin2Count.put(legMode, timebin2count);
		}
	}

	public void handleRemainingTransitUsers(){
		if(!modesForTransitUsers.isEmpty()) {
			Logger.getLogger(DepartureTimeHandler.class).warn("A few transit users are not handle due to stuckAndAbort. Handling them now.");
			for(Id<Person> pId : modesForTransitUsers.keySet()){
				List<String> modes = modesForTransitUsers.get(pId);
				String legMode = modes.contains(TransportMode.pt) ? TransportMode.pt : TransportMode.walk;
				double eventTime = transitUserDepartureTime.remove(pId);
				storeMode(eventTime, legMode);
			}
			modesForTransitUsers.clear();
		}
	}
	
	public Map<String, SortedMap<Double, Integer>> getMode2TimeBin2Count() {
		return mode2TimeBin2Count;
	}
}