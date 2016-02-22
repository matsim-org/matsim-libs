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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.management.timer.Timer;

//import java.text.SimpleDateFormat;
//import java.util.ArrayList;
//import java.util.List;
//import java.util.TimeZone;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.network.algorithms.TransportModeNetworkFilter;
import org.matsim.core.population.MatsimPopulationReader;
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
import org.matsim.population.algorithms.XY2Links;
import org.matsim.pt.router.PreparedTransitSchedule;
import org.matsim.pt.router.TransitRouterImpl;
import org.matsim.pt.router.TransitRouterNetwork;
import org.matsim.pt.router.TransitRouterNetworkTravelTimeAndDisutility;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitScheduleReader;
import org.matsim.vehicles.Vehicle;

import others.sergioo.util.dataBase.DataBaseAdmin;
import playground.sergioo.hitsRouter2013.MultiNodeDijkstra;
import playground.sergioo.hitsRouter2013.TransitRouterVariableImpl;
import playground.sergioo.singapore2012.transitRouterVariable.TransitRouterNetworkTravelTimeAndDisutilityWW;
import playground.sergioo.singapore2012.transitRouterVariable.TransitRouterNetworkWW;
import playground.sergioo.singapore2012.transitRouterVariable.TransitRouterNetworkWW.TransitRouterNetworkLink;
import playground.sergioo.singapore2012.transitRouterVariable.waitTimes.WaitTimeStuckCalculator;
public class HITSAnalyser {

	private HITSData hitsData;
	static Scenario shortestPathCarNetworkOnlyScenario;
	private static NetworkImpl carFreeSpeedNetwork;
	private static NetworkImpl fullCongestedNetwork;
	private static HashMap<Integer, Integer> zip2DGP;
    private static Connection conn;

	public static Connection getConn() {
		return conn;
	}

	private static void setConn(Connection conn) {
		HITSAnalyser.conn = conn;
	}

	private java.util.Date referenceDate; // all dates were referenced against a
											// starting Date of 1st September,
											// 2008, 00:00:00 SGT
	private static HashMap<Integer, Coord> zip2Coord;

    private static PreProcessDijkstra preProcessData;
	private static LeastCostPathCalculator shortestCarNetworkPathCalculator;
	private static XY2Links xY2Links;
	static Map<Id, Link> links;
	private static Dijkstra carCongestedDijkstra;
	private static TransitRouterVariableImpl transitRouter;
    private static Scenario scenario;
	private static TransitRouterNetworkTravelTimeAndDisutilityWW transitTravelFunction;
    private static MyTransitRouterConfig transitRouterConfig;
	private static HashSet<TransitLine> mrtLines;
	private static HashSet<TransitLine> lrtLines;
    private static HashMap<String, Coord> mrtCoords;
	private static HashMap<String, Coord> lrtCoords;

    private static void createRouters(String[] args, boolean freeSpeedRouting) {
		scenario = ScenarioUtils
				.createScenario(ConfigUtils.loadConfig(args[0]));
		(new MatsimNetworkReader(scenario.getNetwork())).readFile(args[1]);
		if (!freeSpeedRouting)
			(new MatsimPopulationReader(scenario)).readFile(args[2]);
		(new TransitScheduleReader(scenario)).readFile(args[3]);
		double startTime = new Double(args[5]), endTime = new Double(args[6]), binSize = new Double(
				args[7]);
		WaitTimeStuckCalculator waitTimeCalculator = new WaitTimeStuckCalculator(
				scenario.getPopulation(), scenario.getTransitSchedule(),
				(int) binSize, (int) (endTime - startTime));
		TravelTimeCalculator travelTimeCalculator = TravelTimeCalculator.create(scenario.getNetwork(), scenario
				.getConfig().travelTimeCalculator());
		System.out.println("Loading events");
		if(!freeSpeedRouting){
			EventsManager eventsManager =
					EventsUtils.createEventsManager(scenario.getConfig());
			eventsManager.addHandler(waitTimeCalculator);
			eventsManager.addHandler(travelTimeCalculator);
			(new MatsimEventsReader(eventsManager)).readFile(args[4]);			
		}
		transitRouterConfig = new MyTransitRouterConfig(scenario.getConfig()
				.planCalcScore(), scenario.getConfig().plansCalcRoute(),
				scenario.getConfig().transitRouter(), scenario.getConfig()
						.vspExperimental());
        TransitRouterNetworkWW transitRouterNetwork = TransitRouterNetworkWW.createFromSchedule(
                scenario.getNetwork(), scenario.getTransitSchedule(),
                transitRouterConfig.getBeelineWalkConnectionDistance());
		TransitRouterNetwork transitScheduleRouterNetwork = TransitRouterNetwork.createFromSchedule(scenario.getTransitSchedule(), transitRouterConfig.getBeelineWalkConnectionDistance());
		PreparedTransitSchedule preparedTransitSchedule = new PreparedTransitSchedule(scenario.getTransitSchedule());
		transitTravelFunction = new TransitRouterNetworkTravelTimeAndDisutilityWW(transitRouterConfig, scenario.getNetwork(), transitRouterNetwork, travelTimeCalculator.getLinkTravelTimes(), waitTimeCalculator.getWaitTimes(), scenario.getConfig().travelTimeCalculator(), startTime, endTime, preparedTransitSchedule);
		transitRouter = new TransitRouterVariableImpl(transitRouterConfig,
				transitTravelFunction, transitRouterNetwork,
				scenario.getNetwork());
		TransitRouterNetworkTravelTimeAndDisutility routerNetworkTravelTimeAndDisutility = new TransitRouterNetworkTravelTimeAndDisutility(transitRouterConfig, preparedTransitSchedule);
        TransitRouterImpl transitScheduleRouter = new TransitRouterImpl(transitRouterConfig, preparedTransitSchedule,
                transitScheduleRouterNetwork, routerNetworkTravelTimeAndDisutility,
                routerNetworkTravelTimeAndDisutility);
		// get the set of mrt and lrt lines for special case routing
		mrtLines = new HashSet<>();
		lrtLines = new HashSet<>();
		Collection<TransitLine> lines = scenario.getTransitSchedule()
				.getTransitLines().values();
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
				if (Arrays.binarySearch(MRT_LINES, 0, 4, line.getId()
						.toString()) >= 0) {
					mrtLines.add(line);
					// get the mrt stops
					for (TransitRoute route : line.getRoutes().values()) {
						mrtStops.addAll(route.getStops());
					}

				}
				if (Arrays.binarySearch(LRT_LINES, 0, 4, line.getId()
						.toString()) >= 0) {
					lrtLines.add(line);
					for (TransitRoute route : line.getRoutes().values()) {
						lrtStops.addAll(route.getStops());
					}
				}
			}
		}
		for (TransitRouteStop stop : mrtStops) {
			mrtCoords.put(stop.getStopFacility().getName(), stop
					.getStopFacility().getCoord());
		}
		for (TransitRouteStop stop : lrtStops) {
			mrtCoords.put(stop.getStopFacility().getName(), stop
					.getStopFacility().getCoord());
		}

		// now for car
		TravelDisutility travelDisutility = new Builder( TransportMode.car, scenario.getConfig().planCalcScore() )
				.createTravelDisutility(travelTimeCalculator
						.getLinkTravelTimes());
		carCongestedDijkstra = new Dijkstra(scenario.getNetwork(),
				travelDisutility, travelTimeCalculator.getLinkTravelTimes());
		HashSet<String> modeSet = new HashSet<>();
		modeSet.add("car");
		carCongestedDijkstra.setModeRestriction(modeSet);
		fullCongestedNetwork = (NetworkImpl) scenario.getNetwork();
		// add a free speed network that is car only, to assign the correct
		// nodes to agents
		TransportModeNetworkFilter filter = new TransportModeNetworkFilter(
				scenario.getNetwork());
		HITSAnalyser.carFreeSpeedNetwork = NetworkImpl.createNetwork();
		HashSet<String> modes = new HashSet<>();
		modes.add(TransportMode.car);
		filter.filter(carFreeSpeedNetwork, modes);
		TravelDisutility travelMinCost = new TravelDisutility() {

			@Override
			public double getLinkTravelDisutility(Link link, double time,
					Person person, Vehicle vehicle) {
				return getLinkMinimumTravelDisutility(link);
			}

			@Override
			public double getLinkMinimumTravelDisutility(Link link) {
				return link.getLength()/link.getFreespeed();
			}
		};
		preProcessData = new PreProcessDijkstra();
		preProcessData.run(carFreeSpeedNetwork);
		TravelTime timeFunction = new TravelTime() {

			@Override
			public double getLinkTravelTime(Link link, double time,
					Person person, Vehicle vehicle) {
				return link.getLength() / link.getFreespeed();
			}
		};

		shortestCarNetworkPathCalculator = new Dijkstra(
				carFreeSpeedNetwork, travelMinCost, timeFunction,preProcessData );
		xY2Links = new XY2Links(carFreeSpeedNetwork, null);
	}

	private HITSAnalyser() throws ParseException {

		DateFormat outdfm = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		// outdfm.setTimeZone(TimeZone.getTimeZone("SGT"));
		referenceDate = outdfm.parse("2008-09-01 00:00:00");
	}

	private HITSAnalyser(HITSData h2) throws ParseException, SQLException {

		this();
		this.setHitsData(h2);
	}

	void setHitsData(HITSData hitsData) {
		this.hitsData = hitsData;
	}

	HITSData getHitsData() {
		return hitsData;
	}

	private static void initXrefs() throws SQLException {
		// fillZoneData();
		setZip2DGP(conn);
		setDGP2Zip(conn);
		setZip2Coord(conn);
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

	private static TimeAndDistance getCarFreeSpeedShortestPathTimeAndDistance(
            Coord startCoord, Coord endCoord) {
		double distance = 0;
		Node startNode = carFreeSpeedNetwork.getNearestNode(startCoord);
		Node endNode = carFreeSpeedNetwork.getNearestNode(endCoord);

		Path path = shortestCarNetworkPathCalculator.calcLeastCostPath(
				startNode, endNode, 0, null, null);
		for (Link l : path.links) {
			distance += l.getLength();
			
		}
		return new TimeAndDistance(path.travelTime, distance);
	}

	private static TimeAndDistance getCarCongestedShortestPathDistance(
            Coord startCoord, Coord endCoord, double time) {
		double distance = 0;

		Node startNode = carFreeSpeedNetwork.getNearestNode(startCoord);
		Node endNode = carFreeSpeedNetwork.getNearestNode(endCoord);

		Path path = carCongestedDijkstra.calcLeastCostPath(startNode, endNode,
				time, null, null);
		for (Link l : path.links) {
			distance += l.getLength();
		}

		return new TimeAndDistance(path.travelTime, distance);
	}

	private static double getStraightLineDistance(Coord startCoord,
                                                  Coord endCoord) {
		double x1 = startCoord.getX();
		double x2 = endCoord.getX();
		double y1 = startCoord.getY();
		double y2 = endCoord.getY();
		double xsq = Math.pow(x2 - x1, 2);
		double ysq = Math.pow(y2 - y1, 2);
		return (Math.sqrt(xsq + ysq));
	}

	private static void setZip2DGP(Connection conn) throws SQLException {
		// init the hashmap
		zip2DGP = new HashMap<>();
		Statement s;
		s = conn.createStatement();
		s.executeQuery("select zip, DGP from pcodes_zone_xycoords ;");
		ResultSet rs = s.getResultSet();
		// iterate through the resultset and populate the hashmap
		while (rs.next()) {
			zip2DGP.put(rs.getInt("zip"), rs.getInt("DGP"));
		}
	}

	private static void setZip2Coord(Connection conn) throws SQLException {
		// init the hashmap
		zip2Coord = new HashMap<>();
		Statement s;
		s = conn.createStatement();
		s.executeQuery("select zip, x_utm48n, y_utm48n from pcodes_zone_xycoords where x_utm48n is not null;");
		ResultSet rs = s.getResultSet();
		// iterate through the resultset and populate the hashmap
		// Scenario scenario =
		// ScenarioUtils.createScenario(ConfigUtils.createConfig());
		while (rs.next()) {
			try {
				zip2Coord.put(
						rs.getInt("zip"),
						new Coord(rs.getDouble("x_utm48n"), rs
								.getDouble("y_utm48n")));

			} catch (NullPointerException e) {
				System.out.println(rs.getInt("zip"));
			}
		}
	}

	private static void setDGP2Zip(Connection conn) throws SQLException {
		Statement s;
		s = conn.createStatement();
		// get the list of DGPs, and associate a list of postal codes with each
        HashMap<Integer, ArrayList<Integer>> DGP2Zip = new HashMap<>();
		s.executeQuery("select distinct DGP from pcodes_zone_xycoords where DGP is not null;");
		ResultSet rs = s.getResultSet();
		// iterate through the list of DGPs, create an arraylist for each, then
		// fill that arraylist with all its associated postal codes
        ArrayList<Integer> DGPs = new ArrayList<>();
		while (rs.next()) {
			ArrayList<Integer> zipsInDGP = new ArrayList<>();
			Statement zs1 = conn.createStatement();
			zs1.executeQuery(String.format(
					"select zip from pcodes_zone_xycoords where DGP= %d;",
					rs.getInt("DGP")));
			ResultSet zrs1 = zs1.getResultSet();
			// add the zip codes to the arraylist for this dgp
			int currDGP = rs.getInt("DGP");
			// add the current DGP to the list of valid DGPs
			DGPs.add(currDGP);
			while (zrs1.next()) {
				zipsInDGP.add(zrs1.getInt("zip"));
			}
			// add the DGP and list of zip codes to the hashmap
			zipsInDGP.trimToSize();
			DGP2Zip.put(currDGP, zipsInDGP);
		}
		DGPs.trimToSize();

	}

	private void getZip2SubDGP(Connection conn) throws SQLException {
		// init the hashmap
        HashMap<Integer, Integer> zip2SubDGP = new HashMap<>();
		Statement s;
		s = conn.createStatement();
		s.executeQuery("select " + "zip, SubDGP from pcodes_zone_xycoords "
				+ ";");
		ResultSet rs = s.getResultSet();
		// iterate through the resultset and populate the hashmap
		while (rs.next()) {
			zip2SubDGP.put(rs.getInt("zip"), rs.getInt("SubDGP"));

		}
	}

	public static Coord getZip2Coord(int zip) {
		try {

			return HITSAnalyser.zip2Coord.get(zip);
		} catch (NullPointerException ne) {
			return null;
		}
	}
	private double getValidStartTime(double startTime){
		while(startTime<0 || startTime>24*3600){
			if(startTime<0){
				startTime = 24*3600+startTime;
			}else if(startTime>24*3600){
				startTime = startTime-24*3600;
			}
		}
		return startTime;
	}
	private void createSQLSummary(Connection conn, double busSearchradius,
			double mrtSearchRadius, boolean writeTransitCoords) {
		// arb code for summary generation
		try {

				System.out
						.println("Starting summary : " + new java.util.Date());
				Statement s = conn.createStatement();
				s.execute("DROP TABLE IF EXISTS trip_summary_routed_refactored;");
				s.execute("CREATE TABLE trip_summary_routed_refactored (pax_idx VARCHAR(45), trip_idx VARCHAR(45),    "
						+ ""
						+ "totalWalkTime int,  totalTravelTime int, estTravelTime int, tripcount int, "
						+ "invehtime int, freeCarDistance double, calcEstTripTime double, mainmode varchar(20), "
						+ "timeperiod varchar(2), busDistance double, trainDistance double, "
						+ "busTrainDistance double,"
						+ "straightLineDistance double,"
						+ " origdgp int, destdgp int, "
						+ "congestedCarDistance double,"
						+ "congestedCarTime double,"
						+ "walkTimeFromRouter double, "
						+ "walkDistanceFromRouter double, "
						+ "waitTimeFromRouter double, "
						+ "transferTimeFromRouter double, "
						+ "transitInVehTimeFromRouter double, "
						+ "transitInVehDistFromRouter double, "
						+ "transitTotalTimeFromRouter double, "
						+ "transitTotalDistanceFromRouter double,"
						+ "stageChainSimple varchar(512));");
				s.execute("DROP TABLE IF EXISTS person_summary;");
				s.execute("CREATE TABLE person_summary (h1_hhid varchar(20), pax_idx VARCHAR(45), "
						+ "numberOfTrips int,  numberOfWalkStagesPax int, "
						+ "totalWalkTimePax double, actMainModeChainPax  varchar(512), actStageChainPax  varchar(512), "
						+ "actChainPax varchar(255))");
				if (writeTransitCoords) {
					s.execute("DROP TABLE IF EXISTS transit_coords_refactored;");
					s.execute("CREATE TABLE transit_coords_refactored ("
							+ "  id INT NOT NULL AUTO_INCREMENT"
							+ ", pax_idx VARCHAR(45)"
							+ ", trip_idx VARCHAR(45)" + ", stage_id int"
							+ ", substage_id int"
							+ ", plan_element_type VARCHAR(255)"
							+ ", x_utm48n_orig double"
							+ ", y_utm48n_orig double"
							+ ", x_utm48n_dest double"
							+ ", y_utm48n_dest double" + ", line VARCHAR(255)"
							+ ", route VARCHAR(255)" + ", distance double"
							+ ", duration double" + ", start_time double"
							+ ", end_time double" + ", PRIMARY KEY (id)" + ")");

				}
				ArrayList<HITSPerson> persons = this.getHitsData().getPersons();
				Logger.getLogger(MultiNodeDijkstra.class).setLevel(Level.INFO);
				PERSONS: for (HITSPerson p : persons) {

					String insertString = String
							.format("INSERT INTO person_summary VALUES(\'%s\',\'%s\',%d,%d,%f,\'%s\',\'%s\',\'%s\');",

							p.h1_hhid, p.pax_idx, p.numberOfTrips,
									p.numberOfWalkStagesPax,
									p.totalWalkTimePax, p.actMainModeChainPax,
									p.actStageChainPax, p.actChainPax);
					s.executeUpdate(insertString);
					ArrayList<HITSTrip> trips = p.getTrips();
					int tripcount = trips.size();
					for (HITSTrip t : trips) {
						double freeCarTripDistance = 0;
						double straightDistance = 0;
						double freeCarTripTime = 0;
						double congestedCarDistance = 0;
						double congestedCarTime = 0;
						double walkTimeTotalFromRouter = 0;
						double walkDistanceTotalFromRouter = 0;
						double walkDistanceAccessFromRouter = 0;
						double walkDistanceEgressFromRouter = 0;
						double walkDistanceTransferFromRouter = 0;
						double walkTimeAccessFromRouter = 0;
						double walkTimeEgressFromRouter = 0;
						double walkTimeTransferFromRouter = 0;
						double waitTimeAccessFromRouter = 0;
						double waitTimeTransferFromRouter = 0;
						double waitTimeTotalFromRouter = 0;
						double transferTimeTotalFromRouter = 0;
						double transitInVehTimeFromRouter = 0;
						double transitInVehDistFromRouter = 0;
						double transitTotalTimeFromRouter = 0;
						double transitTotalDistanceFromRouter = 0;
						Coord origCoord;
						Coord destCoord;
						double startTime = ((double) (t.t3_starttime_24h
								.getTime() - this.referenceDate.getTime()))
								/ (double) Timer.ONE_SECOND;

						
							origCoord = HITSAnalyser.zip2Coord
									.get(t.p13d_origpcode);
							destCoord = HITSAnalyser.zip2Coord
									.get(t.t2_destpcode);
						if(origCoord == null){
							System.out
							.println("Problem ZIP : " + t.p13d_origpcode);
							continue PERSONS;
						}else if(destCoord == null){
							System.out
							.println("Problem ZIP : " + t.t2_destpcode);
							continue PERSONS;
						}
						TimeAndDistance freeCarTimeAndDistance = HITSAnalyser
								.getCarFreeSpeedShortestPathTimeAndDistance(
										origCoord, destCoord);
							freeCarTripDistance = freeCarTimeAndDistance.distance;
							freeCarTripTime = freeCarTimeAndDistance.time;
							straightDistance = HITSAnalyser
									.getStraightLineDistance(origCoord,
											destCoord);
							congestedCarDistance = HITSAnalyser
									.getCarCongestedShortestPathDistance(
											origCoord, destCoord, getValidStartTime(startTime)).distance;
							congestedCarTime = HITSAnalyser
									.getCarCongestedShortestPathDistance(
											origCoord, destCoord, getValidStartTime(startTime)).time;
							
							// route transit-only trips using the transit router
							
							if (t.stageChainSimple.equals(t.stageChainTransit)) {
								Set<TransitLine> lines = new HashSet<>();
								Coord interimOrig = origCoord;
								Coord interimDest = destCoord;
								Path path = null;
								// deal with the 7 most common cases for now
								HITSStage stage = t.getStages().get(0);
								boolean busCheck = stage.t10_mode
										.equals("publBus");
								boolean mrtCheck = stage.t10_mode.equals("mrt");
								boolean lrtCheck = stage.t10_mode.equals("lrt");
								List<TransitStageRoutingInput> transitStages = new ArrayList<>();
								boolean doneCompilingTransitStages = false;
								STAGES: while (!doneCompilingTransitStages) {
									// System.out.println(p.pax_idx + "_"
									// + t.trip_id);
									if (busCheck) {
										lines.add(HITSAnalyser.scenario
												.getTransitSchedule()
												.getTransitLines()
												.get(Id.create(
														stage.t11_boardsvcstn,TransitLine.class)));
										if (stage.nextStage != null) {
											if (stage.nextStage.t10_mode
													.equals("publBus")) {
												stage = stage.nextStage;
												continue;
											} else {

												// going to an lrt or mrt
												// station
												busCheck = false;
												mrtCheck = stage.nextStage.t10_mode
														.equals("mrt");
												lrtCheck = stage.nextStage.t10_mode
														.equals("lrt");
												interimDest = mrtCheck ? mrtCoords
														.get(stage.nextStage.t11_boardsvcstn)
														: lrtCoords
																.get(stage.nextStage.t11_boardsvcstn);
											}
											transitStages
													.add(new TransitStageRoutingInput(
															interimOrig,
															interimDest, lines,
															true));
											lines = new HashSet<>();
											interimOrig = interimDest;
											interimDest = destCoord;
											stage = stage.nextStage;
											continue;
										}// next stage is null
										transitStages
												.add(new TransitStageRoutingInput(
														interimOrig,
														interimDest, lines,
														true));
										doneCompilingTransitStages = true;
									}// end of bus stage chain check
									if (mrtCheck || lrtCheck) {
										HashMap<String, Coord> theCoords = mrtCheck ? mrtCoords
												: lrtCoords;
										lines = mrtCheck ? mrtLines : lrtLines;
										interimOrig = theCoords
												.get(stage.t11_boardsvcstn);
										interimDest = theCoords
												.get(stage.t12_alightstn);
										transitStages
												.add(new TransitStageRoutingInput(
														interimOrig,
														interimDest, lines,
														false));
										if (stage.nextStage != null) {
											busCheck = stage.nextStage.t10_mode
													.equals("publBus");
											mrtCheck = stage.nextStage.t10_mode
													.equals("mrt");
											lrtCheck = stage.nextStage.t10_mode
													.equals("lrt");
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

								String insertString2 = "INSERT INTO transit_coords_refactored"
										+ "(pax_idx, trip_idx, stage_id, substage_id,"
										+ " plan_element_type, "
										+ "x_utm48n_orig, y_utm48n_orig, "
										+ "x_utm48n_dest, y_utm48n_dest, "
										+ "line, route, distance, "
										+ "duration, start_time, end_time)"
										+ " VALUES ";
								int stage_id = 0;
								PATHS: for (int i = 0; i < transitStages.size(); i++) {
									stage_id++;
									int substage_id = 0;
									TransitStageRoutingInput ts = transitStages
											.get(i);
									transitRouter.setAllowedLines(ts.lines);
//									transitScheduleRouter.setA
									double[] radiusFactors = { 1.0, 1.1, 1.25,
											1.5, 2, 2.5, 3, 5, 10, 25 };
									int radiusIdx = 0;
									path = null;
									linkStartTime=getValidStartTime(linkStartTime);
									while (path == null
											&& radiusIdx < radiusFactors.length) {
										double searchRadius = 100;
										if (ts.busStage) {
											searchRadius = busSearchradius * radiusFactors[radiusIdx];
										} else {
											searchRadius = mrtSearchRadius * radiusFactors[radiusIdx];
										}
										HITSAnalyser.transitRouterConfig
										.setSearchradius(searchRadius);
										try{
											System.out.println(ts.lines+ " orig: "+ ts.orig + " dest: " + ts.dest + " time:" + 
										linkStartTime + " searchRadius:" + searchRadius);
											path = transitRouter.calcPathRoute(
													ts.orig, ts.dest,
													linkStartTime, null);
										}catch(NullPointerException e){
											
										}
										radiusIdx++;

									}
									if (path == null) {
										System.out.println("Cannot route " + t.h1_hhid + "_"
												+ t.pax_id + "_" + t.trip_id
												+ "\t" + origCoord + "\t" + destCoord
												+ "\t line: " + lines);
										break;
									}

									if (i == 0) {
										Coord boardCoord = path.nodes.get(0)
												.getCoord();
										walkDistanceAccessFromRouter = CoordUtils
												.calcEuclideanDistance(origCoord, boardCoord);
										walkTimeAccessFromRouter = walkDistanceAccessFromRouter
												/ transitRouterConfig
														.getBeelineWalkSpeed();
										linkStartTime += walkTimeAccessFromRouter;
										substage_id++;
										insertString2 += String
												.format("(\'%s\', \'%s\', %d, %d, \'%s\',"
														+ " %f, %f, %f, %f, \'%s\', \'%s\',"
														+ " %f, %f, %f, %f),",
														t.h1_hhid + "_"
																+ t.pax_id,
														t.h1_hhid + "_"
																+ t.pax_id
																+ "_"
																+ t.trip_id,
														stage_id,
														substage_id,
														"accessWalk",
														origCoord.getX(),
														origCoord.getY(),
														boardCoord.getX(),
														boardCoord.getY(),
														"",
														"",
														walkDistanceAccessFromRouter,
														walkTimeAccessFromRouter,
														startTime,
														startTime
																+ walkTimeAccessFromRouter);
									}
									if (i > 0) {// in-between transitStage
										Coord boardCoord = path.nodes.get(0)
												.getCoord();
										double interModalTransferDistance = CoordUtils
												.calcEuclideanDistance(walkOrigin,
														boardCoord);
										double interModalTransferTime = interModalTransferDistance
												/ transitRouterConfig
														.getBeelineWalkSpeed();
										walkDistanceTransferFromRouter += interModalTransferDistance;
										walkTimeTransferFromRouter += interModalTransferTime;
										substage_id++;
										insertString2 += String
												.format("(\'%s\', \'%s\', %d, %d, \'%s\',"
														+ " %f, %f, %f, %f, \'%s\', \'%s\',"
														+ " %f, %f, %f, %f),",
														t.h1_hhid + "_"
																+ t.pax_id,
														t.h1_hhid + "_"
																+ t.pax_id
																+ "_"
																+ t.trip_id,
														stage_id,
														substage_id,
														"intermodalTransferWalk",
														walkOrigin.getX(),
														walkOrigin.getY(),
														boardCoord.getX(),
														boardCoord.getY(),
														"","",
														interModalTransferDistance,
														interModalTransferTime,
														linkStartTime,
														linkStartTime
																+ interModalTransferTime);


										linkStartTime += interModalTransferTime;
									}
									walkOrigin = path.nodes.get(
											path.nodes.size() - 1).getCoord();
									boolean inVehicle = false;

									for (int j = 0; j < path.links.size(); j++) {
										Link l = path.links.get(j);
										TransitRouterNetworkWW.TransitRouterNetworkLink transitLink = (TransitRouterNetworkLink) l;
										if (transitLink.getRoute() != null) {
											// in line link
											if (!inVehicle) {
												inVehicle = true;
												substage_id++;
											}
											double linkLength = transitLink
													.getLength();
											transitInVehDistFromRouter += linkLength;
											double linkTime = transitTravelFunction
													.getLinkTravelTime(
															transitLink,
															linkStartTime,
															null, null);
											insertString2 += String
													.format("(\'%s\', \'%s\', %d, %d, \'%s\',"
															+ " %f, %f, %f, %f, \'%s\', \'%s\',"
															+ " %f, %f, %f, %f),",
															t.h1_hhid + "_"
																	+ t.pax_id,
															t.h1_hhid + "_"
																	+ t.pax_id
																	+ "_"
																	+ t.trip_id,
															stage_id,
															substage_id,
															(ts.busStage ? "bus"
																	: "mrt_lrt"),
															transitLink.getFromNode().getCoord().getX(),
															transitLink.getFromNode().getCoord().getY(),
															transitLink.getToNode()
																	.getCoord()
																	.getX(),
															transitLink.getToNode()
																	.getCoord()
																	.getY(),
															transitLink
																	.getLine()
																	.getId(),
															transitLink
																	.getRoute()
																	.getId(),
															linkLength,
															linkTime,
															linkStartTime,
															linkStartTime+linkTime);
											linkStartTime += linkTime;
											transitInVehTimeFromRouter += linkTime;
										} else if (transitLink.getToNode().route == null) {
											// transfer link
											double linkLength = transitLink
													.getLength();
											walkDistanceTransferFromRouter += linkLength;
											double linkTime = transitTravelFunction
													.getLinkTravelTime(
															transitLink,
															linkStartTime,
															null, null);
											if(j+2<=path.links.size()){
												stage_id++;
												substage_id = 1;												
											}else{
												substage_id++;
											}
											inVehicle = false;
											insertString2 += String
													.format("(\'%s\', \'%s\', %d, %d, \'%s\',"
															+ " %f, %f, %f, %f, \'%s\', \'%s\',"
															+ " %f, %f, %f, %f),",
															t.h1_hhid + "_"
																	+ t.pax_id,
															t.h1_hhid + "_"
																	+ t.pax_id
																	+ "_"
																	+ t.trip_id,
															stage_id,
															substage_id,
															"transferWalk",
															transitLink.getFromNode()
																	.getCoord()
																	.getX(),
															transitLink.getFromNode()
																	.getCoord()
																	.getY(),
															transitLink.getToNode()
																	.getCoord()
																	.getX(),
															transitLink.getToNode()
																	.getCoord()
																	.getY(),
															"",
															"",
															linkLength,
															linkTime,
															linkStartTime,
															linkStartTime+linkTime);

											linkStartTime += linkTime;
											walkTimeTransferFromRouter += linkTime;

										} else if (transitLink.getFromNode().route == null) {
											// wait link
											substage_id++;
											double linkTime = transitTravelFunction
													.getLinkTravelTime(
															transitLink,
															 linkStartTime,
															null, null);
											insertString2 += String
													.format("(\'%s\', \'%s\', %d, %d, \'%s\',"
															+ " %f, %f, %f, %f, \'%s\', \'%s\',"
															+ " %f, %f, %f, %f),",
															t.h1_hhid + "_"
																	+ t.pax_id,
															t.h1_hhid + "_"
																	+ t.pax_id
																	+ "_"
																	+ t.trip_id,
															stage_id,
															substage_id,
															"wait",
															transitLink.getFromNode()
																	.getCoord()
																	.getX(),
															transitLink.getFromNode()
																	.getCoord()
																	.getY(),
															transitLink.getToNode()
																	.getCoord()
																	.getX(),
															transitLink.getToNode()
																	.getCoord()
																	.getY(),
															"",
															"",
															0.0,
															linkTime,
															linkStartTime,
															linkStartTime+linkTime);
											linkStartTime += linkTime;
											if (i == 0 && j == 0) {
												waitTimeAccessFromRouter += linkTime;
											} else {
												waitTimeTransferFromRouter += linkTime;
											}

										} else
											throw new RuntimeException(
													"Bad transit router link");
									}// end path traversal
									if (i + 1 == transitStages.size()) {
										Coord alightCoord = path.nodes.get(
												path.nodes.size() - 1)
												.getCoord();
										substage_id++;

										walkDistanceEgressFromRouter = CoordUtils
												.calcEuclideanDistance(alightCoord, destCoord);
										walkTimeEgressFromRouter = walkDistanceEgressFromRouter
												/ transitRouterConfig
														.getBeelineWalkSpeed();
										insertString2 += String
												.format("(\'%s\', \'%s\', %d, %d, \'%s\',"
														+ " %f, %f, %f, %f, \'%s\', \'%s\',"
														+ " %f, %f, %f, %f)",
														t.h1_hhid + "_"
																+ t.pax_id,
														t.h1_hhid + "_"
																+ t.pax_id
																+ "_"
																+ t.trip_id,
														stage_id,
														substage_id,
														"egressWalk",
														alightCoord.getX(),
														alightCoord.getY(),
														destCoord.getX(),
														destCoord.getY(),
														"",
														"",
														walkDistanceEgressFromRouter,
														walkTimeEgressFromRouter,
														linkStartTime,
														linkStartTime+walkTimeEgressFromRouter);
										s.executeUpdate(insertString2);
									}
								}
								walkDistanceTotalFromRouter = walkDistanceAccessFromRouter
										+ walkDistanceEgressFromRouter
										+ walkDistanceTransferFromRouter;
								walkTimeTotalFromRouter = walkTimeAccessFromRouter
										+ walkTimeEgressFromRouter
										+ walkTimeTransferFromRouter;
								waitTimeTotalFromRouter = waitTimeAccessFromRouter
										+ waitTimeTransferFromRouter;
								transferTimeTotalFromRouter = walkTimeTransferFromRouter
										+ waitTimeTransferFromRouter;
								transitTotalTimeFromRouter = transitInVehTimeFromRouter
										+ waitTimeTotalFromRouter
										+ walkTimeTotalFromRouter;
								transitTotalDistanceFromRouter = transitInVehDistFromRouter
										+ walkDistanceTotalFromRouter;

							}
						
						String pax_idx = t.h1_hhid + "_" + t.pax_id;
						String trip_idx = t.h1_hhid + "_" + t.pax_id + "_"
								+ t.trip_id;
						// init timePeriod
						String timeperiod = "OP";
						if (startTime > 7.5 && startTime <= 9.5)
							timeperiod = "AM";
						if (startTime > 17.5 && startTime <= 19.5)
							timeperiod = "PM";

						insertString = String
								.format("INSERT INTO trip_summary_routed_refactored VALUES(\'%s\',\'%s\',%d,%d,%d,%d,%d,%f,%f,\'%s\',\'%s\',%f,%f,%f,%f,%d,%d,%f,%f,%f,%f,%f,%f,%f,%f,%f,%f,\'%s\');",
										pax_idx, trip_idx, t.totalWalkTimeTrip,
										t.calculatedJourneyTime,
										t.estimatedJourneyTime, tripcount,
										t.inVehTimeTrip, freeCarTripDistance,
										freeCarTripTime, t.mainmode,
										timeperiod, t.busDistance,
										t.trainDistance, t.busTrainDistance,
										straightDistance,
										HITSAnalyser.zip2DGP.get(t.p13d_origpcode),
										HITSAnalyser.zip2DGP.get(t.t2_destpcode),
										congestedCarDistance,
										congestedCarTime,
										walkTimeTotalFromRouter,
										walkDistanceTotalFromRouter,
										waitTimeTotalFromRouter,
										transferTimeTotalFromRouter,
										transitInVehTimeFromRouter,
										transitInVehDistFromRouter,
										transitTotalTimeFromRouter,
										transitTotalDistanceFromRouter,
										t.stageChainSimple);
						s.executeUpdate(insertString);
					}

				}


		} catch (SQLException e) {

			e.printStackTrace();
		} 

	}




	public void jointTripSummary() {

		/*
		 * this class aims to construct the following table h1_hhid drvId
		 * drvTripId drvMainMode psgrId psgrTripId psgrTotalTripStages
		 * psgrStageId psgrPrevMode psgrNextMode psgrMainMode drvPrevAct
		 * drvNextAct psgrPrevAct psgrNextAct
		 */
		Statement s;
		try {
			s = conn.createStatement();
			s.execute("DROP TABLE IF EXISTS jointTrips;");
			s.execute("CREATE TABLE jointTrips("
					+ "h1_hhid varchar(15),"
					+ "drvId int, drvTripId int, drvMainMode varchar(15),"
					+ "psgrId int, psgrTripId int, psgrTotalTripStages int,"
					+ "psgrStageId int, psgrPrevMode varchar(15), psgrNextMode varchar(15), psgrMainMode varchar(15),"
					+ "psgrOrigZip int, psgrDestZip int, "
					+ "drvPrevAct varchar(15), drvNextAct varchar(15), psgrPrevAct varchar(15), psgrNextAct varchar(15), "
					+ "psgrPrevPlace varchar(15), psgrNextPlace varchar(15), tripType varchar(20)"
					+ ");");
			for (HITSHousehold hh : this.hitsData.getHouseholds()) {
				String hhid = hh.h1_hhid;
				ArrayList<HITSStage> psgrStages = new ArrayList<>();
				ArrayList<HITSTrip> dropOffTrips = new ArrayList<>();
				for (HITSStage stage : hh.getStages()) {
					// first find all the passenger stages
					if (stage.t10_mode.endsWith("Psgr"))
						psgrStages.add(stage);
				}
				for (HITSTrip trip : hh.getTrips()) {
					// if (trip.t6_purpose.equals("pikUpDrop") &&
					// trip.mainmode.endsWith("Drv"))
					if (trip.mainmode != null && trip.mainmode.endsWith("Drv"))
						dropOffTrips.add(trip);
				}
				for (final HITSStage stage : psgrStages) {
					// get all information on the passenger trip
					int drvTripId = -1;
					String drvMainMode = null;
					int psgrId = stage.pax_id;
					int drvId = -1;
					final int psgrTripId = stage.trip_id;
					int psgrTotalTripStages = stage.trip.getStages().size();
					int psgrStageId = stage.stage_id;
					int psgrOrigZip = -1;
					int psgrDestZip = -1;
					String psgrPrevMode = stage.stage_id > 1 ? stage.trip
							.getStages().get(stage.stage_id - 2).t10_mode
							: null;
					String psgrNextMode = psgrStageId < psgrTotalTripStages ? stage.trip
							.getStages().get(stage.stage_id).t10_mode : null;
					String psgrMainMode = stage.trip.mainmode;
					String drvPrevAct = null;
					String drvNextAct = null;
					// find the next and previous passenger
					// activity purpose
					String psgrPrevAct = null;
					String psgrNextAct = null;
					String psgrPrevPlace = null;
					String psgrNextPlace = null;
					String tripType = null;
					ArrayList<HITSTrip> psgrTrips = stage.trip.person
							.getTrips();
					if (psgrTripId > 1) {
						psgrPrevAct = psgrTrips.get(psgrTripId - 2).t6_purpose;
						psgrPrevPlace = psgrTrips.get(psgrTripId - 2).t5_placetype;
					} else {
						psgrPrevAct = "home";
						psgrPrevPlace = "res";
					}
					psgrNextAct = stage.trip.t6_purpose;
					psgrNextPlace = stage.trip.t5_placetype;
					psgrOrigZip = stage.trip.p13d_origpcode;
					psgrDestZip = stage.trip.t2_destpcode;
					// now, find the driver
					// (s)he will be heading where i'm heading, at the same
					// time, on a pikupDrop
					for (HITSTrip trip : dropOffTrips) {
						// sometimes, person can be a passenger as well as a
						// driver
						if (trip.person.pax_id == psgrId)
							break;
						HITSTrip prevTrip = null;
						if (trip.trip_id > 1)
							prevTrip = trip.person.getTrips().get(
									trip.trip_id - 2);

						// check for pickup first
						boolean tripHandled = false;
						// if (prevTrip != null
						// && prevTrip.mainmode == "carDrv"
						// && trip.getStages().get(0).t12a_paxinveh > 1) {
						// if ((trip.p13d_origpcode == psgrOrigZip &&
						// trip.t2_destpcode == psgrDestZip)
						// && (Math.abs(nextTrip.t4_endtime_24h
						// .getTime()
						// - stage.trip.t4_endtime_24h
						// .getTime())) < Timer.ONE_MINUTE * 5L) {
						// if (prevTrip.t6_purpose.equals("pikUpDrop"))
						// tripType = "pickUp";
						// else
						// tripType = "accomp1";
						// drvId = trip.pax_id;
						// drvMainMode = trip.mainmode;
						// drvTripId = trip.trip_id;
						// drvNextAct = trip.t6_purpose;
						// if (prevTrip != null) {
						// drvPrevAct = prevTrip.t6_purpose;
						// }
						// // drvPrevAct = trip.t6_purpose;
						// // we're all done with this trip
						// tripHandled = true;
						// }
						// }
						// if (tripHandled)
						// break;
						// if (trip.p13d_origpcode == psgrOrigZip
						// && ((Math
						// .abs(trip.t3_starttime_24h.getTime()
						// - stage.trip.t3_starttime_24h
						// .getTime())) < Timer.ONE_MINUTE * 5L)
						// && trip.getStages().get(0).t12a_paxinveh > 1) {
						// if (trip.t6_purpose.equals("pikUpDrop"))
						// tripType = "dropOff";
						// else
						// tripType = "accomp2";
						// drvId = trip.pax_id;
						// drvMainMode = trip.mainmode;
						// drvTripId = trip.trip_id;
						// if (nextTrip != null)
						// drvNextAct = nextTrip.t6_purpose;
						// else
						// drvNextAct = "home";
						// // if (prevTrip != null) {
						// // drvPrevAct = prevTrip.t6_purpose;
						// // } else {
						// // drvPrevAct = "home";
						// // }
						// drvPrevAct = trip.t6_purpose;
						// // we're all done with this trip
						// tripHandled = true;
						// }
						// if (tripHandled)
						// break;

						if ((trip.p13d_origpcode == psgrOrigZip
								&& trip.t2_destpcode == psgrDestZip && trip
								.getStages().get(0).t12a_paxinveh > 1)
								&& (((Math.abs(trip.t4_endtime_24h.getTime()
										- stage.trip.t4_endtime_24h.getTime())) < Timer.ONE_MINUTE * 5L) || ((Math
										.abs(trip.t3_starttime_24h.getTime()
												- stage.trip.t3_starttime_24h
														.getTime())) < Timer.ONE_MINUTE * 5L))) {
							drvId = trip.pax_id;
							drvMainMode = trip.mainmode;
							drvTripId = trip.trip_id;
							drvNextAct = trip.t6_purpose;
							if (prevTrip != null) {
								drvPrevAct = prevTrip.t6_purpose;
							} else {
								drvPrevAct = "home";
							}
							if (drvPrevAct.equals("pikUpDrop")) {
								if (drvNextAct.equals("pikUpDrop")) {
									tripType = "pickUp->dropOff";
								} else if (drvNextAct.equals(psgrNextAct)) {
									tripType = "pickUp->JointAct";
								} else {
									tripType = "pickUp->XXX";
								}
							} else if (drvPrevAct.equals(psgrPrevAct)) {
								if (drvNextAct.equals("pikUpDrop")) {
									tripType = "jointAct->dropOff";
								} else if (drvNextAct.equals(psgrNextAct)) {
									tripType = "jointAct->jointAct";
								} else {
									tripType = "jointAct->XXX";
								}
							} else {
								if (drvNextAct.equals("pikUpDrop")) {
									tripType = "XXX->dropOff";
								} else if (drvNextAct.equals(psgrNextAct)) {
									tripType = "XXX->JointAct";
								} else {
									tripType = "XXX->XXX";
								}
							}
							// drvPrevAct = trip.t6_purpose;
							// we're all done with this trip
							tripHandled = true;

						} else if ((trip.p13d_origpcode == psgrOrigZip && trip
								.getStages().get(0).t12a_paxinveh > 1)
								&& (((Math
										.abs(trip.t3_starttime_24h.getTime()
												- stage.trip.t3_starttime_24h
														.getTime())) < Timer.ONE_MINUTE * 5L))) {
							drvId = trip.pax_id;
							drvMainMode = trip.mainmode;
							drvTripId = trip.trip_id;
							drvNextAct = trip.t6_purpose;
							if (prevTrip != null) {
								drvPrevAct = prevTrip.t6_purpose;
							} else {
								drvPrevAct = "home";
							}
							if (drvPrevAct.equals("pikUpDrop")) {
								if (drvNextAct.equals("pikUpDrop")) {
									tripType = "pickUp->dropOff";
								} else if (drvNextAct.equals(psgrNextAct)) {
									tripType = "pickUp->JointAct";
								} else {
									tripType = "pickUp->XXX";
								}
							} else if (drvPrevAct.equals(psgrPrevAct)) {
								if (drvNextAct.equals("pikUpDrop")) {
									tripType = "jointAct->dropOff";
								} else if (drvNextAct.equals(psgrNextAct)) {
									tripType = "jointAct->jointAct";
								} else {
									tripType = "jointAct->XXX";
								}
							} else {
								if (drvNextAct.equals("pikUpDrop")) {
									tripType = "XXX->dropOff";
								} else if (drvNextAct.equals(psgrNextAct)) {
									tripType = "XXX->JointAct";
								} else {
									tripType = "XXX->XXX";
								}
							}
							// drvPrevAct = trip.t6_purpose;
							// we're all done with this trip
							tripHandled = true;

						} else if ((trip.t2_destpcode == psgrDestZip && trip
								.getStages().get(0).t12a_paxinveh > 1)
								&& (((Math.abs(trip.t4_endtime_24h.getTime()
										- stage.trip.t4_endtime_24h.getTime())) < Timer.ONE_MINUTE * 5L))) {
							drvId = trip.pax_id;
							drvMainMode = trip.mainmode;
							drvTripId = trip.trip_id;
							drvNextAct = trip.t6_purpose;
							if (prevTrip != null) {
								drvPrevAct = prevTrip.t6_purpose;
							} else {
								drvPrevAct = "home";
							}
							if (drvPrevAct.equals("pikUpDrop")) {
								if (drvNextAct.equals("pikUpDrop")) {
									tripType = "pickUp->dropOff";
								} else if (drvNextAct.equals(psgrNextAct)) {
									tripType = "pickUp->JointAct";
								} else {
									tripType = "pickUp->XXX";
								}
							} else if (drvPrevAct.equals(psgrPrevAct)) {
								if (drvNextAct.equals("pikUpDrop")) {
									tripType = "jointAct->dropOff";
								} else if (drvNextAct.equals(psgrNextAct)) {
									tripType = "jointAct->jointAct";
								} else {
									tripType = "jointAct->XXX";
								}
							} else {
								if (drvNextAct.equals("pikUpDrop")) {
									tripType = "XXX->dropOff";
								} else if (drvNextAct.equals(psgrNextAct)) {
									tripType = "XXX->JointAct";
								} else {
									tripType = "XXX->XXX";
								}
							}
							// drvPrevAct = trip.t6_purpose;
							// we're all done with this trip
							tripHandled = true;

						}

						if (tripHandled)
							break;

					}

					String insertString = String
							.format("INSERT INTO jointTrips VALUES(\'%s\',"
									+ "%d, %d,\'%s\',"
									+ "%d, %d, %d,"
									+ "%d, \'%s\', \'%s\', \'%s\', "
									+ "%d, %d,"
									+ "\'%s\', \'%s\', \'%s\', \'%s\',\'%s\', \'%s\', \'%s\'"
									+ ");", hhid, drvId, drvTripId,
									drvMainMode, psgrId, psgrTripId,
									psgrTotalTripStages, psgrStageId,
									psgrPrevMode, psgrNextMode, psgrMainMode,
									psgrOrigZip, psgrDestZip, drvPrevAct,
									drvNextAct, psgrPrevAct, psgrNextAct,
									psgrPrevPlace, psgrNextPlace, tripType);
					s.executeUpdate(insertString);
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	public static void main(String[] args) throws Exception {
		HITSAnalyser.createRouters(Arrays.copyOfRange(args, 3, 11),Boolean.parseBoolean(args[2]));
		System.out.println(new java.util.Date());
		HITSAnalyser hp;
		System.out.println(args[0].equals("sql"));
		String fileName = "data/serial";
		DataBaseAdmin dba = new DataBaseAdmin(
				new File("data/hitsdb.properties"));
		Connection conn = dba.getConnection();
		System.out.println("Database connection established");
		HITSAnalyser.setConn(conn);
		HITSAnalyser.initXrefs();

		if (args[0].equals("sql")) {
			// this section determines whether to write the long or short
			// serialized file, default is the full file
			HITSData h;
			if (args[1].equals("short")) {

				h = new HITSData(conn, true);
			} else {
				h = new HITSData(conn, false);
			}
			hp = new HITSAnalyser(h);
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
			hp = new HITSAnalyser((HITSData) ois.readObject());
			ois.close();

		}
		//
		// hp.writePersonSummary();
		// System.out.println("wrote person summary");
		// hp.writeTripSummary();
		// System.out.println("wrote trip summary");
		// hp.jointTripSummary();
		// System.out.println("wrote joint trip summary");
		Logger theLog = Logger.getLogger(MultiNodeDijkstra.class);
		theLog.setLevel(Level.INFO);
		hp.createSQLSummary(conn, 100, 100, true);

		System.out.println("exiting...");
		System.out.println(new java.util.Date());
	}
}



