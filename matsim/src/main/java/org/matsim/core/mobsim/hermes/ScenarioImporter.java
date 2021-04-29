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

package org.matsim.core.mobsim.hermes;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.IdMap;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.PersonLeavesVehicleEvent;
import org.matsim.api.core.v01.events.TransitDriverStartsEvent;
import org.matsim.api.core.v01.events.VehicleEntersTrafficEvent;
import org.matsim.api.core.v01.events.VehicleLeavesTrafficEvent;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.api.experimental.events.AgentWaitingForPtEvent;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.api.experimental.events.TeleportationArrivalEvent;
import org.matsim.core.api.experimental.events.VehicleArrivesAtFacilityEvent;
import org.matsim.core.api.experimental.events.VehicleDepartsAtFacilityEvent;
import org.matsim.core.events.EventArray;
import org.matsim.core.mobsim.hermes.Agent.PlanArray;
import org.matsim.core.population.routes.GenericRouteImpl;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.utils.collections.ArrayMap;
import org.matsim.core.utils.collections.IntArrayMap;
import org.matsim.core.utils.misc.OptionalTime;
import org.matsim.facilities.ActivityFacility;
import org.matsim.pt.routes.TransitPassengerRoute;
import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleCapacity;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class ScenarioImporter {

	final private static Logger log = Logger.getLogger(Hermes.class);

	private static ScenarioImporter instance;

	private Thread resetThread = null;

	// Scenario loaded by matsim;
	private final Scenario scenario;

	protected int agent_persons;

	// hermes route numbers for each line/route. Should be used as follows:
	// route_numbers.get(line id).get(route id) -> hermes-route-number
	protected IdMap<TransitLine, ArrayMap<Id<TransitRoute>, Integer>> route_numbers;

	// matsim stop ids (Id<TransitStopFacility>.index) per route number. Should be used as follows:
	// int[] stop_ids = route_stops_by_route_no[route_no]
	// route_stops_by_route_no[route_no][station_index] -> transit_stop_facility.id.index
	protected int[][] route_stops_by_route_no;

	// matsim line id (Id<TransitLine>.index) of a particular hermes route. Should be used as follows:
	// line_of_route[route_no] -> transit_line.id.index
	protected int[] line_of_route;

	// matsim route id (Id<TransitRoute>.index) of a particular hermes route number. Should be used as follows:
	// route_of_route[route_no] -> transit_route.id.index
	protected int[] route_of_route;

	// Array of links that define the network.
	protected HLink[] hermes_links;

	// Array of agents that participate in the simulation.
	// Note: in order to make MATSim Agent ids, some positions in the array might be null.
	protected Agent[] hermes_agents;

	protected Realm realm;
	private final boolean deterministicPt;
	// Agents waiting in pt stations. Should be used as follows:
	// agent_stops.get(curr station id).get(line id) -> queue of agents
	protected IdMap<TransitStopFacility, IntArrayMap<ArrayDeque<Agent>>> agent_stops;

	private float[] flowCapacityPCEs;
	private float[] storageCapacityPCEs;
	private Map<Id<VehicleType>, Integer> vehicleTypeMapping = new HashMap<>();
	protected final EventsManager eventsManager;
	private final int numberOfThreads;
	private final List<List<Event>> deterministicPtEvents;

	private ScenarioImporter(Scenario scenario, EventsManager eventsManager) {
		numberOfThreads = Math.min(scenario.getConfig().global().getNumberOfThreads(), Runtime.getRuntime().availableProcessors());
		this.deterministicPt = scenario.getConfig().hermes().isDeterministicPt();
		if (deterministicPt) {
			deterministicPtEvents = new ArrayList<>(scenario.getConfig().hermes().getEndTime());
			for (int i = 0; i < scenario.getConfig().hermes().getEndTime(); i++) {
				deterministicPtEvents.add(new ArrayList<>(16));
			}
		} else {
			deterministicPtEvents = Collections.EMPTY_LIST;
		}
		this.scenario = scenario;
		this.eventsManager = eventsManager;
		generateVehicleCategories();
		generateLinks();
		generatePT();
		generateAgents();

	}

	private void generateVehicleCategories() {
		int vehicleTypes = scenario.getVehicles().getVehicleTypes().size();
		if (vehicleTypes >= HermesConfigGroup.MAX_VEHICLE_PCETYPES) {
			throw new RuntimeException(
					"Too many vehicle types defined. A maximum of " + HermesConfigGroup.MAX_VEHICLE_PCETYPES + " is supported.");
		}
		flowCapacityPCEs = new float[vehicleTypes];
		storageCapacityPCEs = new float[vehicleTypes];
		int i = 0;
		for (VehicleType t : scenario.getVehicles().getVehicleTypes().values()) {
			//downscaling of vehicles = Upscaling of PCUs
			flowCapacityPCEs[i] = (float) (t.getPcuEquivalents() / (t.getFlowEfficiencyFactor() * scenario.getConfig().hermes().getFlowCapacityFactor()));
			storageCapacityPCEs[i] = (float) (t.getPcuEquivalents() / scenario.getConfig().hermes().getStorageCapacityFactor());
			vehicleTypeMapping.put(t.getId(), i);
			i++;
		}

	}

	public static void flush() {
		instance = null;
	}

	public static ScenarioImporter instance(Scenario scenario, EventsManager eventsManager) {
		// if instance is null or the scenario changed or events manager changed, re-do everything.
		if (instance == null || !scenario.equals(instance.scenario) || !eventsManager.equals(instance.eventsManager)) {
			log.info("Hermes rebuilding scenario!");
			instance = new ScenarioImporter(scenario, eventsManager);
		}
		return instance;
	}

	public void generate() throws Exception {
		long time = System.currentTimeMillis();

		if (resetThread != null) {
			resetThread.join();
		}

		log.info(String.format("Hermes reset took %d ms  (%d agents %d links)", System.currentTimeMillis() - time, hermes_agents.length, hermes_links.length));
		time = System.currentTimeMillis();
		generatePlans();
		log.info(String.format("Hermes generatePlans took %d ms", System.currentTimeMillis() - time));
		time = System.currentTimeMillis();
		generateRealms();
		log.info(String.format("Hermes generateRealms took %d ms", System.currentTimeMillis() - time));
	}

	public void reset() {
		resetThread = new Thread() {

			@Override
			public void run() {
				log.info("resetting hermes...");
				// reset links
				for (int i = 0; i < hermes_links.length; i++) {
					HLink link = hermes_links[i];
					if (link != null) {
						link.reset();
					}
				}
				// reset agent plans and events
				for (int i = 0; i < hermes_agents.length; i++) {
					if (hermes_agents[i] != null) {
						hermes_agents[i].reset();
					}
				}
				// reset agent_stops
				for (IntArrayMap<ArrayDeque<Agent>> station_id : agent_stops) {
					for (ArrayDeque<Agent> agents_per_line : station_id.values()) {
						agents_per_line.clear();
					}
				}

			}
		};

		resetThread.start();
	}

	private void generateLinks() {
		Network network = scenario.getNetwork();
		Collection<? extends org.matsim.api.core.v01.network.Link> matsim_links =
				network.getLinks().values();
		hermes_links = new HLink[Id.getNumberOfIds(org.matsim.api.core.v01.network.Link.class)];

		for (org.matsim.api.core.v01.network.Link matsim_link : matsim_links) {
			int length = Math.max(1, (int) Math.round(matsim_link.getLength()));
			int speed = Math.max(1, (int) Math.round(matsim_link.getFreespeed()));
			int lanes = (int) Math.round(matsim_link.getNumberOfLanes());
			int storageCapacity = Math.max(1, (int) (Math.ceil(matsim_link.getLength() / network.getEffectiveCellSize() * lanes)));
			int link_id = matsim_link.getId().index();
			final float effectiveflowCapacityPerSec = (float) matsim_link.getFlowCapacityPerSec();

			if (link_id > HermesConfigGroup.MAX_LINK_ID) {
				throw new RuntimeException("exceeded maximum number of links");
			}

			hermes_links[link_id] = new HLink(link_id, storageCapacity, length, speed, effectiveflowCapacityPerSec, scenario.getConfig().hermes().getStuckTime());
		}
	}

	private void initRoutesStations() {
		TransitSchedule ts = this.scenario.getTransitSchedule();

		int routeCount = 0;
		for (TransitLine tl : ts.getTransitLines().values()) {
			routeCount += tl.getRoutes().size();
		}

		this.line_of_route = new int[routeCount];
		this.route_of_route = new int[routeCount];
		this.route_stops_by_route_no = new int[routeCount][];

		this.route_numbers = new IdMap<>(TransitLine.class);
		int routeNo = 0;
		for (TransitLine tl : ts.getTransitLines().values()) {
			ArrayMap<Id<TransitRoute>, Integer> routesMap = new ArrayMap<>();
			this.route_numbers.put(tl.getId(), routesMap);
			int lineId = tl.getId().index();
			for (TransitRoute tr : tl.getRoutes().values()) {
				routesMap.put(tr.getId(), routeNo);
				int routeId = tr.getId().index();
				this.line_of_route[routeNo] = lineId;
				this.route_of_route[routeNo] = routeId;

				List<TransitRouteStop> stops = tr.getStops();
				int[] tr_stops = new int[stops.size()];
				for (int i = 0; i < stops.size(); i++) {
					TransitRouteStop trs = stops.get(i);
					int sid = trs.getStopFacility().getId().index();
					tr_stops[i] = sid;
				}
				this.route_stops_by_route_no[routeNo] = tr_stops;
				routeNo++;
			}
		}
	}

	private void generatePT() {
		initRoutesStations();

		// Initialize agent_stops.
		this.agent_stops = new IdMap<>(TransitStopFacility.class);

		TransitSchedule ts = this.scenario.getTransitSchedule();
		for (TransitLine line : ts.getTransitLines().values()) {
			for (TransitRoute route : line.getRoutes().values()) {
				List<TransitRouteStop> stops = route.getStops();
				for (int i = 0, stopsCount = stops.size(); i < stopsCount; i++) {
					TransitRouteStop routeStop = stops.get(i);
					TransitStopFacility stopFacility = routeStop.getStopFacility();

					IntArrayMap<ArrayDeque<Agent>> linesMap = this.agent_stops.computeIfAbsent(stopFacility.getId(), k -> new IntArrayMap<>());
					linesMap.computeIfAbsent(line.getId().index(), k -> new ArrayDeque<>());
				}
			}
		}
	}

	private void generateRealms() throws Exception {
		realm = new Realm(this, eventsManager);

		// Put agents in their initial location (link or activity center)
		for (Agent agent : hermes_agents) {
			// Some agents might not have plans.
			if (agent == null || agent.plan.size() == 0) {
				continue;
			}
			long planentry = agent.plan().get(0);
			int type = Agent.getPlanHeader(planentry);
			// TODO - I should advance agents in a proper way!
			switch (type) {
				case Agent.LinkType:
					int linkid = Agent.getLinkPlanEntry(planentry);
					int velocity = Agent.getVelocityPlanEntry(planentry);
					HLink link = hermes_links[linkid];
					agent.linkFinishTime = link.length() / Math.min(velocity, link.velocity());
					link.push(agent, 0, getStorageCapacityPCE(Agent.getLinkPCEEntry(planentry)));
					break;
				case Agent.SleepForType:
				case Agent.SleepUntilType:
					int sleep = Agent.getSleepPlanEntry(planentry);
					realm.delayedAgents().get(Math.min(sleep, scenario.getConfig().hermes().getEndTime() + 1)).add(agent);
					break;
				default:
					Logger.getLogger(getClass()).error(String.format("ERROR -> unknown plan element type %d", type));
			}
		}

		for (HLink link : this.hermes_links) {
			if (link != null) {
				int nextwakeup = link.nexttime();
				if (nextwakeup > 0) {
					realm.delayedLinks().get(nextwakeup).add(link);
				}
			}
		}
	}

	private void processPlanActivity(
			Id<Person> id,
			PlanArray flatplan,
			EventArray events,
			Activity act) {
		int time = 0;
		int eventid = 0;
		Id<org.matsim.api.core.v01.network.Link> linkid;
		Id<ActivityFacility> facid = act.getFacilityId();
		String type = act.getType();

		// This logic comes from how QSim agents do it...
		if (facid == null) {
			linkid = act.getLinkId();
		} else {
			ActivityFacility facility = scenario.getActivityFacilities().getFacilities().get(facid);
			if (facility == null || facility.getLinkId() == null) {
				linkid = act.getLinkId();
			} else {
				linkid = facility.getLinkId();
			}
		}

		assert linkid != null;

		// hack to avoid a actstart as first event (hermes does not have it).
		if (flatplan.size() != 0) {
			events.add(new ActivityStartEvent(0, id, linkid, facid, type, act.getCoord()));
			eventid = events.size() - 1;
		} else {
			eventid = 0;
		}

		if (act.getEndTime().isDefined()) {
			time = (int) Math.round(act.getEndTime().seconds());
			flatplan.add(Agent.prepareSleepUntilEntry(eventid, time));
		} else if (act.getMaximumDuration().isDefined()) {
			time = (int) Math.round(act.getMaximumDuration().seconds());
			flatplan.add(Agent.prepareSleepForEntry(eventid, time));
		} else {
			// TODO - better way to handle this?
			flatplan.add(Agent.prepareSleepForEntry(eventid, 0));
		}
		events.add(new ActivityEndEvent(0, id, linkid, facid, type, act.getCoord()));
	}

	private void processPlanNetworkRoute(
			Person person,
			PlanArray flatplan,
			EventArray events,
			Leg leg,
			NetworkRoute netroute,
			Agent agent) {
		var id = person.getId();
		Id<org.matsim.api.core.v01.network.Link> startLId = netroute.getStartLinkId();
		Id<org.matsim.api.core.v01.network.Link> endLId = netroute.getEndLinkId();
		Map<Id<Vehicle>, Vehicle> vehicles = scenario.getVehicles().getVehicles();
		Vehicle v = vehicles.get(VehicleUtils.getVehicleId(person, leg.getMode()));
		Id<VehicleType> vtypeid = v == null ? VehicleUtils.getDefaultVehicleType().getId() : v.getType().getId();
		int pcuCategory = this.vehicleTypeMapping.get(vtypeid);
		Id<Vehicle> vid = v == null ? Id.createVehicleId("v" + person.getId()) : v.getId();
		int velocity = v == null ?
				HermesConfigGroup.MAX_VEHICLE_VELOCITY : (int) Math.round(v.getType().getMaximumVelocity());
		int egressId = endLId.index();

		//initial capacity setting
		if (agent.getFlowCapacityPCUE() == -1) {
			agent.setFlowCapacityPCUE(getFlowCapacityPCE(pcuCategory));
		}
		if (agent.getStorageCapacityPCUE() == -1) {
			agent.setStorageCapacityPCUE(getStorageCapacityPCE(pcuCategory));
		}
		events.add(new PersonEntersVehicleEvent(0, id, vid));
		events.add(new VehicleEntersTrafficEvent(0, id, startLId, vid, leg.getMode(), 1));
		if (netroute.getLinkIds().size() > 1 || !startLId.equals(endLId)) {
			events.add(new LinkLeaveEvent(0, vid, startLId));
		}
		for (Id<org.matsim.api.core.v01.network.Link> linkid : netroute.getLinkIds()) {
			int linkId = linkid.index();
			events.add(new LinkEnterEvent(0, vid, linkid));
			flatplan.add(Agent.prepareLinkEntry(events.size() - 1, linkId, velocity, pcuCategory));
			events.add(new LinkLeaveEvent(0, vid, linkid));
		}
		if (netroute.getLinkIds().size() > 1 || !startLId.equals(endLId)) {
			events.add(new LinkEnterEvent(0, vid, endLId));
			flatplan.add(Agent.prepareLinkEntry(events.size() - 1, egressId, velocity, pcuCategory));
		}
		events.add(new VehicleLeavesTrafficEvent(0, id, endLId, vid, leg.getMode(), 1));
		events.add(new PersonLeavesVehicleEvent(0, id, vid));
	}

	private void populateStops(int srcStopId, int lineId) {
		IntArrayMap<ArrayDeque<Agent>> agents = this.agent_stops.get(srcStopId);
		agents.computeIfAbsent(lineId, k -> new ArrayDeque<>());
	}

	private void processPlanTransitRoute(
			Id<Person> id,
			PlanArray flatplan,
			EventArray events,
			TransitPassengerRoute troute) {
		Id<TransitStopFacility> access = troute.getAccessStopId();
		Id<TransitStopFacility> egress = troute.getEgressStopId();
		int lineid = troute.getLineId().index();
		int accessid = access.index();
		int egressid = egress.index();
		int routeNo = this.route_numbers.get(troute.getLineId()).get(troute.getRouteId());

		populateStops(accessid, lineid);

		// this will be replaced dynamically
		Id<Vehicle> vid = Id.createVehicleId("tr_X");
		// Add public transport access
		events.add(new AgentWaitingForPtEvent(0, id, access, egress));
		flatplan.add(Agent.prepareWaitEntry(events.size() - 1, routeNo, accessid));
		events.add(new PersonEntersVehicleEvent(0, id, vid));
		flatplan.add(Agent.prepareAccessEntry(events.size() - 1, routeNo, accessid));
		events.add(new PersonLeavesVehicleEvent(0, id, vid));
		flatplan.add(Agent.prepareEgressEntry(events.size() - 1, routeNo, egressid));
	}

	private void processPlanElement(
			Person person,
			PlanArray flatplan,
			EventArray events,
			PlanElement element,
			Agent agent) {
		var id = person.getId();
		if (element instanceof Leg) {
			Leg leg = (Leg) element;
			Route route = leg.getRoute();
			String mode = leg.getMode();

			if (route == null) {
				return;
			}

			events.add(new PersonDepartureEvent(0, id, route.getStartLinkId(), leg.getMode()));
			if (route instanceof NetworkRoute) {
				if (scenario.getConfig().hermes().getMainModes().contains(leg.getMode())) {
					processPlanNetworkRoute(person, flatplan, events, leg, (NetworkRoute) route, agent);
				} else {
					processTeleport(id, flatplan, events, (Leg) element, route, mode);
				}
			} else if (route instanceof TransitPassengerRoute) {
				processPlanTransitRoute(id, flatplan, events, (TransitPassengerRoute) route);

			} else if (route instanceof GenericRouteImpl) {
				processTeleport(id, flatplan, events, (Leg) element, route, mode);
			} else {
				throw new RuntimeException("Route type not supported by Hermes: " + route.getRouteType() + "\n Person:" + id + "\n Leg" + leg + "\n Leg" + route);
			}

			events.add(new PersonArrivalEvent(0, id, route.getEndLinkId(), leg.getMode()));

		} else if (element instanceof Activity) {
			processPlanActivity(id, flatplan, events, (Activity) element);
		} else {
			throw new RuntimeException("Unknown plan element " + element);
		}
	}

	private void processTeleport(Id<Person> id, PlanArray flatplan, EventArray events, Leg element, Route route, String mode) {
		double routeTravelTime = route.getTravelTime().orElse(0.0);
		double legTravelTime = element.getTravelTime().orElse(0.0);
		int time = Math.max(0, (int) Math.round(Math.max(routeTravelTime, legTravelTime)) - 2);
		//2 second is deducted as this is the maximum possible loss during interaction activities
		flatplan.add(Agent.prepareSleepForEntry(events.size() - 1, time));
		events.add(new TeleportationArrivalEvent(0, id, route.getDistance(), mode));
	}

	private void generateAgent(
			int agent_id,
			int capacity,
			PlanArray flatplan,
			EventArray events) {

		if (events.size() >= HermesConfigGroup.MAX_EVENTS_AGENT) {
			throw new RuntimeException("exceeded maximum number of agent events");
		}

		hermes_agents[agent_id] = new Agent(agent_id, capacity, flatplan, events);
	}

	private double delay_helper(double expected, OptionalTime delay_a, OptionalTime delay_b) {
		if (delay_a.isDefined()) {
			return expected + delay_a.seconds();
		} else if (delay_b.isDefined()) {
			return expected + delay_b.seconds();
		} else {
			return expected;
		}
	}

	private double arrivalOffsetHelper(Departure depart, TransitRouteStop trs) {
		return delay_helper(depart.getDepartureTime(), trs.getArrivalOffset(), trs.getDepartureOffset());
	}

	private double departureOffsetHelper(Departure depart, TransitRouteStop trs) {
		return delay_helper(depart.getDepartureTime(), trs.getDepartureOffset(), trs.getArrivalOffset());
	}

	private static class TransitRouteContext {
		final Agent agent;
		final PlanArray flatplan;
		final EventArray flatevents;
		final TransitLine tl;
		final TransitRoute tr;
		final int routeNo;
		final Departure depart;
		final List<TransitRouteStop> trs;
		final Id<Vehicle> vehId;
		final double[] averageSpeedBetweenStops;
		int stopidx = 0;
		int time;

		public TransitRouteContext(Agent agent, TransitLine tl, TransitRoute tr, int routeNo, Departure depart, Network network) {
			this.agent = agent;
			this.flatplan = agent.plan;
			this.flatevents = agent.events;
			this.tl = tl;
			this.tr = tr;
			this.routeNo = routeNo;
			this.depart = depart;
			this.trs = tr.getStops();
			this.vehId = depart.getVehicleId();
			this.averageSpeedBetweenStops = calculateSpeedsBetweenStops(network);
			int earlyDeparture = this.trs.get(0).getDepartureOffset().seconds() > 0 ? 0 : 3; // start 3 seconds early if we should depart at offset 0.0, otherwise exactly at the specified time
			this.time = Math.max(0, (int) depart.getDepartureTime() - earlyDeparture);
		}

		private double[] calculateSpeedsBetweenStops(Network network) {
			double[] speeds = new double[this.trs.size()];

			NetworkRoute route = this.tr.getRoute();
			List<Link> links = new ArrayList<>();
			links.add(network.getLinks().get(route.getStartLinkId()));
			for (Id<Link> linkId : route.getLinkIds()) {
				links.add(network.getLinks().get(linkId));
			}
			links.add(network.getLinks().get(route.getEndLinkId()));

			double distance = 0;
			int stopIdx = 0;
			TransitRouteStop nextStop = this.trs.get(stopIdx);
			Id<Link> nextStopLinkId = nextStop.getStopFacility().getLinkId();
			boolean isFirstLink = true;

			double lastDepartureOffset = 0;

			for (Link link : links) {
				if (!isFirstLink) {
					distance += link.getLength();
				}
				isFirstLink = false;

				while (nextStop != null) {
					if (link.getId().equals(nextStopLinkId)) {
						double offset = nextStop.getArrivalOffset().or(nextStop.getDepartureOffset()).orElseThrow((() -> new RuntimeException("Stop has neither arrival nor departure offset")));
						double travelTime = offset - lastDepartureOffset;
						double speed = Math.ceil(distance / travelTime);
						if (speed > HermesConfigGroup.MAX_VEHICLE_VELOCITY) {
							speed = HermesConfigGroup.MAX_VEHICLE_VELOCITY;
						} else if (speed < 1.0) {
							// make sure vehicle always is at least 1m/s fast to prevent 0 values which result in Infinite values later on
							speed = 1.0;
						}
						speeds[stopIdx] = speed;
						stopIdx++;
						distance = 0;
						lastDepartureOffset = nextStop.getDepartureOffset().or(nextStop.getArrivalOffset()).orElseThrow((() -> new RuntimeException("Stop has neither arrival nor departure offset")));

						if (stopIdx < this.trs.size()) {
							nextStop = this.trs.get(stopIdx);
							nextStopLinkId = nextStop.getStopFacility().getLinkId();
						} else {
							nextStop = null;
							nextStopLinkId = null;
							break;
						}
					} else {
						break;
					}
				}
			}
			return speeds;
		}
	}

	private void generateDeterministicVehicleOnLink(TransitRouteContext c, Id<Link> linkId, boolean generateLinkEnterEvent, boolean generateLinkLeaveEvent) {
		if (generateLinkEnterEvent) {
			if (c.time < this.deterministicPtEvents.size()) {
				this.deterministicPtEvents.get(c.time).add(new LinkEnterEvent(c.time, c.vehId, linkId));
			}
		}

		int stopsToHandle = 0;
		while (true) {
			int tmpStopIdx = c.stopidx + stopsToHandle;
			if (tmpStopIdx >= c.trs.size()) {
				break;
			}
			boolean stopOnLink = c.trs.get(tmpStopIdx).getStopFacility().getLinkId().equals(linkId);
			if (stopOnLink) {
				stopsToHandle++;
			} else {
				break;
			}
		}
		if (stopsToHandle > 0) {
			for (int i = 0; i < stopsToHandle; i++) {
				TransitRouteStop routeStop = c.trs.get(c.stopidx);
				Id<TransitStopFacility> stopId = routeStop.getStopFacility().getId();
				int stopIdIndex = stopId.index();
				double arrivalTime = arrivalOffsetHelper(c.depart, routeStop);
				double departureTime = departureOffsetHelper(c.depart, routeStop);

				if (arrivalTime == departureTime && arrivalTime >= 2) {
					arrivalTime -= 2;
				}

				c.time++;
				if (arrivalTime < c.time) {
					arrivalTime = c.time; // there should be at least 1 second between the last stop and this one
				}
				if (departureTime < arrivalTime - 2) {
					departureTime = arrivalTime + 2;
				}

				if (c.stopidx != 0 || c.time < arrivalTime) {
					c.flatplan.add(Agent.prepareSleepUntilEntry(0, (int) arrivalTime));
				}

				c.flatevents.add(new VehicleArrivesAtFacilityEvent(0, c.vehId, stopId, arrivalTime));
				c.flatplan.add(Agent.prepareStopArrivalEntry(c.flatevents.size() - 1, c.routeNo, stopIdIndex));
				c.agent.setServeStop(stopIdIndex);

				// no event associated to stop delay
				c.flatplan.add(Agent.prepareStopDelayEntry((int) departureTime, c.routeNo, stopIdIndex));

				c.flatevents.add(new VehicleDepartsAtFacilityEvent(0, c.vehId, stopId, departureTime));
				c.flatplan.add(Agent.prepareStopDepartureEntry(c.flatevents.size() - 1, c.routeNo, stopIdIndex));

				c.time = (int) departureTime;
				c.stopidx++;
			}
		}

		if (generateLinkLeaveEvent) {
			if (stopsToHandle > 0) {
				// if we handled a stop, then just assume we leave the link directly after handling the stop.
				c.time++;
			} else {
				// otherwise, calculate the travel time on the link
				if (generateLinkEnterEvent) {
					// .. but only if we actually traversed this link, i.e. if we also entered this link
					Link link = this.scenario.getNetwork().getLinks().get(linkId);
					double length = link.getLength();
					if (c.stopidx < c.averageSpeedBetweenStops.length) {
						double avgSpeed = c.averageSpeedBetweenStops[c.stopidx];
						c.time += length / avgSpeed;
					} else {
						// a link after the last stop
						c.time += length / link.getFreespeed();
					}
				} else {
					// we did not handle a stop, but also not enter the link. Most likely it's the first link of the route,
					// but without any stop on it. So just leave the link in the next second.
					c.time++;
				}
			}

			if (c.time < this.deterministicPtEvents.size()) {
				this.deterministicPtEvents.get(c.time).add(new LinkLeaveEvent(c.time, c.vehId, linkId));
			}
		} else if (stopsToHandle == 0) { // last link, which did not have any stop on it
			// make sure to adapt the time, so the driver does not exit the vehicle too early
			Link link = this.scenario.getNetwork().getLinks().get(linkId);
			c.time += link.getLength() / link.getFreespeed();
			c.flatplan.add(Agent.prepareSleepUntilEntry(0, c.time+1)); // wait finishing the agent until we've driven along all links
		}
	}

	private void generateDeterministicVehicleTrip(
			Agent agent,
			TransitLine tl,
			TransitRoute tr,
			Departure depart) {

		Vehicle v = this.scenario.getTransitVehicles().getVehicles().get(depart.getVehicleId());
		int routeNo = this.route_numbers.get(tl.getId()).get(tr.getId());

		TransitRouteContext context = new TransitRouteContext(agent, tl, tr, routeNo, depart, this.scenario.getNetwork());
		PlanArray flatplan = agent.plan;
		EventArray flatevents = agent.events;

		VehicleType vt = v.getType();
		NetworkRoute nr = tr.getRoute();

		Id<Person> driverid = Id.createPersonId("pt_" + v.getId() + "_" + vt.getId());
		String legmode = TransportMode.pt;

		// Prepare to leave
		flatevents.add(new TransitDriverStartsEvent(0, driverid, v.getId(), tl.getId(), tr.getId(), depart.getId()));
		flatevents.add(new PersonDepartureEvent(0, driverid, nr.getStartLinkId(), legmode));
		flatevents.add(new PersonEntersVehicleEvent(0, driverid, v.getId()));

		flatevents.add(new VehicleEntersTrafficEvent(context.time, driverid, nr.getStartLinkId(), v.getId(), legmode, 1));

		// Sleep until the time of departure
		//  the very first flat plan entry does not handle events, so actually add two entries, so the events are correctly handled
		flatplan.add(Agent.prepareSleepUntilEntry(0, Math.max(0, context.time)));
		flatplan.add(Agent.prepareSleepUntilEntry(flatevents.size() - 1, context.time + 1));

		// first link
		generateDeterministicVehicleOnLink(context, nr.getStartLinkId(), false, true);

		// links
		for (Id<org.matsim.api.core.v01.network.Link> link : nr.getLinkIds()) {
			generateDeterministicVehicleOnLink(context, link, true, true);
		}

		// last link
		generateDeterministicVehicleOnLink(context, nr.getEndLinkId(), true, false);

		flatevents.add(new VehicleLeavesTrafficEvent(0, driverid, nr.getEndLinkId(), v.getId(), legmode, 1));
		flatevents.add(new PersonLeavesVehicleEvent(0, driverid, v.getId()));
		flatevents.add(new PersonArrivalEvent(0, driverid, nr.getEndLinkId(), legmode));
	}

	private void generateNondeterministicVehicleOnLink(TransitRouteContext c, Id<Link> linkId, boolean generateLinkEnterEvent, boolean generateLinkLeaveEvent, int velocity, int pcuCategory) {
		if (generateLinkEnterEvent) {
			c.flatevents.add(new LinkEnterEvent(0, c.vehId, linkId));
			c.flatplan.add(Agent.prepareLinkEntry(c.flatevents.size() - 1, linkId.index(), velocity, pcuCategory));
		}

		while (true) {
			if (c.stopidx >= c.trs.size()) {
				break;
			}
			TransitRouteStop routeStop = c.trs.get(c.stopidx);
			boolean stopOnLink = routeStop.getStopFacility().getLinkId().equals(linkId);
			if (stopOnLink) {
				Id<TransitStopFacility> stopId = routeStop.getStopFacility().getId();
				int stopIdIndex = stopId.index();
				double arrivalTime = arrivalOffsetHelper(c.depart, routeStop);
				double departureTime = departureOffsetHelper(c.depart, routeStop);

				c.flatevents.add(new VehicleArrivesAtFacilityEvent(0, c.vehId, stopId, arrivalTime));
				c.flatplan.add(Agent.prepareStopArrivalEntry(c.flatevents.size() - 1, c.routeNo, stopIdIndex));
				c.agent.setServeStop(stopIdIndex);

				// no event associated to stop delay
				c.flatplan.add(Agent.prepareStopDelayEntry((int) departureTime, c.routeNo, stopIdIndex));

				c.flatevents.add(new VehicleDepartsAtFacilityEvent(0, c.vehId, stopId, departureTime));
				c.flatplan.add(Agent.prepareStopDepartureEntry(c.flatevents.size() - 1, c.routeNo, stopIdIndex));

				c.stopidx++;
			} else {
				break;
			}
		}

		if (generateLinkLeaveEvent) {
			c.flatevents.add(new LinkLeaveEvent(0, c.vehId, linkId));
		}
	}

	private void generateVehicleTrip(
			Agent agent,
			TransitLine tl,
			TransitRoute tr,
			Departure depart) {

		int routeNo = this.route_numbers.get(tl.getId()).get(tr.getId());
		TransitRouteContext context = new TransitRouteContext(agent, tl, tr, routeNo, depart, this.scenario.getNetwork());
		PlanArray flatplan = agent.plan;
		EventArray flatevents = agent.events;
		Vehicle v = this.scenario.getTransitVehicles().getVehicles().get(depart.getVehicleId());
		VehicleType vt = v.getType();
		NetworkRoute nr = tr.getRoute();
		int pcuCategory = 0;
		//the PCU category for transit vehicles is never read from plan entry but remains constant over the day
		int velocity = (int) Math.min(Math.round(v.getType().getMaximumVelocity()), HermesConfigGroup.MAX_VEHICLE_VELOCITY);

		Id<Person> driverid = Id.createPersonId("pt_" + v.getId() + "_" + vt.getId());
		String legmode = TransportMode.car;

		// Sleep until the time of departure
		flatplan.add(Agent.prepareSleepUntilEntry(0, (int) Math.round(depart.getDepartureTime())));

		// Prepare to leave
		flatevents.add(new TransitDriverStartsEvent(0, driverid, v.getId(), tl.getId(), tr.getId(), depart.getId()));
		flatevents.add(new PersonDepartureEvent(0, driverid, nr.getStartLinkId(), legmode));
		flatevents.add(new PersonEntersVehicleEvent(0, driverid, v.getId()));
		flatevents.add(new VehicleEntersTrafficEvent(0, driverid, nr.getStartLinkId(), v.getId(), legmode, 1));

		generateNondeterministicVehicleOnLink(context, nr.getStartLinkId(), false, true, velocity, pcuCategory);

		for (Id<org.matsim.api.core.v01.network.Link> link : nr.getLinkIds()) {
			generateNondeterministicVehicleOnLink(context, link, true, true, velocity, pcuCategory);
		}

		generateNondeterministicVehicleOnLink(context, nr.getEndLinkId(), true, false, velocity, pcuCategory);

		flatevents.add(new VehicleLeavesTrafficEvent(0, driverid, nr.getEndLinkId(), v.getId(), legmode, 1));
		flatevents.add(new PersonLeavesVehicleEvent(0, driverid, v.getId()));
		flatevents.add(new PersonArrivalEvent(0, driverid, nr.getEndLinkId(), legmode));
	}

	private void generateTransitVehiclePlans() {
		Map<Id<Vehicle>, Vehicle> vehicles = scenario.getTransitVehicles().getVehicles();
		scenario.getTransitSchedule().getTransitLines().values().stream().forEach((tl) -> {
			for (TransitRoute tr : tl.getRoutes().values()) {
				for (Departure depart : tr.getDepartures().values()) {
					Vehicle v = vehicles.get(depart.getVehicleId());
					int hermes_id = hermes_id(v.getId().index(), true);
					Agent agent = hermes_agents[hermes_id];
					float storageCapacityPCUE = deterministicPt ? 0.1f : (float) v.getType().getPcuEquivalents();
					float flowCapacityPCUE = deterministicPt ? 0.1f : (float) (v.getType().getPcuEquivalents() / v.getType().getFlowEfficiencyFactor());
					// for pt vehicles, storage and flow capacities are never updated
					hermes_agents[hermes_id].setStorageCapacityPCUE(storageCapacityPCUE);
					hermes_agents[hermes_id].setFlowCapacityPCUE(flowCapacityPCUE);
					if (deterministicPt) {
						generateDeterministicVehicleTrip(agent, tl, tr, depart);
					} else {
						generateVehicleTrip(agent, tl, tr, depart);
					}
				}
			}
		});

	}

	private void generatePersonPlans() {
		Population population = scenario.getPopulation();
		population.getPersons().values().parallelStream().forEach((person) -> {
			int hermes_id = hermes_id(person.getId().index(), false);
			PlanArray plan = hermes_agents[hermes_id].plan();
			EventArray events = hermes_agents[hermes_id].events();
			for (PlanElement element : person.getSelectedPlan().getPlanElements()) {
				processPlanElement(person, plan, events, element, hermes_agents[hermes_id]);
			}
		});
	}

	private void generateAgents() {
		Population population = scenario.getPopulation();
		Map<Id<Vehicle>, Vehicle> vehicles = scenario.getTransitVehicles().getVehicles();
		agent_persons = Id.getNumberOfIds(Person.class);
		int nagents = agent_persons + Id.getNumberOfIds(Vehicle.class);
		hermes_agents = new Agent[nagents];

		// Generate persons
		for (Person person : population.getPersons().values()) {
			int hermes_id = hermes_id(person.getId().index(), false);
			assert hermes_agents[hermes_id] == null;
			generateAgent(hermes_id, 0, new PlanArray(), new EventArray());
		}

		// Generate vehicles
		for (Vehicle vehicle : vehicles.values()) {
			VehicleCapacity vc = vehicle.getType().getCapacity();
			int capacity = vc.getSeats() + vc.getStandingRoom();
			int hermes_id = hermes_id(vehicle.getId().index(), true);
			assert hermes_agents[hermes_id] == null;
			generateAgent(hermes_id, capacity, new PlanArray(), new EventArray());
		}
	}

	public int matsim_id(int hermes_id, boolean is_vehicle) {
		if (is_vehicle) {
			return hermes_id - agent_persons;
		} else {
			return hermes_id;
		}
	}

	public int hermes_id(int matsim_id, boolean is_vehicle) {
		if (is_vehicle) {
			return matsim_id + agent_persons;
		} else {
			return matsim_id;
		}
	}

	private void generatePlans() {
		generatePersonPlans();
		generateTransitVehiclePlans();
	}

	public float getFlowCapacityPCE(int index) {
		return flowCapacityPCEs[index];
	}

	public float getStorageCapacityPCE(int index) {
		return storageCapacityPCEs[index];
	}

	public List<List<Event>> getDeterministicPtEvents() {
		return deterministicPtEvents;
	}

	public boolean isDeterministicPt() {
		return deterministicPt;
	}
}
