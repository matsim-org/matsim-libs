/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2017 by the members listed in the COPYING,        *
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

package vwExamples.utils.tripAnalyzer;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.*;
import org.matsim.api.core.v01.events.handler.*;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.drt.passenger.events.DrtRequestSubmittedEvent;
import org.matsim.contrib.drt.passenger.events.DrtRequestSubmittedEventHandler;
import org.matsim.core.api.experimental.events.AgentWaitingForPtEvent;
import org.matsim.core.api.experimental.events.TeleportationArrivalEvent;
import org.matsim.core.api.experimental.events.handler.AgentWaitingForPtEventHandler;
import org.matsim.core.api.experimental.events.handler.TeleportationArrivalEventHandler;
import org.matsim.pt.transitSchedule.api.*;
import org.matsim.vehicles.Vehicle;

import java.util.*;

// One trip is the sum of all legs and "pt interaction" activities between to real, non-"pt interaction", activities
// Coords unavailable in events -> no coords written

/**
 * Please note: Looks up the TransitRoute using the vehicleId. If the same vehicle services multiple TransitRoutes,
 * this program will always save the first TransitRoute found where this vehicle operates as the TransitRoute used
 * for the leg, although the agent used another TransitRoute where the same vehicle operates, too.
 * <p>
 * Drt legs can start when the drt request is submitted. This can happen before or after the PersonDepartureEvent,
 * that means a part of the wait time can take place before the agent has terminated its last activity (or
 * pt interaction) prior to departing for the drt leg. Therefore the wait time is split into gross wait time
 * (wait time between the drt request and the drt vehicle arrival) and (net) wait time (wait time between the
 * departure event and the drt vehicle arrival).
 *
 * @param network
 * @param monitoredModes            : All trips to be monitored have to consist only of legs of these modes
 * @param monitoredStartAndEndLinks : only trips which start or end on one these links will be monitored.
 *                                  Set to null if you want to have all trips from all origins and to all destinations.
 * @author gleich
 */
public class DrtPtTripEventHandler implements ActivityStartEventHandler, ActivityEndEventHandler,
        PersonDepartureEventHandler, PersonArrivalEventHandler, PersonEntersVehicleEventHandler,
        LinkEnterEventHandler, TeleportationArrivalEventHandler, AgentWaitingForPtEventHandler,
        DrtRequestSubmittedEventHandler {

    //	private Set<Id<Person>> agentsOnMonitoredTrip = new HashSet<>(); -> agent2CurrentTripStartLink.contains()
//	private Map<Id<Person>, Boolean> agentHasDrtLeg = new HashMap<>();
    private Network network;
    private TransitSchedule ptSchedule;

    private Map<Id<Person>, List<ExperiencedTrip>> person2ExperiencedTrips = new HashMap<>();

    //	private Map<Id<Person>, Coord> agent2CurrentTripStartCoord = new HashMap<>();
    private Map<Id<Person>, String> agent2CurrentTripActivityBefore = new HashMap<>();
    private Map<Id<Person>, Id<Link>> agent2CurrentTripStartLink = new HashMap<>();
    private Map<Id<Person>, Double> agent2CurrentTripStartTime = new HashMap<>();
    private Map<Id<Person>, List<ExperiencedLeg>> agent2CurrentTripExperiencedLegs = new HashMap<>();

    //	private Map<Id<Person>, Coord> agent2CurrentLegStartCoord = new HashMap<>();
    private Map<Id<Person>, String> agent2CurrentLegMode = new HashMap<>();
    private Map<Id<Person>, Id<Link>> agent2CurrentLegStartLink = new HashMap<>();
    private Map<Id<Person>, Double> agent2CurrentLegStartTime = new HashMap<>();
    private Map<Id<Person>, Double> agent2CurrentLegDrtRequestTime = new HashMap<>();
    private Map<Id<Person>, Id<TransitStopFacility>> agent2CurrentLegStartPtStop = new HashMap<>();
    private Map<Id<Person>, Id<TransitStopFacility>> agent2CurrentLegEndPtStop = new HashMap<>();
    private Map<Id<Person>, Double> agent2CurrentLegEnterVehicleTime = new HashMap<>();
    private Map<Id<Person>, Double> agent2CurrentLegDistanceOffsetAtEnteringVehicle = new HashMap<>();
    private Map<Id<Person>, Id<Vehicle>> agent2CurrentLegVehicle = new HashMap<>();
    private Map<Id<Person>, Double> agent2CurrentTeleportDistance = new HashMap<>();

    private Map<Id<Vehicle>, Double> monitoredVeh2toMonitoredDistance = new HashMap<>();
    private Map<Id<Vehicle>, Id<TransitRoute>> monitoredVeh2toTransitRoute = new HashMap<>();
    private Set<String> monitoredModes = new HashSet<>();
    private Set<Id<Link>> monitoredStartAndEndLinks; // set to null if all links are to be monitored

    /**
     * @param network
     * @param monitoredModes            : All trips to be monitored have to consist only of legs of these modes
     * @param monitoredStartAndEndLinks : only trips which start or end on one these links will be monitored.
     *                                  Set to null if you want to have all trips from all origins and to all destinations
     */
    public DrtPtTripEventHandler(Network network, TransitSchedule ptSchedule, Set<String> monitoredModes, Set<Id<Link>> monitoredStartAndEndLinks) {
        this.network = network;
        this.ptSchedule = ptSchedule;
        this.monitoredModes = monitoredModes; // pt, transit_walk, drt: walk eigentlich nicht, aber in FixedDistanceBased falsch als walk statt transit_walk gesetzt
        this.monitoredStartAndEndLinks = monitoredStartAndEndLinks;
    }

    @Override
    public void reset(int iteration) {
        // TODO Auto-generated method stub

    }

    // in-vehicle distances
    @Override
    public void handleEvent(LinkEnterEvent event) {
        if (monitoredVeh2toMonitoredDistance.containsKey(event.getVehicleId())) {
            monitoredVeh2toMonitoredDistance.put(event.getVehicleId(),
                    monitoredVeh2toMonitoredDistance.get(event.getVehicleId()) + network.getLinks().get(event.getLinkId()).getLength());
        }
    }

    /*
     * Save the activity type of the last activity before the trip. We cannot know yet if this is a trip to be monitored or not
     * (leg mode and arrival link are unknown), so save this for all agents.
     */
    @Override
    public void handleEvent(ActivityEndEvent event) {
        if (!(event.getActType().equals("pt interaction") || event.getActType().equals("drt interaction"))) {
            agent2CurrentTripActivityBefore.put(event.getPersonId(), event.getActType());
        }
    }

    // Detect start of wait time for drt (before a drt leg)
    @Override
    public void handleEvent(DrtRequestSubmittedEvent event) {
        agent2CurrentLegDrtRequestTime.put(event.getPersonId(), event.getTime());
    }

    // Detect start of a leg (and possibly the start of a trip)
    @Override
    public void handleEvent(PersonDepartureEvent event) {
        /*
         * if a trip includes a leg of a mode not contained in monitoredModes, this lead to NullPointerExceptions at
         * handleEvent(PersonArrivalEvent event). This can be avoided by removing the following check of the leg mode,
         * however in this case all legs of all modes will be saved and later while saving ExperiencedTrips in
         * handleEvent(ActivityStartEvent event) those trips not containing would have to be filtered out.
         */
        if (!monitoredModes.contains(event.getLegMode())) {
            return;
        } else {
            if (agent2CurrentLegStartLink.containsKey(event.getPersonId())) {
                throw new RuntimeException("agent " + event.getPersonId() + " has PersonDepartureEvent at time " +
                        event.getTime() + " although the previous leg is not finished yet.");
            } else {
                if (!agent2CurrentTripStartLink.containsKey(event.getPersonId())) {
                    agent2CurrentTripStartLink.put(event.getPersonId(), event.getLinkId());
                    agent2CurrentTripStartTime.put(event.getPersonId(), event.getTime());
                    agent2CurrentTripExperiencedLegs.put(event.getPersonId(), new ArrayList<>());
                }
                agent2CurrentLegStartLink.put(event.getPersonId(), event.getLinkId());
                agent2CurrentLegStartTime.put(event.getPersonId(), event.getTime());
                agent2CurrentLegMode.put(event.getPersonId(), event.getLegMode());
            }
        }
    }

    // Get the from and to TransitStops for pt legs
    @Override
    public void handleEvent(AgentWaitingForPtEvent event) {
        if (agent2CurrentLegMode.get(event.getPersonId()).equals(TransportMode.pt)) {
            agent2CurrentLegStartPtStop.put(event.getPersonId(), event.getWaitingAtStopId());
            agent2CurrentLegEndPtStop.put(event.getPersonId(), event.getDestinationStopId());
        } else {
            throw new RuntimeException("AgentWaitingForPtEvent although current leg mode is not pt for agent " +
                    event.getPersonId() + " at time " + event.getTime());
        }

    }

    // Detect end of wait time and begin of in-vehicle time, monitor used vehicle to count in-vehicle distance
    @Override
    public void handleEvent(PersonEntersVehicleEvent event) {
        if (agent2CurrentTripStartLink.containsKey(event.getPersonId())) {
            agent2CurrentLegVehicle.put(event.getPersonId(), event.getVehicleId());
            agent2CurrentLegEnterVehicleTime.put(event.getPersonId(), event.getTime());
            if (agent2CurrentLegMode.get(event.getPersonId()).equals(TransportMode.pt)) {
                searchTransitRouteOfVehicle(event);
            }
            if (monitoredVeh2toMonitoredDistance.containsKey(event.getVehicleId())) {
                agent2CurrentLegDistanceOffsetAtEnteringVehicle.put(event.getPersonId(),
                        monitoredVeh2toMonitoredDistance.get(event.getVehicleId()));
            } else {
                agent2CurrentLegDistanceOffsetAtEnteringVehicle.put(event.getPersonId(), 0.0);
                // -> start monitoring the vehicle
                monitoredVeh2toMonitoredDistance.put(event.getVehicleId(), 0.0);
            }
        } else {
            return;
        }
    }

    private void searchTransitRouteOfVehicle(PersonEntersVehicleEvent event) {
        if (!monitoredVeh2toTransitRoute.containsKey(event.getVehicleId())) {
            for (TransitLine line : ptSchedule.getTransitLines().values()) {
                for (TransitRoute route : line.getRoutes().values()) {
                    for (Departure departure : route.getDepartures().values()) {
                        if (departure.getVehicleId().equals(event.getVehicleId())) {
                            monitoredVeh2toTransitRoute.put(event.getVehicleId(), route.getId());
                            return;
                        }
                    }
                }
            }
        }
    }

    // teleport walk distances
    @Override
    public void handleEvent(TeleportationArrivalEvent event) {
        if (agent2CurrentTripStartLink.containsKey(event.getPersonId())) {
            // the event should(?!) give the total distance walked -> agent2CurrentTeleportDistance should not contain the agent yet
            agent2CurrentTeleportDistance.put(event.getPersonId(), event.getDistance());
        }
    }

    // Detect end of a leg
    @Override
    public void handleEvent(PersonArrivalEvent event) {
        if (agent2CurrentTripStartLink.containsKey(event.getPersonId())) {
            if (agent2CurrentLegMode.get(event.getPersonId()).equals(event.getLegMode())) {
                double waitTime;
                double grossWaitTime;
                double inVehicleTime;
                double distance;
                Id<TransitRoute> ptRoute;
                // e.g. pt leg
                if (agent2CurrentLegEnterVehicleTime.containsKey(event.getPersonId())) {
                    inVehicleTime = event.getTime() - agent2CurrentLegEnterVehicleTime.get(event.getPersonId());
                    distance = monitoredVeh2toMonitoredDistance.get(agent2CurrentLegVehicle.get(event.getPersonId())) -
                            agent2CurrentLegDistanceOffsetAtEnteringVehicle.get(event.getPersonId());
                    waitTime = agent2CurrentLegEnterVehicleTime.get(event.getPersonId()) -
                            agent2CurrentLegStartTime.get(event.getPersonId());
                    if (event.getLegMode().equals("drt")) {
                        grossWaitTime = agent2CurrentLegEnterVehicleTime.get(event.getPersonId()) -
                                agent2CurrentLegDrtRequestTime.get(event.getPersonId());
                        agent2CurrentLegDrtRequestTime.remove(event.getPersonId());
                    } else {
                        grossWaitTime = waitTime;
                    }
                    // e.g. walk leg
                } else {
                    waitTime = 0.0;
                    grossWaitTime = 0.0;
                    inVehicleTime = event.getTime() - agent2CurrentLegStartTime.get(event.getPersonId());
                    if (agent2CurrentTeleportDistance.containsKey(event.getPersonId())) {
                        distance = agent2CurrentTeleportDistance.get(event.getPersonId());
                    } else {
                        throw new RuntimeException("agent with PersonArrivalEvent but neither teleport distance nor" +
                                " enter vehicle time" + event.getPersonId());
                    }
                }
                if (event.getLegMode().equals(TransportMode.pt)) {
                    ptRoute = monitoredVeh2toTransitRoute.get(agent2CurrentLegVehicle.get(event.getPersonId()));
                } else {
                    ptRoute = Id.create("no pt", TransitRoute.class);
                }
                // Save ExperiencedLeg and remove temporary data
                agent2CurrentTripExperiencedLegs.get(event.getPersonId()).add(new ExperiencedLeg(
                        event.getPersonId(), agent2CurrentLegStartLink.get(event.getPersonId()),
                        event.getLinkId(), (double) agent2CurrentLegStartTime.get(event.getPersonId()),
                        event.getTime(), event.getLegMode(), waitTime, grossWaitTime, inVehicleTime, distance, ptRoute,
                        agent2CurrentLegStartPtStop.get(event.getPersonId()),
                        agent2CurrentLegEndPtStop.get(event.getPersonId())));
                agent2CurrentLegMode.remove(event.getPersonId());
                agent2CurrentLegStartLink.remove(event.getPersonId());
                agent2CurrentLegStartTime.remove(event.getPersonId());
                agent2CurrentLegStartPtStop.remove(event.getPersonId());
                agent2CurrentLegEndPtStop.remove(event.getPersonId());
                agent2CurrentLegEnterVehicleTime.remove(event.getPersonId());
                agent2CurrentLegDistanceOffsetAtEnteringVehicle.remove(event.getPersonId());
                agent2CurrentLegVehicle.remove(event.getPersonId());
                agent2CurrentTeleportDistance.remove(event.getPersonId());
            } else {
                throw new RuntimeException("leg mode at PersonArrivalEvent different from leg mode saved at last " +
                        "PersonDepartureEvent for agent " + event.getPersonId() + " at time " + event.getTime());
            }
        }
    }

    //Test
    int tripCounter = 0;

    // Detect end of a trip
    @Override
    public void handleEvent(ActivityStartEvent event) {
        if (agent2CurrentTripStartLink.containsKey(event.getPersonId())) {
            // Check if this a real activity or whether the trip will continue with another leg after an "pt interaction"
            if (!(event.getActType().equals("pt interaction") || event.getActType().equals("drt interaction"))) {
                //Check if trip starts or ends in the monitored area, that means on the monitored start and end links
                //monitoredStartAndEndLinks=null -> all links are to be monitored
                if (monitoredStartAndEndLinks.size() == 0 ||
                        monitoredStartAndEndLinks.contains(event.getLinkId()) ||
                        monitoredStartAndEndLinks.contains(agent2CurrentTripStartLink.get(event.getPersonId()))) {
                    if (!person2ExperiencedTrips.containsKey(event.getPersonId())) {
                        person2ExperiencedTrips.put(event.getPersonId(), new ArrayList<>());
                    }
                    // Save ExperiencedTrip and remove temporary data
                    person2ExperiencedTrips.get(event.getPersonId()).add(new ExperiencedTrip(
                            event.getPersonId(), agent2CurrentTripActivityBefore.get(event.getPersonId()), event.getActType(),
                            agent2CurrentTripStartLink.get(event.getPersonId()), event.getLinkId(),
                            agent2CurrentTripStartTime.get(event.getPersonId()), event.getTime(),
                            /* events are read in chronological order -> trips are found in chronological order
                             * -> save chronological tripNumber for identification of trips
                             */
                            person2ExperiencedTrips.get(event.getPersonId()).size() + 1,
                            agent2CurrentTripExperiencedLegs.get(event.getPersonId()), monitoredModes));
                    tripCounter++;
                    if (tripCounter % 50000 == 0) System.out.println("ExperiencedTrip " + tripCounter);
                }
                agent2CurrentTripStartTime.remove(event.getPersonId());
                agent2CurrentTripStartLink.remove(event.getPersonId());
                agent2CurrentTripExperiencedLegs.remove(event.getPersonId());
            }
        }
    }

    // Getter
    public Map<Id<Person>, List<ExperiencedTrip>> getPerson2ExperiencedTrips() {
        return person2ExperiencedTrips;
    }

    public Set<String> getMonitoredModes() {
        return monitoredModes;
    }

    public Set<Id<Link>> getMonitoredStartAndEndLinks() {
        return monitoredStartAndEndLinks;
    }


}
