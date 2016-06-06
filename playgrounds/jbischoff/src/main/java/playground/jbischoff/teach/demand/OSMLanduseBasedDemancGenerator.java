package playground.jbischoff.teach.demand;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.core.utils.io.tabularFileParser.TabularFileParser;
import org.matsim.core.utils.io.tabularFileParser.TabularFileParserConfig;
import org.opengis.feature.simple.SimpleFeature;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.io.ParseException;
import com.vividsolutions.jts.io.WKTReader;

import playground.vsp.demandde.commuterDemandCottbus.CommuterDataElement;
import playground.vsp.demandde.commuterDemandCottbus.CommuterDataReader;

public class OSMLanduseBasedDemancGenerator {

	// ------FIELDS TO BE MODIFIED
	// -----------------------------------------------------------------------------------------//
	// Modal-Split values representing the relative amount of car users
	private final double MS_INNER_CITY = 0.55;
	private final double MS_OUTSIDE = 1;

	/*
	 * sample size can be modified via SCALFEACTOR note: since data come from
	 * incomplete commuter statistics a sample size of 100% is equivalent to a
	 * SCALEFACTOR of 1.5 * SCALEFACTOR 1.5 = 100%-Szenario SCALEFACTOR 0.15 =
	 * 10%-Szenario SCALEFACTOR 0.015 = 1%-Szenario
	 */
	private static double SCALEFACTOR = 0.15;

	/*
	 * adds some additional activities
	 */
	private static final boolean ENRICHPLANS = true;

	// ------ FIELDS NOT TO BE MODIFIED
	// ------------------------------------------------------------------------------------//
	private Scenario scenario;
	private Map<String, Geometry> shapeMap;
	private Map<String, Map<String, Geometry>> homeLocations;
	private Map<String, Map<String, Geometry>> workLocations;
	private Map<String, Coord> kindergartens;
	private Map<String, Coord> shops;

	private enum LanduseType {
		HOME, WORK
	};

	private final Random rnd = MatsimRandom.getRandom();
	private final Random locationRandom = MatsimRandom.getRandom();

	private int personcount = 0;
	// ----- FILE PATHS
	// ----------------------------------------------------------------------------------------------------//
	private static final String INPUTFOLDER = "../../../shared-svn/projects/cottbus/data/scenarios/cottbus_scenario/Cottbus-pt/INPUT_mod/demand_generation_tutorial/";

	private static final String BORDERS = INPUTFOLDER + "landuse/borders.shp";
	private static final String RESIDENTIAL = INPUTFOLDER + "landuse/residential.shp";
	private static final String WORKLOCS = INPUTFOLDER + "landuse/work.shp";
	private static final String SHOPS = INPUTFOLDER + "landuse/shops.txt";
	private static final String KINDERGARTEN = INPUTFOLDER + "landuse/kindergaerten.txt";
	private static final String COMMUTERSFILE = INPUTFOLDER + "commuters_brandenburg.csv";

	private static final String PLANSFILEOUTPUT = INPUTFOLDER + "plans_scale" + SCALEFACTOR
			+ Boolean.toString(ENRICHPLANS) + ".xml.gz";
	// -------------------------------------------------------------------------------------------------------------------//

	public static void main(String[] args) {
		OSMLanduseBasedDemancGenerator cd = new OSMLanduseBasedDemancGenerator();
		cd.run();
	}

	OSMLanduseBasedDemancGenerator() {
		this.scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
	}

	private void run() {

		// read all files
		this.shapeMap = readShapeFile(BORDERS, "NR");
		this.workLocations = sortShapeToBoundaries(readShapeFile(WORKLOCS, "osm_id"));
		this.homeLocations = sortShapeToBoundaries(readShapeFile(RESIDENTIAL, "osm_id"));

		this.shops = readFacilityLocations(SHOPS);
		this.kindergartens = readFacilityLocations(KINDERGARTEN);

		// the network covers only SPN & CB.
		CommuterDataReader cdr = new CommuterDataReader();
		cdr.addFilter("12052000"); // 12052000 == City of Cottbus
		cdr.addFilterRange(12071000); // the outskirts (Spree-Neisse)

		cdr.readFile(COMMUTERSFILE);
		for (CommuterDataElement cde : cdr.getCommuterRelations()) {
			double carshare = MS_OUTSIDE;
			if (cde.getFromId().equals("12052000") && cde.getToId().equals("12052000")) {
				carshare = MS_INNER_CITY;
				// share of car users is set for Cottbus
			}
			createPersons(cde.getFromId(), cde.getToId(), cde.getCommuters() * SCALEFACTOR, carshare);
		}

		System.out.println(this.shapeMap.keySet());

		PopulationWriter pw = new PopulationWriter(scenario.getPopulation());
		pw.write(PLANSFILEOUTPUT);
	}

	private Map<String, Map<String, Geometry>> sortShapeToBoundaries(Map<String, Geometry> shapemap) {
		Map<String,Map<String,Geometry>> locations = new HashMap<>(); 
		
		for (Entry<String,Geometry> e : shapemap.entrySet()){
			String muni = getMunicipalityForFeature(e.getValue());
			if (muni!=null){
				if (!locations.containsKey(muni)){
					locations.put(muni, new HashMap<>());
				}
				locations.get(muni).put(e.getKey(), e.getValue());
			}
		}
		
		return locations;
	}

	/**
	 * sets all relevant coordinates and the leg mode and draws a random
	 * activityChain-double for each plan and then calls createOnePerson method
	 * 
	 * @param homeZone
	 * @param workZone
	 * @param commuters
	 * @param relativeAmountOfCarUses
	 */
	private void createPersons(String homeZone, String workZone, double commuters, double relativeAmountOfCarUses) {

		for (int i = 0; i <= commuters; i++) {
			String mode = "car";
			double carcommuters = commuters * relativeAmountOfCarUses;
			if (i > carcommuters)
				mode = "pt";

			// set coordinates of home and work activities to a randomly picked
			// building
			Coord homec = this.getCoordInLanduseFromZone(homeZone, LanduseType.HOME);
			Coord workc = this.getCoordInLanduseFromZone(workZone, LanduseType.WORK);

			double personalRandom = rnd.nextDouble();
			createOnePerson(homec, workc, mode, homeZone + "_" + workZone + "_", personalRandom);
		}
	}

	/**
	 * creates the person and its plan activityChain defines the activity chain
	 * of the plan according to this distribution: 60% home-work-home 10%
	 * home-KINDERGARTEN1-work-KINDERGARTEN2-home 30%
	 * home-work-home-SHOPPEN-home
	 * 
	 * @param i
	 * @param coord
	 * @param coordWork
	 * @param mode
	 * @param toFromPrefix
	 * @param activityChain
	 */
	private void createOnePerson(Coord coord, Coord coordWork, String mode, String toFromPrefix, double activityChain) {
		Id<Person> personId = Id.createPersonId(toFromPrefix + this.personcount);
		this.personcount++;
		// create the Person and Plan instances
		Person person = scenario.getPopulation().getFactory().createPerson(personId);
		Plan plan = scenario.getPopulation().getFactory().createPlan();

		/*
		 * create all activities and legs of the plan randomly distribute start
		 * and end times
		 */

		// 60% plan: home-work-home
		Activity home = scenario.getPopulation().getFactory().createActivityFromCoord("home", coord);
		double startTime = 6.5 * 60 * 60 + (2 * 60 * 60 * rnd.nextDouble()); // ZufallsStartZeit
																				// 7.30-9.30Uhr
		home.setEndTime(startTime);
		plan.addActivity(home);

		Leg hinweg1 = scenario.getPopulation().getFactory().createLeg(mode);
		plan.addLeg(hinweg1);

		// 10% plan: home-KINDERGARTEN1-work-KINDERGARTEN2-home
		// kindergarten AM

		if (ENRICHPLANS) {
			if ((activityChain > 0.6) && (activityChain <= 0.7)) {

				Activity kindergarten1 = scenario.getPopulation().getFactory().createActivityFromCoord("kindergarten1",
						this.findClosestCoordInMap(coord, kindergartens));
				kindergarten1.setMaximumDuration(300 + rnd.nextInt(300)); // 5-10
																			// minutes
				plan.addActivity(kindergarten1);

				Leg hinweg2 = scenario.getPopulation().getFactory().createLeg(mode);
				plan.addLeg(hinweg2);
			}
		}
		Activity work = scenario.getPopulation().getFactory().createActivityFromCoord("work", coordWork);
		double workEndTime = startTime + (7.5 * 60 * 60) + (1 * 60 * 60 * rnd.nextDouble()); // working
																								// time
																								// 7.5-8.5
																								// hours
		work.setEndTime(workEndTime);
		plan.addActivity(work);

		Leg rueckweg1 = scenario.getPopulation().getFactory().createLeg(mode);
		plan.addLeg(rueckweg1);

		// kindergarten PM
		if (ENRICHPLANS) {

			if ((activityChain > 0.6) && (activityChain <= 0.7)) {

				Activity kindergarten2 = scenario.getPopulation().getFactory().createActivityFromCoord("kindergarten2",
						this.findClosestCoordInMap(coord, kindergartens));
				kindergarten2.setMaximumDuration(300 + rnd.nextInt(300)); // 5-10
																			// minutes
				plan.addActivity(kindergarten2);

				Leg back2 = scenario.getPopulation().getFactory().createLeg(mode);
				plan.addLeg(back2);
			}
		}
		// 30% plan: home-work-home-shopping-home
		if (ENRICHPLANS) {

			if (activityChain > 0.7) {

				Activity home2 = scenario.getPopulation().getFactory().createActivityFromCoord("home", coord);
				double startShoppingTime = workEndTime + (1 * 60 * 60) + (0.5 * 60 * 60 * rnd.nextDouble());
				home2.setEndTime(startShoppingTime);
				plan.addActivity(home2);

				Leg zumShoppen = scenario.getPopulation().getFactory().createLeg(mode);
				plan.addLeg(zumShoppen);

				Activity shopping = scenario.getPopulation().getFactory().createActivityFromCoord("shopping",
						this.findClosestCoordInMap(coord, shops));
				shopping.setEndTime(startShoppingTime + (0.5 * 60 * 60) + (1 * 60 * 60 * rnd.nextDouble())); // shopping
																												// 0.5-1.5
																												// hours
				plan.addActivity(shopping);

				Leg vomShoppen = scenario.getPopulation().getFactory().createLeg(mode);
				plan.addLeg(vomShoppen);
			}
		}
		Activity home3 = scenario.getPopulation().getFactory().createActivityFromCoord("home", coord);
		plan.addActivity(home3);

		person.addPlan(plan);
		scenario.getPopulation().addPerson(person);
	}

	/**
	 * read in a shape file and convert it into a map of geometries where keys
	 * are the values of the attribute defined by attrString
	 * 
	 * @param filename
	 * @param attrString
	 *            for counties: Nr for buildings: osm_id
	 * @return shapeMap
	 */
	public Map<String, Geometry> readShapeFile(String filename, String attrString) {

		Map<String, Geometry> shapeMap = new HashMap<String, Geometry>();

		for (SimpleFeature ft : ShapeFileReader.getAllFeatures(filename)) {

			GeometryFactory geometryFactory = new GeometryFactory();
			WKTReader wktReader = new WKTReader(geometryFactory);
			Geometry geometry;

			try {
				geometry = wktReader.read((ft.getAttribute("the_geom")).toString());
				shapeMap.put(ft.getAttribute(attrString).toString(), geometry);

			} catch (ParseException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return shapeMap;
	}

	/**
	 * reads in a facility file and returns a map of the facilities to their
	 * coordinates
	 * 
	 * @param fileName
	 * @return
	 */
	private Map<String, Coord> readFacilityLocations(String fileName) {
		FacilityParser fp = new FacilityParser();
		TabularFileParserConfig config = new TabularFileParserConfig();
		config.setDelimiterRegex("\t");
		config.setCommentRegex("#");
		config.setFileName(fileName);
		new TabularFileParser().parse(config, fp);
		return fp.getFacilityMap();
	}

	/**
	 * finds the closest coordinate in facilityMap to the coordinate origin
	 * 
	 * @param facilityMap
	 * @param origin
	 * @return closest Coord
	 */
	private Coord findClosestCoordInMap(Coord origin, Map<String, Coord> facilityMap) {
		Coord closest = null;
		double closestDistance = Double.MAX_VALUE;
		for (Coord coord : facilityMap.values()) {
			double distance = CoordUtils.calcEuclideanDistance(coord, origin);
			if (distance < closestDistance) {
				closestDistance = distance;
				closest = coord;
			}
		}
		return closest;
	}

	/**
	 * returns a random point within a zone
	 * 
	 * @param zone
	 * @return
	 */
	private Point getRandomPointInFeature(Geometry g) {
		Point p = null;
		double x, y;
		do {
			x = g.getEnvelopeInternal().getMinX() + this.locationRandom.nextDouble()
					* (g.getEnvelopeInternal().getMaxX() - g.getEnvelopeInternal().getMinX());
			y = g.getEnvelopeInternal().getMinY() + this.locationRandom.nextDouble()
					* (g.getEnvelopeInternal().getMaxY() - g.getEnvelopeInternal().getMinY());
			p = MGC.xy2Point(x, y);
		} while (!g.contains(p));
		return p;
	}

	/**
	 * returns the coordinate of a randomly picked building in zone
	 * 
	 * @param workZone
	 * @return
	 */
	private Coord getCoordInLanduseFromZone(String zoneId, LanduseType type) {
		Geometry zone = this.shapeMap.get(zoneId);
		Map<String, Geometry> landuseMap = null;
		if (type.equals(LanduseType.WORK)) {
			landuseMap = workLocations.get(zoneId);
		} else {
			landuseMap = homeLocations.get(zoneId);
		}
		if (landuseMap == null){
//			System.err.println("No Landuse Type "+type+" for zone " +zoneId);
		}
		
		Point p;
		do {
			p = getRandomPointInFeature(zone);

		} while (!pointIsInLanduse(landuseMap, p));
		return MGC.point2Coord(p);
	}

	private String getMunicipalityForFeature(Geometry g) {
		for (Entry<String, Geometry> e : this.shapeMap.entrySet()) {
			if (e.getValue().contains(g.getCentroid())) {
				return e.getKey();
			}
		}
		return null;

	}

	private boolean pointIsInLanduse(Map<String, Geometry> landuseMap, Point p) {
		if (landuseMap==null){
			return true;
		}
		
		for (Geometry g : landuseMap.values()) {
			if (g.contains(p))
				return true;
		}

		return false;
	}

}
