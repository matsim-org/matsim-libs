package playground.pieter.singapore.utils.events;

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
import org.matsim.contrib.analysis.events2traveldiaries.travelcomponents.*;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.api.experimental.events.TeleportationArrivalEvent;
import org.matsim.core.api.experimental.events.VehicleArrivesAtFacilityEvent;
import org.matsim.core.api.experimental.events.handler.TeleportationArrivalEventHandler;
import org.matsim.core.api.experimental.events.handler.VehicleArrivesAtFacilityEventHandler;
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

import others.sergioo.util.dataBase.DataBaseAdmin;
import others.sergioo.util.dataBase.NoConnectionException;
import playground.pieter.singapore.utils.postgresql.*;

/**
 * 
 * @author sergioo, pieterfourie
 * 
 */

public class EventsToPlanElementsSingapore implements TransitDriverStartsEventHandler,
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

	public static void main(String[] args) throws IOException,
			InstantiationException, IllegalAccessException,
			ClassNotFoundException, SQLException, NoConnectionException {
		ScenarioImpl scenario = (ScenarioImpl) ScenarioUtils
				.createScenario(ConfigUtils.loadConfig(args[3]));
		scenario.getConfig().scenario().setUseTransit(true);
		new TransitScheduleReader(scenario).readFile(args[0]);
		new MatsimNetworkReader(scenario).readFile(args[1]);

		EventsManager eventsManager = EventsUtils.createEventsManager();
		File properties = new File("f:/data/matsim2postgres.properties");
		String suffix = args[5];
//		EventsToPlanElements test = new EventsToPlanElements(
//				scenario.getTransitSchedule(), scenario.getNetwork(),
//				scenario.getConfig(), properties, suffix);
		EventsToPlanElementsSingapore test = new EventsToPlanElementsSingapore(
				scenario.getTransitSchedule(), scenario.getNetwork(),
				scenario.getConfig(),properties,  suffix, "u_fouriep");
		eventsManager.addHandler(test);
		new MatsimEventsReader(eventsManager).readFile(args[2]);
		if (test.isWriteIdsForLinks())
			test.getLinkWriter().finish();
		if (Boolean.parseBoolean(args[4]))
			test.writeSimulationResultsToSQL(properties, args[2], suffix);
		System.out.println(test.getStuck());
//		if (test.isWriteIdsForLinks())
//			test.indexLinkRecords(properties, suffix);
	}

	public void indexLinkRecords(File properties, String suffix) {
		String[] indexStatements = {
				"DROP INDEX IF EXISTS " + schemaName + ".matsim_link_traffic"
						+ suffix + "_link_id;",
				"DROP INDEX IF EXISTS " + schemaName + ".matsim_link_traffic"
						+ suffix + "_person_id;",
				"DROP INDEX IF EXISTS " + schemaName + ".matsim_link_traffic"
						+ suffix + "_mode;",
				"DROP INDEX IF EXISTS " + schemaName + ".matsim_link_traffic"
						+ suffix + "_line;",
				"CREATE INDEX matsim_link_traffic" + suffix
						+ "_link_id ON " + schemaName + ".matsim_link_traffic"
						+ suffix + "(link_id);",
				"CREATE INDEX matsim_link_traffic" + suffix
						+ "_person_id ON " + schemaName + ".matsim_link_traffic"
						+ suffix + "(person_id);",
				"CREATE INDEX matsim_link_traffic" + suffix
						+ "_mode ON " + schemaName + ".matsim_link_traffic" + suffix
						+ "(mode);",
				"CREATE INDEX matsim_link_traffic" + suffix
						+ "_line ON " + schemaName + ".matsim_link_traffic" + suffix
						+ "(line);" };
		for (String indexStatement : indexStatements) {

			try {
				System.out.println(indexStatement);
				new DataBaseAdmin(properties).executeStatement(indexStatement);
			} catch (Exception e) {
				e.printStackTrace();
			}
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
	private final TransitSchedule transitSchedule;
	private final double walkSpeed;
	private boolean writeIdsForLinks = false;
	private HashSet<Id> personIdsForLinks;
	private final String schemaName;
	private HashSet<Id> transitDriverIds;

	public EventsToPlanElementsSingapore(TransitSchedule transitSchedule,
			Network network, Config config, String suffix, String schemaName) {
		this.transitSchedule = transitSchedule;
		this.network = network;
		this.walkSpeed = new TransitRouterConfig(config).getBeelineWalkSpeed();
		this.schemaName = schemaName;
//		Vehicles vehicles = VehicleUtils.createVehiclesContainer();
//		new VehicleReaderV1(vehicles).readFile(transitVehiclesfile);
//		transitVehicleIds.addAll(vehicles.getVehicles().keySet());
	}

	private EventsToPlanElementsSingapore(TransitSchedule transitSchedule,
                                          Network network, Config config, File connectionProperties,
                                          String suffix, String schemaName) {
//		this(transitSchedule, network, config, suffix, transitVehiclesfile);
		this(transitSchedule, network, config, suffix, schemaName);
		this.setWriteIdsForLinks(true);
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

	private String getMode(String transportMode, Id line) {
		if (transportMode.toLowerCase().contains("bus"))
			return "bus";
		else if (transportMode.toLowerCase().contains("rail"))
			return "lrt";
		else if (transportMode.toLowerCase().contains("subway"))
			if (line.toString().contains("PE")
					|| line.toString().contains("SE")
					|| line.toString().contains("SW"))
				return "lrt";
			else
				return "mrt";
		else
			return "other";
	}

	private void samplePersonIdsForLinkWriting(File connectionProperties) {
		try {
			DataBaseAdmin dba = new DataBaseAdmin(connectionProperties);
			String idSelectionStatement = "SELECT person_id FROM m_calibration.matsim_persons where sample_selector <= 0.1";
//			String idSelectionStatement = "SELECT person_id FROM u_fouriep.idsforlinkanalysis";
			ResultSet rs = dba.executeQuery(idSelectionStatement);
			this.personIdsForLinks = new HashSet<>();
			while(rs.next()){
				personIdsForLinks.add(Id.createPersonId(rs.getString("person_id")));
			}
			
		} catch (InstantiationException | NoConnectionException | SQLException | IOException | ClassNotFoundException | IllegalAccessException e) {
			e.printStackTrace();
		}

    }

	private void writePersonOnLink(LinkLeaveEvent event, TravellerChain chain) {
		if(!personIdsForLinks.contains(event.getPersonId()))
			return;
		Object[] linkArgs = { event.getLinkId().toString(),
				event.getPersonId().toString(),
				"car",
				"",
                (int) chain.getLinkEnterTime(),
                (int) event.getTime()};
		linkWriter.addLine(linkArgs);
	}

	private void writeTransitIdsForLink(PTVehicle vehicle, LinkLeaveEvent event) {
		for (Id i : vehicle.passengers.keySet()) {
			if(!personIdsForLinks.contains(i))
				continue;
			Object[] linkArgs = {
					event.getLinkId().toString(),
					i.toString(),
					getMode(transitSchedule.getTransitLines()
							.get(vehicle.transitLineId).getRoutes()
							.get(vehicle.transitRouteId).getTransportMode(),
							vehicle.transitLineId),
					vehicle.transitLineId.toString(),
                    (int) vehicle.getLinkEnterTime(),
                    (int) event.getTime()};
			linkWriter.addLine(linkArgs);
		}

	}

	public PostgresqlCSVWriter getLinkWriter() {
		return linkWriter;
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
			if (event.getPersonId().toString().startsWith("pt_tr"))
				return;
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
			if (event.getPersonId().toString().startsWith("pt_tr"))
				return;
			TravellerChain chain = chains.get(event.getPersonId());
			if (event.getLegMode().equals("car")) {
				Journey journey = chain.getJourneys().getLast();
				journey.setDest(network.getLinks().get(event.getLinkId())
						.getCoord());
				journey.setEndTime(event.getTime());
				chain.inCar = false;
			} else if (event.getLegMode().equals("transit_walk")) {
				Journey journey = chain.getJourneys().getLast();
				Walk walk = journey.getWalks().getLast();
				walk.setDest(network.getLinks().get(event.getLinkId())
						.getCoord());
				walk.setEndTime(event.getTime());
				walk.setDistance(walk.getDuration() * walkSpeed);
			} else if (event.getLegMode().equals("pt")) {
				Journey journey = chain.getJourneys().getLast();
				Trip trip = journey.getTrips().getLast();
				trip.setDest(network.getLinks().get(event.getLinkId())
						.getCoord());
				trip.setEndTime(event.getTime());
				journey.setPossibleTransfer(new Transfer());
				journey.getPossibleTransfer().setStartTime(event.getTime());
				journey.getPossibleTransfer().setFromTrip(trip);
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
			if (event.getPersonId().toString().startsWith("pt_tr"))
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
			if (!event.getPersonId().toString().startsWith("pt_tr")) {
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
		try {
			if (event.getPersonId().toString().startsWith("pt_tr"))
				return;
			if (event.getVehicleId().toString().startsWith("tr")) {
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
				trip.setMode(getMode(
						transitSchedule.getTransitLines()
								.get(vehicle.transitLineId).getRoutes()
								.get(vehicle.transitRouteId).getTransportMode(),
						vehicle.transitLineId));
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
		try {
			if (!event.getPersonId().toString().startsWith("pt_tr")) {
				if (event.getVehicleId().toString().startsWith("tr")) {
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
				if (isWriteIdsForLinks())
					writeTransitIdsForLink(vehicle, event);
			} else {
				TravellerChain chain = chains.get(event.getPersonId());
				if (chain.inCar) {
					Journey journey = chain.getJourneys().getLast();
					journey.incrementCarDistance(network.getLinks()
							.get(event.getLinkId()).getLength());
					journey.getTrips().getLast()
							.setDistance(journey.getDistance());
					if (isWriteIdsForLinks())
						writePersonOnLink(event, chain);
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
			if (event.getPersonId().toString().startsWith("pt_tr"))
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
		String update = "		UPDATE " + tripTableName
				+ " SET boarding_stop = matsim_to_transitstops_lookup.stop_id "
				+ " FROM m_calibration.matsim_to_transitstops_lookup "
				+ " WHERE boarding_stop = matsim_stop ";
		dba.executeUpdate(update);
		update = "		UPDATE "
				+ tripTableName
				+ " SET alighting_stop = matsim_to_transitstops_lookup.stop_id "
				+ " FROM m_calibration.matsim_to_transitstops_lookup "
				+ " WHERE alighting_stop = matsim_stop ";
		dba.executeUpdate(update);
		update = "		UPDATE " + journeyTableName
				+ " SET first_boarding_stop = matsim_to_transitstops_lookup.stop_id "
				+ " FROM m_calibration.matsim_to_transitstops_lookup "
				+ " WHERE first_boarding_stop = matsim_stop ";
		dba.executeUpdate(update);
		update = "		UPDATE "
				+ journeyTableName
				+ " SET last_alighting_stop = matsim_to_transitstops_lookup.stop_id "
				+ " FROM m_calibration.matsim_to_transitstops_lookup "
				+ " WHERE last_alighting_stop = matsim_stop ";
		dba.executeUpdate(update);

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

	public boolean isWriteIdsForLinks() {
		return writeIdsForLinks;
	}

	void setWriteIdsForLinks(boolean writeIdsForLinks) {
		this.writeIdsForLinks = writeIdsForLinks;
	}

	public int getStuck() {
		return stuck;
	}

	void setStuck(int stuck) {
		this.stuck = stuck;
	}

}
