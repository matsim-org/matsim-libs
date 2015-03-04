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
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.PersonStuckEvent;
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
 * 
 * IMPORTANT: This is an adapted and extended version of staheale's class 'TripHandler'.
 * 
 */
public class TripHandler implements PersonDepartureEventHandler, PersonArrivalEventHandler,
		ActivityStartEventHandler, PersonStuckEventHandler, LinkLeaveEventHandler {
	
	// TODO-boescpa create "path" for pt and transit_walk too! {Possibly will have to adapt TestScenarioAnalyzer and TestTopdadTripProcessor if this is done...}
	
	BoxedHashMap<Id,Id> startLink;
	BoxedHashMap<Id,Double> startTime;
	BoxedHashMap<Id,String> mode;
	BoxedHashMap<Id,String> purpose;
	BoxedHashMap<Id,LinkedList<Id>> path;
	BoxedHashMap<Id,Id> endLink;
	BoxedHashMap<Id,Double> endTime;

	HashSet<Id> ptTripList;
	HashMap<Id,Id> ptStageEndLinkId;
	HashMap<Id,Double> ptStageEndTime;
	HashSet<Id> transitWalkList;
	HashSet<Id> endInitialisedList;

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
		ptStageEndLinkId = new HashMap<>();
		ptStageEndTime = new HashMap<>();
		ptTripList = new HashSet<>();
		transitWalkList = new HashSet<>();
		endInitialisedList = new HashSet<>();
	}

	@Override
	public void handleEvent(PersonArrivalEvent event) {
		//store endLink and endTime if it's a pt stage
		if (event.getLegMode().equals("pt") || event.getLegMode().equals("transit_walk")) {
			ptStageEndLinkId.put(event.getPersonId(), event.getLinkId());
			ptStageEndTime.put(event.getPersonId(), event.getTime());
		}
		//add endLink and endTime if it's not pt stage
		else {
			endLink.put(event.getPersonId(), event.getLinkId());
			endTime.put(event.getPersonId(), event.getTime());
		}
	}

	@Override
	public void handleEvent(PersonDepartureEvent event) {
		// handle pt trips
		switch (event.getLegMode()) {
			case "pt":
				// do not add a pt trip if it's only a stage
				if (!ptTripList.contains(event.getPersonId())) {
					// add person to pt trip list
					ptTripList.add(event.getPersonId());
					if (transitWalkList.contains(event.getPersonId())) {
						transitWalkList.remove(event.getPersonId());
					}
					// initialize pt trip
					startLink.put(event.getPersonId(), event.getLinkId());
					startTime.put(event.getPersonId(), event.getTime());
					mode.put(event.getPersonId(), event.getLegMode());
					path.put(event.getPersonId(), new LinkedList<Id>());
					// set endLink and endTime to null (in case an agent enters a pt vehicle and the pt vehicle is stuck in the end)
					if (!endInitialisedList.contains(event.getPersonId())) {
						endLink.put(event.getPersonId(), null);
						endTime.put(event.getPersonId(), null);
						purpose.put(event.getPersonId(), null);
						endInitialisedList.add(event.getPersonId());
					}
				} else {
					//replace transit walk mode with pt
					ArrayList<String> personModes = mode.getValues(event.getPersonId());
					if (!personModes.get(personModes.size() - 1).equals("pt")) {
						personModes.set((personModes.size() - 1), "pt");
					}
				}
				break;
			// handle transit walk trips
			case "transit_walk":
				if (!transitWalkList.contains(event.getPersonId())) {
					transitWalkList.add(event.getPersonId());
				}
				// do not add a transit walk trip if it's only a stage
				if (!ptTripList.contains(event.getPersonId())) {
					startLink.put(event.getPersonId(), event.getLinkId());
					startTime.put(event.getPersonId(), event.getTime());
					mode.put(event.getPersonId(), event.getLegMode());
					path.put(event.getPersonId(), new LinkedList<Id>());
					// set endLink and endTime to null (in case an agent enters a pt vehicle and the pt vehicle is stuck in the end)
					if (!endInitialisedList.contains(event.getPersonId())) {
						endLink.put(event.getPersonId(), null);
						endTime.put(event.getPersonId(), null);
						purpose.put(event.getPersonId(), null);
						endInitialisedList.add(event.getPersonId());
					}
				}
				break;
			// handle other trips
			default:
				startLink.put(event.getPersonId(), event.getLinkId());
				startTime.put(event.getPersonId(), event.getTime());
				mode.put(event.getPersonId(), event.getLegMode());
				path.put(event.getPersonId(), new LinkedList<Id>());
				break;
		}
	}

	@Override
	public void handleEvent(ActivityStartEvent event) {
		if (event.getActType().equals("pt interaction")) {
			// add person to pt trip list
			if (!ptTripList.contains(event.getPersonId())) {
				ptTripList.add(event.getPersonId());
				if (transitWalkList.contains(event.getPersonId())) {
					transitWalkList.remove(event.getPersonId());
				}
			}
		}
		else {
			if (ptTripList.contains(event.getPersonId()) || transitWalkList.contains(event.getPersonId())) {
				if (endInitialisedList.contains(event.getPersonId())) {
					endLink.removeLast(event.getPersonId());
					endTime.removeLast(event.getPersonId());
					purpose.removeLast(event.getPersonId());
				}
				endLink.put(event.getPersonId(), ptStageEndLinkId.get(event.getPersonId()));
				endTime.put(event.getPersonId(), ptStageEndTime.get(event.getPersonId()));
				ptTripList.remove(event.getPersonId());
				transitWalkList.remove(event.getPersonId());
			}
			if (endInitialisedList.contains(event.getPersonId())) {
				endInitialisedList.remove(event.getPersonId());
			}
			purpose.put(event.getPersonId(), event.getActType());
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
		ArrayList<LinkedList<Id>> al = path.getValues(event.getPersonId());
		LinkedList<Id> currentPath = al.get(al.size()-1);
		currentPath.add(event.getLinkId());
	}
}
