package playground.pieter.singapore.hits;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.Set;

import javax.management.timer.Timer;

//import java.text.SimpleDateFormat;
//import java.util.ArrayList;
//import java.util.List;
//import java.util.TimeZone;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.travelsummary.events2traveldiaries.travelcomponents.Activity;
import org.matsim.contrib.travelsummary.events2traveldiaries.travelcomponents.Journey;
import org.matsim.contrib.travelsummary.events2traveldiaries.travelcomponents.Transfer;
import org.matsim.contrib.travelsummary.events2traveldiaries.travelcomponents.TravellerChain;
import org.matsim.contrib.travelsummary.events2traveldiaries.travelcomponents.Trip;
import org.matsim.contrib.travelsummary.events2traveldiaries.travelcomponents.Wait;
import org.matsim.contrib.travelsummary.events2traveldiaries.travelcomponents.Walk;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.network.algorithms.TransportModeNetworkFilter;
import org.matsim.core.router.Dijkstra;
import org.matsim.core.router.costcalculators.RandomizingTimeDistanceTravelDisutility.Builder;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;
import org.matsim.core.router.util.PreProcessDijkstra;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.trafficmonitoring.TravelTimeCalculator;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.facilities.ActivityFacility;
import org.matsim.population.algorithms.XY2Links;
import org.matsim.pt.router.PreparedTransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitScheduleReader;
import org.matsim.vehicles.Vehicle;

import others.sergioo.util.dataBase.DataBaseAdmin;
import others.sergioo.util.dataBase.NoConnectionException;
import playground.pieter.singapore.utils.postgresql.PostgresType;
import playground.pieter.singapore.utils.postgresql.PostgresqlCSVWriter;
import playground.pieter.singapore.utils.postgresql.PostgresqlColumnDefinition;
import playground.sergioo.hitsRouter2013.TransitRouterVariableImpl;
import playground.sergioo.singapore2012.transitRouterVariable.TransitRouterNetworkTravelTimeAndDisutilityWS;
import playground.sergioo.singapore2012.transitRouterVariable.TransitRouterNetworkWW;
import playground.sergioo.singapore2012.transitRouterVariable.TransitRouterNetworkWW.TransitRouterNetworkLink;
import playground.sergioo.singapore2012.transitRouterVariable.stopStopTimes.StopStopTimeCalculator;
import playground.sergioo.singapore2012.transitRouterVariable.waitTimes.WaitTimeCalculator;

public class HITSAnalyserPostgresqlSummary {

	private HITSData hitsData;
	static Scenario shortestPathCarNetworkOnlyScenario;
	private static NetworkImpl carFreeSpeedNetwork;
	private static NetworkImpl fullCongestedNetwork;
	private static Connection conn;

	public static Connection getConn() {
		return conn;
	}

	private static void setConn(Connection conn) {
		HITSAnalyserPostgresqlSummary.conn = conn;
	}

	private java.util.Date referenceDate; // all dates were referenced against a
	private HashMap<String, TravellerChain> chains;
	// starting Date of 1st September,
	// 2008, 00:00:00 SGT
	private static HashMap<Integer, Coord> zip2Coord;

	private static ArrayList<Integer> DGPs;

	private static PreProcessDijkstra preProcessData;
	private static LeastCostPathCalculator shortestCarNetworkPathCalculator;
	private static XY2Links xY2Links;
	static Map<Id, Link> links;
	private static Dijkstra carCongestedDijkstra;
	private static TransitRouterVariableImpl transitRouter;
	private static Scenario scenario;
	private static TransitRouterNetworkTravelTimeAndDisutilityWS transitTravelFunction;
    private static MyTransitRouterConfig transitRouterConfig;
	private static HashSet<TransitLine> mrtLines;
	private static HashSet<TransitLine> lrtLines;
    private static HashMap<String, Coord> mrtCoords;
	private static HashMap<String, Coord> lrtCoords;
	private static boolean freeSpeedRouting;
	private static String transitEventsFileName;
	private static String plansFileName;

    private static void createRouters(String[] args, boolean fSR) {
		freeSpeedRouting = fSR;
		scenario = ScenarioUtils.createScenario(ConfigUtils.loadConfig(args[0]));
		(new MatsimNetworkReader(scenario.getNetwork())).readFile(args[1]);
		(new TransitScheduleReader(scenario)).readFile(args[3]);
		double startTime = new Double(args[5]), endTime = new Double(args[6]), binSize = new Double(args[7]);

		StopStopTimeCalculator stopStopTime = new StopStopTimeCalculator(scenario.getTransitSchedule(),
				scenario.getConfig());
		System.out.println("Loading events");
//		if (!freeSpeedRouting) {
//			transitEventsFileName = args[2];
//			EventsManager eventsManager = EventsUtils.createEventsManager(scenario.getConfig());
//			eventsManager.addHandler(stopStopTime);
//			(new MatsimEventsReader(eventsManager)).readFile(transitEventsFileName);
//		}
		transitRouterConfig = new MyTransitRouterConfig(scenario.getConfig().planCalcScore(), scenario.getConfig()
				.plansCalcRoute(), scenario.getConfig().transitRouter(), scenario.getConfig().vspExperimental());
        TransitRouterNetworkWW transitRouterNetwork = TransitRouterNetworkWW.createFromSchedule(scenario.getNetwork(),
                scenario.getTransitSchedule(), transitRouterConfig.getBeelineWalkConnectionDistance());
		PreparedTransitSchedule preparedTransitSchedule = new PreparedTransitSchedule(scenario.getTransitSchedule());

		TravelTimeCalculator travelTimeCalculator = TravelTimeCalculator.create(scenario.getNetwork(), scenario.getConfig().travelTimeCalculator());
		WaitTimeCalculator waitTimeCalculator = new WaitTimeCalculator(scenario.getTransitSchedule(), (int) binSize,
				(int) (endTime - startTime));
		if (!freeSpeedRouting) {
            String carEventsFileName = args[4];
			EventsManager eventsManager = EventsUtils.createEventsManager(scenario.getConfig());
			eventsManager.addHandler(travelTimeCalculator);
			eventsManager.addHandler(stopStopTime);
			(new MatsimEventsReader(eventsManager)).readFile(carEventsFileName);
		}
		transitTravelFunction = new TransitRouterNetworkTravelTimeAndDisutilityWS(transitRouterConfig,
                transitRouterNetwork, waitTimeCalculator.getWaitTimes(), stopStopTime.getStopStopTimes(), scenario
						.getConfig().travelTimeCalculator(), scenario.getConfig().qsim(), preparedTransitSchedule);
		transitRouter = new TransitRouterVariableImpl(transitRouterConfig, transitTravelFunction, transitRouterNetwork,
				scenario.getNetwork());
		
		// get the set of mrt and lrt lines for special case routing
		mrtLines = new HashSet<>();
		lrtLines = new HashSet<>();
		Collection<TransitLine> lines = scenario.getTransitSchedule().getTransitLines().values();
        String[] MRT_LINES = new String[]{"EW", "NS", "NE", "CC"};
        String[] LRT_LINES = new String[]{"SW", "SE", "PE", "BP"};
		Arrays.sort(MRT_LINES);
		Arrays.sort(LRT_LINES);
		List<TransitRouteStop> mrtStops = new ArrayList<>();
		List<TransitRouteStop> lrtStops = new ArrayList<>();
		mrtCoords = new HashMap<>();
		lrtCoords = new HashMap<>();

		for (TransitLine line : lines) {
			if (line.getRoutes().size() != 0) {
				if (Arrays.binarySearch(MRT_LINES, 0, 4, line.getId().toString()) >= 0) {
					mrtLines.add(line);
					// get the mrt stops
					for (TransitRoute route : line.getRoutes().values()) {
						mrtStops.addAll(route.getStops());
					}

				}
				if (Arrays.binarySearch(LRT_LINES, 0, 4, line.getId().toString()) >= 0) {
					lrtLines.add(line);
					for (TransitRoute route : line.getRoutes().values()) {
						lrtStops.addAll(route.getStops());
					}
				}
			}
		}
		for (TransitRouteStop stop : mrtStops) {
			mrtCoords.put(stop.getStopFacility().getName(), stop.getStopFacility().getCoord());
		}
		for (TransitRouteStop stop : lrtStops) {
			mrtCoords.put(stop.getStopFacility().getName(), stop.getStopFacility().getCoord());
		}

		// now for car

		TravelDisutility travelDisutility = new Builder( TransportMode.car, scenario.getConfig().planCalcScore() )
				.createTravelDisutility(travelTimeCalculator.getLinkTravelTimes());
		carCongestedDijkstra = new Dijkstra(scenario.getNetwork(), travelDisutility,
				travelTimeCalculator.getLinkTravelTimes());
		HashSet<String> modeSet = new HashSet<>();
		modeSet.add("car");
		carCongestedDijkstra.setModeRestriction(modeSet);
		fullCongestedNetwork = (NetworkImpl) scenario.getNetwork();
		// add a free speed network that is car only, to assign the correct
		// nodes to agents
		TransportModeNetworkFilter filter = new TransportModeNetworkFilter(scenario.getNetwork());
		HITSAnalyserPostgresqlSummary.carFreeSpeedNetwork = NetworkImpl.createNetwork();
		HashSet<String> modes = new HashSet<>();
		modes.add(TransportMode.car);
		filter.filter(carFreeSpeedNetwork, modes);
		TravelDisutility travelMinCost = new TravelDisutility() {

			@Override
			public double getLinkTravelDisutility(Link link, double time, Person person, Vehicle vehicle) {
				return getLinkMinimumTravelDisutility(link);
			}

			@Override
			public double getLinkMinimumTravelDisutility(Link link) {
				return link.getLength() / link.getFreespeed();
			}
		};
		preProcessData = new PreProcessDijkstra();
		preProcessData.run(carFreeSpeedNetwork);
		TravelTime timeFunction = new TravelTime() {

			@Override
			public double getLinkTravelTime(Link link, double time, Person person, Vehicle vehicle) {
				return link.getLength() / link.getFreespeed();
			}
		};

		shortestCarNetworkPathCalculator = new Dijkstra(carFreeSpeedNetwork, travelMinCost, timeFunction,
				preProcessData);
		xY2Links = new XY2Links(carFreeSpeedNetwork, null);
	}

	void writeSimulationResultsToSQL(File connectionProperties, String extraPostScript)
			throws InstantiationException, IllegalAccessException, ClassNotFoundException, IOException, SQLException,
			NoConnectionException {
		System.out.println("Writing to SQL");
		DateFormat df = new SimpleDateFormat("yyyy_MM_dd");
		String formattedDate = df.format(new Date());
		String postScript = freeSpeedRouting ? extraPostScript + "_freespeedrouted" : extraPostScript;
		// start with activities
		String actTableName = "m_calibration.hits_activities" + postScript;
		List<PostgresqlColumnDefinition> columns = new ArrayList<>();
		columns.add(new PostgresqlColumnDefinition("activity_id", PostgresType.INT, "primary key"));
		columns.add(new PostgresqlColumnDefinition("person_id", PostgresType.TEXT));
		columns.add(new PostgresqlColumnDefinition("pcode", PostgresType.TEXT));
		columns.add(new PostgresqlColumnDefinition("type", PostgresType.TEXT));
		columns.add(new PostgresqlColumnDefinition("start_time", PostgresType.INT));
		columns.add(new PostgresqlColumnDefinition("end_time", PostgresType.INT));
		DataBaseAdmin actDBA = new DataBaseAdmin(connectionProperties);
		PostgresqlCSVWriter activityWriter = new PostgresqlCSVWriter("ACTS", actTableName, actDBA, 10000, columns);
		if (!freeSpeedRouting) {
			activityWriter.addComment(String.format("HITS activities, routed using events file %s and plans file %s",
					transitEventsFileName, plansFileName, formattedDate));
		}

		String journeyTableName = "m_calibration.hits_journeys" + postScript;
		columns = new ArrayList<>();
		columns.add(new PostgresqlColumnDefinition("journey_id", PostgresType.INT, "primary key"));
		columns.add(new PostgresqlColumnDefinition("person_id", PostgresType.TEXT));
		columns.add(new PostgresqlColumnDefinition("trip_idx_hits", PostgresType.TEXT));
		columns.add(new PostgresqlColumnDefinition("start_time", PostgresType.INT));
		columns.add(new PostgresqlColumnDefinition("end_time", PostgresType.INT));
		columns.add(new PostgresqlColumnDefinition("distance", PostgresType.FLOAT8));
		columns.add(new PostgresqlColumnDefinition("main_mode", PostgresType.TEXT));
		columns.add(new PostgresqlColumnDefinition("from_act", PostgresType.INT));
		columns.add(new PostgresqlColumnDefinition("to_act", PostgresType.INT));
		columns.add(new PostgresqlColumnDefinition("in_vehicle_distance", PostgresType.FLOAT8));
		columns.add(new PostgresqlColumnDefinition("in_vehicle_time", PostgresType.INT));
		columns.add(new PostgresqlColumnDefinition("access_walk_distance", PostgresType.FLOAT8));
		columns.add(new PostgresqlColumnDefinition("access_walk_time", PostgresType.INT));
		columns.add(new PostgresqlColumnDefinition("access_wait_time", PostgresType.INT));
		columns.add(new PostgresqlColumnDefinition("egress_walk_distance", PostgresType.FLOAT8));
		columns.add(new PostgresqlColumnDefinition("egress_walk_time", PostgresType.INT));
		columns.add(new PostgresqlColumnDefinition("transfer_walk_distance", PostgresType.FLOAT8));
		columns.add(new PostgresqlColumnDefinition("transfer_walk_time", PostgresType.INT));
		columns.add(new PostgresqlColumnDefinition("transfer_wait_time", PostgresType.INT));
		DataBaseAdmin journeyDBA = new DataBaseAdmin(connectionProperties);
		PostgresqlCSVWriter journeyWriter = new PostgresqlCSVWriter("JOURNEYS", journeyTableName, journeyDBA, 5000,
				columns);
		if (!freeSpeedRouting) {
			journeyWriter.addComment(String.format(
					"HITS journeys, routed using events file %s and plans file %s on %s", transitEventsFileName,
					plansFileName, formattedDate));
		}

		String tripTableName = "m_calibration.hits_trips" + postScript;
		columns = new ArrayList<>();
		columns.add(new PostgresqlColumnDefinition("trip_id", PostgresType.INT, "primary key"));
		columns.add(new PostgresqlColumnDefinition("journey_id", PostgresType.INT));
		columns.add(new PostgresqlColumnDefinition("start_time", PostgresType.INT));
		columns.add(new PostgresqlColumnDefinition("end_time", PostgresType.INT));
		columns.add(new PostgresqlColumnDefinition("distance", PostgresType.FLOAT8));
		columns.add(new PostgresqlColumnDefinition("mode", PostgresType.TEXT));
		columns.add(new PostgresqlColumnDefinition("line", PostgresType.TEXT));
		columns.add(new PostgresqlColumnDefinition("route", PostgresType.TEXT));
		columns.add(new PostgresqlColumnDefinition("boarding_stop", PostgresType.TEXT));
		columns.add(new PostgresqlColumnDefinition("alighting_stop", PostgresType.TEXT));
		DataBaseAdmin tripDBA = new DataBaseAdmin(connectionProperties);
		PostgresqlCSVWriter tripWriter = new PostgresqlCSVWriter("TRIPS", tripTableName, tripDBA, 10000, columns);
		if (!freeSpeedRouting) {
			tripWriter.addComment(String.format("HITS trips, routed using events file %s and plans file %s",
					transitEventsFileName, plansFileName, formattedDate));
		}

		String transferTableName = "m_calibration.hits_transfers" + postScript;
		columns = new ArrayList<>();
		columns.add(new PostgresqlColumnDefinition("transfer_id", PostgresType.INT, "primary key"));
		columns.add(new PostgresqlColumnDefinition("journey_id", PostgresType.INT));
		columns.add(new PostgresqlColumnDefinition("start_time", PostgresType.INT));
		columns.add(new PostgresqlColumnDefinition("end_time", PostgresType.INT));
		columns.add(new PostgresqlColumnDefinition("from_trip", PostgresType.INT));
		columns.add(new PostgresqlColumnDefinition("to_trip", PostgresType.INT));
		columns.add(new PostgresqlColumnDefinition("walk_distance", PostgresType.FLOAT8));
		columns.add(new PostgresqlColumnDefinition("walk_time", PostgresType.INT));
		columns.add(new PostgresqlColumnDefinition("wait_time", PostgresType.INT));
		DataBaseAdmin transferDBA = new DataBaseAdmin(connectionProperties);
		PostgresqlCSVWriter transferWriter = new PostgresqlCSVWriter("TRANSFERS", transferTableName, transferDBA, 1000,
				columns);
		if (!freeSpeedRouting) {
			transferWriter.addComment(String.format(
					"HITS transfers, routed using events file %s and plans file %s on %s", transitEventsFileName,
					plansFileName, formattedDate));
		}

		for (Entry<String, TravellerChain> entry : chains.entrySet()) {
			String pax_id = entry.getKey();
			TravellerChain chain = entry.getValue();
			for (Activity act : chain.getActs()) {
				Object[] args = {act.getElementId(), pax_id, act.getFacility(), act.getType(),
                        (int) act.getStartTime(), (int) act.getEndTime()};
				activityWriter.addLine(args);
			}
			for (Journey journey : chain.getJourneys()) {
				try {

					Object[] journeyArgs = {journey.getElementId(), pax_id, journey.getTrip_idx(),
                            (int) journey.getStartTime(), (int) journey.getEndTime(),
                            journey.getDistance(), journey.getMainMode(),
                            journey.getFromAct().getElementId(),
                            journey.getToAct().getElementId(), journey.getInVehDistance(),
                            (int) journey.getInVehTime(), journey.getAccessWalkDistance(),
                            (int) journey.getAccessWalkTime(),
                            (int) journey.getAccessWaitTime(),
                            journey.getEgressWalkDistance(), (int) journey.getEgressWalkTime(),
                            journey.getTransferWalkDistance(), (int) journey.getTransferWalkTime(),
                            (int) journey.getTransferWaitTime()
					};
					journeyWriter.addLine(journeyArgs);
					if (!journey.isCarJourney()) {
						for (Trip trip : journey.getTrips()) {
							Object[] tripArgs = {trip.getElementId(),
                                    journey.getElementId(), (int) trip.getStartTime(),
                                    (int) trip.getEndTime(), trip.getDistance(),
									trip.getMode(), trip.getLine(), trip.getRoute(), trip.getBoardingStop(),
									trip.getAlightingStop() };
							tripWriter.addLine(tripArgs);
						}
						for (Transfer transfer : journey.getTransfers()) {
							Object[] transferArgs = {transfer.getElementId(),
                                    journey.getElementId(), (int) transfer.getStartTime(),
                                    (int) transfer.getEndTime(),
                                    transfer.getFromTrip().getElementId(),
                                    transfer.getToTrip().getElementId(),
                                    transfer.getWalkDistance(), (int) transfer.getWalkTime(),
                                    (int) transfer.getWaitTime()};
							transferWriter.addLine(transferArgs);
						}
					} else {
						for (Trip trip : journey.getTrips()) {
							Object[] tripArgs = {trip.getElementId(),
                                    journey.getElementId(), (int) trip.getStartTime(),
                                    (int) trip.getEndTime(), trip.getDistance(),
									trip.getMode(), "null", "null", "null", "null" };
							tripWriter.addLine(tripArgs);
						}
					}
				} catch (Exception e) {

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
		String update = "		UPDATE " + tripTableName + " SET boarding_stop = matsim_to_transitstops_lookup.stop_id "
				+ " FROM m_calibration.matsim_to_transitstops_lookup " + " WHERE boarding_stop = matsim_stop ";
		dba.executeUpdate(update);
		update = "		UPDATE " + tripTableName + " SET alighting_stop = matsim_to_transitstops_lookup.stop_id "
				+ " FROM m_calibration.matsim_to_transitstops_lookup " + " WHERE alighting_stop = matsim_stop ";
		dba.executeUpdate(update);
		// drop and write a number of indices

		HashMap<String, String[]> idxNames = new HashMap<>();
		String[] idx1 = { "person_id", "pcode", "type" };
		idxNames.put(actTableName, idx1);
		String[] idx2 = { "person_id", "trip_idx_hits", "from_act", "to_act", "main_mode" };
		idxNames.put(journeyTableName, idx2);
		String[] idx3 = { "journey_id", "mode", "line", "route", "boarding_stop", "alighting_stop" };
		idxNames.put(tripTableName, idx3);
		String[] idx4 = { "journey_id", "from_trip", "to_trip" };
		idxNames.put(transferTableName, idx4);
		for (Entry<String, String[]> entry : idxNames.entrySet()) {
			String tableName = entry.getKey();
			String[] columnNames = entry.getValue();
			for (String columnName : columnNames) {
				String indexName = tableName.split("\\.")[1] + "_" + columnName;
				String fullIndexName = tableName.split("\\.")[0] + "." + indexName;
				String indexStatement;
				try {
					indexStatement = "DROP INDEX " + fullIndexName + " ;\n ";
					dba.executeStatement(indexStatement);
					System.out.println(indexStatement);
				} catch (SQLException e) {
					e.printStackTrace();
				}

				try {
					indexStatement = "CREATE INDEX " + indexName + " ON " + tableName + "(" + columnName + ");\n";
					dba.executeStatement(indexStatement);
					System.out.println(indexStatement);
				} catch (SQLException e) {
					e.printStackTrace();
				}

			}
		}

	}

	private HITSAnalyserPostgresqlSummary() throws ParseException {

		DateFormat outdfm = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		// outdfm.setTimeZone(TimeZone.getTimeZone("SGT"));
		referenceDate = outdfm.parse("2008-09-01 00:00:00");
	}

	private HITSAnalyserPostgresqlSummary(HITSData h2) throws ParseException, SQLException {

		this();
		this.setHitsData(h2);
	}

	void setHitsData(HITSData hitsData) {
		this.hitsData = hitsData;
	}

	HITSData getHitsData() {
		return hitsData;
	}

	public void writeTripSummary() throws IOException {
		ArrayList<HITSTrip> ht = this.getHitsData().getTrips();
		FileWriter outFile = new FileWriter("f:/temp/tripsummary.csv");
		PrintWriter out = new PrintWriter(outFile);
		out.write(HITSTrip.HEADERSTRING);
		for (HITSTrip t : ht) {
			out.write(t.toString());
		}
		out.close();
	}

	private String getMode(boolean busStage, Id line) {
		if (busStage)
			return "bus";
		if (line.toString().contains("PE") || line.toString().contains("SE") || line.toString().contains("SW"))
			return "lrt";
		else
			return "mrt";
	}

	public void writePersonSummary() throws IOException {
		ArrayList<HITSPerson> hp = this.getHitsData().getPersons();
		FileWriter outFile = new FileWriter("f:/temp/personsummary.csv");
		PrintWriter out = new PrintWriter(outFile);
		out.write(HITSPerson.HEADERSTRING);
		for (HITSPerson p : hp) {
			out.write(p.toString());
		}
		out.close();
	}

	public static TimeAndDistance getCarFreeSpeedShortestPathTimeAndDistance(Coord startCoord, Coord endCoord) {
		double distance = 0;
		Node startNode = carFreeSpeedNetwork.getNearestNode(startCoord);
		Node endNode = carFreeSpeedNetwork.getNearestNode(endCoord);

		Path path = shortestCarNetworkPathCalculator.calcLeastCostPath(startNode, endNode, 0, null, null);
		for (Link l : path.links) {
			distance += l.getLength();

		}
		return new TimeAndDistance(path.travelTime, distance);
	}

	private static TimeAndDistance getCarCongestedShortestPathDistance(Coord startCoord, Coord endCoord, double time) {
		double distance = 0;

		Node startNode = carFreeSpeedNetwork.getNearestNode(startCoord);
		Node endNode = carFreeSpeedNetwork.getNearestNode(endCoord);

		Path path = carCongestedDijkstra.calcLeastCostPath(startNode, endNode, time, null, null);
		for (Link l : path.links) {
			distance += l.getLength();
		}

		return new TimeAndDistance(path.travelTime, distance);
	}

	public static double getStraightLineDistance(Coord startCoord, Coord endCoord) {
		double x1 = startCoord.getX();
		double x2 = endCoord.getX();
		double y1 = startCoord.getY();
		double y2 = endCoord.getY();
		double xsq = Math.pow(x2 - x1, 2);
		double ysq = Math.pow(y2 - y1, 2);
		return (Math.sqrt(xsq + ysq));
	}

	private static void setZip2Coord(Connection conn) throws SQLException {
		// init the hashmap
		zip2Coord = new HashMap<>();
		Statement s;
		s = conn.createStatement();
		s.executeQuery("select pcode, x_utm48n, y_utm48n from m_calibration.spatial_pcode where x_utm48n is not null;");
		ResultSet rs = s.getResultSet();
		// iterate through the resultset and populate the hashmap
		// Scenario scenario =
		// ScenarioUtils.createScenario(ConfigUtils.createConfig());
		while (rs.next()) {
			try {
				zip2Coord.put(rs.getInt("pcode"), new Coord(rs.getDouble("x_utm48n"), rs.getDouble("y_utm48n")));

			} catch (NullPointerException e) {
				System.out.println(rs.getInt("pcode"));
			}
		}
	}

	public static Coord getZip2Coord(int zip) {
		try {

			return HITSAnalyserPostgresqlSummary.zip2Coord.get(zip);
		} catch (NullPointerException ne) {
			return null;
		}
	}

	private double getValidStartTime(double startTime) {
		while (startTime < 0 || startTime > 24 * 3600) {
			if (startTime < 0) {
				startTime = 24 * 3600 + startTime;
			} else if (startTime > 24 * 3600) {
				startTime = startTime - 24 * 3600;
			}
		}
		return startTime;
	}

	private void compileAlternativeTravellerChains(String mode, double searchradius) {
		int actsCount = 0, journeysCount = 0, transfersCount = 0, tripsCount = 0;
		System.out.println("Starting alternative summary: " + new Date());
		ArrayList<HITSPerson> persons = this.getHitsData().getPersons();
		this.chains = new HashMap<>();
		PERSONS: for (HITSPerson p : persons) {
			// if( counter > 100)
			// return;
			TravellerChain chain = new TravellerChain();
			this.chains.put(p.pax_idx, chain);
			ArrayList<HITSTrip> trips = p.getTrips();
			TRIPS: for (HITSTrip t : trips) {
				Coord origCoord;
				Coord destCoord;
				double startTime = ((double) (t.t3_starttime_24h.getTime() - this.referenceDate.getTime()))
						/ (double) Timer.ONE_SECOND;
				startTime = startTime>(24*3600)?startTime%(24*3600):startTime;
				double endTime = ((double) (t.t4_endtime_24h.getTime() - this.referenceDate.getTime()))
						/ (double) Timer.ONE_SECOND;

				origCoord = HITSAnalyserPostgresqlSummary.zip2Coord.get(t.p13d_origpcode);
				destCoord = HITSAnalyserPostgresqlSummary.zip2Coord.get(t.t2_destpcode);
				if (origCoord == null) {
					System.out.println("Problem ZIP : " + t.p13d_origpcode);
					continue PERSONS;
				} else if (destCoord == null) {
					System.out.println("Problem ZIP : " + t.t2_destpcode);
					continue PERSONS;
				}
				if (t.trip_id == 1) {
					Activity home = chain.addActivity();
					actsCount++;
					home.setEndTime(startTime);
					home.setFacility(Id.create(t.p13d_origpcode, ActivityFacility.class));
					home.setStartTime(Math.min(startTime, 0));
					home.setCoord(origCoord);
					home.setType("home");
				} else {
					// get the last activity created
					try {
						Activity activity = chain.getActs().getLast();
						activity.setEndTime(startTime);
					} catch (NoSuchElementException n) {
						continue PERSONS;
					}
				}

				Journey journey = chain.addJourney();
				journeysCount++;
				journey.setCarJourney(mode.equals("car"));
				journey.setTrip_idx(t.h1_hhid + "_" + t.pax_id + "_" + t.trip_id);
				journey.setStartTime(startTime);
				journey.setFromAct(chain.getActs().getLast());
				// create the next activity
				Activity act = chain.addActivity();
				actsCount++;
				act.setFacility(Id.create(t.t2_destpcode,ActivityFacility.class));
				act.setStartTime(endTime);
				act.setType(t.t6_purpose);
				journey.setToAct(act);
				Set<TransitLine> lines = new HashSet<>();
				lines.addAll(scenario.getTransitSchedule().getTransitLines().values());
				transitRouter.setAllowedLines(lines);
				if (journey.isCarJourney()) {
					TimeAndDistance carTimeDistance = HITSAnalyserPostgresqlSummary
							.getCarCongestedShortestPathDistance(origCoord, destCoord, getValidStartTime(startTime));
					Trip trip = journey.addTrip();
					tripsCount++;
					trip.setStartTime(startTime);
					trip.setEndTime(startTime + carTimeDistance.time);
					trip.setDistance(carTimeDistance.distance);
					journey.setCarDistance(carTimeDistance.distance);
					trip.setMode("car");
					journey.setEndTime(trip.getEndTime());
				} else {
					Path path = null;
					double linkStartTime = startTime;

					int substage_id = 0;
					double[] radiusFactors = { 1.0, 1.5, 2.5, 5, 10, 25, 50 };
					int radiusIdx = 0;
					path = null;
					linkStartTime = getValidStartTime(linkStartTime);
					while (path == null && radiusIdx < radiusFactors.length) {

						HITSAnalyserPostgresqlSummary.transitRouterConfig.setSearchradius(searchradius
								* radiusFactors[radiusIdx]);

						try {
							path = transitRouter.calcPathRoute(origCoord, destCoord, linkStartTime, null);
						} catch (NullPointerException e) {

						}
						radiusIdx++;

					}
					if (path == null) {
						System.out.println("Cannot route " + t.h1_hhid + "_" + t.pax_id + "_" + t.trip_id + "\t" + origCoord
								+ "\t" + destCoord);
						break;
					}

					boolean inVehicle = false;
					for (int j = 0; j < path.links.size(); j++) {
						if (j == 0) {
							Walk walk = journey.addWalk();
							Coord boardCoord = path.nodes.get(0).getCoord();
							double walkDistanceAccessFromRouter = CoordUtils.calcEuclideanDistance(origCoord, boardCoord);

							double walkTimeAccessFromRouter = walkDistanceAccessFromRouter
									/ transitRouterConfig.getBeelineWalkSpeed();
							walk.setStartTime(linkStartTime);
							linkStartTime += walkTimeAccessFromRouter;
							walk.setEndTime(linkStartTime);
							walk.setDistance(walkDistanceAccessFromRouter);
							walk.setAccessWalk(true);
							substage_id++;

						}
						Link l = path.links.get(j);
						TransitRouterNetworkLink transitLink = (TransitRouterNetworkLink) l;
						if (transitLink.getRoute() != null) {
							// in line link
							if (!inVehicle) {
								inVehicle = true;
								Trip trip = journey.addTrip();
								tripsCount++;
								trip.setStartTime(linkStartTime);
								trip.setBoardingStop(journey.getWaits().getLast().getStopId());
								trip.setLine(transitLink.getLine().getId());
								trip.setRoute(transitLink.getRoute().getId());
								trip.setMode(scenario.getTransitSchedule().getTransitLines().get(trip.getLine())
										.getRoutes().get(trip.getRoute()).getTransportMode().trim());
								trip.setEndTime(linkStartTime);
								trip.setDistance(0);
								if (journey.getPossibleTransfer() != null) {
									Transfer transfer = journey.getPossibleTransfer();
									transfer.setToTrip(trip);
									transfer.setEndTime(linkStartTime);
									journey.addTransfer(transfer); transfersCount++;
									journey.setPossibleTransfer(null);
								}
								substage_id++;
							}
							Trip trip = journey.getTrips().getLast();
							double linkLength = transitLink.getLength();
							trip.incrementDistance(linkLength);
							double linkTime = transitTravelFunction.getLinkTravelTime(transitLink, linkStartTime, null,
									null);
							trip.incrementTime(linkTime);
							linkStartTime += linkTime;
						} else if (transitLink.getToNode().route == null) {
							// transfer link
							double linkLength = transitLink.getLength();
							double linkTime = transitTravelFunction.getLinkTravelTime(transitLink, linkStartTime, null,
									null);
							Walk walk;
							if (substage_id > 1) {
								Trip trip = journey.getTrips().getLast();
								trip.setAlightingStop(l.getToNode().getId());
								Transfer transfer = new Transfer();
								journey.setPossibleTransfer(transfer);
								transfer.setFromTrip(trip);
								transfer.setStartTime(linkStartTime);
								walk = journey.addWalk();
								walk.setStartTime(linkStartTime);
								walk.setDistance(linkLength);
								transfer.getWalks().add(walk);
							} else {
								// this is still part of the access walk
								walk = journey.getWalks().getLast();
								walk.setDistance(walk.getDistance() + linkLength);
							}
							if (j + 2 <= path.links.size()) {
								substage_id = 1;
							} else {
								substage_id++;
							}
							inVehicle = false;
							linkStartTime += linkTime;
							walk.setEndTime(linkStartTime);

						} else if (transitLink.getFromNode().route == null) {
							// wait link
							substage_id++;
							double linkTime = transitTravelFunction.getLinkTravelTime(transitLink, linkStartTime, null,
									null);

							Wait wait = journey.addWait();
							if(journey.getPossibleTransfer() != null){
								journey.getPossibleTransfer().getWaits().add(wait);
							}
							wait.setStartTime(linkStartTime);
							wait.setStopId(l.getFromNode().getId());
							linkStartTime += linkTime;
							wait.setEndTime(linkStartTime);
							if (j == 0) {
								wait.setAccessWait(true);
							}

						} else
							throw new RuntimeException("Bad transit router link");
					}// end path traversal

					Coord alightCoord = path.nodes.get(path.nodes.size() - 1).getCoord();
					substage_id++;

					double walkDistanceEgressFromRouter = CoordUtils.calcEuclideanDistance(alightCoord, destCoord);
					double walkTimeEgressFromRouter = walkDistanceEgressFromRouter
							/ transitRouterConfig.getBeelineWalkSpeed();
					Walk walk = journey.addWalk();
					walk.setStartTime(linkStartTime);
					walk.setEndTime(linkStartTime + walkTimeEgressFromRouter);
					walk.setDistance(walkDistanceEgressFromRouter);
					walk.setEgressWalk(true);
					journey.setEndTime(journey.getWalks().getLast().getEndTime());

				}
			}
//			System.out.print(chain);
//			System.exit(0);
		}
		System.err.printf("\n\na: %d j: %d, t: %d x: %d\n\n",actsCount,journeysCount,tripsCount,transfersCount);

	}

	private void compileTravellerChains(double busSearchradius, double mrtSearchRadius) {
		System.out.println("Starting summary : " + new Date());
		int actsCount = 0, journeysCount = 0, transfersCount = 0, tripsCount = 0;
		ArrayList<HITSPerson> persons = this.getHitsData().getPersons();
		this.chains = new HashMap<>();
		PERSONS: for (HITSPerson p : persons) {
			// if( counter > 100)
			// return;
			TravellerChain chain = new TravellerChain();
			this.chains.put(p.pax_idx, chain);
			ArrayList<HITSTrip> trips = p.getTrips();
			for (HITSTrip t : trips) {
				Coord origCoord;
				Coord destCoord;
				double startTime = ((double) (t.t3_starttime_24h.getTime() - this.referenceDate.getTime()))
						/ (double) Timer.ONE_SECOND;

				double endTime = ((double) (t.t4_endtime_24h.getTime() - this.referenceDate.getTime()))
						/ (double) Timer.ONE_SECOND;

				origCoord = HITSAnalyserPostgresqlSummary.zip2Coord.get(t.p13d_origpcode);
				destCoord = HITSAnalyserPostgresqlSummary.zip2Coord.get(t.t2_destpcode);
				if (origCoord == null) {
					System.out.println("Problem ZIP : " + t.p13d_origpcode);
					continue PERSONS;
				} else if (destCoord == null) {
					System.out.println("Problem ZIP : " + t.t2_destpcode);
					continue PERSONS;
				}
				if (t.trip_id == 1) {
					Activity home = chain.addActivity();
					actsCount++;
					home.setEndTime(startTime);
					home.setFacility(Id.create(t.p13d_origpcode,ActivityFacility.class));
					home.setStartTime(Math.min(startTime, 0));
					home.setType("home");
				} else {
					// get the last activity created
					try {
						Activity activity = chain.getActs().getLast();
						activity.setEndTime(startTime);
					} catch (NoSuchElementException n) {
						continue PERSONS;
					}
				}
				// add the journey

//				 if (!(t.mainmode.equals("publBus")
//				 || t.mainmode.equals("mrt")
//				 || t.mainmode.equals("lrt") || t.mainmode
//				 .equals("carDrv")))
//				 continue;
				Journey journey = chain.addJourney();
				journeysCount++;
				try {
					journey.setCarJourney(!
							(t.mainmode.equals("publBus")
									 || t.mainmode.equals("mrt")
									 || t.mainmode.equals("lrt"))
							);
				} catch (NullPointerException ne) {
					System.out.println("null on main mode");
				}
				journey.setTrip_idx(t.h1_hhid + "_" + t.pax_id + "_" + t.trip_id);
				journey.setStartTime(startTime);
				journey.setFromAct(chain.getActs().getLast());
				journey.setEndTime(endTime);
				// create the next activity
				Activity act = chain.addActivity();
				actsCount++;
				act.setFacility(Id.create(t.t2_destpcode,ActivityFacility.class));
				act.setStartTime(endTime);
				act.setType(t.t6_purpose);
				journey.setToAct(act);
				if (journey.isCarJourney()) {
					TimeAndDistance carTimeDistance = HITSAnalyserPostgresqlSummary
							.getCarCongestedShortestPathDistance(origCoord, destCoord, getValidStartTime(startTime));
					Trip trip = journey.addTrip();
					tripsCount++;
					trip.setStartTime(startTime);
					trip.setEndTime(startTime + carTimeDistance.time);
					trip.setDistance(carTimeDistance.distance);
					journey.setCarDistance(carTimeDistance.distance);
					trip.setMode(t.mainmode);
					journey.setMainmode(t.mainmode);
				}
				// route transit-only trips using the transit router
				if (t.stageChainSimple.equals(t.stageChainTransit)) {
					Set<TransitLine> lines = new HashSet<>();
					Coord interimOrig = origCoord;
					Coord interimDest = destCoord;
					Path path = null;
					// deal with the 7 most common cases for now
					HITSStage stage = t.getStages().get(0);
					boolean busCheck = stage.t10_mode.equals("publBus");
					boolean mrtCheck = stage.t10_mode.equals("mrt");
					boolean lrtCheck = stage.t10_mode.equals("lrt");
					List<TransitStageRoutingInput> transitStages = new ArrayList<>();
					boolean doneCompilingTransitStages = false;
					STAGES: while (!doneCompilingTransitStages) {
						// System.out.println(p.pax_idx + "_"
						// + t.trip_id);
						if (busCheck) {
							lines.add(HITSAnalyserPostgresqlSummary.scenario.getTransitSchedule().getTransitLines()
									.get(Id.create(stage.t11_boardsvcstn,TransitLine.class)));
							if (stage.nextStage != null) {
								if (stage.nextStage.t10_mode.equals("publBus")) {
									stage = stage.nextStage;
									continue;
								} else {

									// going to an lrt or mrt
									// station
									busCheck = false;
									mrtCheck = stage.nextStage.t10_mode.equals("mrt");
									lrtCheck = stage.nextStage.t10_mode.equals("lrt");
									interimDest = mrtCheck ? mrtCoords.get(stage.nextStage.t11_boardsvcstn) : lrtCoords
											.get(stage.nextStage.t11_boardsvcstn);
								}
								transitStages.add(new TransitStageRoutingInput(interimOrig, interimDest, lines, true));
								lines = new HashSet<>();
								interimOrig = interimDest;
								interimDest = destCoord;
								stage = stage.nextStage;
								continue;
							}// next stage is null
							transitStages.add(new TransitStageRoutingInput(interimOrig, interimDest, lines, true));
							doneCompilingTransitStages = true;
						}// end of bus stage chain check
						if (mrtCheck || lrtCheck) {
							HashMap<String, Coord> theCoords = mrtCheck ? mrtCoords : lrtCoords;
							lines = mrtCheck ? mrtLines : lrtLines;
							interimOrig = theCoords.get(stage.t11_boardsvcstn);
							interimDest = theCoords.get(stage.t12_alightstn);
							transitStages.add(new TransitStageRoutingInput(interimOrig, interimDest, lines, false));
							if (stage.nextStage != null) {
								busCheck = stage.nextStage.t10_mode.equals("publBus");
								mrtCheck = stage.nextStage.t10_mode.equals("mrt");
								lrtCheck = stage.nextStage.t10_mode.equals("lrt");
								lines = new HashSet<>();
								interimOrig = interimDest;
								interimDest = destCoord;
								stage = stage.nextStage;
                            } else {
								doneCompilingTransitStages = true;

							}
						}
					}

					// traverse the list of transitStages and
					// generate paths
					double linkStartTime = startTime;
					Coord walkOrigin = origCoord;

					PATHS: for (int i = 0; i < transitStages.size(); i++) {
						int substage_id = 0;
						TransitStageRoutingInput ts = transitStages.get(i);
						transitRouter.setAllowedLines(ts.lines);
						// transitScheduleRouter.setA
						double[] radiusFactors = { 1.0, 1.5, 2.5, 5, 10, 25 };
						int radiusIdx = 0;
						path = null;
						linkStartTime = getValidStartTime(linkStartTime);
						while (path == null && radiusIdx < radiusFactors.length) {
							if (ts.busStage) {
								HITSAnalyserPostgresqlSummary.transitRouterConfig.setSearchradius(busSearchradius
										* radiusFactors[radiusIdx]);
							} else {
								HITSAnalyserPostgresqlSummary.transitRouterConfig.setSearchradius(mrtSearchRadius
										* radiusFactors[radiusIdx]);
							}
							try {
								path = transitRouter.calcPathRoute(ts.orig, ts.dest, linkStartTime, null);
							} catch (NullPointerException e) {

							}
							radiusIdx++;

						}
						if (path == null) {
							System.out.println("Cannot route " + t.h1_hhid + "_" + t.pax_id + "_" + t.trip_id + "\t"
									+ origCoord + "\t" + destCoord + "\t line: " + lines);
							break;
						}

						if (i == 0) {
							Walk walk = journey.addWalk();
							Coord boardCoord = path.nodes.get(0).getCoord();
							double walkDistanceAccessFromRouter = CoordUtils.calcEuclideanDistance(origCoord, boardCoord);

							double walkTimeAccessFromRouter = walkDistanceAccessFromRouter
									/ transitRouterConfig.getBeelineWalkSpeed();
							walk.setStartTime(linkStartTime);
							linkStartTime += walkTimeAccessFromRouter;
							walk.setEndTime(linkStartTime);
							walk.setDistance(walkDistanceAccessFromRouter);
							walk.setAccessWalk(true);
							substage_id++;

						}
						if (i > 0) {// in-between transitStage
							Coord boardCoord = path.nodes.get(0).getCoord();
							double interModalTransferDistance = CoordUtils.calcEuclideanDistance(walkOrigin, boardCoord);
							double interModalTransferTime = interModalTransferDistance
									/ transitRouterConfig.getBeelineWalkSpeed();
							Walk walk = journey.addWalk();
							walk.setStartTime(linkStartTime);
							walk.setDistance(interModalTransferDistance);
							substage_id++;
							linkStartTime += interModalTransferTime;
							walk.setEndTime(linkStartTime);
						}
						walkOrigin = path.nodes.get(path.nodes.size() - 1).getCoord();
						boolean inVehicle = false;

						for (int j = 0; j < path.links.size(); j++) {
							Link l = path.links.get(j);
							TransitRouterNetworkLink transitLink = (TransitRouterNetworkLink) l;
							if (transitLink.getRoute() != null) {
								// in line link
								if (!inVehicle) {
									inVehicle = true;
									Trip trip = journey.addTrip();
									tripsCount++;
									trip.setStartTime(linkStartTime);
									trip.setBoardingStop(journey.getWaits().getLast().getStopId());
									trip.setLine(transitLink.getLine().getId());
									trip.setRoute(transitLink.getRoute().getId());
									trip.setMode(getMode(ts.busStage, trip.getLine()));
									trip.setEndTime(linkStartTime);
									trip.setDistance(0);
									if (journey.getPossibleTransfer() != null) {
										Transfer transfer = journey.getPossibleTransfer();
										transfer.setToTrip(trip);
										transfer.setEndTime(linkStartTime);
										journey.addTransfer(transfer);
										transfersCount++; transfersCount++;
										journey.setPossibleTransfer(null);
									}
									substage_id++;
								}
								Trip trip = journey.getTrips().getLast();
								double linkLength = transitLink.getLength();
								trip.incrementDistance(linkLength);
								double linkTime = transitTravelFunction.getLinkTravelTime(transitLink, linkStartTime,
										null, null);
								trip.incrementTime(linkTime);
								linkStartTime += linkTime;
							} else if (transitLink.getToNode().route == null) {
								// transfer link
								double linkLength = transitLink.getLength();
								double linkTime = transitTravelFunction.getLinkTravelTime(transitLink, linkStartTime,
										null, null);
								Walk walk;
								if (substage_id > 1) {
									Trip trip = journey.getTrips().getLast();
									trip.setAlightingStop(l.getToNode().getId());
									Transfer transfer = new Transfer();
									journey.setPossibleTransfer(transfer);
									transfer.setFromTrip(trip);
									transfer.setStartTime(linkStartTime);
									walk = journey.addWalk();
									walk.setStartTime(linkStartTime);
									walk.setDistance(linkLength);
									transfer.getWalks().add(walk);
								} else {
									// this is still part of the access walk
									walk = journey.getWalks().getLast();
									walk.setDistance(walk.getDistance() + linkLength);
								}
								if (j + 2 <= path.links.size()) {
									substage_id = 1;
								} else {
									substage_id++;
								}
								inVehicle = false;
								linkStartTime += linkTime;
								walk.setEndTime(linkStartTime);

							} else if (transitLink.getFromNode().route == null) {
								// wait link
								substage_id++;
								double linkTime = transitTravelFunction.getLinkTravelTime(transitLink, linkStartTime,
										null, null);
								Wait wait = journey.addWait();
								if(journey.getPossibleTransfer() != null){
									journey.getPossibleTransfer().getWaits().add(wait);
								}
									
								wait.setStartTime(linkStartTime);
								wait.setStopId(l.getFromNode().getId());
								linkStartTime += linkTime;
								wait.setEndTime(linkStartTime);
								if (i == 0 && j == 0) {
									wait.setAccessWait(true);
								}

							} else
								throw new RuntimeException("Bad transit router link");
						}// end path traversal
						if (i + 1 == transitStages.size()) {
							Coord alightCoord = path.nodes.get(path.nodes.size() - 1).getCoord();
							substage_id++;

							double walkDistanceEgressFromRouter = CoordUtils.calcEuclideanDistance(alightCoord, destCoord);
							double walkTimeEgressFromRouter = walkDistanceEgressFromRouter
									/ transitRouterConfig.getBeelineWalkSpeed();
							Walk walk = journey.addWalk();
							walk.setStartTime(linkStartTime);
							walk.setEndTime(linkStartTime + walkTimeEgressFromRouter);
							walk.setDistance(walkDistanceEgressFromRouter);
							walk.setEgressWalk(true);
						}
					}
				}
			}
		}
		System.out.printf("a: %d j: %d, t: %d x: %d",actsCount,journeysCount,tripsCount,transfersCount);
	}

	public static void main(String[] args) throws Exception {
		HITSAnalyserPostgresqlSummary.createRouters(Arrays.copyOfRange(args, 3, 11), Boolean.parseBoolean(args[2]));
		System.out.println(new java.util.Date());
		HITSAnalyserPostgresqlSummary hp;
		System.out.println(args[0].equals("sql"));
		String fileName = "data/serial";
		DataBaseAdmin dba = new DataBaseAdmin(new File("data/matsim2postgres.properties"));
		Connection conn = dba.getConnection();
		System.out.println("Database connection established");
		HITSAnalyserPostgresqlSummary.setConn(conn);
		HITSAnalyserPostgresqlSummary.setZip2Coord(conn);

		if (args[0].equals("sql")) {
			// this section determines whether to write the long or short
			// serialized file, default is the full file
			HITSData h;
			if (args[1].equals("short")) {

				h = new HITSData(conn, true);
			} else {
				h = new HITSData(conn, false);
			}
			hp = new HITSAnalyserPostgresqlSummary(h);
			// object serialization
			fileName = fileName + (args[1].equals("short") ? "short" : "");
			FileOutputStream fos = new FileOutputStream(fileName);
			ObjectOutputStream oos = new ObjectOutputStream(fos);
			oos.writeObject(hp.hitsData);
			oos.flush();
			oos.close();

		} else {
			// load and deserialize
			FileInputStream fis;
			fileName = fileName + (args[1].equals("short") ? "short" : "");
			fis = new FileInputStream(fileName);
			ObjectInputStream ois = new ObjectInputStream(fis);
			hp = new HITSAnalyserPostgresqlSummary((HITSData) ois.readObject());
			ois.close();

		}
		//
		// hp.writePersonSummary();
		// System.out.println("wrote person summary");
		// hp.writeTripSummary();
		// System.out.println("wrote trip summary");
		// hp.jointTripSummary();
		// System.out.println("wrote joint trip summary");
		hp.compileTravellerChains(100, 100);
//		hp.compileAlternativeTravellerChains("car", 400);
		hp.writeSimulationResultsToSQL(new File("data/matsim2postgres.properties"), "");
		System.out.println("exiting...");
		System.out.println(new java.util.Date());
	}
}
