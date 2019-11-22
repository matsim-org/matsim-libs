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

import org.apache.commons.lang3.mutable.MutableDouble;
import org.apache.log4j.Logger;
import org.locationtech.jts.geom.Geometry;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.*;
import org.matsim.api.core.v01.events.handler.*;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.drt.passenger.events.DrtRequestSubmittedEvent;
import org.matsim.contrib.drt.passenger.events.DrtRequestSubmittedEventHandler;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
import org.matsim.contrib.dvrp.vrpagent.VrpAgentLogic;
import org.matsim.core.api.experimental.events.AgentWaitingForPtEvent;
import org.matsim.core.api.experimental.events.TeleportationArrivalEvent;
import org.matsim.core.api.experimental.events.handler.AgentWaitingForPtEventHandler;
import org.matsim.core.api.experimental.events.handler.TeleportationArrivalEventHandler;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.misc.Time;
import org.matsim.pt.transitSchedule.api.*;
import org.matsim.vehicles.Vehicle;

import analysis.drtOccupancy.DynModeTripsAnalyser;
import scala.Int;

import java.util.*;
import java.util.Map.Entry;

// One trip is the sum of all legs and "pt interaction" activities between to real, non-"pt interaction", activities
// Coords unavailable in events -> no coords written

/**
 * Please note: Looks up the TransitRoute using the vehicleId. If the same
 * vehicle services multiple TransitRoutes, this program will always save the
 * first TransitRoute found where this vehicle operates as the TransitRoute used
 * for the leg, although the agent used another TransitRoute where the same
 * vehicle operates, too.
 * <p>
 * Drt legs can start when the drt request is submitted. This can happen before
 * or after the PersonDepartureEvent, that means a part of the wait time can
 * take place before the agent has terminated its last activity (or pt
 * interaction) prior to departing for the drt leg. Therefore the wait time is
 * split into gross wait time (wait time between the drt request and the drt
 * vehicle arrival) and (net) wait time (wait time between the departure event
 * and the drt vehicle arrival).
 *
 * @param network
 * @param monitoredModes
 *            : All trips to be monitored have to consist only of legs of these
 *            modes
 * @param monitoredStartAndEndLinks
 *            : only trips which start or end on one these links will be
 *            monitored. Set to null if you want to have all trips from all
 *            origins and to all destinations.
 * @author gleich
 */
public class DrtPtTripEventHandler implements ActivityStartEventHandler, ActivityEndEventHandler,
		PersonDepartureEventHandler, PersonArrivalEventHandler, PersonEntersVehicleEventHandler, LinkEnterEventHandler,
		LinkLeaveEventHandler, TeleportationArrivalEventHandler, AgentWaitingForPtEventHandler,
		VehicleLeavesTrafficEventHandler, DrtRequestSubmittedEventHandler, TransitDriverStartsEventHandler, PersonStuckEventHandler {

	// private Set<Id<Person>> agentsOnMonitoredTrip = new HashSet<>(); ->
	// agent2CurrentTripStartLink.contains()
	// private Map<Id<Person>, Boolean> agentHasDrtLeg = new HashMap<>();
	private Network network;
	private TransitSchedule ptSchedule;
	private Map<String, Geometry> zoneMap;
	private Map<Id<Person>, List<ExperiencedTrip>> person2ExperiencedTrips = new HashMap<>();

	// private Map<Id<Person>, Coord> agent2CurrentTripStartCoord = new HashMap<>();
	private Map<Id<Person>, String> agent2CurrentTripActivityBefore = new HashMap<>();
	private Map<Id<Person>, Id<Link>> agent2CurrentTripStartLink = new HashMap<>();
	private Map<Id<Person>, Double> agent2CurrentTripStartTime = new HashMap<>();
	private Map<Id<Person>, List<ExperiencedLeg>> agent2CurrentTripExperiencedLegs = new HashMap<>();

	// private Map<Id<Person>, Coord> agent2CurrentLegStartCoord = new HashMap<>();
	private Map<Id<Person>, String> agent2CurrentLegMode = new HashMap<>();
	private Map<Id<Person>, Id<Link>> agent2CurrentLegStartLink = new HashMap<>();
	private Map<Id<Person>, Double> agent2CurrentLegStartTime = new HashMap<>();
	private Map<Id<Person>, Double> agent2CurrentLegDrtRequestTime = new HashMap<>();
	private Map<Id<Person>, Id<TransitStopFacility>> agent2CurrentLegStartPtStop = new HashMap<>();
	private Map<Id<Person>, Id<TransitStopFacility>> agent2CurrentLegEndPtStop = new HashMap<>();
	private Map<Id<Person>, Double> agent2CurrentLegEnterVehicleTime = new HashMap<>();
	private Map<Id<Person>, Double> agent2CurrentLegDistanceOffsetAtEnteringVehicle = new HashMap<>();
	private Map<Id<Person>, List<Id<Link>>> agent2CurrentLegRoute = new HashMap<>();
	private Map<Id<Person>, Id<Vehicle>> agent2CurrentLegVehicle = new HashMap<>();
	private Map<Id<Person>, Double> agent2CurrentTeleportDistance = new HashMap<>();
	private Map<String, Map<Double, Set<Id<Vehicle>>>> zone2BinActiveVehicleMap = new HashMap<>();
	private Map<Id<Link>, String> link2Zone = new HashMap<>();
	private Map<Id<Vehicle>, String> vehicleId2Mode = new HashMap<>();
	// private Map<Id<Vehicle>, Tuple<MutableDouble, MutableDouble>> LinkTravelTimes
	// = new HashMap<>();
	private Map<Id<Vehicle>, Double> linkEnterTimes = new HashMap<>();
	private Map<String, MutableDouble> ModeMileageMap = new HashMap<>();

	private Map<Id<Vehicle>, Double> monitoredVeh2toMonitoredDistance = new HashMap<>();
	private Map<Id<Vehicle>, Id<TransitRoute>> monitoredVeh2toTransitRoute = new HashMap<>();
	private Set<String> monitoredModes = new HashSet<>();
	private Set<Id<Link>> monitoredStartAndEndLinks; // set to null if all links are to be monitored
	private int timeSpanReachedCounter;
	private int interval;
	private Set<String> activeVehicle;
	private int stuckPersonCounter;

	/**
	 * @param network
	 * @param monitoredModes
	 *            : All trips to be monitored have to consist only of legs of these
	 *            modes
	 * @param monitoredStartAndEndLinks
	 *            : only trips which start or end on one these links will be
	 *            monitored. Set to null if you want to have all trips from all
	 *            origins and to all destinations
	 */
	public DrtPtTripEventHandler(Network network, TransitSchedule ptSchedule, Set<String> monitoredModes,
			Set<Id<Link>> monitoredStartAndEndLinks, Map<Id<Link>, String> link2Zone, Map<String, Geometry> zoneMap) {
		this.network = network;
		this.ptSchedule = ptSchedule;
		this.monitoredModes = monitoredModes; // pt, transit_walk, drt: walk eigentlich nicht, aber in
												// FixedDistanceBased falsch als walk statt transit_walk gesetzt
		this.monitoredStartAndEndLinks = monitoredStartAndEndLinks;
		this.link2Zone = link2Zone;
		this.vehicleId2Mode = new HashMap<Id<Vehicle>, String>();
		this.zoneMap = zoneMap;
		this.timeSpanReachedCounter = 0;
		this.interval = 120;
		this.activeVehicle = new HashSet<String>();
		this.activeVehicle.add(TransportMode.car);
		this.activeVehicle.add(TransportMode.drt);
		this.activeVehicle.add("commercial");
		this.stuckPersonCounter=0;

		this.ModeMileageMap = new HashMap<String, MutableDouble>();

		// Initialize zone2BinMap
		System.out.println("Initialze zone2TimeBinsMap");
		for (String zone : zoneMap.keySet()) {
			zone2BinActiveVehicleMap.put(zone, new HashMap<Double, Set<Id<Vehicle>>>());

			for (int time = 0; time < 30 * 3600; time = time + interval) {
				zone2BinActiveVehicleMap.get(zone).put((double) time, new HashSet<Id<Vehicle>>());
			}
		}

		// Initialize ModeMileageMap
		// ModeMileageMap.put(TransportMode.pt, new MutableDouble(0));
		ModeMileageMap.put("tram", new MutableDouble(0));
		ModeMileageMap.put("bus", new MutableDouble(0));
		ModeMileageMap.put("rail", new MutableDouble(0));
		ModeMileageMap.put(TransportMode.drt, new MutableDouble(0));
		ModeMileageMap.put(TransportMode.car, new MutableDouble(0));
		ModeMileageMap.put("commercial", new MutableDouble(0));

	}

	@Override
	public void reset(int iteration) {
		// TODO Auto-generated method stub

	}

	@Override
	public void handleEvent(LinkLeaveEvent event) {

		if (linkEnterTimes.containsKey(event.getVehicleId())) {
			Id<Vehicle> vehicleId = event.getVehicleId();
			Id<Link> actualLinkId = event.getLinkId();

			double enterTime = linkEnterTimes.remove(event.getVehicleId());
			double leaveTime = event.getTime();
			String zone = link2Zone.get(actualLinkId);
			double linkLength = network.getLinks().get(actualLinkId).getLength();

			// Determine time borders for this travelTime
			int leftBinBorder = (int) ((Math.floor(enterTime / interval)) * interval);
			int rightBinBorder = (int) ((Math.ceil(leaveTime / interval)) * interval);

			String transportMode = null;

			if (vehicleId2Mode.containsKey(vehicleId)) {
				transportMode = vehicleId2Mode.get(vehicleId);
			} else {
				// If vehicle is not detected as drt nor pt, it needs to be a car
				transportMode = TransportMode.car;
				
				//Freight agents are counted as commercial
				if(vehicleId.toString().contains("freight"))
				{
					transportMode="commercial";
				}

			}

			// Only process if link is in zone
			if (link2Zone.containsKey(actualLinkId)) {

				ModeMileageMap.get(transportMode).add(linkLength);

				// Loop over all time bins that are relevant, i.e. vehicle was in congestion
				for (int time = leftBinBorder; time < rightBinBorder; time = time + interval) {
					double actBin = time;
					if ((zone != null) && (activeVehicle.contains(transportMode))) {
						// add a car to bin and zone, if it doesn't already exits
						if (zone2BinActiveVehicleMap.get(zone).containsKey(actBin)) {
							zone2BinActiveVehicleMap.get(zone).get(actBin).add(vehicleId);

						} else {

							if (timeSpanReachedCounter < 10) {
								Logger.getLogger(DrtPtTripEventHandler.class).error(
										"Time Range insuffucient. Simulation > 24 h. Increase time span of analysis. Supress additional warnings");

							}
							timeSpanReachedCounter++;

						}
					}

				}
			}

		}

	}

	// in-vehicle distances
	@Override
	public void handleEvent(LinkEnterEvent event) {
		
		
		if (monitoredVeh2toMonitoredDistance.containsKey(event.getVehicleId())) {
			monitoredVeh2toMonitoredDistance.put(event.getVehicleId(),
					monitoredVeh2toMonitoredDistance.get(event.getVehicleId())
							+ network.getLinks().get(event.getLinkId()).getLength());
		}

		linkEnterTimes.put(event.getVehicleId(), event.getTime());

		// Add links to routes to allow transport performance calculation
		if (link2Zone.containsKey(event.getLinkId())) {

			// Detect transport mode over the vehicleId
			String transportMode;

			if (vehicleId2Mode.containsKey(event.getVehicleId())) {
				transportMode = vehicleId2Mode.get(event.getVehicleId());
			} else {
				// If vehicle is not detected as drt nor pt, it needs to be a car
				transportMode = TransportMode.car;

			}

			// If transport mode is e.g. pt or drt this mode is pooled
			// We need to look up for which person is this link enter event relevant
			// Link enter events are stored to construct routes.
			if (transportMode != TransportMode.car) {

				agent2CurrentLegVehicle.entrySet().parallelStream().forEach(personEntry -> {

					if (personEntry.getValue() == event.getVehicleId()) {

						if (agent2CurrentLegRoute.containsKey(personEntry.getKey())) {
							agent2CurrentLegRoute.get(personEntry.getKey()).add(event.getLinkId());

						} else {
							agent2CurrentLegRoute.put(personEntry.getKey(), new ArrayList<Id<Link>>());

						}
					}

				});

				// transport mode equals car, in this case = 
			} else {

				if (agent2CurrentLegRoute.containsKey(Id.create(event.getVehicleId().toString(), Person.class))) {
					agent2CurrentLegRoute.get(Id.create(event.getVehicleId().toString(), Person.class))
							.add(event.getLinkId());

				} else {
					agent2CurrentLegRoute.put(Id.create(event.getVehicleId().toString(), Person.class),
							new ArrayList<Id<Link>>());

				}

			}
		}

		// for (Entry<Id<Person>, Id<Vehicle>> personEntry :
		// agent2CurrentLegVehicle.entrySet()) {
		//
		// if (personEntry.getValue() == event.getVehicleId()) {
		//
		// if (agent2CurrentLegRoute.containsKey(personEntry.getKey())) {
		// agent2CurrentLegRoute.get(personEntry.getKey()).add(event.getLinkId());
		//
		// } else {
		// agent2CurrentLegRoute.put(personEntry.getKey(), new ArrayList<Id<Link>>());
		//
		// }
		// }
		// }

	}

	/*
	 * Save the activity type of the last activity before the trip. We cannot know
	 * yet if this is a trip to be monitored or not (leg mode and arrival link are
	 * unknown), so save this for all agents.
	 */
	@Override
	public void handleEvent(ActivityEndEvent event) {
		if (!(event.getActType().equals("pt interaction") || event.getActType().equals("drt interaction"))) {
			agent2CurrentTripActivityBefore.put(event.getPersonId(), event.getActType());
		}

		if (event.getActType().equals(VrpAgentLogic.BEFORE_SCHEDULE_ACTIVITY_TYPE)) {
			handleBeforeScheduleEvent(event);
		}
	}

	private void handleBeforeScheduleEvent(ActivityEndEvent event) {
		vehicleId2Mode.put(p2vid(event.getPersonId()), TransportMode.drt);
	}

	private Id<Vehicle> p2vid(Id<Person> pid) {
		Id<Vehicle> vid = Id.create(pid.toString(), Vehicle.class);
		return vid;
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
		 * if a trip includes a leg of a mode not contained in monitoredModes, this lead
		 * to NullPointerExceptions at handleEvent(PersonArrivalEvent event). This can
		 * be avoided by removing the following check of the leg mode, however in this
		 * case all legs of all modes will be saved and later while saving
		 * ExperiencedTrips in handleEvent(ActivityStartEvent event) those trips not
		 * containing would have to be filtered out.
		 */
		if (!monitoredModes.contains(event.getLegMode())) {
			return;
		} else {
			if (agent2CurrentLegStartLink.containsKey(event.getPersonId())) {
				throw new RuntimeException("agent " + event.getPersonId() + " has PersonDepartureEvent at time "
						+ event.getTime() + " although the previous leg is not finished yet.");
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
			throw new RuntimeException("AgentWaitingForPtEvent although current leg mode is not pt for agent "
					+ event.getPersonId() + " at time " + event.getTime());
		}

	}

	// Detect end of wait time and begin of in-vehicle time, monitor used vehicle to
	// count in-vehicle distance
	@Override
	public void handleEvent(PersonEntersVehicleEvent event) {
		if (agent2CurrentTripStartLink.containsKey(event.getPersonId())) {

			// Relevant person is in vehicle
			// agent2CurrentLegVehicle is the vehicle in which the person sits
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
			// the event should(?!) give the total distance walked ->
			// agent2CurrentTeleportDistance should not contain the agent yet
			agent2CurrentTeleportDistance.put(event.getPersonId(), event.getDistance());
		}
	}

	// Detect end of a leg
	@Override
	public void handleEvent(PersonArrivalEvent event) {
		if (agent2CurrentTripStartLink.containsKey(event.getPersonId())) {

			List<Id<Link>> routeList = new ArrayList<Id<Link>>();

			if (agent2CurrentLegMode.get(event.getPersonId()).equals(event.getLegMode())) {
							
				
				double waitTime;
				double grossWaitTime;
				double inVehicleTime;
				double distance;
				Id<TransitRoute> ptRoute;
				// e.g. pt leg
				if (agent2CurrentLegEnterVehicleTime.containsKey(event.getPersonId())) {
					inVehicleTime = event.getTime() - agent2CurrentLegEnterVehicleTime.get(event.getPersonId());
					distance = monitoredVeh2toMonitoredDistance.get(agent2CurrentLegVehicle.get(event.getPersonId()))
							- agent2CurrentLegDistanceOffsetAtEnteringVehicle.get(event.getPersonId());
					waitTime = agent2CurrentLegEnterVehicleTime.get(event.getPersonId())
							- agent2CurrentLegStartTime.get(event.getPersonId());
					if (event.getLegMode().equals("drt")) {
						grossWaitTime = agent2CurrentLegEnterVehicleTime.get(event.getPersonId())
								- agent2CurrentLegDrtRequestTime.get(event.getPersonId());
						agent2CurrentLegDrtRequestTime.remove(event.getPersonId());
					} else {
						grossWaitTime = waitTime;
					}

					routeList = agent2CurrentLegRoute.get(event.getPersonId());
					String legMode = event.getLegMode();

					// if (legMode.equals(TransportMode.drt)) {
					// System.out.println(event.getPersonId()+ " || " + Time.writeTime(
					// event.getTime()) + " || " +routeList);
					//
					// }

					// e.g. walk leg
				} else {

					waitTime = 0.0;
					grossWaitTime = 0.0;
					inVehicleTime = event.getTime() - agent2CurrentLegStartTime.get(event.getPersonId());
					if (agent2CurrentTeleportDistance.containsKey(event.getPersonId())) {
						distance = agent2CurrentTeleportDistance.get(event.getPersonId());
					} else {
						throw new RuntimeException("agent with PersonArrivalEvent but neither teleport distance nor"
								+ " enter vehicle time" + event.getPersonId());
					}
				}
				if (event.getLegMode().equals(TransportMode.pt)) {
					ptRoute = monitoredVeh2toTransitRoute.get(agent2CurrentLegVehicle.get(event.getPersonId()));
				} else {
					ptRoute = Id.create("no pt", TransitRoute.class);
				}


				// Save ExperiencedLeg and remove temporary data
				agent2CurrentTripExperiencedLegs.get(event.getPersonId())
						.add(new ExperiencedLeg(event.getPersonId(), agent2CurrentLegStartLink.get(event.getPersonId()),
								event.getLinkId(), (double) agent2CurrentLegStartTime.get(event.getPersonId()),
								event.getTime(), event.getLegMode(), waitTime, grossWaitTime, inVehicleTime, distance,
								ptRoute, agent2CurrentLegStartPtStop.get(event.getPersonId()),
								agent2CurrentLegEndPtStop.get(event.getPersonId()), routeList));
				agent2CurrentLegMode.remove(event.getPersonId());
				agent2CurrentLegStartLink.remove(event.getPersonId());
				agent2CurrentLegStartTime.remove(event.getPersonId());
				agent2CurrentLegStartPtStop.remove(event.getPersonId());
				agent2CurrentLegEndPtStop.remove(event.getPersonId());
				agent2CurrentLegEnterVehicleTime.remove(event.getPersonId());
				agent2CurrentLegDistanceOffsetAtEnteringVehicle.remove(event.getPersonId());
				agent2CurrentLegVehicle.remove(event.getPersonId());
				agent2CurrentTeleportDistance.remove(event.getPersonId());
				agent2CurrentLegRoute.remove(event.getPersonId());
			} else {
				throw new RuntimeException("leg mode at PersonArrivalEvent different from leg mode saved at last "
						+ "PersonDepartureEvent for agent " + event.getPersonId() + " at time " + event.getTime());
			}
		}
	}

	// Test
	int tripCounter = 0;

	// Detect end of a trip
	@Override
	public void handleEvent(ActivityStartEvent event) {
		if (agent2CurrentTripStartLink.containsKey(event.getPersonId())) {
			// Check if this a real activity or whether the trip will continue with another
			// leg after an "pt interaction"
			if (!(event.getActType().equals("pt interaction") || event.getActType().equals("drt interaction"))) {
				// Check if trip starts or ends in the monitored area, that means on the
				// monitored start and end links
				// monitoredStartAndEndLinks=null -> all links are to be monitored
				if (monitoredStartAndEndLinks.size() == 0 || monitoredStartAndEndLinks.contains(event.getLinkId())
						|| monitoredStartAndEndLinks.contains(agent2CurrentTripStartLink.get(event.getPersonId()))) {
					if (!person2ExperiencedTrips.containsKey(event.getPersonId())) {
						person2ExperiencedTrips.put(event.getPersonId(), new ArrayList<>());
					}
					// Save ExperiencedTrip and remove temporary data
					person2ExperiencedTrips.get(event.getPersonId())
							.add(new ExperiencedTrip(event.getPersonId(),
									agent2CurrentTripActivityBefore.get(event.getPersonId()), event.getActType(),
									agent2CurrentTripStartLink.get(event.getPersonId()), event.getLinkId(),
									agent2CurrentTripStartTime.get(event.getPersonId()), event.getTime(),
									/*
									 * events are read in chronological order -> trips are found in chronological
									 * order -> save chronological tripNumber for identification of trips
									 */
									person2ExperiencedTrips.get(event.getPersonId()).size() + 1,
									agent2CurrentTripExperiencedLegs.get(event.getPersonId()), monitoredModes));
					tripCounter++;
					if (tripCounter % 50000 == 0)
						System.out.println("ExperiencedTrip " + tripCounter);
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

	public Map<String, Map<Double, Set<Id<Vehicle>>>> getZone2BinActiveVehicleMap() {
		return zone2BinActiveVehicleMap;
	}
	
	public int getStuckEvents() {
		return this.stuckPersonCounter;
	}

	public Map<String, MutableDouble> getModeMileageMap() {
		return ModeMileageMap;
	}

	public Set<String> getMonitoredModes() {
		return monitoredModes;
	}

	public Set<Id<Link>> getMonitoredStartAndEndLinks() {
		return monitoredStartAndEndLinks;
	}

	@Override
	public void handleEvent(TransitDriverStartsEvent event) {

		Id<Vehicle> transitVehicleId = event.getVehicleId();
		if (transitVehicleId.toString().contains("rail")) {
			vehicleId2Mode.put(transitVehicleId, "rail");
		} else if (transitVehicleId.toString().contains("bus")) {
			vehicleId2Mode.put(transitVehicleId, "bus");
		} else if (transitVehicleId.toString().contains("tram")) {
			vehicleId2Mode.put(transitVehicleId, "tram");
		}
		// vehicleId2Mode.put(transitVehicleId, TransportMode.pt);

	}

	@Override
	public void handleEvent(VehicleLeavesTrafficEvent event) {
		if (linkEnterTimes.containsKey(event.getVehicleId())) {
			Id<Vehicle> vehicleId = event.getVehicleId();
			Id<Link> actualLinkId = event.getLinkId();

			double enterTime = linkEnterTimes.remove(event.getVehicleId());
			double leaveTime = event.getTime();
			String zone = link2Zone.get(actualLinkId);
			double linkLength = network.getLinks().get(actualLinkId).getLength();

			// Determine time borders for this travelTime
			int leftBinBorder = (int) ((Math.floor(enterTime / interval)) * interval);
			int rightBinBorder = (int) ((Math.ceil(leaveTime / interval)) * interval);

			// if ((rightBinBorder-leftBinBorder)>1000.0)
			// {
			// System.out.println("actualLinkId: "+actualLinkId+ " || "+ vehicleId + " || "
			// + zone);
			// }

			// Link l = network.getLinks().get(event.getLinkId());
			// double freeSpeedTravelTime = Math.min( l.getLength() /
			// l.getFreespeed(),travelTime);
			// the last link

			// int cnt =0 ;

			String transportMode = null;

			if (vehicleId2Mode.containsKey(vehicleId)) {
				transportMode = vehicleId2Mode.get(vehicleId);
			} else {
				// If vehicle is not detected as drt nor pt, it needs to be a car
				transportMode = TransportMode.car;
				
				//Freight agents are counted as commercial
				if(vehicleId.toString().contains("freight"))
				{
					transportMode="commercial";
				}

			}

			// Only process if link is in zone
			if (link2Zone.containsKey(actualLinkId)) {

				// Link is relevant for mileage analysis
				ModeMileageMap.get(transportMode).add(linkLength);

				// Loop over all time bins that are relevant, i.e. vehicle was in congestion
				for (int time = leftBinBorder; time < rightBinBorder; time = time + interval) {
					double actBin = time;
					if ((zone != null) && (activeVehicle.contains(transportMode))) {
						// add a car to bin and zone, if it doesn't already exits
						if (zone2BinActiveVehicleMap.get(zone).containsKey(actBin)) {
							zone2BinActiveVehicleMap.get(zone).get(actBin).add(vehicleId);

						} else {

							if (timeSpanReachedCounter < 10) {
								Logger.getLogger(DrtPtTripEventHandler.class).error(
										"Time Range insuffucient. Simulation > 24 h. Increase time span of analysis. Supress additional warnings");

							}
							timeSpanReachedCounter++;

						}
					}

				}
			}

		}

	}

	@Override
	public void handleEvent(PersonStuckEvent event) {
		stuckPersonCounter++;
		
	}

	// @Override
	// public void handleEvent(PersonStuckEvent event) {
	// stuckCnt++;
	// System.out.println(stuckCnt);
	//
	// }

}
