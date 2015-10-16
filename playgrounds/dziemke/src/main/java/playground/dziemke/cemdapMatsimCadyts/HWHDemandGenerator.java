package playground.dziemke.cemdapMatsimCadyts;

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
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.opengis.feature.simple.SimpleFeature;

import playground.dziemke.utils.ShapeReader;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;

/**
 * @author dziemke
 * @see adapted from oneperson/DemandGeneratorOnePersonSinglePlan.java
 */
public class HWHDemandGenerator {
	
	
	public static void main(String[] args) {
		// main parameters
		double scalingFactor = 0.01;
		double carShareBE = 0.37;
		double carShareBB = 0.55;
		double socialSecurityFactor = 1.52;
		// double adultsWorkersFactor = 1.9;
		double adultsWorkersFactor = 1.;
		double expansionFactor = 1.;
		
		// new
		int numberOfNonStayHomePlansPerPerson = 7;
		boolean addStayHomePlan = true;
		// end new
		
		// Gemeindeschluessel of Berlin is 11000000
		Integer planningAreaId = 11000000;
		
		// Input and output files
		String outputFile = "D:/Workspace/container/demand/input/hwh/population6.xml";
		
		String commuterFileIn = "D:/VSP/CemdapMatsimCadyts/Data/BA-Pendlerstatistik/Berlin2009/B2009Ge.txt";
		String commuterFileOut = "D:/VSP/CemdapMatsimCadyts/Data/BA-Pendlerstatistik/Berlin2009/B2009Ga.txt";

		// maybe not in correct coordinate projection... does this matter here?
		String shapeFileMunicipalities = "D:/Workspace/container/demand/input/shapefiles/gemeindenBerlin.shp";
		String shapeFileLors = "D:/Workspace/container/demand/input/shapefiles/Bezirksregion_EPSG_25833.shp";
		
		String shapeFileEvaluationArea = "D:/Workspace/container/demand/input/shapefiles/gemeindenLOR_DHDN_GK4.shp";
		
		
		
		// this shapefile is definitely in correct coordinate projection
		// it is also used in "cemdap2matsim/single/CemdapStops2MatsimPlansConverter.java"
		Map<Integer, Geometry> zoneGeometries = ShapeReader.read(shapeFileEvaluationArea, "NR");
						
		CommuterFileReader commuterFileReader = new CommuterFileReader(shapeFileMunicipalities, commuterFileIn, carShareBB,	commuterFileOut, 
				//carShareBE, scalingFactor * socialSecurityFactor * adultsWorkersFactor * expansionFactor, planningAreaId.toString());
				carShareBE, scalingFactor * socialSecurityFactor * adultsWorkersFactor * expansionFactor, planningAreaId);
		List<CommuterRelation> commuterRelations = commuterFileReader.getCommuterRelations();
		
		
		// read in LOR file and store LORs to a map
		Map <Integer, String> lors = new HashMap <Integer, String>();
		Collection<SimpleFeature> allLors = ShapeFileReader.getAllFeatures(shapeFileLors);
		
		for (SimpleFeature lor : allLors) {
			Integer lorschluessel = Integer.parseInt((String) lor.getAttribute("SCHLUESSEL"));
			String name = (String) lor.getAttribute("LOR");
			lors.put(lorschluessel, name);
		}
		
		
		// generate the population					
		Config config = ConfigUtils.createConfig();
		Scenario scenario = ScenarioUtils.createScenario(config);
	
		Population population = scenario.getPopulation();
			
		for (int i = 0; i < commuterRelations.size(); i++) {
	       	int quantity = commuterRelations.get(i).getQuantity();
	       	
	       	int source = commuterRelations.get(i).getFrom();
			int sink = commuterRelations.get(i).getTo();
	       	
			for (int j = 0; j<quantity; j++){
				int homeTSZLocation;
				// int locationOfWork;
				
				if (source == planningAreaId){
					homeTSZLocation = getRandomLor(lors);
				} else {
					homeTSZLocation = source;
				}
				
				// new
				Geometry homeGeometry = zoneGeometries.get(homeTSZLocation);
				
				Id<Person> id = Id.create(source + "_" + sink + "_" + j, Person.class);
				Person person = population.getFactory().createPerson(id);
				// Plan plan = population.getFactory().createPlan();
				
				Coord homeLocation = shoot(homeGeometry);
				
				for (int k=1; k<=numberOfNonStayHomePlansPerPerson; k++) {
					Plan plan = population.getFactory().createPlan();
										
					int locationOfWork;
					// end new
					
					if (sink == planningAreaId){
						locationOfWork = getRandomLor(lors);
					} else {
						locationOfWork = sink;
					}
					
					// Geometry homeGeometry = zoneGeometries.get(homeTSZLocation);
					Geometry workGeometry = zoneGeometries.get(locationOfWork);
									
					// Id id = new IdImpl(source + "_" + sink + "_" + j);
					// Person person = population.getFactory().createPerson(id);
					// Plan plan = population.getFactory().createPlan();
					
					// Coord homeLocation = shoot(homeGeometry);
					Coord workLocation = shoot(workGeometry);
					
					Activity homeActivity = population.getFactory().createActivityFromCoord("home", homeLocation);
					//int random = (int) (Math.random() * 7200) - 3600;
					int random = (int) (Math.random() * 10800) - 5400;
					// homeActivity.setEndTime(7.5*60*60 + random);
					homeActivity.setEndTime(8*60*60 + random);
					plan.addActivity(homeActivity);
					
					Leg legToWork = population.getFactory().createLeg(TransportMode.car);
					plan.addLeg(legToWork);
					
					Activity workActivity = population.getFactory().createActivityFromCoord("work", workLocation);
					//int random2 = (int) (Math.random() * 7200) - 3600;
					int random2 = (int) (Math.random() * 10800) - 5400;
					//workActivity.setEndTime(16*60*60 + random2);
					workActivity.setEndTime(16.5*60*60 + random2);
					plan.addActivity(workActivity);
					
					Leg legToHome = population.getFactory().createLeg(TransportMode.car);
					plan.addLeg(legToHome);
					
					Activity homeActivity2 = population.getFactory().createActivityFromCoord("home", homeLocation);
					plan.addActivity(homeActivity2);
					
					person.addPlan(plan);
				}
				
				// new
				if (addStayHomePlan == true) {
					Plan plan = population.getFactory().createPlan();
					Activity homeActivity = population.getFactory().createActivityFromCoord("home", homeLocation);
					plan.addActivity(homeActivity);
					person.addPlan(plan);
				}
				// end new
			
				population.addPerson(person);
			}
		}
		
		// write population file
		PopulationWriter populationWriter = new PopulationWriter(scenario.getPopulation(), scenario.getNetwork());
		populationWriter.write(outputFile);
	}

	
	private static Integer getRandomLor(Map <Integer, String> lors) {
		List <Integer> keys = new ArrayList<Integer>(lors.keySet());
		Random	random = new Random();
		Integer randomLor = keys.get(random.nextInt(keys.size()));
		return randomLor;
	}

	
	private static Coord shoot(Geometry geometry) {
		Random rnd = new Random();
		//Point point = getRandomPointInFeature(r, zone);
		Point point = null;
		double x, y;
		
		do {
			x = geometry.getEnvelopeInternal().getMinX() + rnd.nextDouble() * (geometry.getEnvelopeInternal().getMaxX() - geometry.getEnvelopeInternal().getMinX());
			y = geometry.getEnvelopeInternal().getMinY() + rnd.nextDouble() * (geometry.getEnvelopeInternal().getMaxY() - geometry.getEnvelopeInternal().getMinY());
			point = MGC.xy2Point(x, y);
		} while (!geometry.contains(point));

		Coord coord = new Coord(point.getX(), point.getY());
		return coord;
	}	
}