package playground.pieter.travelsummary;

import java.io.File;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.bcel.generic.ISUB;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.PersonLeavesVehicleEvent;
import org.matsim.api.core.v01.events.PersonStuckEvent;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.TransitDriverStartsEvent;
import org.matsim.api.core.v01.events.handler.ActivityEndEventHandler;
import org.matsim.api.core.v01.events.handler.ActivityStartEventHandler;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.events.handler.PersonEntersVehicleEventHandler;
import org.matsim.api.core.v01.events.handler.PersonLeavesVehicleEventHandler;
import org.matsim.api.core.v01.events.handler.PersonStuckEventHandler;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.events.handler.TransitDriverStartsEventHandler;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.api.experimental.events.TeleportationArrivalEvent;
import org.matsim.core.api.experimental.events.VehicleArrivesAtFacilityEvent;
import org.matsim.core.api.experimental.events.handler.TeleportationArrivalEventHandler;
import org.matsim.core.api.experimental.events.handler.VehicleArrivesAtFacilityEventHandler;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.PtConstants;
import org.matsim.pt.router.TransitRouterConfig;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitScheduleReader;
import org.matsim.vehicles.VehicleReaderV1;
import org.matsim.vehicles.VehicleUtils;
import org.matsim.vehicles.Vehicles;
import org.matsim.vehicles.VehiclesFactory;

import others.sergioo.util.dataBase.DataBaseAdmin;
import others.sergioo.util.dataBase.NoConnectionException;
import playground.pieter.singapore.utils.postgresql.*;
import playground.pieter.travelsummary.travelcomponents.*;

/**
 * 
 * @author sergioo, pieterfourie
 *         <P>
 *         Converts events into journeys, trips/stages, transfers and activities
 *         tables. Originally designed for transit scenarios with full transit
 *         simulation, it needs some work for general scenarios with teleported
 *         modes.
 */

public class EventsToTravelSummaryTables implements TransitDriverStartsEventHandler,
		PersonEntersVehicleEventHandler, PersonLeavesVehicleEventHandler,
		PersonDepartureEventHandler, PersonArrivalEventHandler,
		ActivityStartEventHandler, ActivityEndEventHandler,
		PersonStuckEventHandler, LinkEnterEventHandler, LinkLeaveEventHandler,
		TeleportationArrivalEventHandler, VehicleArrivesAtFacilityEventHandler {

	// Private classes
	private class PTVehicle {

		// Attributes
		private Id transitLineId;
		private Id transitRouteId;
		boolean in = false;
		private Map<Id, Double> passengers = new HashMap<Id, Double>();
		private double distance;
		Id lastStop;
		private double linkEnterTime = 0.0;

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

		public double getLinkEnterTime() {
			return linkEnterTime;
		}

		public void setLinkEnterTime(double linkEnterTime) {
			this.linkEnterTime = linkEnterTime;
		}

	}

	// Attributes
	private Map<Id, TravellerChain> chains = new HashMap<Id, TravellerChain>();
	private Map<Id, Coord> locations = new HashMap<Id, Coord>();
	private String eventsFileName;
	private Network network;
	private Map<Id, PTVehicle> ptVehicles = new HashMap<Id, PTVehicle>();
	private int stuck = 0;
	private TransitSchedule transitSchedule;
	private final double walkSpeed;
	private String schemaName;
	private HashSet<Id> transitDriverIds = new HashSet<Id>();
	private boolean isTransitScenario = false;
	private HashMap<Id,Id> driverIdFromVehicleId = new HashMap<Id, Id>();

	public EventsToTravelSummaryTables(TransitSchedule transitSchedule,
			Network network, Config config, String suffix, String schemaName) {
		this.transitSchedule = transitSchedule;
		this.isTransitScenario = true;
		this.network = network;
		this.walkSpeed = new TransitRouterConfig(config).getBeelineWalkSpeed();
		this.schemaName = schemaName;
	}
	
	public EventsToTravelSummaryTables(
			Network network, Config config, String suffix, String schemaName) {
		this.network = network;
		this.walkSpeed = new TransitRouterConfig(config).getBeelineWalkSpeed();
		this.schemaName = schemaName;
	}

	@Override
	public void handleEvent(ActivityEndEvent event) {
		try {
			if(isTransitScenario){
				if (transitDriverIds.contains(event.getPersonId()))
					return;				
			}
			TravellerChain chain = chains.get(event.getPersonId());
			locations.put(event.getPersonId(),
					network.getLinks().get(event.getLinkId()).getCoord());
			if (chain == null) {
				chain = new TravellerChain();
				chains.put(event.getPersonId(), chain);
				Activity act = chain.addActivity();
				act.setCoord(network.getLinks().get(event.getLinkId())
						.getCoord());
				act.setEndTime(event.getTime());
				act.setFacility(event.getFacilityId());
				act.setStartTime(0.0);
				act.setType(event.getActType());

			} else if (!event.getActType().equals(
					PtConstants.TRANSIT_ACTIVITY_TYPE)) {
				Activity act = chain.getActs().getLast();
				act.setEndTime(event.getTime());
			}
		} catch (Exception e) {
			String fullStackTrace = org.apache.commons.lang.exception.ExceptionUtils
					.getFullStackTrace(e);
			System.err.println(fullStackTrace);
			System.err.println(event.toString());
		}
	}

	@Override
	public void handleEvent(ActivityStartEvent event) {
		try {
			if(isTransitScenario){
				if (transitDriverIds.contains(event.getPersonId()))
					return;				
			}
			TravellerChain chain = chains.get(event.getPersonId());
			boolean beforeInPT = chain.isInPT();
			if (event.getActType().equals(PtConstants.TRANSIT_ACTIVITY_TYPE)) {
				chain.setInPT(true);

			} else {
				chain.setInPT(false);
				chain.traveling = false;
				Activity act = chain.addActivity();
				act.setCoord(network.getLinks().get(event.getLinkId())
						.getCoord());
				act.setFacility(event.getFacilityId());
				act.setStartTime(event.getTime());
				act.setType(event.getActType());
				// end the preceding journey
				Journey journey = chain.getJourneys().getLast();
				journey.setDest(act.getCoord());
				journey.setEndTime(event.getTime());
				journey.setToAct(act);
				if (beforeInPT)
					journey.getWalks().getLast().setEgressWalk(true);
			}
		} catch (Exception e) {
			String fullStackTrace = org.apache.commons.lang.exception.ExceptionUtils
					.getFullStackTrace(e);
			System.err.println(fullStackTrace);
			System.err.println(event.toString());
		}
	}

	@Override
	public void handleEvent(PersonArrivalEvent event) {
		try {
			if(isTransitScenario){
				if (transitDriverIds.contains(event.getPersonId()))
					return;				
			}
			TravellerChain chain = chains.get(event.getPersonId());
			if (event.getLegMode().equals("car")) {
				Journey journey = chain.getJourneys().getLast();
				journey.setDest(network.getLinks().get(event.getLinkId())
						.getCoord());
				journey.setEndTime(event.getTime());
				Trip trip = journey.getTrips().getLast();
				trip.setDistance(journey.getDistance());
				trip.setEndTime(event.getTime());
				chain.inCar = false;
			} else if (event.getLegMode().equals("transit_walk")) {
				Journey journey = chain.getJourneys().getLast();
				Walk walk = journey.getWalks().getLast();
				walk.setDest(network.getLinks().get(event.getLinkId())
						.getCoord());
				walk.setEndTime(event.getTime());
				walk.setDistance(walk.getDuration() * walkSpeed);
			} else if (event.getLegMode().equals("pt")) {
				if(isTransitScenario){
					Journey journey = chain.getJourneys().getLast();
					Trip trip = journey.getTrips().getLast();
					trip.setDest(network.getLinks().get(event.getLinkId())
							.getCoord());
					trip.setEndTime(event.getTime());
					journey.setPossibleTransfer(new Transfer());
					journey.getPossibleTransfer().setStartTime(event.getTime());
					journey.getPossibleTransfer().setFromTrip(trip);
				}else{
					Journey journey = chain.getJourneys().getLast();
					journey.setEndTime(event.getTime());
					journey.setDest(network.getLinks().get(event.getLinkId())
							.getCoord());
					journey.setEndTime(event.getTime());
				}
			}
		} catch (Exception e) {
			String fullStackTrace = org.apache.commons.lang.exception.ExceptionUtils
					.getFullStackTrace(e);
			System.err.println(fullStackTrace);
			System.err.println(event.toString());
		}
	}

	@Override
	public void handleEvent(PersonDepartureEvent event) {
		try {
			if (transitDriverIds.contains(event.getPersonId()))
				return;
			TravellerChain chain = chains.get(event.getPersonId());
			Journey journey;
			if (event.getLegMode().equals("transit_walk")) {
				if (!chain.traveling) {
					chain.traveling = true;
					journey = chain.addJourney();
					journey.setOrig(network.getLinks().get(event.getLinkId())
							.getCoord());
					journey.setFromAct(chain.getActs().getLast());
					journey.setStartTime(event.getTime());
					Walk walk = journey.addWalk();
					walk.setAccessWalk(true);
					walk.setStartTime(event.getTime());
					walk.setOrig(journey.getOrig());
				} else {
					journey = chain.getJourneys().getLast();
					Walk walk = journey.addWalk();
					walk.setStartTime(event.getTime());
					walk.setOrig(network.getLinks().get(event.getLinkId())
							.getCoord());
					journey.getPossibleTransfer().getWalks().add(walk);
				}
			} else if (event.getLegMode().equals("car")) {
				chain.inCar = true;
				journey = chain.addJourney();
				journey.setCarJourney(true);
				journey.setOrig(network.getLinks().get(event.getLinkId())
						.getCoord());
				journey.setFromAct(chain.getActs().getLast());
				journey.setStartTime(event.getTime());
				Trip trip = journey.addTrip();
				trip.setMode("car");
				trip.setStartTime(event.getTime());
			} else if (event.getLegMode().equals("pt")) {
				if(isTransitScenario){
					// person waits till they enter the vehicle
					journey = chain.getJourneys().getLast();
					Wait wait = journey.addWait();
					if (journey.getWaits().size() == 1)
						wait.setAccessWait(true);
					wait.setStartTime(event.getTime());
					wait.setCoord(network.getLinks().get(event.getLinkId())
							.getCoord());
					if (!wait.isAccessWait()) {
						journey.getPossibleTransfer().getWaits().add(wait);
					}
				}else{
					journey = chain.addJourney();
					journey.setTeleportJourney(true);
					journey.setOrig(network.getLinks().get(event.getLinkId())
							.getCoord());
					journey.setFromAct(chain.getActs().getLast());
					journey.setStartTime(event.getTime());
					journey.setMainmode(event.getLegMode());
					Trip trip = journey.addTrip();
					trip.setMode(event.getLegMode());
					trip.setStartTime(event.getTime());
				}
			}
		} catch (Exception e) {
			String fullStackTrace = org.apache.commons.lang.exception.ExceptionUtils
					.getFullStackTrace(e);
			System.err.println(fullStackTrace);
			System.err.println(event.toString());
		}
	}

	@Override
	public void handleEvent(PersonStuckEvent event) {
		try {
			if (!transitDriverIds.contains(event.getPersonId())) {
				TravellerChain chain = chains.get(event.getPersonId());
				setStuck(getStuck() + 1);
				if (chain.getJourneys().size() > 0)
					chain.getJourneys().removeLast();
			}
		} catch (Exception e) {
			String fullStackTrace = org.apache.commons.lang.exception.ExceptionUtils
					.getFullStackTrace(e);
			System.err.println(fullStackTrace);
			System.err.println(event.toString());
		}
	}

	@Override
	public void handleEvent(PersonEntersVehicleEvent event) {
		if (!isTransitScenario)
			return;
		try {
			if (transitDriverIds.contains(event.getPersonId()))
				return;
			if (ptVehicles.keySet().contains(event.getVehicleId())) {
				TravellerChain chain = chains.get(event.getPersonId());
				Journey journey = chain.getJourneys().getLast();
				// first, handle the end of the wait
				journey.getWaits().getLast().setEndTime(event.getTime());
				// now, create a new trip
				ptVehicles.get(event.getVehicleId()).addPassenger(
						event.getPersonId());
				Trip trip = journey.addTrip();
				PTVehicle vehicle = ptVehicles.get(event.getVehicleId());
				trip.setLine(vehicle.transitLineId);
				trip.setMode(
						transitSchedule.getTransitLines()
								.get(vehicle.transitLineId).getRoutes()
								.get(vehicle.transitRouteId).getTransportMode());
				trip.setBoardingStop(vehicle.lastStop);
				trip.setOrig(journey.getWaits().getLast().getCoord());
				trip.setRoute(ptVehicles.get(event.getVehicleId()).transitRouteId);
				trip.setStartTime(event.getTime());
				// check for the end of a transfer
				if (journey.getPossibleTransfer() != null) {
					journey.getPossibleTransfer().setToTrip(trip);
					journey.getPossibleTransfer().setEndTime(event.getTime());
					journey.addTransfer(journey.getPossibleTransfer());
					journey.setPossibleTransfer(null);
				}
			}else{
				// add the person to the map that keeps track of who drives what vehicle
				driverIdFromVehicleId.put(event.getVehicleId(), event.getPersonId());
			}
		} catch (Exception e) {
			String fullStackTrace = org.apache.commons.lang.exception.ExceptionUtils
					.getFullStackTrace(e);
			System.err.println(fullStackTrace);
			System.err.println(event.toString());
		}
	}

	@Override
	public void handleEvent(PersonLeavesVehicleEvent event) {
		if (transitDriverIds.contains(event.getPersonId()))
			return;
		try {
				if (ptVehicles.keySet().contains(event.getVehicleId())) {
					TravellerChain chain = chains.get(event.getPersonId());
					chain.traveledVehicle = true;
					PTVehicle vehicle = ptVehicles.get(event.getVehicleId());
					double stageDistance = vehicle.removePassenger(event
							.getPersonId());
					Trip trip = chain.getJourneys().getLast().getTrips()
							.getLast();
					trip.setDistance(stageDistance);
					trip.setAlightingStop(vehicle.lastStop);
				}else{
					driverIdFromVehicleId.remove(event.getVehicleId());
				}
			
		} catch (Exception e) {
			String fullStackTrace = org.apache.commons.lang.exception.ExceptionUtils
					.getFullStackTrace(e);
			System.err.println(fullStackTrace);
			System.err.println(event.toString());
		}
	}

	@Override
	public void handleEvent(LinkEnterEvent event) {
		try {
			if (ptVehicles.keySet().contains(event.getVehicleId())) {
				PTVehicle ptVehicle = ptVehicles.get(event.getVehicleId());
				ptVehicle.in = true;
				ptVehicle.setLinkEnterTime(event.getTime());
			} else {
				chains.get(driverIdFromVehicleId.get(event.getVehicleId())).setLinkEnterTime(
						event.getTime());
			}

		} catch (Exception e) {
			String fullStackTrace = org.apache.commons.lang.exception.ExceptionUtils
					.getFullStackTrace(e);
			System.err.println(fullStackTrace);
			System.err.println(event.toString());
		}

	}

	@Override
	public void handleEvent(LinkLeaveEvent event) {
		try {
			if (ptVehicles.keySet().contains(event.getVehicleId())) {
				PTVehicle vehicle = ptVehicles.get(event.getVehicleId());
				if (vehicle.in)
					vehicle.in = false;
				vehicle.incDistance(network.getLinks().get(event.getLinkId())
						.getLength());

			} else {
				TravellerChain chain = chains.get(driverIdFromVehicleId.get(event.getVehicleId()));
				if (chain.inCar) {
					Journey journey = chain.getJourneys().getLast();
					journey.incrementCarDistance(network.getLinks()
							.get(event.getLinkId()).getLength());


				}
			}
		} catch (Exception e) {
			String fullStackTrace = org.apache.commons.lang.exception.ExceptionUtils
					.getFullStackTrace(e);
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
			transitDriverIds.add(event.getDriverId());
		} catch (Exception e) {
			String fullStackTrace = org.apache.commons.lang.exception.ExceptionUtils
					.getFullStackTrace(e);
			System.err.println(fullStackTrace);
			System.err.println(event.toString());
		}
	}

	@Override
	public void handleEvent(TeleportationArrivalEvent event) {
		try {
			if (transitDriverIds.contains(event.getPersonId()))
				return;
			TravellerChain chain = chains.get(event.getPersonId());
			if (chain.traveledVehicle)
				chain.traveledVehicle = false;
		} catch (Exception e) {
			String fullStackTrace = org.apache.commons.lang.exception.ExceptionUtils
					.getFullStackTrace(e);
			System.err.println(fullStackTrace);
			System.err.println(event.toString());
		}
	}

	@Override
	public void handleEvent(VehicleArrivesAtFacilityEvent event) {
		try {
			ptVehicles.get(event.getVehicleId()).lastStop = event
					.getFacilityId();
		} catch (Exception e) {
			String fullStackTrace = org.apache.commons.lang.exception.ExceptionUtils
					.getFullStackTrace(e);
			System.err.println(fullStackTrace);
			System.err.println(event.toString());
		}
	}

	// Methods
	@Override
	public void reset(int iteration) {

	}

	

	public void writeSimulationResultsToCSV(String path,
			String suffix) throws InstantiationException,
			IllegalAccessException, ClassNotFoundException, IOException,
			SQLException, NoConnectionException {
		DateFormat df = new SimpleDateFormat("yyyy_MM_dd");
		String formattedDate = df.format(new Date());
		// start with activities
		String actTableName = "matsim_activities" + suffix;
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
		columns.add(new PostgresqlColumnDefinition("sample_selector",
				PostgresType.FLOAT8));
		CSVWriter activityWriter = new CSVWriter("ACTS",
				actTableName, path, 100000, columns);
		activityWriter.addComment(String.format(
				"MATSim activities from events file %s, created on %s.",
				eventsFileName, formattedDate));

		String journeyTableName =  "matsim_journeys" + suffix;
		columns = new ArrayList<PostgresqlColumnDefinition>();
		columns.add(new PostgresqlColumnDefinition("journey_id",
				PostgresType.INT, "primary key"));
		columns.add(new PostgresqlColumnDefinition("person_id",
				PostgresType.TEXT));
		columns.add(new PostgresqlColumnDefinition("start_time",
				PostgresType.INT));
		columns.add(new PostgresqlColumnDefinition("end_time", PostgresType.INT));
		columns.add(new PostgresqlColumnDefinition("distance",
				PostgresType.INT));
		columns.add(new PostgresqlColumnDefinition("main_mode",
				PostgresType.TEXT));
		columns.add(new PostgresqlColumnDefinition("from_act", PostgresType.INT));
		columns.add(new PostgresqlColumnDefinition("to_act", PostgresType.INT));
		columns.add(new PostgresqlColumnDefinition("in_vehicle_distance",
				PostgresType.INT));
		columns.add(new PostgresqlColumnDefinition("in_vehicle_time",
				PostgresType.INT));
		columns.add(new PostgresqlColumnDefinition("access_walk_distance",
				PostgresType.INT));
		columns.add(new PostgresqlColumnDefinition("access_walk_time",
				PostgresType.INT));
		columns.add(new PostgresqlColumnDefinition("access_wait_time",
				PostgresType.INT));
		columns.add(new PostgresqlColumnDefinition("first_boarding_stop",
				PostgresType.TEXT));
		columns.add(new PostgresqlColumnDefinition("egress_walk_distance",
				PostgresType.INT));
		columns.add(new PostgresqlColumnDefinition("egress_walk_time",
				PostgresType.INT));
		columns.add(new PostgresqlColumnDefinition("last_alighting_stop",
				PostgresType.TEXT));
		columns.add(new PostgresqlColumnDefinition("transfer_walk_distance",
				PostgresType.INT));
		columns.add(new PostgresqlColumnDefinition("transfer_walk_time",
				PostgresType.INT));
		columns.add(new PostgresqlColumnDefinition("transfer_wait_time",
				PostgresType.INT));		
		columns.add(new PostgresqlColumnDefinition("sample_selector",
				PostgresType.FLOAT8));
		CSVWriter journeyWriter = new CSVWriter("JOURNEYS",
				journeyTableName, path, 50000, columns);
		journeyWriter.addComment(String.format(
				"MATSim journeys from events file %s, created on %s.",
				eventsFileName, formattedDate));

		String tripTableName ="matsim_trips" + suffix;
		columns = new ArrayList<PostgresqlColumnDefinition>();
		columns.add(new PostgresqlColumnDefinition("trip_id", PostgresType.INT));
		columns.add(new PostgresqlColumnDefinition("journey_id",
				PostgresType.INT));
		columns.add(new PostgresqlColumnDefinition("start_time",
				PostgresType.INT));
		columns.add(new PostgresqlColumnDefinition("end_time", PostgresType.INT));
		columns.add(new PostgresqlColumnDefinition("distance",
				PostgresType.INT));
		columns.add(new PostgresqlColumnDefinition("mode", PostgresType.TEXT));
		columns.add(new PostgresqlColumnDefinition("line", PostgresType.TEXT));
		columns.add(new PostgresqlColumnDefinition("route", PostgresType.TEXT));
		columns.add(new PostgresqlColumnDefinition("boarding_stop",
				PostgresType.TEXT));
		columns.add(new PostgresqlColumnDefinition("alighting_stop",
				PostgresType.TEXT));
		columns.add(new PostgresqlColumnDefinition("sample_selector",
				PostgresType.FLOAT8));
		CSVWriter tripWriter = new CSVWriter("TRIPS",
				tripTableName, path, 100000, columns);
		tripWriter.addComment(String.format(
				"MATSim trips (stages) from events file %s, created on %s.",
				eventsFileName, formattedDate));

		String transferTableName = "matsim_transfers" + suffix;
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
				PostgresType.INT));
		columns.add(new PostgresqlColumnDefinition("walk_time",
				PostgresType.INT));
		columns.add(new PostgresqlColumnDefinition("wait_time",
				PostgresType.INT));
		columns.add(new PostgresqlColumnDefinition("sample_selector",
				PostgresType.FLOAT8));
		CSVWriter transferWriter = new CSVWriter(
				"TRANSFERS", transferTableName, path, 100000, columns);
		transferWriter.addComment(String.format(
				"MATSim transfers from events file %s, created on %s.",
				eventsFileName, formattedDate));

		for (Entry<Id, TravellerChain> entry : chains.entrySet()) {
			String pax_id = entry.getKey().toString();
			TravellerChain chain = entry.getValue();
			for (Activity act : chain.getActs()) {
				try {
					Object[] args = { new Integer(act.getElementId()), pax_id,
							act.getFacility(), act.getType(),
							new Integer((int) act.getStartTime()),
							new Integer((int) act.getEndTime()),
							new Double(Math.random()) };
					activityWriter.addLine(args);
				} catch (Exception e) {
					System.out.println("HARK!");
//					System.err.println(act);
					;
				}
			}
			for (Journey journey : chain.getJourneys()) {
				try {

					Object[] journeyArgs = {
							new Integer(journey.getElementId()), pax_id,
							new Integer((int) journey.getStartTime()),
							new Integer((int) journey.getEndTime()),
							new Integer((int) journey.getDistance()),
							journey.getMainMode(),
							new Integer(journey.getFromAct().getElementId()),
							new Integer(journey.getToAct().getElementId()),
							new Integer((int) journey.getInVehDistance()),
							new Integer((int) journey.getInVehTime()),
							new Integer((int) journey.getAccessWalkDistance()),
							new Integer((int) journey.getAccessWalkTime()),
							new Integer((int) journey.getAccessWaitTime()),
							journey.getFirstBoardingStop(),
							new Integer((int) journey.getEgressWalkDistance()),
							new Integer((int) journey.getEgressWalkTime()),
							journey.getLastAlightingStop(),
							new Integer((int) journey.getTransferWalkDistance()),
							new Integer((int) journey.getTransferWalkTime()),
							new Integer((int) journey.getTransferWaitTime()),
							new Double(Math.random())

					};
					journeyWriter.addLine(journeyArgs);
					if (!(journey.isCarJourney() || journey.isTeleportJourney())) {
						for (Trip trip : journey.getTrips()) {
							Object[] tripArgs = {
									new Integer(trip.getElementId()),
									new Integer(journey.getElementId()),
									new Integer((int) trip.getStartTime()),
									new Integer((int) trip.getEndTime()),
									new Integer((int) trip.getDistance()),
									trip.getMode(), trip.getLine(),
									trip.getRoute(), trip.getBoardingStop(),
									trip.getAlightingStop(),
									new Double(Math.random()) };
							tripWriter.addLine(tripArgs);
						}
						for (Transfer transfer : journey.getTransfers()) {
							Object[] transferArgs = {
									new Integer(transfer.getElementId()),
									new Integer(journey.getElementId()),
									new Integer((int) transfer.getStartTime()),
									new Integer((int) transfer.getEndTime()),
									new Integer(transfer.getFromTrip()
											.getElementId()),
									new Integer(transfer.getToTrip()
											.getElementId()),
									new Integer((int) transfer.getWalkDistance()),
									new Integer((int) transfer.getWalkTime()),
									new Integer((int) transfer.getWaitTime()),
									new Double(Math.random()) };
							transferWriter.addLine(transferArgs);
						}
					}else{
						for (Trip trip : journey.getTrips()) {
							Object[] tripArgs = {
									new Integer(trip.getElementId()),
									new Integer(journey.getElementId()),
									new Integer((int) trip.getStartTime()),
									new Integer((int) trip.getEndTime()),
									journey.isTeleportJourney()? new Integer((int) trip.getDistance()):null,
									trip.getMode(), null,
									null, null,
									null,
									new Double(Math.random()) };
							tripWriter.addLine(tripArgs);
						}
					}
				} catch (NullPointerException e) {
					setStuck(getStuck() + 1);
				}
			}

		}
		
			activityWriter.finish();
			journeyWriter.finish();
			tripWriter.finish();
			transferWriter.finish();
	}

	public int getStuck() {
		return stuck;
	}

	public void setStuck(int stuck) {
		this.stuck = stuck;
	}

}
