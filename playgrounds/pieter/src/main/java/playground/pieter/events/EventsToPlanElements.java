package playground.pieter.events;

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

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.PersonLeavesVehicleEvent;
import org.matsim.api.core.v01.events.PersonStuckEvent;
import org.matsim.api.core.v01.events.TransitDriverStartsEvent;
import org.matsim.api.core.v01.events.handler.ActivityEndEventHandler;
import org.matsim.api.core.v01.events.handler.ActivityStartEventHandler;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.events.handler.PersonEntersVehicleEventHandler;
import org.matsim.api.core.v01.events.handler.PersonLeavesVehicleEventHandler;
import org.matsim.api.core.v01.events.handler.PersonStuckEventHandler;
import org.matsim.api.core.v01.events.handler.TransitDriverStartsEventHandler;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.analysis.travelsummary.travelcomponents.*;
import org.matsim.core.api.experimental.events.TeleportationArrivalEvent;
import org.matsim.core.api.experimental.events.VehicleArrivesAtFacilityEvent;
import org.matsim.core.api.experimental.events.handler.TeleportationArrivalEventHandler;
import org.matsim.core.api.experimental.events.handler.VehicleArrivesAtFacilityEventHandler;
import org.matsim.core.config.Config;
import org.matsim.pt.PtConstants;
import org.matsim.pt.router.TransitRouterConfig;
import org.matsim.pt.transitSchedule.api.TransitSchedule;

import others.sergioo.util.dataBase.DataBaseAdmin;
import others.sergioo.util.dataBase.NoConnectionException;
import playground.pieter.singapore.utils.postgresql.*;


/**
 * 
 * @author sergioo, pieterfourie
 *         <P>
 *         Converts events into journeys, trips/stages, transfers and activities
 *         tables. Originally designed for transit scenarios with full transit
 *         simulation, it needs some work for general scenarios with teleported
 *         modes.
 */

class EventsToPlanElements implements TransitDriverStartsEventHandler,
		PersonEntersVehicleEventHandler, PersonLeavesVehicleEventHandler,
		PersonDepartureEventHandler, PersonArrivalEventHandler,
		ActivityStartEventHandler, ActivityEndEventHandler,
		PersonStuckEventHandler, LinkEnterEventHandler, LinkLeaveEventHandler,
		TeleportationArrivalEventHandler, VehicleArrivesAtFacilityEventHandler {

	// Private classes
	private class PTVehicle {

		// Attributes
		private final Id transitLineId;
		private final Id transitRouteId;
		boolean in = false;
		private final Map<Id, Double> passengers = new HashMap<>();
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




	private Map<Id, Integer> acts = new HashMap<>();
	// Attributes
	private final Map<Id, TravellerChain> chains = new HashMap<>();
	private final Map<Id, Coord> locations = new HashMap<>();
	private String eventsFileName;
	// a writer to record the ids entering and exiting each link
	private PostgresqlCSVWriter linkWriter;
	private final Network network;
	private final Map<Id, PTVehicle> ptVehicles = new HashMap<>();
	private int stuck = 0;
	private TransitSchedule transitSchedule;
	private final double walkSpeed;
    private final String schemaName;
	private final HashSet<Id> transitDriverIds = new HashSet<>();
	private boolean isTransitScenario = false;

	public EventsToPlanElements(TransitSchedule transitSchedule,
			Network network, Config config, String suffix, String schemaName) {
		this.transitSchedule = transitSchedule;
		this.isTransitScenario = true;
		this.network = network;
		this.walkSpeed = new TransitRouterConfig(config).getBeelineWalkSpeed();
		TravelComponent.walkSpeed = walkSpeed;
		this.schemaName = schemaName;
//		Vehicles vehicles = VehicleUtils.createVehiclesContainer();
//		new VehicleReaderV1(vehicles).readFile(transitVehiclesfile);
//		transitVehicleIds.addAll(vehicles.getVehicles().keySet());
	}
	public EventsToPlanElements(
			Network network, Config config, String suffix, String schemaName) {
		this.network = network;
		this.walkSpeed = new TransitRouterConfig(config).getBeelineWalkSpeed();
		TravelComponent.walkSpeed = walkSpeed;
		this.schemaName = schemaName;
	}

	
	public EventsToPlanElements(TransitSchedule transitSchedule,
			Network network, Config config, File connectionProperties,
			String suffix, String schemaName) {
//		this(transitSchedule, network, config, suffix, transitVehiclesfile);
		this(transitSchedule, network, config, suffix, schemaName);
		samplePersonIdsForLinkWriting(connectionProperties);
		List<PostgresqlColumnDefinition> columns = new ArrayList<>();
		columns.add(new PostgresqlColumnDefinition("link_id", PostgresType.TEXT));
		columns.add(new PostgresqlColumnDefinition("person_id",
				PostgresType.TEXT));
		columns.add(new PostgresqlColumnDefinition("mode", PostgresType.TEXT));
		columns.add(new PostgresqlColumnDefinition("line", PostgresType.TEXT));
		columns.add(new PostgresqlColumnDefinition("enter_time",
				PostgresType.INT));
		columns.add(new PostgresqlColumnDefinition("exit_time",
				PostgresType.INT));
		try {
			linkWriter = new PostgresqlCSVWriter("LINKWRITER",
					"" + schemaName + ".matsim_link_traffic" + suffix,
					new DataBaseAdmin(connectionProperties), 1000, columns);
		} catch (InstantiationException | SQLException | IOException | ClassNotFoundException | IllegalAccessException e) {
			e.printStackTrace();
		}
    }



	private void samplePersonIdsForLinkWriting(File connectionProperties) {
		try {
			DataBaseAdmin dba = new DataBaseAdmin(connectionProperties);
			String idSelectionStatement = "SELECT person_id FROM " + schemaName + ".matsim_persons where sample_selector <= 0.01";
			ResultSet rs = dba.executeQuery(idSelectionStatement);
            HashSet<Id> personIdsForLinks = new HashSet<>();
			while(rs.next()){
				personIdsForLinks.add(Id.createPersonId(rs.getString("person_id")));
			}
			
		} catch (InstantiationException | NoConnectionException | SQLException | IOException | ClassNotFoundException | IllegalAccessException e) {
			e.printStackTrace();
		}

    }


	public PostgresqlCSVWriter getLinkWriter() {
		return linkWriter;
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
			}else{
				Journey journey = chain.getJourneys().getLast();
				journey.setEndTime(event.getTime());
				journey.setDest(network.getLinks().get(event.getLinkId())
						.getCoord());
				journey.setEndTime(event.getTime());
				
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
			}else{
				//teleport mode
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
				chains.get(event.getPersonId()).setLinkEnterTime(
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
				TravellerChain chain = chains.get(event.getPersonId());
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

	void writeSimulationResultsToSQL(File connectionProperties,
                                     String suffix) throws InstantiationException,
			IllegalAccessException, ClassNotFoundException, IOException,
			SQLException, NoConnectionException {
		DateFormat df = new SimpleDateFormat("yyyy_MM_dd");
		String formattedDate = df.format(new Date());
		// start with activities
		String actTableName = "" + schemaName + ".matsim_activities" + suffix;
		List<PostgresqlColumnDefinition> columns = new ArrayList<>();
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
		DataBaseAdmin actDBA = new DataBaseAdmin(connectionProperties);
		PostgresqlCSVWriter activityWriter = new PostgresqlCSVWriter("ACTS",
				actTableName, actDBA, 1000, columns);
		activityWriter.addComment(String.format(
				"MATSim activities from events file %s, created on %s.",
				eventsFileName, formattedDate));

		String journeyTableName = "" + schemaName + ".matsim_journeys" + suffix;
		columns = new ArrayList<>();
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
		DataBaseAdmin journeyDBA = new DataBaseAdmin(connectionProperties);
		PostgresqlCSVWriter journeyWriter = new PostgresqlCSVWriter("JOURNEYS",
				journeyTableName, journeyDBA, 1000, columns);
		journeyWriter.addComment(String.format(
				"MATSim journeys from events file %s, created on %s.",
				eventsFileName, formattedDate));

		String tripTableName = "" + schemaName + ".matsim_trips" + suffix;
		columns = new ArrayList<>();
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
		DataBaseAdmin tripDBA = new DataBaseAdmin(connectionProperties);
		PostgresqlCSVWriter tripWriter = new PostgresqlCSVWriter("TRIPS",
				tripTableName, tripDBA, 1000, columns);
		tripWriter.addComment(String.format(
				"MATSim trips (stages) from events file %s, created on %s.",
				eventsFileName, formattedDate));

		String transferTableName = "" + schemaName + ".matsim_transfers" + suffix;
		columns = new ArrayList<>();
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
		DataBaseAdmin transferDBA = new DataBaseAdmin(connectionProperties);
		PostgresqlCSVWriter transferWriter = new PostgresqlCSVWriter(
				"TRANSFERS", transferTableName, transferDBA, 1000, columns);
		transferWriter.addComment(String.format(
				"MATSim transfers from events file %s, created on %s.",
				eventsFileName, formattedDate));

		for (Entry<Id, TravellerChain> entry : chains.entrySet()) {
			String pax_id = entry.getKey().toString();
			TravellerChain chain = entry.getValue();
			for (Activity act : chain.getActs()) {
				try {
					Object[] args = {act.getElementId(), pax_id,
							act.getFacility(), act.getType(),
                            (int) act.getStartTime(),
                            (int) act.getEndTime(),
                            Math.random()};
					activityWriter.addLine(args);
				} catch (Exception e) {
					System.out.println("HARK!");
//					System.err.println(act);
                }
			}
			for (Journey journey : chain.getJourneys()) {
				try {

					Object[] journeyArgs = {
                            journey.getElementId(), pax_id,
                            (int) journey.getStartTime(),
                            (int) journey.getEndTime(),
                            (int) journey.getDistance(),
							journey.getMainMode(),
                            journey.getFromAct().getElementId(),
                            journey.getToAct().getElementId(),
                            (int) journey.getInVehDistance(),
                            (int) journey.getInVehTime(),
                            (int) journey.getAccessWalkDistance(),
                            (int) journey.getAccessWalkTime(),
                            (int) journey.getAccessWaitTime(),
							journey.getFirstBoardingStop(),
                            (int) journey.getEgressWalkDistance(),
                            (int) journey.getEgressWalkTime(),
							journey.getLastAlightingStop(),
                            (int) journey.getTransferWalkDistance(),
                            (int) journey.getTransferWalkTime(),
                            (int) journey.getTransferWaitTime(),
                            Math.random()

					};
					journeyWriter.addLine(journeyArgs);
					if (!journey.isCarJourney()) {
						for (Trip trip : journey.getTrips()) {
							Object[] tripArgs = {
                                    trip.getElementId(),
                                    journey.getElementId(),
                                    (int) trip.getStartTime(),
                                    (int) trip.getEndTime(),
                                    (int) trip.getDistance(),
									trip.getMode().trim(), trip.getLine(),
									trip.getRoute(), trip.getBoardingStop(),
									trip.getAlightingStop(),
                                    Math.random()};
							tripWriter.addLine(tripArgs);
						}
						for (Transfer transfer : journey.getTransfers()) {
							Object[] transferArgs = {
                                    transfer.getElementId(),
                                    journey.getElementId(),
                                    (int) transfer.getStartTime(),
                                    (int) transfer.getEndTime(),
                                    transfer.getFromTrip()
                                            .getElementId(),
                                    transfer.getToTrip()
                                            .getElementId(),
                                    (int) transfer.getWalkDistance(),
                                    (int) transfer.getWalkTime(),
                                    (int) transfer.getWaitTime(),
                                    Math.random()};
							transferWriter.addLine(transferArgs);
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
			

		DataBaseAdmin dba = new DataBaseAdmin(connectionProperties);
		// need to update the transit stop ids so they are consistent with LTA
		// list


		HashMap<String, String[]> idxNames = new HashMap<>();
		String[] idx1 = { "person_id", "facility_id", "type" };
		idxNames.put(actTableName, idx1);
		String[] idx2 = { "person_id", "from_act", "to_act", "main_mode" };
		idxNames.put(journeyTableName, idx2);
		String[] idx3 = { "journey_id", "mode", "line", "route",
				"boarding_stop", "alighting_stop" };
		idxNames.put(tripTableName, idx3);
		String[] idx4 = { "journey_id", "from_trip", "to_trip" };
		idxNames.put(transferTableName, idx4);
		for (Entry<String, String[]> entry : idxNames.entrySet()) {
			String tableName = entry.getKey();
			String[] columnNames = entry.getValue();
			for (String columnName : columnNames) {
				String indexName = tableName.split("\\.")[1] + "_" + columnName;
				String fullIndexName = tableName.split("\\.")[0] + "."
						+ indexName;
				String indexStatement;
				try {
					indexStatement = "DROP INDEX IF EXISTS " + fullIndexName + " ;\n ";
					dba.executeStatement(indexStatement);
					System.out.println(indexStatement);
				} catch (SQLException e) {
					e.printStackTrace();
				}

				try {
					indexStatement = "CREATE INDEX " + indexName + " ON "
							+ tableName + "(" + columnName + ");\n";
					dba.executeStatement(indexStatement);
					System.out.println(indexStatement);
				} catch (SQLException e) {
					e.printStackTrace();
				}

			}
		}

	}

	public void writeSimulationResultsToSQL(File properties, String eventsFileName,
			String suffix) throws InstantiationException,
			IllegalAccessException, ClassNotFoundException, IOException,
			SQLException, NoConnectionException {
		eventsFileName = eventsFileName.replaceAll("\\\\", "/");
		eventsFileName = eventsFileName.replaceAll(":", "");
		this.eventsFileName = eventsFileName;
		writeSimulationResultsToSQL(properties, suffix);
	}

	public void writeSimulationResultsToCSV(String path,
			String suffix) throws InstantiationException,
			IllegalAccessException, ClassNotFoundException, IOException,
			SQLException, NoConnectionException {
		DateFormat df = new SimpleDateFormat("yyyy_MM_dd");
		String formattedDate = df.format(new Date());
		// start with activities
		String actTableName = "matsim_activities" + suffix;
		List<PostgresqlColumnDefinition> columns = new ArrayList<>();
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
		columns = new ArrayList<>();
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
		columns = new ArrayList<>();
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
		columns = new ArrayList<>();
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
					Object[] args = {act.getElementId(), pax_id,
							act.getFacility(), act.getType(),
                            (int) act.getStartTime(),
                            (int) act.getEndTime(),
                            Math.random()};
					activityWriter.addLine(args);
				} catch (Exception e) {
					System.out.println("HARK!");
//					System.err.println(act);
                }
			}
			for (Journey journey : chain.getJourneys()) {
				try {

					Object[] journeyArgs = {
                            journey.getElementId(), pax_id,
                            (int) journey.getStartTime(),
                            (int) journey.getEndTime(),
                            (int) journey.getDistance(),
							journey.getMainMode(),
                            journey.getFromAct().getElementId(),
                            journey.getToAct().getElementId(),
                            (int) journey.getInVehDistance(),
                            (int) journey.getInVehTime(),
                            (int) journey.getAccessWalkDistance(),
                            (int) journey.getAccessWalkTime(),
                            (int) journey.getAccessWaitTime(),
							journey.getFirstBoardingStop(),
                            (int) journey.getEgressWalkDistance(),
                            (int) journey.getEgressWalkTime(),
							journey.getLastAlightingStop(),
                            (int) journey.getTransferWalkDistance(),
                            (int) journey.getTransferWalkTime(),
                            (int) journey.getTransferWaitTime(),
                            Math.random()

					};
					journeyWriter.addLine(journeyArgs);
					if (!(journey.isCarJourney() || journey.isTeleportJourney())) {
						for (Trip trip : journey.getTrips()) {
							Object[] tripArgs = {
                                    trip.getElementId(),
                                    journey.getElementId(),
                                    (int) trip.getStartTime(),
                                    (int) trip.getEndTime(),
                                    (int) trip.getDistance(),
									trip.getMode(), trip.getLine(),
									trip.getRoute(), trip.getBoardingStop(),
									trip.getAlightingStop(),
                                    Math.random()};
							tripWriter.addLine(tripArgs);
						}
						for (Transfer transfer : journey.getTransfers()) {
							Object[] transferArgs = {
                                    transfer.getElementId(),
                                    journey.getElementId(),
                                    (int) transfer.getStartTime(),
                                    (int) transfer.getEndTime(),
                                    transfer.getFromTrip()
                                            .getElementId(),
                                    transfer.getToTrip()
                                            .getElementId(),
                                    (int) transfer.getWalkDistance(),
                                    (int) transfer.getWalkTime(),
                                    (int) transfer.getWaitTime(),
                                    Math.random()};
							transferWriter.addLine(transferArgs);
						}
					}else{
						for (Trip trip : journey.getTrips()) {
							Object[] tripArgs = {
                                    trip.getElementId(),
                                    journey.getElementId(),
                                    (int) trip.getStartTime(),
                                    (int) trip.getEndTime(),
									journey.isTeleportJourney()? (int) trip.getDistance() :null,
									trip.getMode(), null,
									null, null,
									null,
                                    Math.random()};
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

	void setStuck(int stuck) {
		this.stuck = stuck;
	}

}
