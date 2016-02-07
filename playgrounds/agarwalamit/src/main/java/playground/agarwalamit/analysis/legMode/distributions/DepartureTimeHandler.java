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

package playground.agarwalamit.analysis.legMode.distributions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.TransitDriverStartsEvent;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.events.handler.TransitDriverStartsEventHandler;
import org.matsim.api.core.v01.population.Person;

/**
 * This excludes the departure of transit drivers.
 * @author amit
 */

public class DepartureTimeHandler implements PersonDepartureEventHandler, TransitDriverStartsEventHandler {

	private final double timeBinSize;
	private final Map<String, SortedMap<Double, Integer> > mode2TimeBin2Count = new HashMap<>();
	private final List<Id<Person>> transitDriverPersons = new ArrayList<>();

	public DepartureTimeHandler(final double timebinsize) {
		this.timeBinSize = timebinsize;
	}

	public DepartureTimeHandler(final double simulationEndTime, final int noOfTimeBins) {
		this(  simulationEndTime/noOfTimeBins );
	}

	@Override
	public void reset(int iteration) {
		this.mode2TimeBin2Count.clear();
	}

	@Override
	public void handleEvent(PersonDepartureEvent event) {
		if( transitDriverPersons.remove(event.getPersonId()) ) {
			// transit driver drives "car" which should not be counted in the modal share.
		} else {
			double time = Math.max(1, Math.ceil( event.getTime()/this.timeBinSize) ) * this.timeBinSize;
			String legMode = event.getLegMode();

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
	}

	@Override
	public void handleEvent(TransitDriverStartsEvent event) {
		transitDriverPersons.add(event.getDriverId());
	}

	public Map<String, SortedMap<Double, Integer>> getMode2TimeBin2Count() {
		return mode2TimeBin2Count;
	}
}