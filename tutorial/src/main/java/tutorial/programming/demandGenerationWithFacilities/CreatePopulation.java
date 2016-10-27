package tutorial.programming.demandGenerationWithFacilities;


import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.core.population.PersonUtils;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.facilities.ActivityFacility;
import org.matsim.utils.objectattributes.ObjectAttributes;


class CreatePopulation {
	
	private Scenario scenario;
	
	// [[ 0 ]] here you have to fill in the path of the census file
	private static final String censusFile = "examples/tutorial/programming/demandGenerationWithFacilities/census.txt";
	private static final String municipalitiesFile = "examples/tutorial/programming/demandGenerationWithFacilities/swiss_municipalities.txt";
	
	private QuadTree<ActivityFacility> homeFacilitiesTree;
	private QuadTree<ActivityFacility> workFacilitiesTree;
	
	private TreeMap<String, Coord> municipalityCentroids = new TreeMap<>();
	private Random random = new Random(3838494); 
	
	private ObjectAttributes personHomeAndWorkLocations = new ObjectAttributes();
	private final static Logger log = Logger.getLogger(CreatePopulation.class);

	// --------------------------------------------------------------------------
	
	public void run(Scenario scenario1) {
		this.scenario = scenario1;
		this.init();
		this.populationCreation();
	}
	
	private void init() {		
		/*
		 * Build quad trees for assigning home and work locations
		 */
		this.homeFacilitiesTree = CreatePopulation.createActivitiesTree("home", this.scenario); 
		this.workFacilitiesTree = CreatePopulation.createActivitiesTree("work", this.scenario); 
		
		this.readMunicipalities();
	}
	
	private void populationCreation() {
		/*
		 * For convenience and code readability store population and population factory in a local variable 
		 */
		Population population = this.scenario.getPopulation();   
		PopulationFactory populationFactory = population.getFactory();

		/*
		 * Read the census file
		 * Create the persons and add the socio-demographics
		 */
		try 
			( BufferedReader bufferedReader = new BufferedReader(new FileReader(CreatePopulation.censusFile)) )
			{
			String line = bufferedReader.readLine(); //skip header
			
			int index_personId = 4;
			int index_age = 6;
			int index_workLocation = 8;
			int index_xHomeCoord = 10;
			int index_yHomeCoord = 11;
			
			while ((line = bufferedReader.readLine()) != null) {
				String parts[] = line.split("\t");
				
				/*
				 * Create a person and add it to the population
				 */
				Person person = populationFactory.createPerson(Id.create(parts[index_personId], Person.class));

				person.getCustomAttributes().put(PersonUtils.AGE, Integer.parseInt(parts[index_age]));
				
				boolean employed = true;
				if (parts[index_workLocation].equals("-1")) employed = false;
				final Boolean employed1 = employed; 
				person.getCustomAttributes().put(PersonUtils.EMPLOYED, employed1);
				
				population.addPerson(person);

				/* 
				 * Assign a home location and buffer it somewhere 
				 * This could also be done in the persons knowledge. But we use ObjectAttributes here.
				 * Try to understand what is happening here [[ 2 ]]
				 */
				Coord homeCoord = new Coord(Double.parseDouble(parts[index_xHomeCoord]), Double.parseDouble(parts[index_yHomeCoord]));
				ActivityFacility homeFacility = this.homeFacilitiesTree.getClosest(homeCoord.getX(), homeCoord.getY());
				if (homeFacility == null) {
					throw new RuntimeException();
				}
				personHomeAndWorkLocations.putAttribute(person.getId().toString(), "home", homeFacility);
				
				if (employed) {
					/*
					 * Assign a work location and buffer it somewhere. 
					 * This could also be done in the persons knowledge. But we use ObjectAttributes here.
					 */
					String municipalityId = parts[index_workLocation];
					ActivityFacility workFacility = this.getWorkFacility(municipalityId);
					personHomeAndWorkLocations.putAttribute(person.getId().toString(), "work", workFacility);
				}
			}
			bufferedReader.close();
			
		} // end try
		catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private void readMunicipalities() {
		try {
			BufferedReader bufferedReader = new BufferedReader(new FileReader(CreatePopulation.municipalitiesFile));
			String line = bufferedReader.readLine(); //skip header
					
			while ((line = bufferedReader.readLine()) != null) {
				String parts[] = line.split("\t");
				
				String id = parts[0];
				/*
				 * COORD: pay attention to coordinate systems!
				 */
				Coord coord = new Coord(Double.parseDouble(parts[1]), Double.parseDouble(parts[2]));
				this.municipalityCentroids.put(id, coord);;
			}
			bufferedReader.close();
			
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	private ActivityFacility getWorkFacility(String municipalityId) {
		Coord coord = this.municipalityCentroids.get(municipalityId);
		ArrayList<ActivityFacility> list = 
			(ArrayList<ActivityFacility>) this.workFacilitiesTree.getDisk(coord.getX(), coord.getY(), 8000);
		
		// pick a facility randomly from this list
		// TODO: check range of randomIndex. Is last element of list ever chosen?
		int randomIndex = (int)(random.nextFloat() * (list.size() - 1));
		return list.get(randomIndex);
	}

	public Scenario getScenario() {
		return scenario;
	}

	public ObjectAttributes getPersonHomeAndWorkLocations() {
		return personHomeAndWorkLocations;
	}
	
	static QuadTree<ActivityFacility> createActivitiesTree(String activityType, Scenario scenario) {
		QuadTree<ActivityFacility> facQuadTree = CreatePopulation.builFacQuadTree(activityType, scenario.getActivityFacilities().getFacilitiesForActivityType(activityType));
		return facQuadTree;
	}

	private static QuadTree<ActivityFacility> builFacQuadTree(String type, Map<Id<ActivityFacility>, ? extends ActivityFacility> facilities_of_type) {
		log.info(" building " + type + " facility quad tree");
		double minx = Double.POSITIVE_INFINITY;
		double miny = Double.POSITIVE_INFINITY;
		double maxx = Double.NEGATIVE_INFINITY;
		double maxy = Double.NEGATIVE_INFINITY;
	
		for (final ActivityFacility f : facilities_of_type.values()) {
			if (f.getCoord().getX() < minx) { minx = f.getCoord().getX(); }
			if (f.getCoord().getY() < miny) { miny = f.getCoord().getY(); }
			if (f.getCoord().getX() > maxx) { maxx = f.getCoord().getX(); }
			if (f.getCoord().getY() > maxy) { maxy = f.getCoord().getY(); }
		}
		minx -= 1.0;
		miny -= 1.0;
		maxx += 1.0;
		maxy += 1.0;
		System.out.println("        xrange(" + minx + "," + maxx + "); yrange(" + miny + "," + maxy + ")");
		QuadTree<ActivityFacility> quadtree = new QuadTree<ActivityFacility>(minx, miny, maxx, maxy);
		for (final ActivityFacility f : facilities_of_type.values()) {
			quadtree.put(f.getCoord().getX(),f.getCoord().getY(),f);
		}
		log.info("Quadtree size: " + quadtree.size());
		return quadtree;
	}
}
