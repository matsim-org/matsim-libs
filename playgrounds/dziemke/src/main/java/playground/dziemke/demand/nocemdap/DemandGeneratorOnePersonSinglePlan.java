package playground.dziemke.demand.nocemdap;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.opengis.feature.simple.SimpleFeature;

import playground.dziemke.demand.CommuterRelation;
import playground.dziemke.demand.PendlerMatrixReader;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;

/**
 * @author dziemke
 * @see adapted from oneperson/DemandGeneratorOnePersonSinglePlan.java
 */
public class DemandGeneratorOnePersonSinglePlan implements Runnable{
  
	private double scalingFactor = 0.01;
	private double carMarketShare = 0.67;
	private double fullyEmployedShare = 1.29;
	
	private String commuterFileIn = "D:/Workspace/container/demand/input/B2009Ge.csv";
	private String commuterFileOut = "D:/Workspace/container/demand/input/B2009Ga.csv";
	
	// maybe not in correct coordinate projection... does this matter here?
	private String shapeFileMunicipalities = "D:/Workspace/container/demand/input/shapefiles/gemeindenBerlin.shp";
	private String shapeFileLors = "D:/Workspace/container/demand/input/shapefiles/Bezirksregion_EPSG_25833.shp";
	
	// this shapefile is definitely in correct coordinate projection
	// it is also used in "cemdap2matsim/single/CemdapStops2MatsimPlansConverter.java"
	private Map<Integer, Geometry> zoneGeometries = ShapeReader.read("D:/Workspace/container/demand/input/shapefiles/gemeindenLOR_DHDN_GK4.shp");
	
	private PendlerMatrixReader pendlerMatrixReader = new PendlerMatrixReader(shapeFileMunicipalities, commuterFileIn, 
			commuterFileOut, scalingFactor, carMarketShare, fullyEmployedShare);
	private List <CommuterRelation> commuterRelations = pendlerMatrixReader.getCommuterRelations();
	
	private Map <Integer, String> lors = new HashMap <Integer, String>();
		
	private Config config = ConfigUtils.createConfig();
	private Scenario scenario = ScenarioUtils.createScenario(config);


	public static void main(String[] args) {
		DemandGeneratorOnePersonSinglePlan demandGenerator = new DemandGeneratorOnePersonSinglePlan();
		demandGenerator.run();		
	}


	@Override
	public void run() {
		readShape();
		generatePersons();
		PopulationWriter populationWriter = new PopulationWriter(scenario.getPopulation(), scenario.getNetwork());
		populationWriter.write("D:/Workspace/container/demand/input/nocemdap/population2.xml");
	}
	
	
	private void readShape() {
		Collection<SimpleFeature> allLors = ShapeFileReader.getAllFeatures(this.shapeFileLors);
	
		for (SimpleFeature lor : allLors) {
			Integer lorschluessel = Integer.parseInt((String) lor.getAttribute("SCHLUESSEL"));
			String name = (String) lor.getAttribute("LOR");
			this.lors.put(lorschluessel, name);
		}
	}
		
		
	private void generatePersons() {
		System.out.println("======================" + "\n"
				   + "Start generating persons" + "\n"
				   + "======================" + "\n");
		
		// Random random = new Random();
		
		Population population = scenario.getPopulation();
		
		for (int i = 0; i<this.commuterRelations.size(); i++){
        	int quantity = this.commuterRelations.get(i).getQuantity();
        	
        	int source = this.commuterRelations.get(i).getFrom();
			int sink = this.commuterRelations.get(i).getTo();
        	
			for (int j = 0; j<quantity; j++){
				int homeTSZLocation;
				int locationOfWork;
				
				// Gemeindeschluessel "11000000" = Berlin
				if (source == 11000000){
					homeTSZLocation = getRandomLor();
				} else {
					homeTSZLocation = source;
				}
								
				if (sink == 11000000){
					locationOfWork = getRandomLor();
				} else {
					locationOfWork = sink;
				}
				
				Geometry homeGeometry = zoneGeometries.get(homeTSZLocation);
				Geometry workGeometry = zoneGeometries.get(locationOfWork);
								
				Person person = population.getFactory().createPerson(createId(source, sink, j));
				Plan plan = population.getFactory().createPlan();
				
				Coord homeLocation = shoot(homeGeometry);
				Coord workLocation = shoot(workGeometry);
				
				Activity homeActivity = population.getFactory().createActivityFromCoord("home", homeLocation);
				int random = (int) (Math.random() * 7200) - 3600;
				// homeActivity.setEndTime(9*60*60 + random);
				homeActivity.setEndTime(7.5*60*60 + random);
				plan.addActivity(homeActivity);
				
				Leg legToWork = population.getFactory().createLeg(TransportMode.car);
				plan.addLeg(legToWork);
				
				Activity workActivity = population.getFactory().createActivityFromCoord("work", workLocation);
				int random2 = (int) (Math.random() * 7200) - 3600;
				// workActivity.setEndTime(17*60*60 + random2);
				workActivity.setEndTime(16*60*60 + random2);				
				plan.addActivity(workActivity);
				
				Leg legToHome = population.getFactory().createLeg(TransportMode.car);
				plan.addLeg(legToHome);
				
				Activity homeActivity2 = population.getFactory().createActivityFromCoord("home", homeLocation);
				plan.addActivity(homeActivity2);
				
				person.addPlan(plan);
				population.addPerson(person);
			}
		}
	}
	
	
	public Integer getRandomLor() {
		List <Integer> keys = new ArrayList<Integer>(this.lors.keySet());
		Random	random = new Random();
		Integer randomLor = keys.get(random.nextInt(keys.size()));
		return randomLor;
	}
	
	
	private Id createId(int source, int sink, int i) {
		return new IdImpl(source + "_" + sink + "_" + i);
	}
	
	
	private Coord shoot(Geometry zone) {
		Random r = new Random();
		Point point = getRandomPointInFeature(r, zone);
		CoordImpl coordImpl = new CoordImpl(point.getX(), point.getY());
		return coordImpl;
	}
	
	
	private static Point getRandomPointInFeature(Random rnd, Geometry g) {
		Point p = null;
		double x, y;
		do {
			x = g.getEnvelopeInternal().getMinX() + rnd.nextDouble() * (g.getEnvelopeInternal().getMaxX() - g.getEnvelopeInternal().getMinX());
			y = g.getEnvelopeInternal().getMinY() + rnd.nextDouble() * (g.getEnvelopeInternal().getMaxY() - g.getEnvelopeInternal().getMinY());
			p = MGC.xy2Point(x, y);
		} while (!g.contains(p));
		return p;
	}
	
}