/* *********************************************************************** *
 * project: org.matsim.*
 * ActTimesCollector.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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

package playground.christoph.analysis.wardrop;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.api.experimental.events.ActivityEndEvent;
import org.matsim.core.api.experimental.events.ActivityStartEvent;
import org.matsim.core.api.experimental.events.handler.ActivityEndEventHandler;
import org.matsim.core.api.experimental.events.handler.ActivityStartEventHandler;

public class ActTimesCollector implements ActivityStartEventHandler, ActivityEndEventHandler {

//	private static final Logger log = Logger.getLogger(ActTimesCollector.class);
	
	// <Person's Id, Person's EventData>
	protected TreeMap<Id, EventData> data = new TreeMap<Id, EventData>();
	protected Network network;
	protected Population population;
	
	protected double startTime = 0.0;
	protected double endTime = Double.MAX_VALUE;
	private final Map<Id, Integer> agentActIndex = new HashMap<Id, Integer>();
	
	public void reset(int iteration) {
		data.clear();
		network = null;
		population = null;
	}
	
	@Override
	public void handleEvent(ActivityStartEvent event) {	
		Id personId = event.getPersonId();
		Activity eventAct = getCurrentActivity(event.getPersonId());
		
		if (!data.containsKey(personId)) data.put(personId, new EventData());
		
		EventData eventData = data.get(personId);
				
		eventData.addStartActivityEvent(event.getTime(), eventAct.getCoord());
	}

	@Override
	public void handleEvent(ActivityEndEvent event) {
		Id personId = event.getPersonId();
		Activity eventAct = getCurrentActivity(event.getPersonId());
	
		if (!data.containsKey(personId)) data.put(personId, new EventData());
		
		EventData eventData = data.get(personId);
	}

	public List<Double> getStartTimes(Id id) {
		EventData eventData = data.get(id);
		
		if (eventData != null) {
			return eventData.getStartTimes();
		}
		else {
			return new ArrayList<Double>();
		}
	}

	public List<Double> getEndTimes(Id id) {		
		EventData eventData = data.get(id);
	
		if (eventData != null) {
			return eventData.getEndTimes();
		}
		else {
			return new ArrayList<Double>();
		}
	}

	public List<Coord> getStartCoords(Id id) {
		EventData eventData = data.get(id);
		
		if (eventData != null) {
			return eventData.getStartCoords();
		}
		else {
			return new ArrayList<Coord>();
		}
	}

	public List<Coord> getEndCoords(Id id) {
		EventData eventData = data.get(id);
		
		if (eventData != null) {
			return eventData.getEndCoords();
		}
		else {
			return new ArrayList<Coord>();
		}
	}

	public List<Id> getStartId(Id id) {
		EventData eventData = data.get(id);
		
		if (eventData != null) {
			return eventData.getStartId();
		}
		else {
			return new ArrayList<Id>();
		}
	}

	public List<Id> getEndId(Id id) {
		EventData eventData = data.get(id);
		
		if (eventData != null) {
			return eventData.getEndId();
		}
		else {
			return new ArrayList<Id>();
		}
	}
	
	public void setNetwork(Network nw) {
		network = nw;
	}
	
	public Network getNetwork() {
		return network;
	}
	
	public void setPopulation(Population pop) {
		population = pop;
	}
	
	public Population getPopulation() {
		return population;
	}
	
	class EventData {
		List<Double> startTimes;
		List<Double> endTimes;
		List<Coord> startCoords;
		List<Coord> endCoords;
		List<Id> startId;
		List<Id> endId;
				
		public EventData() {
			startTimes = new ArrayList<Double>();
			endTimes = new ArrayList<Double>();
			startCoords = new ArrayList<Coord>();
			endCoords = new ArrayList<Coord>();
			startId = new ArrayList<Id>();
			endId = new ArrayList<Id>();
		}
		
		public void addStartActivityEvent(double time, Coord coord) {
			startTimes.add(time);
			startCoords.add(coord);
			trimmArrayLists();
		}
		
		public void addStartActivityEvent(double time, Id linkId) {
			startTimes.add(time);
			startId.add(linkId);
			trimmArrayLists();
		}
		
		public void addEndActivityEvent(double time, Coord coord) {
			endTimes.add(time);
			endCoords.add(coord);
			trimmArrayLists();
		}
		
		public void addEndActivityEvent(double time, Id linkId) {
			endTimes.add(time);
			endId.add(linkId);
			trimmArrayLists();
		}

		// should free some memory
		public void trimmArrayLists() {
			((ArrayList<Double>)startTimes).trimToSize();
			((ArrayList<Double>)endTimes).trimToSize();
			((ArrayList<Coord>)startCoords).trimToSize();
			((ArrayList<Coord>)endCoords).trimToSize();
			((ArrayList<Id>)startId).trimToSize();
			((ArrayList<Id>)endId).trimToSize();		
		}
		
		public List<Double> getStartTimes() {
			return startTimes;
		}

		public List<Double> getEndTimes() {
			return endTimes;
		}

		public List<Coord> getStartCoords() {
			return startCoords;
		}

		public List<Coord> getEndCoords() {
			return endCoords;
		}

		public List<Id> getStartId() {
			return startId;
		}

		public List<Id> getEndId() {
			return endId;
		}
		
	}	// class EventData

	public double getStartTime() {
		return this.startTime;
	}

	public void setStartTime(double startTime) {
		this.startTime = startTime;
	}

	public double getEndTime() {
		return this.endTime;
	}

	public void setEndTime(double endTime) {
		this.endTime = endTime;
	}
	
	private Activity getNextActivity(final Id agentId) {
		Integer idx = this.agentActIndex.get(agentId);
		if (idx == null) {
			idx = Integer.valueOf(1);
		} else {
			idx = Integer.valueOf(idx.intValue() + 2);
		}
		this.agentActIndex.put(agentId, idx);
		return (Activity) this.population.getPersons().get(agentId).getSelectedPlan().getPlanElements().get(idx.intValue());
	}
	
	private Activity getCurrentActivity(final Id agentId) {
		Integer idx = this.agentActIndex.get(agentId);
		if (idx == null) {
			return null;
		}
		return (Activity) this.population.getPersons().get(agentId).getSelectedPlan().getPlanElements().get(idx.intValue());
	}
	
}