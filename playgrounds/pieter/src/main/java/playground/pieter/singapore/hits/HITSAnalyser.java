package playground.pieter.singapore.hits;

//import java.text.SimpleDateFormat;
//import java.util.ArrayList;
//import java.util.List;
//import java.util.TimeZone;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.management.timer.Timer;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.router.AStarEuclidean;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;
import org.matsim.core.router.util.PreProcessEuclidean;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.population.algorithms.XY2Links;
import org.matsim.vehicles.Vehicle;

public class HITSAnalyser {

	private HITSData hitsData;
	static Scenario scenario;
	static NetworkImpl network;
	private static HashMap<Integer, Integer> zip2DGP;
	private HashMap<Integer, Integer> zip2SubDGP;
	private static HashMap<Integer, ArrayList<Integer>> DGP2Zip;
	private HashMap<Integer, HashMap<String, ArrayList<Integer>>> DGP2Type2Zip;
	static Connection conn;

	public static Connection getConn() {
		return conn;
	}

	public static void setConn(Connection conn) {
		HITSAnalyser.conn = conn;
	}

	private java.util.Date referenceDate; // all dates were referenced against a
											// starting Date of 1st September,
											// 2008, 00:00:00 SGT
	static HashMap<Integer, Coord> zip2Coord;

	private int dgpErrCount;
	private static ArrayList<Integer> DGPs;

	static PreProcessEuclidean preProcessData;
	private static LeastCostPathCalculator leastCostPathCalculator;
	static XY2Links xY2Links;
	static Map<Id, Link> links;

	/**
	 * pre-processes the network for astar, and associating acts with links
	 */
	static {
		preProcessNetwork();

	}

	static void preProcessNetwork() {
		HITSAnalyser.scenario = ScenarioUtils.createScenario(ConfigUtils
				.createConfig());
		new MatsimNetworkReader(scenario)
				.readFile("f:/workspace/krakatauPG/input/singapore1_no_rail_CLEAN.xml");
		HITSAnalyser.network = (NetworkImpl) scenario.getNetwork();

		TravelDisutility travelMinCost = new TravelDisutility() {


			@Override
			public double getLinkTravelDisutility(Link link, double time,
					Person person, Vehicle vehicle) {
				// TODO Auto-generated method stub
				return getLinkMinimumTravelDisutility(link);
			}

			@Override
			public double getLinkMinimumTravelDisutility(Link link) {
				// TODO Auto-generated method stub
				return link.getLength();
			}
		};
		preProcessData = new PreProcessEuclidean(travelMinCost);
		preProcessData.run(network);
		TravelTime timeFunction = new TravelTime() {

			public double getLinkTravelTime(Link link, double time) {
				return link.getLength() / link.getFreespeed();
			}
		};

		leastCostPathCalculator = new AStarEuclidean(network, preProcessData,
				timeFunction);
		xY2Links = new XY2Links(network);
		links = network.getLinks();
	}

	public HITSAnalyser() throws ParseException {

		DateFormat outdfm = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		// outdfm.setTimeZone(TimeZone.getTimeZone("SGT"));
		referenceDate = outdfm.parse("2008-09-01 00:00:00");
	}

	public HITSAnalyser(HITSData h2) throws ParseException, SQLException {

		this();
		this.setHitsData(h2);
	}

	public void setHitsData(HITSData hitsData) {
		this.hitsData = hitsData;
	}

	public HITSData getHitsData() {
		return hitsData;
	}

	public static void initXrefs() throws SQLException {
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

	public static double getShortestPathDistance(Coord startCoord,
			Coord endCoord) {
		double distance = 0;
		Node startNode = network.getNearestNode(startCoord);
		Node endNode = network.getNearestNode(endCoord);

		Path path = leastCostPathCalculator.calcLeastCostPath(startNode,
				endNode, 0,null,null);
		for (Link l : path.links) {
			distance += l.getLength();
		}
		return distance;
	}

	public static double getStraightLineDistance(Coord startCoord,
			Coord endCoord) {
		double x1 = startCoord.getX();
		double x2 = endCoord.getX();
		double y1 = startCoord.getY();
		double y2 = endCoord.getY();
		double xsq = Math.pow(x2 - x1, 2);
		double ysq = Math.pow(y2 - y1, 2);
		return (Math.sqrt(xsq + ysq));
	}

	public static double getShortestPathTime(Coord startCoord, Coord endCoord) {
		double travelTime = 0;
		Node startNode = network.getNearestNode(startCoord);
		Node endNode = network.getNearestNode(endCoord);

		Path path = leastCostPathCalculator.calcLeastCostPath(startNode, endNode, 0, null, null);
		for (Link l : path.links) {
			travelTime += l.getLength() / (l.getFreespeed() / 3.6);
		}
		return travelTime;
	}

	static void setZip2DGP(Connection conn) throws SQLException {
		// init the hashmap
		zip2DGP = new HashMap<Integer, Integer>();
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
		zip2Coord = new HashMap<Integer, Coord>();
		Statement s;
		s = conn.createStatement();
		s.executeQuery("select zip, x_utm48n, y_utm48n from pcodes_zone_xycoords where x_utm48n is not null;");
		ResultSet rs = s.getResultSet();
		// iterate through the resultset and populate the hashmap

		while (rs.next()) {
			try {
				zip2Coord.put(
						rs.getInt("zip"),
						scenario.createCoord(rs.getDouble("x_utm48n"),
								rs.getDouble("y_utm48n")));

			} catch (NullPointerException e) {
				System.out.println(rs.getInt("zip"));
			}
		}
	}

	static void setDGP2Zip(Connection conn) throws SQLException {
		Statement s;
		s = conn.createStatement();
		// get the list of DGPs, and associate a list of postal codes with each
		DGP2Zip = new HashMap<Integer, ArrayList<Integer>>();
		s.executeQuery("select distinct DGP from pcodes_zone_xycoords where DGP is not null;");
		ResultSet rs = s.getResultSet();
		// iterate through the list of DGPs, create an arraylist for each, then
		// fill that arraylist with all its associated postal codes
		DGPs = new ArrayList<Integer>();
		while (rs.next()) {
			ArrayList<Integer> zipsInDGP = new ArrayList<Integer>();
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
		this.zip2SubDGP = new HashMap<Integer, Integer>();
		Statement s;
		s = conn.createStatement();
		s.executeQuery("select " + "zip, SubDGP from pcodes_zone_xycoords "
				+ ";");
		ResultSet rs = s.getResultSet();
		// iterate through the resultset and populate the hashmap
		while (rs.next()) {
			this.zip2SubDGP.put(rs.getInt("zip"), rs.getInt("SubDGP"));

		}
	}

	public static Coord getZip2Coord(int zip) {
		try {

			return HITSAnalyser.zip2Coord.get(zip);
		} catch (NullPointerException ne) {
			return null;
		}
	}

	private void createSQLSummary(Connection conn) {
		// arb code for summary generation
		try {
			boolean again = true;
			while (again) {
				System.out
						.println("Starting summary : " + new java.util.Date());
				Statement s = conn.createStatement();
				s.execute("DROP TABLE IF EXISTS trip_summary;");
				s.execute("CREATE TABLE trip_summary (pax_idx VARCHAR(45), trip_idx VARCHAR(45),   "
						+ "totalWalkTime int,  totalTravelTime int, estTravelTime int, tripcount int, "
						+ "invehtime int, estTripDistance double, calcEstTripTime double, mainmode varchar(20), "
						+ "timeperiod varchar(2), busDistance double, trainDistance double, "
						+ "busTrainDistance double,"
						+ "straightLineDistance double,"
						+ " origdgp int, destdgp int);");
				s.execute("DROP TABLE IF EXISTS person_summary;");
				s.execute("CREATE TABLE person_summary (h1_hhid varchar(20), pax_idx VARCHAR(45), "
						+ "numberOfTrips int,  numberOfWalkStagesPax int, "
						+ "totalWalkTimePax double, actMainModeChainPax  varchar(512), actStageChainPax  varchar(512), "
						+ "actChainPax varchar(255))");
				ArrayList<HITSPerson> persons = this.getHitsData().getPersons();
				for (HITSPerson p : persons) {

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
						double tripDistance = 0;
						double straightDistance = 0;
						double tripTime = 0;
						try {
							tripDistance = HITSAnalyser
									.getShortestPathDistance(
											HITSAnalyser.zip2Coord
													.get(t.p13d_origpcode),
											HITSAnalyser.zip2Coord
													.get(t.t2_destpcode));
							tripTime = HITSAnalyser.getShortestPathTime(
									HITSAnalyser.zip2Coord
											.get(t.p13d_origpcode),
									HITSAnalyser.zip2Coord.get(t.t2_destpcode));
							straightDistance = HITSAnalyser
									.getStraightLineDistance(
											HITSAnalyser.zip2Coord
													.get(t.p13d_origpcode),
											HITSAnalyser.zip2Coord
													.get(t.t2_destpcode));
						} catch (NullPointerException e) {
							System.out
									.println("Problem ZIPs : "
											+ t.p13d_origpcode + " , "
											+ t.t2_destpcode);
							continue;
						}
						String pax_idx = t.h1_hhid + "_" + t.pax_id;
						String trip_idx = t.h1_hhid + "_" + t.pax_id + "_"
								+ t.trip_id;
						// init timePeriod
						String timeperiod = "OP";
						double startTime = ((double) (t.t3_starttime_24h
								.getTime() - this.referenceDate.getTime()))
								/ (double) Timer.ONE_HOUR;
						if (startTime > 7.5 && startTime <= 9.5)
							timeperiod = "AM";
						if (startTime > 17.5 && startTime <= 19.5)
							timeperiod = "PM";

						insertString = String
								.format("INSERT INTO trip_summary VALUES(\'%s\',\'%s\',%d,%d,%d,%d,%d,%f,%f,\'%s\',\'%s\',%f,%f,%f,%f,%d,%d);",
										pax_idx, trip_idx, t.totalWalkTimeTrip,
										t.calculatedJourneyTime,
										t.estimatedJourneyTime, tripcount,
										t.inVehTimeTrip, tripDistance,
										tripTime, t.mainmode, timeperiod,
										t.busDistance, t.trainDistance,
										t.busTrainDistance, straightDistance,
										this.zip2DGP.get(t.p13d_origpcode),
										this.zip2DGP.get(t.t2_destpcode));
						s.executeUpdate(insertString);
					}

				}
				// freezes program for debugging
				BufferedReader lilRead = new BufferedReader(
						new InputStreamReader(System.in));
				System.out.println("Done : " + new java.util.Date());
				System.out.print("EXIT? : ");
				String exiter = lilRead.readLine();
				if (exiter == "n")
					again = true;
				else
					again = false;
			}

		} catch (SQLException e) {

			e.printStackTrace();
		} catch (IOException e) {

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
				ArrayList<HITSStage> psgrStages = new ArrayList<HITSStage>();
				ArrayList<HITSTrip> dropOffTrips = new ArrayList<HITSTrip>();
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
					Date tripStart = stage.trip.t3_starttime_24h;
					Date tripEnd = stage.trip.t4_endtime_24h;
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
						HITSTrip nextTrip = null;
						HITSTrip prevTrip = null;
						if (trip.person.getTrips().size() > trip.trip_id)
							nextTrip = trip.person.getTrips().get(trip.trip_id);
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
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public static void main(String[] args) throws Exception {
		System.out.println(new java.util.Date());
		HITSAnalyser hp;
		System.out.println(args[0].equals("sql"));
		String fileName = "f:/temp/serial";
		Connection conn = null;
		String userName = "root";
		String password = "kosirosi";
		String url = "jdbc:mysql://localhost:3306/hits";
		Class.forName("com.mysql.jdbc.Driver").newInstance();
		conn = DriverManager.getConnection(url, userName, password);
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
		hp.jointTripSummary();
		System.out.println("wrote joint trip summary");

		// hp.createSQLSummary(conn);

		System.out.println("exiting...");
		System.out.println(new java.util.Date());
	}

}
