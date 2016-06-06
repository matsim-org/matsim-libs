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

package playground.boescpa.analysis.trips;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.*;
import org.matsim.api.core.v01.events.handler.*;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import playground.boescpa.lib.obj.BoxedHashMap;

import java.util.*;

/**
 * Handles events to create "trips". 
 * 
 * @author boescpa
 */
public class TripEventHandler implements PersonDepartureEventHandler, PersonArrivalEventHandler,
		ActivityStartEventHandler, PersonStuckEventHandler, LinkLeaveEventHandler {
	
	// TODO-boescpa create "path" for pt and transit_walk too!

    private static boolean anonymizeTrips = false;
    public static void setAnonymizeTrips(boolean anonymizeTrips) {
        TripEventHandler.anonymizeTrips = anonymizeTrips;
    }

    private boolean tripsChanged;
    private List<Trip> trips;
    private final Network network;

	private List<String> modePriorities;

    private Set<Id<Person>> agents;
	private BoxedHashMap<Id<Person>,Id<Link>> startLink;
	private BoxedHashMap<Id<Person>,Double> startTime;
	private BoxedHashMap<Id<Person>,String> mode;
	private BoxedHashMap<Id<Person>,String> purpose;
	private BoxedHashMap<String,List<Id<Link>>> path;
	private BoxedHashMap<Id<Person>,Id<Link>> endLink;
	private BoxedHashMap<Id<Person>,Double> endTime;

	private Set<Id<Person>> currentTripList;
	private Map<Id<Person>,Id<Link>> stageEndLinkId;
	private Map<Id<Person>,Double> stageEndTime;

    public TripEventHandler(Network network) {
        this.reset(0);
        this.network = network;
    }

	@Override
	public void reset(int iteration) {
        tripsChanged = true;
        agents = new HashSet<>();
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
	final public void handleEvent(PersonArrivalEvent event) {
		//store endLink and endTime for the current stage
		stageEndLinkId.put(event.getPersonId(), event.getLinkId());
		stageEndTime.put(event.getPersonId(), event.getTime());
	}

	@Override
	final public void handleEvent(PersonDepartureEvent event) {
        Id<Person> personId = event.getPersonId();
		// do not add departure if it's only a stage
		if (!currentTripList.contains(personId)) {
			currentTripList.add(personId);
            // register person
            agents.add(personId);
			// initialize new trip
            tripsChanged = true;
            startLink.put(personId, event.getLinkId());
			startTime.put(personId, event.getTime());
			mode.put(personId, event.getLegMode());
			path.put(personId.toString(), new LinkedList<Id<Link>>());
			// set endLink and endTime to null (in case an agent is stuck in the end)
			endLink.put(personId, null);
			endTime.put(personId, null);
			purpose.put(personId, null);
		}
		// if new mode more dominant then stored mode then change mode
        replaceModeIfEventModeDominant(personId, event.getLegMode());
	}

	private void replaceModeIfEventModeDominant(Id<Person> personId, String newMode) {
		List<String> personModes = mode.getValues(personId);
		int priorityEventMode = modePriorities.indexOf(newMode);
		int priorityCurrentMode = modePriorities.indexOf(personModes.get(personModes.size() - 1));
		if (priorityEventMode < priorityCurrentMode) {
			personModes.set((personModes.size() - 1), newMode);
		}
	}

	@Override
	final public void handleEvent(ActivityStartEvent event) {
		// do not add activity if it's a pt interaction
		if (!event.getActType().equals("pt interaction")) {
            Id<Person> personId = event.getPersonId();
            handleTripEnd(personId, stageEndLinkId.get(personId), stageEndTime.get(personId), event.getActType());
		}
	}

	@Override
	final public void handleEvent(PersonStuckEvent event) {
        handleTripEnd(event.getPersonId(), event.getLinkId(), event.getTime(), "stuck");
	}

	@Override
	final public void handleEvent(LinkLeaveEvent event) {
		List<List<Id<Link>>> al = path.getValues(event.getVehicleId().toString());
		if (al != null) {
			List<Id<Link>> currentPath = al.get(al.size() - 1);
			currentPath.add(event.getLinkId());
		}
	}

    private void handleTripEnd(Id<Person> personId, Id<Link> linkId, double time, String purpose) {
        // remove the nulls set for the situation when a person is stuck
        this.endLink.removeLast(personId);
        this.endTime.removeLast(personId);
        this.purpose.removeLast(personId);
        // add the true values:
        this.endLink.put(personId, linkId);
        this.endTime.put(personId, time);
        this.purpose.put(personId, purpose);
        // clean up:
        this.currentTripList.remove(personId);
    }

    final public List<Trip> getTrips() {
        if (tripsChanged) {
            this.createTrips();
            tripsChanged = false;
        }
        return Collections.unmodifiableList(this.trips);
    }

    private void createTrips() {
        this.trips = new LinkedList<>();
        int incognitoPersonId = 0;
        Trip tempTrip;
        for (Id<Person> personId : agents) {
            if (agentIsToConsider(personId)) {
                // get the agent's trips
                List<Id<Link>> startLinks = this.startLink.getValues(personId);
                List<String> modes = this.mode.getValues(personId);
                List<String> purposes = this.purpose.getValues(personId);
                List<Double> startTimes = this.startTime.getValues(personId);
                List<Id<Link>> endLinks = this.endLink.getValues(personId);
                List<Double> endTimes = this.endTime.getValues(personId);
                List<List<Id<Link>>> pathList = this.path.getValues(personId.toString());
                Id<Person> tripPerson = anonymizeTrips ? Id.createPersonId(incognitoPersonId++) : personId;

                for (int i = 0; i < startLinks.size(); i++) {
                    if (endLinks.get(i) != null) {
                        tempTrip = new Trip(
                                tripPerson,
                                startTimes.get(i),
                                startLinks.get(i),
                                network.getLinks().get(startLinks.get(i)).getCoord().getX(),
                                network.getLinks().get(startLinks.get(i)).getCoord().getY(),
                                endTimes.get(i),
                                endLinks.get(i),
                                network.getLinks().get(endLinks.get(i)).getCoord().getX(),
                                network.getLinks().get(endLinks.get(i)).getCoord().getY(),
                                modes.get(i),
                                purposes.get(i),
                                TripUtils.calcTravelTime(startTimes.get(i), endTimes.get(i)),
                                TripUtils.calcTravelDistance(pathList.get(i), network, startLinks.get(i), endLinks.get(i))
                        );
                    } else {
                        tempTrip = new Trip(
                                tripPerson,
                                startTimes.get(i),
                                startLinks.get(i),
                                network.getLinks().get(startLinks.get(i)).getCoord().getX(),
                                network.getLinks().get(startLinks.get(i)).getCoord().getY(),
                                0,
                                null,
                                0,
                                0,
                                modes.get(i),
                                purposes.get(i),
                                0,
                                0
                        );
                    }
                    this.trips.add(tempTrip);
                }
            }
        }
    }

	protected boolean agentIsToConsider(Id<Person> personId) {
		return !personId.toString().contains("pt");
	}
}
