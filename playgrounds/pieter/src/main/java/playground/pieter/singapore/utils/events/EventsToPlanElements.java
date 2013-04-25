package playground.pieter.singapore.utils.events;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.PushbackReader;
import java.io.StringReader;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.Map.Entry;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.events.ActivityEndEvent;
import org.matsim.core.api.experimental.events.ActivityStartEvent;
import org.matsim.core.api.experimental.events.AgentArrivalEvent;
import org.matsim.core.api.experimental.events.AgentDepartureEvent;
import org.matsim.core.api.experimental.events.AgentStuckEvent;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.api.experimental.events.LinkEnterEvent;
import org.matsim.core.api.experimental.events.LinkLeaveEvent;
import org.matsim.core.api.experimental.events.PersonEntersVehicleEvent;
import org.matsim.core.api.experimental.events.PersonLeavesVehicleEvent;
import org.matsim.core.api.experimental.events.TransitDriverStartsEvent;
import org.matsim.core.api.experimental.events.TravelledEvent;
import org.matsim.core.api.experimental.events.VehicleArrivesAtFacilityEvent;
import org.matsim.core.api.experimental.events.handler.ActivityEndEventHandler;
import org.matsim.core.api.experimental.events.handler.ActivityStartEventHandler;
import org.matsim.core.api.experimental.events.handler.AgentArrivalEventHandler;
import org.matsim.core.api.experimental.events.handler.AgentDepartureEventHandler;
import org.matsim.core.api.experimental.events.handler.AgentStuckEventHandler;
import org.matsim.core.api.experimental.events.handler.LinkEnterEventHandler;
import org.matsim.core.api.experimental.events.handler.LinkLeaveEventHandler;
import org.matsim.core.api.experimental.facilities.ActivityFacility;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.events.handler.EventHandler;
import org.matsim.core.events.handler.PersonEntersVehicleEventHandler;
import org.matsim.core.events.handler.PersonLeavesVehicleEventHandler;
import org.matsim.core.events.handler.TransitDriverStartsEventHandler;
import org.matsim.core.events.handler.TravelledEventHandler;
import org.matsim.core.events.handler.VehicleArrivesAtFacilityEventHandler;
import org.matsim.core.facilities.ActivityFacilitiesImpl;
import org.matsim.core.facilities.ActivityFacilityImpl;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.pt.PtConstants;
import org.matsim.pt.router.TransitRouterConfig;
import org.matsim.pt.router.TransitRouterImpl;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitScheduleReader;
import org.postgresql.PGConnection;
import org.postgresql.copy.CopyManager;

import others.sergioo.util.dataBase.DataBaseAdmin;
import others.sergioo.util.dataBase.NoConnectionException;
import playground.pieter.singapore.utils.postgresql.*;
/**
 * 
 * @author sergioo
 * 
 */
class PlanElement {

	public double getDuration() {
		return endTime - startTime;
	}

	private static int id = 0; // for enumeration

	double startTime;
	double endTime = 30 * 3600;

	private int elementId;

	public PlanElement() {
		elementId = id++;
	}

	public int getElementId() {
		return elementId;
	}
}

public class EventsToPlanElements implements TransitDriverStartsEventHandler,
		PersonEntersVehicleEventHandler, PersonLeavesVehicleEventHandler,
		AgentDepartureEventHandler, AgentArrivalEventHandler,
		ActivityStartEventHandler, ActivityEndEventHandler,
		AgentStuckEventHandler, LinkEnterEventHandler, LinkLeaveEventHandler,
		TravelledEventHandler, VehicleArrivesAtFacilityEventHandler {

	// Private classes
	private class PTVehicle {

		// Attributes
		private Id transitLineId;
		private Id transitRouteId;
		boolean in = false;
		private Map<Id, Double> passengers = new HashMap<Id, Double>();
		private double distance;
		Id lastStop;

		// Constructors
		public PTVehicle(Id transitLineId, Id transitRouteId) {
			this.transitLineId = transitLineId;
			this.transitRouteId = transitRouteId;
		}

		// Methods
		public void incDistance(double linkDistance) {
			distance += linkDistance;
		}

		public void addPassenger(Id passengerId) {
			passengers.put(passengerId, distance);
		}

		public double removePassenger(Id passengerId) {
			return distance - passengers.remove(passengerId);
		}

	}

	private class Activity extends PlanElement {
		Id facility;
		Coord coord;
		String type;

		public String toString() {
			return String
					.format("ACT: type: %s start: %6.0f end: %6.0f dur: %6.0f x: %6.0f y: %6.0f facId: %s\n",
							type, startTime, endTime, getDuration(),
							coord.getX(), coord.getY(), facility.toString());
		}
	}

	private class Journey extends PlanElement {
		Activity fromAct;
		Activity toAct;
		boolean carJourney = false;
		LinkedList<Trip> trips = new LinkedList<EventsToPlanElements.Trip>();
		LinkedList<Transfer> transfers = new LinkedList<EventsToPlanElements.Transfer>();
		LinkedList<Wait> waits = new LinkedList<EventsToPlanElements.Wait>();
		LinkedList<Walk> walks = new LinkedList<EventsToPlanElements.Walk>();
		LinkedList<PlanElement> planElements = new LinkedList<PlanElement>();

		public Trip addTrip() {
			Trip trip = new Trip();
			trip.journey = this;
			trips.add(trip);
			planElements.add(trip);
			return trip;
		}

		public Wait addWait() {
			Wait wait = new Wait();
			wait.journey = this;
			waits.add(wait);
			planElements.add(wait);
			return wait;
		}

		public Walk addWalk() {
			Walk walk = new Walk();
			walk.journey = this;
			walks.add(walk);
			planElements.add(walk);
			return walk;
		}

		public void addTransfer(Transfer xfer) {
			xfer.journey = this;
			transfers.add(xfer);
			planElements.add(xfer);
		}

		Coord orig;
		Coord dest;
		Transfer possibleTransfer;
		private double carDistance;

		public String toString() {
			return String
					.format("JOURNEY: start: %6.0f end: %6.0f dur: %6.0f invehDist: %6.0f walkDist: %6.0f \n %s",
							startTime, endTime, getDuration(),
							getInVehDistance(), getWalkDistance(),
							planElements.toString());
		}

		private double getInVehDistance() {
			if (!carJourney) {
				double distance = 0;
				for (Trip t : trips) {
					distance += t.distance;
				}
				return distance;
			}
			return carDistance;
		}

		private double getWalkDistance() {
			if (!carJourney) {
				double distance = 0;
				for (Walk w : walks) {
					distance += w.distance;
				}
				return distance;
			}
			return 0;
		}

		private double getInVehTime() {
			if (!carJourney) {
				double time = 0;
				for (Trip t : trips) {
					time += t.getDuration();
				}
				return time;
			}
			return getDuration();
		}

		private double getWalkTime() {
			if (!carJourney) {
				double time = 0;
				for (Walk w : walks) {
					time += w.getDuration();
				}
				return time;
			}
			return 0;
		}

		private double getWaitTime() {
			if (!carJourney) {
				double time = 0;
				for (Wait w : waits) {
					time += w.getDuration();
				}
				return time;
			}
			return 0;
		}

		private String getMainMode() {
			if (carJourney) {
				return "car";
			}
			try {
				Trip longestTrip = trips.getFirst();
				if (trips.size() > 1) {
					for (int i = 1; i < trips.size(); i++) {
						if (trips.get(i).distance > longestTrip.distance) {
							longestTrip = trips.get(i);
						}
					}
				}
				return longestTrip.mode;

			} catch (NoSuchElementException e) {
				return "walk";

			}
		}

		public double getDistance() {

			return getInVehDistance() + getWalkDistance();
		}

		public double getAccessWalkDistance() {
			if (!carJourney) {
				return walks.getFirst().distance;
			}
			return 0;
		}

		public double getAccessWalkTime() {
			if (!carJourney) {
				return walks.getFirst().getDuration();
			}
			return 0;
		}

		public double getAccessWaitTime() {
			if (!carJourney) {
				try {
					return waits.getFirst().getDuration();

				} catch (NoSuchElementException e) {

				}
			}
			return 0;
		}

		public double getEgressWalkDistance() {
			if (!carJourney) {
				for (Walk w : walks) {
					if (w.egressWalk)
						return w.distance;
				}
			}
			return 0;
		}

		public double getEgressWalkTime() {
			if (!carJourney) {
				for (Walk w : walks) {
					if (w.egressWalk)
						return w.getDuration();
				}
			}

			return 0;
		}
	}

	private class Trip extends PlanElement {
		Journey journey;
		String mode;
		Id line;
		Id route;
		Coord orig;
		Coord dest;
		Id boardingStop;
		Id alightingStop;
		double distance;

		public String toString() {
			return String
					.format("\tTRIP: mode: %s start: %6.0f end: %6.0f distance: %6.0f \n",
							mode, startTime, endTime, distance);
		}
	}

	private class Wait extends PlanElement {
		public Journey journey;
		Coord coord;
		boolean accessWait = false;

		public String toString() {
			return String.format(
					"\tWAIT: start: %6.0f end: %6.0f dur: %6.0f \n", startTime,
					endTime, endTime - startTime);
		}
	}

	private class Walk extends PlanElement {
		public Journey journey;
		Coord orig;
		Coord dest;
		double distance;
		boolean accessWalk = false;
		boolean egressWalk = false;

		public String toString() {
			return String.format(
					"\tWALK: start: %6.0f end: %6.0f distance: %6.0f \n",
					startTime, endTime, distance);
		}
	}

	private class Transfer extends PlanElement {
		public Journey journey;
		Trip fromTrip;
		Trip toTrip;
		LinkedList<Wait> waits = new LinkedList<EventsToPlanElements.Wait>();
		LinkedList<Walk> walks = new LinkedList<EventsToPlanElements.Walk>();

		public String toString() {
			return String.format(
					"TRANSFER start: %f end: %f walkTime: %f waitTime: %f \n",
					startTime, endTime, getWalkTime(), getWaitTime());
		}

		private double getWaitTime() {
			try {
				double waitTime = 0;
				for (Wait w : waits) {
					waitTime += w.getDuration();
				}
				return waitTime;
			} catch (NullPointerException e) {

			}
			return 0;
		}

		private double getWalkTime() {
			try {
				double walkTime = 0;
				for (Walk w : walks) {
					walkTime += w.getDuration();
				}
				return walkTime;
			} catch (NullPointerException e) {

			}
			return 0;
		}

		private double getWalkDistance() {
			try {
				double walkDist = 0;
				for (Walk w : walks) {
					walkDist += w.distance;
				}
				return walkDist;
			} catch (NullPointerException e) {

			}
			return 0;
		}
	}

	private class TravellerChain {
		// use linked lists so I can use the getlast method
		LinkedList<Activity> acts = new LinkedList<EventsToPlanElements.Activity>();
		LinkedList<Journey> journeys = new LinkedList<EventsToPlanElements.Journey>();
		LinkedList<PlanElement> planElements = new LinkedList<PlanElement>();

		public Journey addJourney() {
			Journey journey = new Journey();
			journeys.add(journey);
			planElements.add(journey);
			return journey;
		}

		public Activity addActivity() {
			Activity activity = new Activity();
			acts.add(activity);
			planElements.add(activity);
			return activity;
		}

		boolean inPT = false;
		public boolean inCar;
		public boolean traveledVehicle;
		public boolean traveling;

	}

	// Attributes
	private Map<Id, TravellerChain> chains = new HashMap<Id, EventsToPlanElements.TravellerChain>();
	private Map<Id, PTVehicle> ptVehicles = new HashMap<Id, EventsToPlanElements.PTVehicle>();
	private TransitSchedule transitSchedule;
	private Map<Id, Coord> locations = new HashMap<Id, Coord>();
	private Network network;
	private Map<Id, Integer> acts = new HashMap<Id, Integer>();
	private int stuck = 0;
	private final double walkSpeed;

	public EventsToPlanElements(TransitSchedule transitSchedule,
			Network network, Config config) {
		this.transitSchedule = transitSchedule;
		this.network = network;
		this.walkSpeed = new TransitRouterConfig(config).getBeelineWalkSpeed();
	}

	// Methods
	@Override
	public void reset(int iteration) {

	}

	private String getMode(String transportMode, Id line) {
		if (transportMode.contains("bus"))
			return "bus";
		else if (transportMode.contains("rail"))
			return "lrt";
		else if (transportMode.contains("subway"))
			if (line.toString().contains("PE")
					|| line.toString().contains("SE")
					|| line.toString().contains("SW"))
				return "lrt";
			else
				return "mrt";
		else
			return "other";
	}

	@Override
	public void handleEvent(ActivityEndEvent event) {
		try {
			if (event.getPersonId().toString().startsWith("pt_tr"))
				return;
			TravellerChain chain = chains.get(event.getPersonId());
			locations.put(event.getPersonId(),
					network.getLinks().get(event.getLinkId()).getCoord());
			if (chain == null) {
				chain = new TravellerChain();
				chains.put(event.getPersonId(), chain);
				Activity act = chain.addActivity();
				act.coord = network.getLinks().get(event.getLinkId())
						.getCoord();
				act.endTime = event.getTime();
				act.facility = event.getFacilityId();
				act.startTime = 0.0;
				act.type = event.getActType();

			} else if (!event.getActType().equals(
					PtConstants.TRANSIT_ACTIVITY_TYPE)) {
				Activity act = chain.acts.getLast();
				act.endTime = event.getTime();
			}
		} catch (Exception e) {
			String fullStackTrace = org.apache.commons.lang.exception.ExceptionUtils.getFullStackTrace(e);
			System.err.println(fullStackTrace);
			System.err.println(event.toString());
		}
	}

	@Override
	public void handleEvent(ActivityStartEvent event) {
		try {
			if (event.getPersonId().toString().startsWith("pt_tr"))
				return;
			TravellerChain chain = chains.get(event.getPersonId());
			boolean beforeInPT = chain.inPT;
			if (event.getActType().equals(PtConstants.TRANSIT_ACTIVITY_TYPE)) {
				chain.inPT = true;

			} else {
				chain.inPT = false;
				chain.traveling = false;
				Activity act = chain.addActivity();
				act.coord = network.getLinks().get(event.getLinkId()).getCoord();
				act.facility = event.getFacilityId();
				act.startTime = event.getTime();
				act.type = event.getActType();
				// end the preceding journey
				Journey journey = chain.journeys.getLast();
				journey.dest = act.coord;
				journey.endTime = event.getTime();
				journey.toAct = act;
				if (beforeInPT)
					journey.walks.getLast().egressWalk = true;
			}
		} catch (Exception e) {
			String fullStackTrace = org.apache.commons.lang.exception.ExceptionUtils.getFullStackTrace(e);
			System.err.println(fullStackTrace);
			System.err.println(event.toString());
		}
	}

	@Override
	public void handleEvent(AgentDepartureEvent event) {
		try {
			if (event.getPersonId().toString().startsWith("pt_tr"))
				return;
			TravellerChain chain = chains.get(event.getPersonId());
			Journey journey;
			if (event.getLegMode().equals("transit_walk")) {
				if (!chain.traveling) {
					chain.traveling = true;
					journey = chain.addJourney();
					journey.orig = network.getLinks().get(event.getLinkId())
							.getCoord();
					journey.fromAct = chain.acts.getLast();
					journey.startTime = event.getTime();
					Walk walk = journey.addWalk();
					walk.accessWalk = true;
					walk.startTime = event.getTime();
					walk.orig = journey.orig;
				} else {
					journey = chain.journeys.getLast();
					Walk walk = journey.addWalk();
					walk.startTime = event.getTime();
					walk.orig = network.getLinks().get(event.getLinkId())
							.getCoord();
					journey.possibleTransfer.walks.add(walk);
				}
			} else if (event.getLegMode().equals("car")) {
				chain.inCar = true;
				journey = chain.addJourney();
				journey.carJourney = true;
				journey.orig = network.getLinks().get(event.getLinkId()).getCoord();
				journey.fromAct = chain.acts.getLast();
				journey.startTime = event.getTime();
			} else if (event.getLegMode().equals("pt")) {
				// person waits till they enter the vehicle
				journey = chain.journeys.getLast();
				Wait wait = journey.addWait();
				if (journey.waits.size() == 1)
					wait.accessWait = true;
				wait.startTime = event.getTime();
				wait.coord = network.getLinks().get(event.getLinkId()).getCoord();
				if (!wait.accessWait) {
					journey.possibleTransfer.waits.add(wait);
				}
			}
		} catch (Exception e) {
			String fullStackTrace = org.apache.commons.lang.exception.ExceptionUtils.getFullStackTrace(e);
			System.err.println(fullStackTrace);
			System.err.println(event.toString());
		}
	}

	@Override
	public void handleEvent(AgentArrivalEvent event) {
		try {
			if (event.getPersonId().toString().startsWith("pt_tr"))
				return;
			TravellerChain chain = chains.get(event.getPersonId());
			if (event.getLegMode().equals("car")) {
				Journey journey = chain.journeys.getLast();
				journey.dest = network.getLinks().get(event.getLinkId()).getCoord();
				journey.endTime = event.getTime();
				chain.inCar = false;
			} else if (event.getLegMode().equals("transit_walk")) {
				Journey journey = chain.journeys.getLast();
				Walk walk = journey.walks.getLast();
				walk.dest = network.getLinks().get(event.getLinkId()).getCoord();
				walk.endTime = event.getTime();
				walk.distance = walk.getDuration() * walkSpeed;
			} else if (event.getLegMode().equals("pt")) {
				Journey journey = chain.journeys.getLast();
				Trip trip = journey.trips.getLast();
				trip.dest = network.getLinks().get(event.getLinkId()).getCoord();
				trip.endTime = event.getTime();
				journey.possibleTransfer = new Transfer();
				journey.possibleTransfer.startTime = event.getTime();
				journey.possibleTransfer.fromTrip = trip;
			}
		} catch (Exception e) {
			String fullStackTrace = org.apache.commons.lang.exception.ExceptionUtils.getFullStackTrace(e);
			System.err.println(fullStackTrace);
			System.err.println(event.toString());
		}
	}

	@Override
	public void handleEvent(PersonEntersVehicleEvent event) {
		try {
			if (event.getPersonId().toString().startsWith("pt_tr"))
				return;
			if (event.getVehicleId().toString().startsWith("tr")) {
				TravellerChain chain = chains.get(event.getPersonId());
				Journey journey = chain.journeys.getLast();
				// first, handle the end of the wait
				journey.waits.getLast().endTime = event.getTime();
				// now, create a new trip
				ptVehicles.get(event.getVehicleId()).addPassenger(
						event.getPersonId());
				Trip trip = journey.addTrip();
				PTVehicle vehicle = ptVehicles.get(event.getVehicleId());
				trip.line = vehicle.transitLineId;
				trip.mode = getMode(
						transitSchedule.getTransitLines()
								.get(vehicle.transitLineId).getRoutes()
								.get(vehicle.transitRouteId).getTransportMode(),
						vehicle.transitLineId);
				trip.boardingStop = vehicle.lastStop;
				trip.orig = journey.waits.getLast().coord;
				trip.route = ptVehicles.get(event.getVehicleId()).transitRouteId;
				trip.startTime = event.getTime();
				// check for the end of a transfer
				if (journey.possibleTransfer != null) {
					journey.possibleTransfer.toTrip = trip;
					journey.possibleTransfer.endTime = event.getTime();
					journey.addTransfer(journey.possibleTransfer);
					journey.possibleTransfer = null;
				}
			}
		} catch (Exception e) {
			String fullStackTrace = org.apache.commons.lang.exception.ExceptionUtils.getFullStackTrace(e);
			System.err.println(fullStackTrace);
			System.err.println(event.toString());
		}
	}

	@Override
	public void handleEvent(PersonLeavesVehicleEvent event) {
		try {
			if (!event.getPersonId().toString().startsWith("pt_tr")) {
				if (event.getVehicleId().toString().startsWith("tr")) {
					TravellerChain chain = chains.get(event.getPersonId());
					chain.traveledVehicle = true;
					PTVehicle vehicle = ptVehicles.get(event.getVehicleId());
					double stageDistance = vehicle.removePassenger(event
							.getPersonId());
					Trip trip = chain.journeys.getLast().trips.getLast();
					trip.distance = stageDistance;
					trip.alightingStop = vehicle.lastStop;
				}
			}
		} catch (Exception e) {
			String fullStackTrace = org.apache.commons.lang.exception.ExceptionUtils.getFullStackTrace(e);
			System.err.println(fullStackTrace);
			System.err.println(event.toString());
		}
	}

	@Override
	public void handleEvent(LinkEnterEvent event) {
		try {
			if (event.getVehicleId().toString().startsWith("tr"))
				ptVehicles.get(event.getVehicleId()).in = true;
		} catch (Exception e) {
			String fullStackTrace = org.apache.commons.lang.exception.ExceptionUtils.getFullStackTrace(e);
			System.err.println(fullStackTrace);
			System.err.println(event.toString());
		}

	}

	@Override
	public void handleEvent(LinkLeaveEvent event) {
		try {
			if (event.getVehicleId().toString().startsWith("tr")) {
				PTVehicle vehicle = ptVehicles.get(event.getVehicleId());
				if (vehicle.in)
					vehicle.in = false;
				vehicle.incDistance(network.getLinks().get(event.getLinkId())
						.getLength());
			} else {
				TravellerChain chain = chains.get(event.getPersonId());
				if (chain.inCar) {
					chain.journeys.getLast().carDistance += network.getLinks()
							.get(event.getLinkId()).getLength();
				}
			}
		} catch (Exception e) {
			String fullStackTrace = org.apache.commons.lang.exception.ExceptionUtils.getFullStackTrace(e);
			System.err.println(fullStackTrace);
			System.err.println(event.toString());
		}
	}

	@Override
	public void handleEvent(AgentStuckEvent event) {
		try {
			if (!event.getPersonId().toString().startsWith("pt_tr")) {
				TravellerChain chain = chains.get(event.getPersonId());
				stuck++;
				if (chain.journeys.size() > 0)
					chain.journeys.removeLast();
			}
		} catch (Exception e) {
			String fullStackTrace = org.apache.commons.lang.exception.ExceptionUtils.getFullStackTrace(e);
			System.err.println(fullStackTrace);
			System.err.println(event.toString());
		}
	}

	@Override
	public void handleEvent(TravelledEvent event) {
		try {
			if (event.getPersonId().toString().startsWith("pt_tr"))
				return;
			TravellerChain chain = chains.get(event.getPersonId());
			if (chain.traveledVehicle)
				chain.traveledVehicle = false;
		} catch (Exception e) {
			String fullStackTrace = org.apache.commons.lang.exception.ExceptionUtils.getFullStackTrace(e);
			System.err.println(fullStackTrace);
			System.err.println(event.toString());
		}
	}

	@Override
	public void handleEvent(TransitDriverStartsEvent event) {
		try {
			ptVehicles.put(
					event.getVehicleId(),
					new PTVehicle(event.getTransitLineId(), event
							.getTransitRouteId()));
		} catch (Exception e) {
			String fullStackTrace = org.apache.commons.lang.exception.ExceptionUtils.getFullStackTrace(e);
			System.err.println(fullStackTrace);
			System.err.println(event.toString());
		}
	}

	@Override
	public void handleEvent(VehicleArrivesAtFacilityEvent event) {
		try {
			ptVehicles.get(event.getVehicleId()).lastStop = event.getFacilityId();
		} catch (Exception e) {
			String fullStackTrace = org.apache.commons.lang.exception.ExceptionUtils.getFullStackTrace(e);
			System.err.println(fullStackTrace);
			System.err.println(event.toString());
		}
	}

	public void writeSimulationResultsToSQL(File connectionProperties)
			throws InstantiationException, IllegalAccessException,
			ClassNotFoundException, IOException, SQLException,
			NoConnectionException {
		DateFormat df = new SimpleDateFormat("yyyy_MM_dd");
		String formattedDate = df.format(new Date());
		// start with activities
		String actTableName = "m_calibration.matsim_activities_"
				+ formattedDate;
		List<PostgresqlColumnDefinition> columns = new ArrayList<PostgresqlColumnDefinition>();
		columns.add(new PostgresqlColumnDefinition("activity_id",
				PostgresType.INT, "primary key"));
		columns.add(new PostgresqlColumnDefinition("person_id",
				PostgresType.TEXT));
		columns.add(new PostgresqlColumnDefinition("facility_id",
				PostgresType.TEXT));
		columns.add(new PostgresqlColumnDefinition("type", PostgresType.TEXT));
		columns.add(new PostgresqlColumnDefinition("start_time",
				PostgresType.INT));
		columns.add(new PostgresqlColumnDefinition("end_time", PostgresType.INT));
		DataBaseAdmin actDBA = new DataBaseAdmin(connectionProperties);
		PostgresqlCSVWriter activityWriter = new PostgresqlCSVWriter("ACTS",
				actTableName, actDBA, 10000, columns);

		String journeyTableName = "m_calibration.matsim_journeys_"
				+ formattedDate;
		columns = new ArrayList<PostgresqlColumnDefinition>();
		columns.add(new PostgresqlColumnDefinition("journey_id",
				PostgresType.INT, "primary key"));
		columns.add(new PostgresqlColumnDefinition("person_id",
				PostgresType.TEXT));
		columns.add(new PostgresqlColumnDefinition("start_time",
				PostgresType.INT));
		columns.add(new PostgresqlColumnDefinition("end_time", PostgresType.INT));
		columns.add(new PostgresqlColumnDefinition("distance",
				PostgresType.FLOAT8));
		columns.add(new PostgresqlColumnDefinition("main_mode",
				PostgresType.TEXT));
		columns.add(new PostgresqlColumnDefinition("from_act", PostgresType.INT));
		columns.add(new PostgresqlColumnDefinition("to_act", PostgresType.INT));
		columns.add(new PostgresqlColumnDefinition("in_vehicle_distance",
				PostgresType.FLOAT8));
		columns.add(new PostgresqlColumnDefinition("in_vehicle_time",
				PostgresType.INT));
		columns.add(new PostgresqlColumnDefinition("access_walk_distance",
				PostgresType.FLOAT8));
		columns.add(new PostgresqlColumnDefinition("access_walk_time",
				PostgresType.INT));
		columns.add(new PostgresqlColumnDefinition("access_wait_time",
				PostgresType.INT));
		columns.add(new PostgresqlColumnDefinition("egress_walk_distance",
				PostgresType.FLOAT8));
		columns.add(new PostgresqlColumnDefinition("egress_walk_time",
				PostgresType.INT));
		DataBaseAdmin journeyDBA = new DataBaseAdmin(connectionProperties);
		PostgresqlCSVWriter journeyWriter = new PostgresqlCSVWriter("JOURNEYS",
				journeyTableName, journeyDBA, 5000, columns);

		String tripTableName = "m_calibration.matsim_trips_" + formattedDate;
		columns = new ArrayList<PostgresqlColumnDefinition>();
		columns.add(new PostgresqlColumnDefinition("trip_id", PostgresType.INT,
				"primary key"));
		columns.add(new PostgresqlColumnDefinition("journey_id",
				PostgresType.INT));
		columns.add(new PostgresqlColumnDefinition("start_time",
				PostgresType.INT));
		columns.add(new PostgresqlColumnDefinition("end_time", PostgresType.INT));
		columns.add(new PostgresqlColumnDefinition("distance",
				PostgresType.FLOAT8));
		columns.add(new PostgresqlColumnDefinition("mode", PostgresType.TEXT));
		columns.add(new PostgresqlColumnDefinition("line", PostgresType.TEXT));
		columns.add(new PostgresqlColumnDefinition("route", PostgresType.TEXT));
		columns.add(new PostgresqlColumnDefinition("boarding_stop",
				PostgresType.TEXT));
		columns.add(new PostgresqlColumnDefinition("alighting_stop",
				PostgresType.TEXT));
		DataBaseAdmin tripDBA = new DataBaseAdmin(connectionProperties);
		PostgresqlCSVWriter tripWriter = new PostgresqlCSVWriter("TRIPS",
				tripTableName, tripDBA, 10000, columns);

		String transferTableName = "m_calibration.matsim_transfers_"
				+ formattedDate;
		columns = new ArrayList<PostgresqlColumnDefinition>();
		columns.add(new PostgresqlColumnDefinition("transfer_id",
				PostgresType.INT, "primary key"));
		columns.add(new PostgresqlColumnDefinition("journey_id",
				PostgresType.INT));
		columns.add(new PostgresqlColumnDefinition("start_time",
				PostgresType.INT));
		columns.add(new PostgresqlColumnDefinition("end_time", PostgresType.INT));
		columns.add(new PostgresqlColumnDefinition("from_trip",
				PostgresType.INT));
		columns.add(new PostgresqlColumnDefinition("to_trip", PostgresType.INT));
		columns.add(new PostgresqlColumnDefinition("walk_distance",
				PostgresType.FLOAT8));
		columns.add(new PostgresqlColumnDefinition("walk_time",
				PostgresType.INT));
		columns.add(new PostgresqlColumnDefinition("wait_time",
				PostgresType.INT));
		DataBaseAdmin transferDBA = new DataBaseAdmin(connectionProperties);
		PostgresqlCSVWriter transferWriter = new PostgresqlCSVWriter(
				"TRANSFERS", transferTableName, transferDBA, 1000, columns);

		for (Entry<Id, TravellerChain> entry : chains.entrySet()) {
			String pax_id = entry.getKey().toString();
			TravellerChain chain = entry.getValue();
			for (Activity act : chain.acts) {
				Object[] args = { new Integer(act.getElementId()), pax_id,
						act.facility, act.type,
						new Integer((int) act.startTime),
						new Integer((int) act.endTime) };
				activityWriter.addLine(args);
			}
			for (Journey journey : chain.journeys) {
				try {

					Object[] journeyArgs = {
							new Integer(journey.getElementId()), pax_id,
							new Integer((int) journey.startTime),
							new Integer((int) journey.endTime),
							new Double(journey.getDistance()),
							journey.getMainMode(),
							new Integer(journey.fromAct.getElementId()),
							new Integer(journey.toAct.getElementId()),
							new Double(journey.getInVehDistance()),
							new Integer((int) journey.getInVehTime()),
							new Double(journey.getAccessWalkDistance()),
							new Integer((int) journey.getAccessWalkTime()),
							new Integer((int) journey.getAccessWaitTime()),
							new Double(journey.getEgressWalkDistance()),
							new Integer((int) journey.getEgressWalkTime())

					};
					journeyWriter.addLine(journeyArgs);
					if (!journey.carJourney) {
						for (Trip trip : journey.trips) {
							Object[] tripArgs = {
									new Integer(trip.getElementId()),
									new Integer(journey.getElementId()),
									new Integer((int) trip.startTime),
									new Integer((int) trip.endTime),
									new Double(trip.distance), trip.mode,
									trip.line, trip.route, trip.boardingStop,
									trip.alightingStop };
							tripWriter.addLine(tripArgs);
						}
						for (Transfer transfer : journey.transfers) {
							Object[] transferArgs = {
									new Integer(transfer.getElementId()),
									new Integer(journey.getElementId()),
									new Integer((int) transfer.startTime),
									new Integer((int) transfer.endTime),
									new Integer(
											transfer.fromTrip.getElementId()),
									new Integer(transfer.toTrip.getElementId()),
									new Double(transfer.getWalkDistance()),
									new Integer((int) transfer.getWalkTime()),
									new Integer((int) transfer.getWaitTime()) };
							transferWriter.addLine(transferArgs);
						}
					}
				} catch (NullPointerException e) {
					stuck++;
				}
			}

		}
		activityWriter.finish();
		journeyWriter.finish();
		tripWriter.finish();
		transferWriter.finish();
		String indexName = actTableName.substring(14);
		String indexStatement = "CREATE INDEX " + indexName + "_paxidx ON "
				+ actTableName + "(person_id);\n";
		indexStatement += "CREATE INDEX " + indexName + "_facidx ON "
				+ actTableName + "(facility_id);\n";
		indexStatement += "CREATE INDEX " + indexName + "_type ON "
				+ actTableName + "(type);\n";
		DataBaseAdmin dba = new DataBaseAdmin(connectionProperties);
		dba.executeStatement(indexStatement);
		System.out.println(indexStatement);

		indexName = journeyTableName.substring(14);
		indexStatement = "CREATE INDEX " + indexName + "_paxidx ON "
				+ journeyTableName + "(person_id);\n";
		indexStatement += "CREATE INDEX " + indexName + "_from_act ON "
				+ journeyTableName + "(from_act);\n";
		indexStatement += "CREATE INDEX " + indexName + "_to_act ON "
				+ journeyTableName + "(to_act);\n";
		indexStatement += "CREATE INDEX " + indexName + "_mainmode ON "
				+ journeyTableName + "(main_mode);\n";
		dba.executeStatement(indexStatement);
		System.out.println(indexStatement);

		indexName = tripTableName.substring(14);
		indexStatement = "CREATE INDEX " + indexName + "_journey_id ON "
				+ tripTableName + "(journey_id);\n";
		indexStatement += "CREATE INDEX " + indexName + "_mode ON "
				+ tripTableName + "(mode);\n";
		indexStatement += "CREATE INDEX " + indexName + "_line ON "
				+ tripTableName + "(line);\n";
		indexStatement += "CREATE INDEX " + indexName + "_route ON "
				+ tripTableName + "(route);\n";
		indexStatement += "CREATE INDEX " + indexName + "_board ON "
				+ tripTableName + "(boarding_stop);\n";
		indexStatement += "CREATE INDEX " + indexName + "_alight ON "
				+ tripTableName + "(alighting_stop);\n";
		dba.executeStatement(indexStatement);
		System.out.println(indexStatement);

		indexName = transferTableName.substring(14);
		indexStatement = "CREATE INDEX " + indexName + "_journey_id ON "
				+ transferTableName + "(journey_id);\n";
		indexStatement += "CREATE INDEX " + indexName + "_from_trip ON "
				+ transferTableName + "(from_trip);\n";
		indexStatement += "CREATE INDEX " + indexName + "_to_trip ON "
				+ transferTableName + "(to_trip);\n";
		dba.executeStatement(indexStatement);
		System.out.println(indexStatement);

	}

	public static void main(String[] args) throws IOException,
			InstantiationException, IllegalAccessException,
			ClassNotFoundException, SQLException, NoConnectionException {
		ScenarioImpl scenario = (ScenarioImpl) ScenarioUtils
				.createScenario(ConfigUtils.loadConfig(args[3]));
		scenario.getConfig().scenario().setUseTransit(true);
		new TransitScheduleReader(scenario).readFile(args[0]);
		new MatsimNetworkReader(scenario).readFile(args[1]);

		EventsManager eventsManager = EventsUtils.createEventsManager();
		EventsToPlanElements test = new EventsToPlanElements(
				scenario.getTransitSchedule(), scenario.getNetwork(),
				scenario.getConfig());
		eventsManager.addHandler(test);
		new MatsimEventsReader(eventsManager).readFile(args[2]);
		// TravellerChain chain = test.chains
		// .get(new org.matsim.core.basic.v01.IdImpl("4101962"));
		// System.out.println(chain.planElements);
		//
		// chain = test.chains
		// .get(new org.matsim.core.basic.v01.IdImpl("77878"));
		// System.out.println(chain.planElements);
		File properties = new File("data/matsim2postgres.properties");
		test.writeSimulationResultsToSQL(properties);
		System.out.println(test.stuck);
	}

}

