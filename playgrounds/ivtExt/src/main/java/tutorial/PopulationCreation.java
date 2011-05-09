package tutorial;


import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Random;
import java.util.TreeMap;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.core.api.experimental.facilities.ActivityFacility;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.utils.objectattributes.ObjectAttributes;


import utils.BuildTrees;


public class PopulationCreation {
	
	private Scenario scenario;
	
	// [[ 0 ]] here you have to fill in the path of the census file
	private String censusFile = "./input/census.txt";	// [[ 0 ]] censusFile = ""	
	private String municipalitiesFile = "./input/swiss_municipalities.txt";
	
	private QuadTree<ActivityFacility> homeFacilitiesTree;
	private QuadTree<ActivityFacility> workFacilitiesTree;
	
	private TreeMap<Id, Coord> municipalityCentroids = new TreeMap<Id, Coord>();
	private Random random = new Random(3838494); 
	
	private ObjectAttributes personHomeAndWorkLocations = new ObjectAttributes();

	// --------------------------------------------------------------------------
	
	public void run(Scenario scenario) {
		this.scenario = scenario;
		this.init();
		this.populationCreation();
	}
	
	private void init() {		
		/*
		 * Build quad trees for assigning home and work locations
		 */
		BuildTrees quadTreeCreator = new BuildTrees();
		this.homeFacilitiesTree = quadTreeCreator.createActivitiesTree("home", this.scenario); 
		this.workFacilitiesTree = quadTreeCreator.createActivitiesTree("work", this.scenario); 
		
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
		try {
			BufferedReader bufferedReader = new BufferedReader(new FileReader(this.censusFile));
			String line = bufferedReader.readLine(); //skip header
			
			/* 
			 * [[ 1 ]] here you have to set the indices accordingly. 
			 *  Please note that in programming we always start with 0 and not with 1
			 */
			int index_personId = 4;		// = [[ 1 ]]  = 0;
			int index_age = 6;
			int index_workLocation = 8;
			int index_xHomeCoord = 10;
			int index_yHomeCoord = 11;
			
			while ((line = bufferedReader.readLine()) != null) {
				String parts[] = line.split("\t");
				
				/*
				 * Create a person and add it to the population
				 */
				Person person = populationFactory.createPerson(this.scenario.createId(parts[index_personId]));
				((PersonImpl)person).setAge(Integer.parseInt(parts[index_age]));
				
				boolean employed = true;
				if (parts[index_workLocation].equals("-1")) employed = false; 
				((PersonImpl)person).setEmployed(employed);
				population.addPerson(person);

				/* 
				 * Assign a home location and buffer it somewhere 
				 * This could also be done in the persons knowledge. But we use ObjectAttributes here.
				 * Try to understand what is happening here [[ 2 ]]
				 */
				Coord homeCoord = new CoordImpl(Double.parseDouble(parts[index_xHomeCoord]),
						Double.parseDouble(parts[index_yHomeCoord]));
				ActivityFacility homeFacility = this.homeFacilitiesTree.get(homeCoord.getX(), homeCoord.getY());
				personHomeAndWorkLocations.putAttribute(person.getId().toString(), "home", homeFacility);
				
				if (employed) {
					/*
					 * Assign a work location and buffer it somewhere. 
					 * This could also be done in the persons knowledge. But we use ObjectAttributes here.
					 */
					Id municipalityId = new IdImpl(Integer.parseInt(parts[index_workLocation]));
					ActivityFacility workFacility = this.getWorkFacility(municipalityId);
					personHomeAndWorkLocations.putAttribute(person.getId().toString(), "work", workFacility);
				}
			}
			
		} // end try
		catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private void readMunicipalities() {
		try {
			BufferedReader bufferedReader = new BufferedReader(new FileReader(this.municipalitiesFile));
			String line = bufferedReader.readLine(); //skip header
					
			while ((line = bufferedReader.readLine()) != null) {
				String parts[] = line.split("\t");
				
				Id id = new IdImpl(parts[0]);
				/*
				 * COORD: pay attention to coordinate systems!
				 */
				Coord coord = new CoordImpl(Double.parseDouble(parts[1]), Double.parseDouble(parts[2]));
				this.municipalityCentroids.put(id, coord);;
			}
			
		} // end try
		catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private ActivityFacility getWorkFacility(Id municipalityId) {
		Coord coord = this.municipalityCentroids.get(municipalityId);
		ArrayList<ActivityFacility> list = 
			(ArrayList<ActivityFacility>) this.workFacilitiesTree.get(coord.getX(), coord.getY(), 8000);
		
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
}
