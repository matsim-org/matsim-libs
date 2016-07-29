package patryk.populationgeneration;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.population.PersonUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.utils.objectattributes.ObjectAttributes;
import org.matsim.utils.objectattributes.ObjectAttributesXmlWriter;
import org.opengis.feature.simple.SimpleFeature;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.io.ParseException;
import com.vividsolutions.jts.io.WKTReader;

public class CreateDemand {

	private final static Logger log = Logger.getLogger(CreateDemand.class);

	public static final String NETWORKFILE = "networks/network_v09.xml";
	public static final String COUNTIES = "demand_test/shapes/stockholm_counties/stockholm_county_EPSG3857.shp";
	public static final String BUILDINGS = "demand_test/shapes/stockholm_buildings_small/by_sthlm_EPSG3857.shp";
	public static final String PLANSFILEOUTPUT = "prep_01_april/plans_10sample_v03.xml";
	public static final String POPFILE = "demand_test/data/population/synpop.csv";
	public static final String AGENTATTR = "prep_01_april/agentattributes_10sample_v03.xml";
	
	// Files with xy coordinates for home and work locations
	public static final String AGENTHXYOUTPUT = "prep_01_april/agentsxy_buildings_h.txt"; // home
	public static final String AGENTWXYOUTPUT = "prep_01_april/agentsxy_buildings_w.txt"; // work
	public static final String SINGLEFAMH = "prep_01_april/singleFam_h.txt";
	public static final String MULTIFAMH = "prep_01_april/multiFam_h.txt";
	
	private Scenario scenario;

	private static double SCALEFACTOR = 0.10; // SCALEFACTOR represents the size of the population. 0.02 = 2 % of the population
	
	/* currently, everyone that has a driv lic and a car available uses a car. from the travel survey the mode share of car is
	 * 35 % which translates to 50 % of those who have a driv lic and a car available */
	private static double CARSHARE = 0.5; 
	private static int workersInHomeBuildings = 5; // Percentage of workers that are assigned not to work buildings but to homes

	private static Map<Integer, Double> departures;

	private final List<PendlerRelation> relations;
	private final List<County> counties;

	private int unknownZone;
	private int noLicenseOrCar;
	private int noWorkingPlace;
	private int linesProcessed;
	
	static {
		departures = new HashMap<>();
		departures.put(5, 0.10);
		departures.put(6, 0.30);
		departures.put(7, 0.25);
		departures.put(8, 0.25);
		departures.put(9, 0.10);
	}

	CreateDemand() {
		this.scenario = ScenarioUtils
				.createScenario(ConfigUtils.createConfig());
		new MatsimNetworkReader(scenario.getNetwork()).readFile(NETWORKFILE);

		StockholmPendlerParser pp = new StockholmPendlerParser();
		pp.read(POPFILE);

		this.noLicenseOrCar = pp.getNumberOfPeopleWithoutLicenseOrCar();
		this.noWorkingPlace = pp.getNumberOfPeopleWithoutWorkingPlace();
		this.linesProcessed = pp.getLinesProcessed();
		this.relations = pp.getRelations();
		this.counties = pp.getCounties();
		readCountiesShapeFile(COUNTIES, "ZONE");
		readBuildings(BUILDINGS);
	}

	private void run() {
		int everyXPerson = (int)(1.d/(SCALEFACTOR*CARSHARE));
		int numberPersonsProcessed = 0;
		
		ObjectAttributes agentAttributes = scenario.getPopulation().getPersonAttributes();
		
		for (PendlerRelation rel : relations) {

			double commuters = rel.getNumber();

			Geometry home = rel.getHome().getGeometry(); 
			Geometry work = rel.getWork().getGeometry();

			if (home == null || work == null) {
				unknownZone += commuters;
				continue;
			}
			
			int persId = 0;
			for (int i = 0; i < commuters; i++) {
				boolean addPerson = numberPersonsProcessed % everyXPerson == 0;
				
				if (addPerson) {
					int age = rel.getAges().get(i);
					int income = rel.getIncome().get(i);
					int housingType = rel.getHousingType().get(i);
					Coord workc = drawCoordFromRandomBuilding(rel.getWork(), "work", 0);
					createOnePerson(persId++, workc, age, income, housingType, "car",
							rel.getRelationKey() + "_", agentAttributes, rel.getHome());
				}
				numberPersonsProcessed++;
			}
		}

		PopulationWriter pw = new PopulationWriter(scenario.getPopulation(),
				scenario.getNetwork());
		pw.write(PLANSFILEOUTPUT);
		
		// Write the attributes of the persons to a file
		ObjectAttributesXmlWriter writer = new ObjectAttributesXmlWriter(agentAttributes);
		writer.writeFile(AGENTATTR);

		log.info("# lines processed: " + linesProcessed);
		log.info("# people living or working in unknown zone: " + unknownZone);
		log.info("# people without working place: " + noWorkingPlace);
		log.info("# people without license or car: " + noLicenseOrCar);
		log.info("# people processed: " + numberPersonsProcessed);
		log.info("# agents added: " + scenario.getPopulation().getPersons().size());
	}

	private void createOnePerson(int i, Coord coordWork,
			int age, int income, int housingType, String mode, String toFromPrefix, ObjectAttributes agentAttributes, County county) {
		
		Person person = scenario.getPopulation().getFactory()
				.createPerson(Id.createPersonId(toFromPrefix + i));

		PersonUtils.setEmployed(person, true);
		
		Plan plan = scenario.getPopulation().getFactory().createPlan();

		int departure = chooseDepartureTime();
		int timeAtWork = createWorkTime();
		
		agentAttributes.putAttribute(person.getId().toString(), "Age", age);
		agentAttributes.putAttribute(person.getId().toString(), "Income", income);
		agentAttributes.putAttribute(person.getId().toString(), "HousingType", housingType);
		
		Coord coord = drawCoordFromRandomBuilding(county, "home", housingType);

		Activity home = scenario.getPopulation().getFactory()
				.createActivityFromCoord("home", coord);
		home.setEndTime(departure);
		plan.addActivity(home);
		
		Leg hinweg = scenario.getPopulation().getFactory().createLeg(mode);
		plan.addLeg(hinweg);

		Activity work = scenario.getPopulation().getFactory()
				.createActivityFromCoord("work", coordWork);
		work.setEndTime(departure + timeAtWork);
		plan.addActivity(work);

		Leg rueckweg = scenario.getPopulation().getFactory().createLeg(mode);
		plan.addLeg(rueckweg);

		Activity home2 = scenario.getPopulation().getFactory()
				.createActivityFromCoord("home", coord);
		plan.addActivity(home2);

		person.addPlan(plan);
		scenario.getPopulation().addPerson(person);
		
		if (housingType == 0) {
			try(PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(SINGLEFAMH, true)))) {
			    out.println(String.valueOf(coord.getX()) + ";" + String.valueOf(coord.getY()));
			}catch (IOException e) {
			    // ...
			}
		}
		else {
			try(PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(MULTIFAMH, true)))) {
			    out.println(String.valueOf(coord.getX()) + ";" + String.valueOf(coord.getY()));
			}catch (IOException e) {
			    // ...
			}
		}
		
		try(PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(AGENTHXYOUTPUT, true)))) {
		    out.println(String.valueOf(coord.getX()) + ";" + String.valueOf(coord.getY()));
		}catch (IOException e) {
		    // ...
		}
		
		try(PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(AGENTWXYOUTPUT, true)))) {
		    out.println(String.valueOf(coordWork.getX()) + ";" + String.valueOf(coordWork.getY()));
		}catch (IOException e) {
		    // ...
		}
	}

	private int chooseDepartureTime() {
/*		final Random r = MatsimRandom.getLocalInstance();
		
		double rnd = r.nextDouble();
		double tmp = 0;
		for(int h=5;h<10;h++) {
			if(rnd > tmp && rnd < tmp + departures.get(h) ) {
				return h * 3600 + (int)(3600 * r.nextDouble());
			}
			tmp += departures.get(h);
		}
*/
		return 8*3600; // Departure time 8.00 initialt f�r alla agenter
	}
	

	private int createWorkTime() {	
/*		int[] range = {462, 560, 700, 887, 1002, 1332, 2017, 2665, 1885, 1038, 546, 268};
		int[] cRange = new int[range.length];
		int[] time = {330, 360, 390, 420, 450, 480, 510, 540, 570, 600, 630, 660};
		
		int lastSize = 0;
		for (int i=0; i<range.length;  i++) {
			cRange[i] = range[i] + lastSize;
			lastSize = cRange[i];	
		}
		
		Random rand = MatsimRandom.getLocalInstance();
		int draw = rand.nextInt(cRange[cRange.length-1]);
		int j = 0;
		while (draw>cRange[j]) {
			j++;
		}
		
		return time[j]*60;
*/
		return 9*3600; // Alla agenter �r borta 9 timmar initialt
	}

	private Coord drawCoordFromRandomBuilding(County county, String activity, int housingType) {
		Random rnd = MatsimRandom.getLocalInstance();	
		Geometry randombuilding = null;
		if (activity == "home"){
			if (housingType == 0) {
				if (!county.getSingleFamBuildings().isEmpty()) {
					randombuilding = county.getSingleFamBuildings().get(rnd.nextInt(county.getSingleFamBuildings().size()));
//					return drawRandomPointFromGeometry(randomBuilding);
				}
//				else {
//					return drawRandomPointFromGeometry(county); 
//				}
			}
			else {
				if (!county.getMultiFamBuildings().isEmpty()) {		
					ArrayList<Integer> probIntervals = createIntervals(county.getMultiFamBuildingSizes());
					randombuilding = assignBuilding(county.getMultiFamBuildings(), probIntervals);
//					return drawRandomPointFromGeometry(randomBuilding);
				}
//				else
//				{
//					return drawRandomPointFromGeometry(county);
//				}
			}
		}			
		else {
			if(!county.getWorkbuildings().isEmpty() || !county.getHomebuildings().isEmpty()) {
				int draw = rnd.nextInt(100);
				if (draw>workersInHomeBuildings) {	
					if(!county.getWorkbuildings().isEmpty()) {
						ArrayList<Integer> probIntervals = createIntervals(county.getWorkBuildingSizes());
						randombuilding = assignBuilding(county.getWorkbuildings(), probIntervals);
//						return drawRandomPointFromGeometry(randomBuilding);
					}
					else {
						randombuilding = county.getHomebuildings().get(rnd.nextInt(county.getHomebuildings().size()));
//						return drawRandomPointFromGeometry(randomBuilding);
					}
				}
				else {
					if (!county.getHomebuildings().isEmpty()) {
						randombuilding = county.getHomebuildings().get(rnd.nextInt(county.getHomebuildings().size()));
//						return drawRandomPointFromGeometry(randomBuilding);
					}
					else {
						ArrayList<Integer> probIntervals = createIntervals(county.getWorkBuildingSizes());
						randombuilding = assignBuilding(county.getWorkbuildings(), probIntervals);
//						return drawRandomPointFromGeometry(randomBuilding);
					}
				}
			}
		}
		if(randombuilding != null){
			return drawRandomPointFromGeometry(randombuilding);
		}
		else {
			return drawRandomPointFromGeometry(county.getGeometry());
		}
	}
	
	//draw a random building where the probabilities are defined by the ranges in bins
	private Geometry assignBuilding(List<Geometry> buildings, ArrayList<Integer> bins){
		Random rand = MatsimRandom.getLocalInstance();
		int draw = rand.nextInt(bins.get(bins.size()-1));
		int j = 0;
		while (draw>bins.get(j)) {
			j++;
		}
		return buildings.get(j);
	}
	
	private ArrayList<Integer> createIntervals(ArrayList<Integer> buildingSizes) {
		ArrayList<Integer> intervals = new ArrayList<Integer>();
		int lastSize = 0;
		for (int i=0; i < buildingSizes.size();  i++) {
			intervals.add(buildingSizes.get(i) + lastSize);
			lastSize = lastSize + buildingSizes.get(i);	
		}
		return intervals;
	}

	// Return random point in geometry
	private Coord drawRandomPointFromGeometry(Geometry geom) {
		Random rnd = MatsimRandom.getLocalInstance();
		Point p;
		double x, y;
		do {
			x = geom.getEnvelopeInternal().getMinX()
					+ rnd.nextDouble()
					* (geom.getEnvelopeInternal().getMaxX() - geom
							.getEnvelopeInternal().getMinX());
			y = geom.getEnvelopeInternal().getMinY()
					+ rnd.nextDouble()
					* (geom.getEnvelopeInternal().getMaxY() - geom
							.getEnvelopeInternal().getMinY());
			p = MGC.xy2Point(x, y);
		} while (!geom.contains(p));
		Coord coord = new Coord(p.getX(), p.getY());
		return coord;
	}

	public void readCountiesShapeFile(String filename, String attrString) {

		Map<String, County> countiesMap = new HashMap<>();
		for (County c : counties) {
			countiesMap.put(c.getKey(), c);
		}

		GeometryFactory geometryFactory = new GeometryFactory();
		WKTReader wktReader = new WKTReader(geometryFactory);

		for (SimpleFeature ft : ShapeFileReader.getAllFeatures(filename)) {
			try {
				String zoneKey = ft.getAttribute(attrString).toString();
				County c = countiesMap.get(zoneKey.substring(0,
						zoneKey.indexOf('.')));
				if (c != null) {
					c.setGeometry(wktReader.read((ft.getAttribute("the_geom"))
							.toString()));
				}
			} catch (ParseException e) {
				throw new RuntimeException(e);
			}

		}
	}

	// Read buildings shapefile
	// ANDAMAL_1T: building use
	public void readBuildings(String filename) {
		
		GeometryFactory geometryFactory = new GeometryFactory();
		WKTReader wktReader = new WKTReader(geometryFactory);

		for (SimpleFeature ft : ShapeFileReader.getAllFeatures(filename)) {

			Geometry geometry;
			String buildingType;			
			try {
				geometry = wktReader.read((ft.getAttribute("the_geom"))
						.toString());
				buildingType = ft.getAttribute("ANDAMAL_1T").toString();
				int buildingSize = Integer.valueOf(ft.getAttribute("AREA").toString());

				for (County county : counties) {
					if (county.getGeometry() != null
							&& county.getGeometry().intersects(geometry)) {
						county.addBuilding(geometry, buildingType);
						county.addArea(buildingSize, buildingType);
					}
				}
			} catch (ParseException e) {
				throw new RuntimeException(e);
			}
		}
	}

	public static void main(String[] args) {
		
		CreateDemand cd = new CreateDemand();
		cd.run();
	}

}