package patryk.popgen2;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

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
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.population.algorithms.XY2Links;
import org.matsim.utils.objectattributes.ObjectAttributes;
import org.opengis.feature.simple.SimpleFeature;

import patryk.utils.LinksRemover;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.io.ParseException;
import com.vividsolutions.jts.io.WKTReader;

public class CreateDemand2 {
	
	private final String populationFile = "./data/synthetic_population/agentData.csv";
	private final String zonesShapefile = "./data/shapes/zones_EPSG3857.shp";
	private final String buildingsShapefile = "./data/shapes/by_reduc_EPSG3857.shp";
	private final String networkFile = "./data/network/network_v09.xml";
	private final String initialPlansFile = "./data/demand_output/initial_plans.xml";
	private final String agentHomeXY = "./data/demand_output/agenthomeXY.txt";
	private final String agentWorkXY = "./data/demand_output/agentWorkXY.txt";
	private final double scaleFactor = 0.02;
	private final double workersInHomeBuildings = 0.05;
	
	private final HashMap<String, Zone> zones;
	private ArrayList<ParsedPerson> persons;
	private Scenario scenario;
	
	public CreateDemand2() {
		this.scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new MatsimNetworkReader(scenario).readFile(networkFile);
		
		PopulationParser parser = new PopulationParser();
		parser.read(populationFile);
		this.zones = parser.getZones();
		this.persons = parser.getParsedPersons();
		
		readZonesSHP(zonesShapefile);
		readBuildingsSHP(buildingsShapefile);
	}
	
	private void run() {
		int processedCarDrivers = 0;
		int agentsAdded = 0;
		int everyXthPerson = (int) (1/scaleFactor);
		
		ObjectAttributes agentAttributes = scenario.getPopulation().getPersonAttributes();
		LinksRemover linksRem = new LinksRemover(scenario.getNetwork());
		linksRem.run();
		
		XY2Links xy2links = new XY2Links(scenario);
		
		for (ParsedPerson pPerson : persons) {
			if (pPerson.getMode().equals("car") && (pPerson.getHomeZone().substring(0, 2).equals("71") || pPerson.getHomeZone().substring(0, 2).equals("72")) 
					&& (pPerson.getWorkZone().substring(0, 2).equals("71") || pPerson.getWorkZone().substring(0, 2).equals("72"))) {
				if (processedCarDrivers % everyXthPerson == 0) {
					createAgent(pPerson, agentAttributes, xy2links);
					agentsAdded++;
				}
				processedCarDrivers++;
			}
		}
		System.out.println(agentsAdded);
		PopulationWriter popwriter = new PopulationWriter(scenario.getPopulation(), scenario.getNetwork());
		popwriter.write(initialPlansFile);
	}
	
	private void createAgent(ParsedPerson pPerson, ObjectAttributes agentAttributes, XY2Links xy2links) {
		Person agent = scenario.getPopulation().getFactory().createPerson(Id.createPersonId(pPerson.getId()));
		
		agentAttributes.putAttribute(agent.getId().toString(), "age", pPerson.getAge());
		agentAttributes.putAttribute(agent.getId().toString(), "income", pPerson.getIncome());
		agentAttributes.putAttribute(agent.getId().toString(), "sex", pPerson.getSex());
		agentAttributes.putAttribute(agent.getId().toString(), "housingType", pPerson.getHousingType());
		
		Plan plan = scenario.getPopulation().getFactory().createPlan();
		
		Coord homeCoord = drawCoordFromBuilding(zones.get(pPerson.getHomeZone()),
				pPerson.getHousingType(), "home");
		Activity homeMorning = scenario.getPopulation().getFactory()
				.createActivityFromCoord("home", homeCoord);
		homeMorning.setEndTime(departureTime());
		plan.addActivity(homeMorning);
		
		Leg homeToWork = scenario.getPopulation().getFactory().createLeg("car");
		plan.addLeg(homeToWork);
		
		Coord workCoord = drawCoordFromBuilding(zones.get(pPerson.getWorkZone()),
				pPerson.getHousingType(), "work");
		Activity work = scenario.getPopulation().getFactory()
				.createActivityFromCoord("work", workCoord);
		work.setEndTime(workDuration());
		plan.addActivity(work);
		
		Leg workToHome = scenario.getPopulation().getFactory().createLeg("car");
		plan.addLeg(workToHome);
		
		Activity homeEvening = scenario.getPopulation().getFactory()
				.createActivityFromCoord("home", homeCoord);
		plan.addActivity(homeEvening);
		
		agent.addPlan(plan);
		scenario.getPopulation().addPerson(agent);
		
		xy2links.run(agent);
		
		try(PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(agentHomeXY, true)))) {
		    out.println(String.valueOf(homeCoord.getX()) + ";" + String.valueOf(homeCoord.getY()));
		}catch (IOException e) {
		    throw new RuntimeException(e);
		}
		
		try(PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(agentWorkXY, true)))) {
		    out.println(String.valueOf(workCoord.getX()) + ";" + String.valueOf(workCoord.getY()));
		}catch (IOException e) {
		    throw new RuntimeException(e);
		}
	}
	
	private int departureTime() {
		return 7*3600;
	}
	
	private int workDuration() {
		return 9*3600;
	}
	
	private Coord drawCoordFromBuilding(Zone zone, int housingType, String activity) {
		Random rnd = MatsimRandom.getLocalInstance();
		Building randomBuilding = null;
		
		if (activity.equals("home")) {
			if (housingType == 0) {
				if (!zone.getSingleFamilyBuildings().isEmpty()) {
					randomBuilding = zone.getSingleFamilyBuildings()
							.get(rnd.nextInt(zone.getSingleFamilyBuildings().size()));
				}
			}
		
			else {
				if (!zone.getMultiFamilyBuildings().isEmpty()) {		
					ArrayList<Integer> intervalLimits = 
							createIntervalLimits(zone.getMultiFamilyBuildingSizes());
					int indexOfBuilding = assignBuilding(intervalLimits);
					randomBuilding = zone.getMultiFamilyBuildings().get(indexOfBuilding);
				}
			}
		}
	
		else {
			if(!zone.getWorkBuildings().isEmpty() || !zone.getHomeBuildings().isEmpty()) {	
				int draw = rnd.nextInt(100);
				if (draw > workersInHomeBuildings) {	
					if(!zone.getWorkBuildings().isEmpty()) {
						ArrayList<Integer> intervalLimits = createIntervalLimits(zone.getWorkBuildingSizes());
						int indexOfBuilding = assignBuilding(intervalLimits);
						randomBuilding = zone.getWorkBuildings().get(indexOfBuilding);
					}
					else {
						randomBuilding = zone.getHomeBuildings().get(rnd.nextInt(zone.getHomeBuildings().size()));
					}
				}
				else {
					if (!zone.getHomeBuildings().isEmpty()) {
						randomBuilding = zone.getHomeBuildings().get(rnd.nextInt(zone.getHomeBuildings().size()));
					}
					else {
						randomBuilding = zone.getWorkBuildings().get(rnd.nextInt(zone.getWorkBuildings().size()));
					}
				}
			}
		}
			
		if(randomBuilding != null){
			return drawRandomPointFromGeometry(randomBuilding.getGeometry());
		}
		else {
			return drawRandomPointFromGeometry(zone.getGeometry());
		}
	}
		
	private ArrayList<Integer> createIntervalLimits(ArrayList<Integer> buildingSizes) {
		ArrayList<Integer> intervals = new ArrayList<Integer>();
		int lastSize = 0;
		for (int i=0; i < buildingSizes.size();  i++) {
			intervals.add(buildingSizes.get(i) + lastSize);
			lastSize = lastSize + buildingSizes.get(i);	
		}
		return intervals;	
	}
	
	private int assignBuilding(ArrayList<Integer> intervalLimits) {	
		Random rand = MatsimRandom.getLocalInstance();
		int draw = rand.nextInt(intervalLimits.get(intervalLimits.size()-1));
		int index = 0;
		while (draw>intervalLimits.get(index)) {
			index++;
		}
		return index;
	}
	
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
		Coord coord = new CoordImpl(p.getX(), p.getY());
		return coord;
	}
	
	public void readZonesSHP(String filename) {
		GeometryFactory geometryFactory = new GeometryFactory();
		WKTReader wktReader = new WKTReader(geometryFactory);

		for (SimpleFeature ft : ShapeFileReader.getAllFeatures(filename)) {
			try {
				String zoneId = ft.getAttribute("ZONE").toString();
				Zone zone = zones.get(zoneId.substring(0, zoneId.indexOf('.')));
				if (zone != null) {
					zone.setGeometry(wktReader.read((ft.getAttribute("the_geom")).toString()));
				}
			} catch (ParseException e) {
				throw new RuntimeException(e);
			}
		}
	}
	
	public void readBuildingsSHP(String filename) {
		GeometryFactory geometryFactory = new GeometryFactory();
		WKTReader wktReader = new WKTReader(geometryFactory);

		for (SimpleFeature ft : ShapeFileReader.getAllFeatures(filename)) {
			boolean featureAdded = false;
			
			try {
				Geometry geometry = wktReader.read((ft.getAttribute("the_geom")).toString());
				String buildingType = ft.getAttribute("ANDAMAL_1T").toString();
				int buildingSize = Integer.valueOf(ft.getAttribute("AREA").toString());
				Building building = new Building(geometry, buildingSize);
				building.setBuildingType(buildingType);
				
				for (Zone zone : zones.values()) {
					if (zone.getGeometry() != null && zone.getGeometry().intersects(geometry)
							&& featureAdded == false) {
						zone.addBuilding(building);
						featureAdded = true;
					}
				}
			} catch (ParseException e) {
				throw new RuntimeException(e);
			}
		}
	} 

	public static void main(String[] args) {
		CreateDemand2 cd = new CreateDemand2();
		cd.run();
	}
}
