/* *********************************************************************** *
 * project: org.matsim.*
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

package playground.boescpa.lib.tools.tripCreation;

import java.util.*;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.*;
import org.matsim.api.core.v01.events.handler.ActivityStartEventHandler;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.events.handler.PersonStuckEventHandler;
import playground.boescpa.lib.obj.BoxedHashMap;

/**
 * Handles events to create "trips". 
 * 
 * @author boescpa
 */
public class TripHandler implements PersonDepartureEventHandler, PersonArrivalEventHandler,
		ActivityStartEventHandler, PersonStuckEventHandler, LinkLeaveEventHandler {
	
	// TODO-boescpa create "path" for pt and transit_walk too! {Possibly will have to adapt TestScenarioAnalyzer and TestTopdadTripProcessor if this is done...}

	private List<String> modePriorities;

	BoxedHashMap<Id,Id> startLink;
	BoxedHashMap<Id,Double> startTime;
	BoxedHashMap<Id,String> mode;
	BoxedHashMap<Id,String> purpose;
	BoxedHashMap<Id,LinkedList<Id>> path;
	BoxedHashMap<Id,Id> endLink;
	BoxedHashMap<Id,Double> endTime;

	HashSet<Id> currentTripList;
	HashMap<Id,Id> stageEndLinkId;
	HashMap<Id,Double> stageEndTime;

	public BoxedHashMap<Id, Id> getStartLink() {
		return startLink;
	}
	
	public BoxedHashMap<Id, Double> getStartTime() {
		return startTime;
	}
	
	public BoxedHashMap<Id, String> getMode() {
		return mode;
	}
	
	public BoxedHashMap<Id, String> getPurpose() {
		return purpose;
	}
	
	public BoxedHashMap<Id, LinkedList<Id>> getPath() {
		return path;
	}
	
	public BoxedHashMap<Id, Id> getEndLink() {
		return endLink;
	}

	public BoxedHashMap<Id, Double> getEndTime() {
		return endTime;
	}

	@Override
	public void reset(int iteration) {
		startLink = new BoxedHashMap<>();
		startTime = new BoxedHashMap<>();
		mode = new BoxedHashMap<>();
		purpose = new BoxedHashMap<>();
		path = new BoxedHashMap<>();
		endLink = new BoxedHashMap<>();
		endTime = new BoxedHashMap<>();
		stageEndLinkId = new HashMap<>();
		stageEndTime = new HashMap<>();
		currentTripList = new HashSet<>();

		// the sequence of the modes defines their priority from high to low when determining the main mode of a multi-stage trip:
		this.modePriorities = new ArrayList<>();
		this.modePriorities.add(TransportMode.car);
		this.modePriorities.add(TransportMode.ride);
		this.modePriorities.add(TransportMode.pt);
		this.modePriorities.add(TransportMode.bike);
		this.modePriorities.add(TransportMode.walk);
		this.modePriorities.add(TransportMode.transit_walk);
		this.modePriorities.add(TransportMode.other);
	}

	@Override
	public void handleEvent(PersonArrivalEvent event) {
		//store endLink and endTime for the current stage
		stageEndLinkId.put(event.getPersonId(), event.getLinkId());
		stageEndTime.put(event.getPersonId(), event.getTime());
	}

	@Override
	public void handleEvent(PersonDepartureEvent event) {
		// do not add departure if it's only a stage
		if (!currentTripList.contains(event.getPersonId())) {
			currentTripList.add(event.getPersonId());
			startLink.put(event.getPersonId(), event.getLinkId());
			startTime.put(event.getPersonId(), event.getTime());
			mode.put(event.getPersonId(), event.getLegMode());
			path.put(event.getPersonId(), new LinkedList<Id>());
			// set endLink and endTime to null (in case an agent is stuck in the end)
			endLink.put(event.getPersonId(), null);
			endTime.put(event.getPersonId(), null);
			purpose.put(event.getPersonId(), null);
		}
		// if pt but not noted as such yet, set it as a pt-trip now
		replaceModeIfEventModeDominant(event);
	}

	private void replaceModeIfEventModeDominant(PersonDepartureEvent event) {
		ArrayList<String> personModes = mode.getValues(event.getPersonId());
		int priorityEventMode = modePriorities.indexOf(event.getLegMode());
		int priorityCurrentMode = modePriorities.indexOf(personModes.get(personModes.size() - 1));
		if (priorityEventMode < priorityCurrentMode) {
			personModes.set((personModes.size() - 1), event.getLegMode());
		}
	}

	@Override
	public void handleEvent(ActivityStartEvent event) {
		// do not add activity if it's a pt interaction
		if (!event.getActType().equals("pt interaction")) {
			// remove the nulls set for the situation when a person is stuck
			endLink.removeLast(event.getPersonId());
			endTime.removeLast(event.getPersonId());
			purpose.removeLast(event.getPersonId());
			// add the true values:
			endLink.put(event.getPersonId(), stageEndLinkId.get(event.getPersonId()));
			endTime.put(event.getPersonId(), stageEndTime.get(event.getPersonId()));
			purpose.put(event.getPersonId(), event.getActType());
			// clean up:
			currentTripList.remove(event.getPersonId());
		}
	}

	@Override
	public void handleEvent(PersonStuckEvent event) {
		endLink.put(event.getPersonId(), event.getLinkId());
		endTime.put(event.getPersonId(), event.getTime());
		purpose.put(event.getPersonId(), "stuck");
	}

	@Override
	public void handleEvent(LinkLeaveEvent event) {
		ArrayList<LinkedList<Id>> al = path.getValues(event.getDriverId());
		LinkedList<Id> currentPath = al.get(al.size()-1);
		currentPath.add(event.getLinkId());
	}
}
