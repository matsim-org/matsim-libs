package playground.pieter.singapore.hits;

//import java.text.SimpleDateFormat;
//import java.util.ArrayList;
//import java.util.List;
//import java.util.TimeZone;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import javax.management.timer.Timer;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.network.algorithms.TransportModeNetworkFilter;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.population.algorithms.XY2Links;

public class HITSToMATSim {

	private HITSData hitsData;
	private Scenario scenario; // used if you want to generate matsim
								// populations
	private Population population;
	private NetworkImpl network;
	private HashMap<Integer, Integer> zip2DGP;
	private HashMap<Integer, ArrayList<Integer>> DGP2Zip;
	private HashMap<Integer, HashMap<String, ArrayList<Integer>>> DGP2Type2Zip;
	private Connection conn;

	private java.util.Date referenceDate; // all dates were referenced against a
											// starting Date of 1st September,
											// 2008, 00:00:00 SGT
	private HashMap<Integer, Coord> zip2Coord;

	private int dgpErrCount;
	private ArrayList<Integer> DGPs;

	private LeastCostPathCalculator leastCostPathCalculator;
	private HashMap<String, Double> personShortestPathDayTotals;
    private Map<Id<Link>, Link> links;

	private HITSToMATSim(HITSData h2, Connection conn2) throws ParseException {
		this();
		this.conn = conn2;
		this.setHitsData(h2);
	}

	private HITSToMATSim() throws ParseException {
		DateFormat outdfm = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		// outdfm.setTimeZone(TimeZone.getTimeZone("SGT"));
		referenceDate = outdfm.parse("2008-09-01 00:00:00");
	}

	void generatePopulation(int sampleSize, boolean dummyActs,
                            String populationXMLFileName, String mixed) throws InstantiationException,
			IllegalAccessException, ClassNotFoundException, SQLException,
			FileNotFoundException {

		this.scenario = ScenarioUtils
				.createScenario(ConfigUtils.createConfig());
		this.population = scenario.getPopulation();
		new MatsimNetworkReader(scenario.getNetwork())
				.readFile("data/singapore1_no_rail_CLEAN.xml");
		NetworkImpl subNet = NetworkImpl.createNetwork();
		TransportModeNetworkFilter t = new TransportModeNetworkFilter(scenario.getNetwork());
		HashSet set = new HashSet<String>();
		set.add("car");
		t.filter(subNet, set);
		this.network = subNet;
		this.preProcessNetwork();

		this.setZip2DGP(conn);
		this.setDGP2Zip(conn);
		this.setZip2Coord(conn);
		this.setDGP2Type2Zip(conn);
        switch (mixed) {
            case "mixed":
                generateCarAndTransitMixedPlans(conn, sampleSize, dummyActs);
                break;
            case "transit":
                generateCarAndTransitOnlyPlans(conn, sampleSize, dummyActs, true);
                break;
            default:
                generateCarAndTransitOnlyPlans(conn, sampleSize, dummyActs, false);
                break;
        }
		PopulationWriter populationWriter = new PopulationWriter(
				scenario.getPopulation(), scenario.getNetwork());
		populationWriter.write(populationXMLFileName);

	}

	HITSData getHitsData() {
		return hitsData;
	}

	void setHitsData(HITSData hitsData) {
		this.hitsData = hitsData;
	}

	/**
	 * @param p
	 *            The HITSPeron this plan belongs to
	 * @param dummyActs
	 *            Should this plan use dummy acts; not implemented currently
	 * @param legmode
	 *            pt or car
	 * @param referencePlan
	 *            true if this plan has to be referenced against the exact
	 *            coordinates of the postal code, else it randomises location
	 *            based on DGP and place type
	 * @return Plan
	 * @throws SQLException
	 */
	private Plan createCarOrTransitPlan(HITSPerson p, boolean dummyActs,
			String legmode, boolean referencePlan) throws SQLException {

		// get the person's trips
		ArrayList<HITSTrip> ht = p.getTrips();
		Plan plan = this.population.getFactory().createPlan();
		int n = 0;
		// need to know when we reach the last trip, to create activity with no
		// end time
		int maxTripIdx = ht.size() - 1;
		int homeZip = 0;
		Coord homeCoord = null;

		for (HITSTrip t : ht) {
			// find the origin of the current trip, add its coord
			// if it's the first trip, we'll have to use the dwelling type as
			// placetype
			String placeType = "any";
			String actType = "dummy";

			if (n == 0) {
				placeType = this.hitsData.getHouseholdById(t.h1_hhid).h2_dwell;
				actType = "home";
				homeZip = t.p13d_origpcode;
				// if it's an in-between trip, just use placeType from the
				// previous entry
			} else if (n <= maxTripIdx) {
				placeType = ht.get(n - 1).t5_placetype;
				actType = ht.get(n - 1).t6_purpose;
			}

			if (placeType == "any")
				System.out.println("err");
			Coord origin = referencePlan ? zip2Coord.get(t.p13d_origpcode)
					: getRandomCoordInDGPFromZipAndHITSPlaceType(
							t.p13d_origpcode, placeType);
			if (n == 0)
				homeCoord = origin;

			// for now, if the zip doesn't resolve to a coord, skip this plan
			if (origin == null)
				return null;

			Activity act = population.getFactory().createActivityFromCoord(
//					hitsActTypeToMATSimType(actType), origin);
			actType, origin);
			
			// make sure the end time is no earlier than the start of the
			// simulation
			act.setEndTime(Math.max(
					0,
					(t.t3_starttime_24h.getTime() - this.referenceDate
							.getTime()) / Timer.ONE_SECOND)
					+ (referencePlan?0:randomSeconds(30))

			);
			plan.addActivity(act);
			
			// if the plan calls for a mix of modes,then read the main mode of
			// the trip, otherwise stick to one mode
			if (legmode.equals("either")) {
                switch (t.mainmode) {
                    case "carDrv":
                        plan.addLeg(createDriveLeg());
                        break;
                    case "pt":
                        plan.addLeg(createTransitLeg());
                        break;
                    default:
                        return null;
                }
			} else {
                switch (legmode) {
                    case "car":
                        plan.addLeg(createDriveLeg());
                        break;
                    case "pt":
                        plan.addLeg(createTransitLeg());
                        break;
                    default:
                        return null;
                }
			}

			// if this is also the last trip, then end with the destination
			if (n == maxTripIdx) {
				// find the destination of the current trip, add its coord
				Coord dest = null;
				if (t.t2_destpcode == homeZip) {
					dest = homeCoord;
				} else {

					dest = referencePlan ? zip2Coord.get(t.p13d_origpcode)
							: getRandomCoordInDGPFromZipAndHITSPlaceType(
									t.t2_destpcode, t.t5_placetype);
				}
				if (dest == null)
					return null;

				Activity actend = population.getFactory()
						.createActivityFromCoord(
								hitsActTypeToMATSimType(t.t6_purpose), dest);
				// this activity has no end time
				plan.addActivity(actend);
			}

			n++;
		}
		return plan;

	}

	private Leg createDriveLeg() {
		return population.getFactory().createLeg(TransportMode.car);

	}

	private Leg createTransitLeg() {
		return population.getFactory().createLeg(TransportMode.pt);

	}

	private void generateCarAndTransitOnlyPlans(Connection conn,
			int sampleSize, boolean dummyActs, boolean transitOnly) throws SQLException {
		// iterate through all persons, find those with non-walking trips,
		// determine the main mode each trip, and if the person uses either
		// transit or private vehicle during the day,
		// make their plan the corresponding mode
		ArrayList<HITSPerson> hp = this.getHitsData().getPersons();

		// a couple of counters
		int nullPlans = 0;
		int otherModes = 0;
		double otherModesInflated = 0;
		int mixedPlans = 0;
		double mixedPlansInflated = 0;
		double carPlansInflated = 0;
		int carPlans = 0;
		double transitPlansinflated = 0;
		int transitPlans = 0;
		this.personShortestPathDayTotals = new HashMap<>();

		// start iterating
		for (HITSPerson p : hp) {
			ArrayList<HITSTrip> ht = p.getTrips();
			// if the person doesn't have any trips, skip ahead, or only one
			// trip (spoilt record), skip over
			if (ht.size() <= 1) {
				nullPlans++;
				continue;
			}
			// check if any trips are for car as driver, or transit, to get the
			// set of viable plans
			boolean carSwitch = false;
			boolean ptSwitch = false;
			boolean otherSwitch = false;
			boolean nullModes = false;
            label:
            for (HITSTrip t : ht) {
                // first, check for null modes, and skip if they exist
                if (t.mainmode == null) {
                    nullModes = true;
                    break;
                }
                // check the trip for having car mode, if it trips the 'other'
                // switch, it's passed over
                switch (t.mainmode) {
                    case "carDrv":
                        carSwitch = true;
                        break;
                    case "publBus":
                    case "mrt":
                    case "lrt":
                        ptSwitch = true;
                        break;
                    default:
                        otherSwitch = true;
                        otherModes++;
                        otherModesInflated += p.scheduleFactor;
                        break label;
                }

            }
			if (nullModes) {
				continue;
			}
			// ///////////////////
			// ///////////////////
			// if(carSwitch){
			// continue;
			// }
			// /////////////////////
			// /////////////////////

			if (carSwitch && otherSwitch || carSwitch && ptSwitch) {
				mixedPlans++;
				mixedPlansInflated += p.scheduleFactor;
				continue;
			}

			if (otherSwitch) {
				continue;
			}

			if (carSwitch && transitOnly) {
				continue;
			}
			// now, only car drivers and transit remain
			String legmode = "";
			if (carSwitch) {
				carPlansInflated += p.scheduleFactor;
				legmode = "car";
			} else {
				transitPlansinflated += p.scheduleFactor;
				legmode = "pt";
			}
			int reps = (int) Math.ceil(p.scheduleFactor * sampleSize / 100);
			System.out.println(p.actMainModeChainPax + " : x " + reps);

			for (int i = 1; i <= reps; i++) {
				String fullID = p.h1_hhid + "_" + p.pax_id + "_" + i;
				// generate a matsim person and plan for this rep
				Person matsimPerson = population.getFactory().createPerson(
						Id.createPersonId(fullID));
				Plan matsimPlan = null;

				matsimPlan = createCarOrTransitPlan(p, dummyActs, legmode,
                        i == 1);

				if (matsimPlan != null) {
//					xY2Links.run(matsimPlan);
					matsimPerson.addPlan(matsimPlan);
					population.addPerson(matsimPerson);
					// add the shortest path total travel distance for the
					// reference plan
					if (i == 1) {
						try {
							this.personShortestPathDayTotals.put(p.h1_hhid
									+ "_" + p.pax_id,
									getShortestPathTotalDistance(matsimPlan));

						} catch (NullPointerException e) {
							System.out.println("Unroutable plan for person "
									+ fullID);
						}
					}
				} else {
					// something wrong with this person's plan
					System.out.println(fullID
							+ " generated an error. Skipping his record.");
					break;
				}
				if (carSwitch) {

					carPlans++;
				} else {
					transitPlans++;
				}
			}

		}

		System.out.println("number of other main mode only persons: "
				+ otherModes + "\t  Inflated: " + otherModesInflated);
		System.out.println("number of mixed main mode persons: " + mixedPlans
				+ "\t  Inflated: " + mixedPlansInflated);
		System.out.println("total number of CAR plans generated this way: "
				+ carPlans + " (" + sampleSize + " pct sample)");
		System.out
				.println("total number of CAR plans times avg trip inflation factor: "
						+ carPlansInflated);
		System.out.println("total number of TRANSIT plans generated this way: "
				+ transitPlans);
		System.out
				.println("total number of TRANSIT plans times avg trip inflation factor: "
						+ transitPlansinflated
						+ " ("
						+ sampleSize
						+ " pct sample)");
		System.out
				.println("total number of dgp nulls (e.g. zip points to null dgp, or hits zip not in postal codes db): "
						+ this.dgpErrCount);

	}

	private void generateCarAndTransitMixedPlans(Connection conn,
			int sampleSize, boolean dummyActs) throws SQLException {
		// iterate through all persons, find those with non-walking trips,
		// determine the main mode each trip, and if the person uses either
		// transit or private vehicle during the day,
		// make their plan the corresponding mode
		ArrayList<HITSPerson> hp = this.getHitsData().getPersons();

		// a couple of counters
		int nullPlans = 0;
		int otherModes = 0;
		double otherModesInflated = 0;
		int mixedPlans = 0;
		double mixedPlansInflated = 0;
		double carPlansInflated = 0;
		int carPlans = 0;
		double transitPlansinflated = 0;
		int transitPlans = 0;
		double carTransitPlansinflated = 0;
		int carTransitPlans = 0;
		this.personShortestPathDayTotals = new HashMap<>();

		// start iterating
		for (HITSPerson p : hp) {
			ArrayList<HITSTrip> ht = p.getTrips();
			// if the person doesn't have any trips, skip ahead, or only one
			// trip (spoilt record), skip over
			if (ht.size() <= 1) {
				nullPlans++;
				continue;
			}
			// check if any trips are for car as driver, or transit, to get the
			// set of viable plans
			boolean carSwitch = false;
			boolean ptSwitch = false;
			boolean otherSwitch = false;
			boolean nullModes = false;
            label:
            for (HITSTrip t : ht) {
                // first, check for null modes, and skip if they exist
                if (t.mainmode == null) {
                    nullModes = true;
                    break;
                }
                // check the trip for having car mode, if it trips the 'other'
                // switch, it's passed over
                switch (t.mainmode) {
                    case "carDrv":
                        carSwitch = true;
                        break;
                    case "publBus":
                    case "mrt":
                    case "lrt":
                        //change the mode of this trip to transit (non-permanent)
                        t.mainmode = "pt";
                        ptSwitch = true;
                        break;
                    default:
                        otherSwitch = true;
                        otherModes++;
                        otherModesInflated += p.scheduleFactor;
                        break label;
                }

            }
			if (nullModes) {
				continue;
			}

			if (carSwitch && otherSwitch) {
				mixedPlans++;
				mixedPlansInflated += p.scheduleFactor;
				continue;
			}

			if (otherSwitch) {
				continue;
			}

			// now, only car drivers and transit remain
			String legmode = "";
			if (carSwitch & !ptSwitch) {
				carPlansInflated += p.scheduleFactor;
				legmode = "car";
			} else if (!carSwitch & ptSwitch) {
				transitPlansinflated += p.scheduleFactor;
				legmode = "pt";
			} else {
				carTransitPlansinflated += p.scheduleFactor;
				legmode = "either";
			}
			int reps = (int) (p.scheduleFactor * sampleSize / 100);
			System.out.println(p.actMainModeChainPax + " : x " + reps);

			for (int i = 1; i <= reps; i++) {
				String fullID = p.h1_hhid + "_" + p.pax_id + "_" + i;
				// generate a matsim person and plan for this rep
				Person matsimPerson = population.getFactory().createPerson(
						Id.createPersonId(fullID));
				Plan matsimPlan = null;

				matsimPlan = createCarOrTransitPlan(p, dummyActs, legmode,
                        i == 1);

				if (matsimPlan != null) {
//					xY2Links.run(matsimPlan);
					matsimPerson.addPlan(matsimPlan);
					population.addPerson(matsimPerson);
				} else {
					// something wrong with this person's plan
					System.out.println(fullID
							+ " generated an error. Skipping his record.");
					break;
				}
				if (carSwitch & !ptSwitch) {

					carPlans++;
				} else if (!carSwitch & ptSwitch) {
					transitPlans++;
				} else {
					carTransitPlans++;
				}
			}

		}

		System.out.println("number of other main mode only persons: "
				+ otherModes + "\t  Inflated: " + otherModesInflated);
		System.out.println("number of mixed main mode persons: " + mixedPlans
				+ "\t  Inflated: " + mixedPlansInflated);
		System.out.println("total number of CAR plans generated this way: "
				+ carPlans + " (" + sampleSize + " pct sample)");
		System.out
				.println("total number of CAR plans times avg trip inflation factor: "
						+ carPlansInflated);
		System.out.println("total number of TRANSIT plans generated this way: "
				+ transitPlans);
		System.out
				.println("total number of TRANSIT plans times avg trip inflation factor: "
						+ transitPlansinflated
						+ " ("
						+ sampleSize
						+ " pct sample)");
		System.out
				.println("total number of CARplusTRANSIT plans generated this way: "
						+ carTransitPlans);
		System.out
				.println("total number of CARplusTRANSIT plans times avg trip inflation factor: "
						+ carTransitPlansinflated
						+ " ("
						+ sampleSize
						+ " pct sample)");
		System.out
				.println("total number of dgp nulls (e.g. zip points to null dgp, or hits zip not in postal codes db): "
						+ this.dgpErrCount);

	}

	private Coord getRandomCoordInDGPFromZip(int pcode) {
		try {
			// gets the postal code's DGP from zip, then assigns it a random
			// postal code within the zip,
			// ignoring placetype, and returns the postal code's coordinates
			int dgp = this.zip2DGP.get(pcode);
			// if dgp is 0, it means this postal code didn't associate correctly
			// during the spatial join
			if (dgp == 0) {
				// increase the error count, and give this poor sod a random dgp
				// as start location
				this.dgpErrCount++;
				int dgpindex = (int) Math.floor(Math.random()
						* this.DGPs.size());
				dgp = this.DGPs.get(dgpindex);
			}
			int maxIdx = this.DGP2Zip.get(dgp).size();
			int rnd = getRandomIndex(maxIdx);
			int outZip = this.DGP2Zip.get(dgp).get(rnd);
			return shoot(zip2Coord.get(outZip));
		} catch (NullPointerException e) {
			System.out.println("ZIP " + pcode
					+ " generated null point exception, returning random DGP.");

			// increase the error count, and give this poor sod a random dgp as
			// start location
			this.dgpErrCount++;
			int dgpindex = (int) Math.floor(Math.random() * this.DGPs.size());
			int dgp = this.DGPs.get(dgpindex);

			int maxIdx = this.DGP2Zip.get(dgp).size();
			int rnd = getRandomIndex(maxIdx);
			int outZip = this.DGP2Zip.get(dgp).get(rnd);
			return shoot(zip2Coord.get(outZip));
		}
	}

	private Coord getRandomCoordInDGPFromZipAndHITSPlaceType(int zip,
			String placeType) {
		try {

			int outZip = 0;

			int dgp = zip2DGP.get(zip);
			// if dgp is 0, it means this postal code didn't associate correctly
			// during the spatial join
			if (dgp == 0) {
				// increase the error count, and try to return the coordinate of
				// the input zip
				this.dgpErrCount++;
				outZip = zip;
			} else {

				ArrayList<Integer> zips = this.DGP2Type2Zip.get(dgp).get(
						placeType);
				int maxIdx = zips.size();
				if (maxIdx == 0) {
					System.out.println("none for this placetype in dgp");
					outZip = zip;
				} else {

					int rnd = getRandomIndex(maxIdx);
					outZip = zips.get(rnd);
				}
			}
			return shoot(zip2Coord.get(outZip));
		} catch (NullPointerException e) {

			return null;
		}

	}

	private int getRandomIndex(int maxIdx) {
		// returns a random integer smaller than maxIdx
		return (int) Math.floor(Math.random() * maxIdx);
	}

	private double getShortestPathTotalDistance(Plan plan) {
		// ArrayList<Link> shortPathLinks = new ArrayList<Link>();
		double distance = 0;

		List<?> actslegs = plan.getPlanElements();
		int i = 0;
		for (int j = 2; j < actslegs.size(); j = j + 2) {
			ActivityImpl startAct = (ActivityImpl) actslegs.get(i);
			ActivityImpl endAct = (ActivityImpl) actslegs.get(j);
			Node startNode = links.get(startAct.getLinkId()).getToNode();
			Node endNode = links.get(endAct.getLinkId()).getFromNode();
			Path path = leastCostPathCalculator.calcLeastCostPath(startNode,
					endNode, 0, null, null);
			path.links.add(0, links.get(startAct.getLinkId()));
			for (Link l : path.links) {
				distance += l.getLength();
			}
			// sets the start to the current activity's link
			i = j;

		}

		return distance;
	}

	private String hitsActTypeToMATSimType(String actType) {
		if (actType.equals("home") || actType.equals("work")
				|| actType.equals("edu") || actType.equals("pikUpDrop")) {
			return actType;
		} else {

			return "other";
		}
	}

	private Coord shoot(Coord coord) {
		// scatters coords within a 200x200m2 square
		int squareSize = 200;
		coord.setX(coord.getX() + squareSize * Math.random() - squareSize / 2);
		coord.setY(coord.getY() + squareSize * Math.random() - squareSize / 2);
		return coord;
	}

	/**
	 * pre-processes the network for astar, and associating acts with links
	 */
	private void preProcessNetwork() {
        XY2Links xY2Links = new XY2Links(network, null);
		this.links = network.getLinks();
	}

	private long randomSeconds(int mins) {
		// returns a random number of seconds spanning -mins/2 : +mins/2
		return (long) ((Math.random() * 60 * mins) - mins / 2);
	}

	private void setDGP2Type2Zip(Connection conn2) throws SQLException {
		// Init the overall hasmap
		this.DGP2Type2Zip = new HashMap<>();
		Statement s = conn2.createStatement();
		// get the list of DGPs;
		s.executeQuery("select distinct DGP from hits2type2zip2dgp;");
		ResultSet rsDgp = s.getResultSet();
		while (rsDgp.next()) {
			int dgp = rsDgp.getInt("DGP");
			HashMap<String, ArrayList<Integer>> types2Zips = new HashMap<>();
			// then, get all the hitstypes in this dgp
			Statement s1 = conn2.createStatement();
			s1.executeQuery("select distinct hits_type from hits2type2zip2dgp where DGP="
					+ dgp + ";");
			ResultSet rsHitsType = s1.getResultSet();
			while (rsHitsType.next()) {
				String ht = rsHitsType.getString("hits_type");
				ArrayList<Integer> zips = new ArrayList<>();
				// then, all the zip codes associated with this hits type in the
				// dgp
				Statement s2 = conn2.createStatement();
				s2.executeQuery("select distinct zip from hits2type2zip2dgp where DGP="
						+ dgp + " and hits_type = \'" + ht + "\';");
				ResultSet rsZip = s2.getResultSet();
				while (rsZip.next()) {
					int zip = rsZip.getInt("zip");
					zips.add(zip);
				}
				types2Zips.put(ht, zips);
			}
			this.DGP2Type2Zip.put(dgp, types2Zips);
		}
		System.out.println("Populated DGP2Type2Zip");
	}

	private void setZip2DGP(Connection conn) throws SQLException {
		// init the hashmap
		this.zip2DGP = new HashMap<>();
		Statement s;
		s = conn.createStatement();
		s.executeQuery("select zip, DGP from pcodes_zone_xycoords ;");
		ResultSet rs = s.getResultSet();
		// iterate through the resultset and populate the hashmap
		while (rs.next()) {
			this.zip2DGP.put(rs.getInt("zip"), rs.getInt("DGP"));
		}
	}

	private void setZip2Coord(Connection conn) throws SQLException {
		// init the hashmap
		this.zip2Coord = new HashMap<>();
		Statement s;
		s = conn.createStatement();
		s.executeQuery("select zip, x_utm48n, y_utm48n from pcodes_zone_xycoords ;");
		ResultSet rs = s.getResultSet();
		// iterate through the resultset and populate the hashmap
		while (rs.next()) {
			this.zip2Coord.put(
					rs.getInt("zip"),
					new Coord(rs.getDouble("x_utm48n"), rs.getDouble("y_utm48n")));
		}
	}

	private void setDGP2Zip(Connection conn) throws SQLException {
		Statement s;
		s = conn.createStatement();
		// get the list of DGPs, and associate a list of postal codes with each
		this.DGP2Zip = new HashMap<>();
		s.executeQuery("select distinct DGP from pcodes_zone_xycoords where DGP is not null;");
		ResultSet rs = s.getResultSet();
		// iterate through the list of DGPs, create an arraylist for each, then
		// fill that arraylist with all its associated postal codes
		this.DGPs = new ArrayList<>();
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
			this.DGPs.add(currDGP);
			while (zrs1.next()) {
				zipsInDGP.add(zrs1.getInt("zip"));
			}
			// add the DGP and list of zip codes to the hashmap
			zipsInDGP.trimToSize();
			DGP2Zip.put(currDGP, zipsInDGP);
		}
		this.DGPs.trimToSize();

	}

	public static void main(String[] args) throws Exception {
		System.out.println(new java.util.Date());

		Connection conn = null;
		String userName = "fouriep";
		String password = "K0s1R0s1";
		String url = "jdbc:mysql://krakatau.sec.ethz.local/hits";
		Class.forName("com.mysql.jdbc.Driver").newInstance();
		conn = DriverManager.getConnection(url, userName, password);
		System.out.println("Database connection established");

		HITSToMATSim hp;
		System.out.println("Loading from SQL: " + args[0].equals("sql"));
		String fileName = "c:/Temp/serial";
		if (args[0].equals("sql")) {
			// this section determines whether to write the long or short
			// serialized file, default is the full file
			HITSData h;
			if (args[1].equals("short")) {

				h = new HITSData(conn, true);
			} else {
				h = new HITSData(conn, false);
			}
			hp = new HITSToMATSim(h, conn);
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
			hp = new HITSToMATSim((HITSData) ois.readObject(), conn);
			ois.close();
			System.out.println("loaded households:\n");

		}

		// 100 generates a full sample
		hp.generatePopulation(1, false,
				"data/HITS_reference_mix.xml.gz", "non");

		System.out.println("exiting...");
		System.out.println(new java.util.Date());
		conn.close();
	}

}
