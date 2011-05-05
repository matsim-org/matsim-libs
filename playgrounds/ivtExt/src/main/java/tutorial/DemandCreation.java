package tutorial;


import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.Vector;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.core.api.experimental.facilities.ActivityFacility;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.utils.objectattributes.ObjectAttributes;

import utils.BuildTrees;


public class DemandCreation {
	
	private Scenario scenario;
	
	// We need another population, the PUS population
	private Scenario scenarioPUS;
	
	// [[ 3 ]] here you have to fill in the path of the pus files
	private String pusTripsFile = "";
	private String pusPersonsFile = "";
	
	private ObjectAttributes personHomeAndWorkLocations;
	private Random random = new Random(); 
	
	private List<Id> pusWorkers = new Vector<Id>();
	private List<Id> pusNonWorkers = new Vector<Id>();
	
	private QuadTree<ActivityFacility> shopFacilitiesTree;
	private QuadTree<ActivityFacility> leisureFacilitiesTree;
	private QuadTree<ActivityFacility> educationFacilitiesTree;
	
	
	public void run(Scenario scenario, ObjectAttributes personHomeAndWorkLocations) {
		this.scenario = scenario;
		this.personHomeAndWorkLocations = personHomeAndWorkLocations;
		this.init();
		this.createPUSPersons();
		this.createPUSPlans();
		this.assignPUSPlansToMATSimPopulation();
	}
	
	private void init() {		
		/*
		 * Build quad trees for assigning home and work locations
		 */
		BuildTrees quadTreeCreator = new BuildTrees();
		this.shopFacilitiesTree = quadTreeCreator.createActivitiesTree("shop", this.scenario); 
		this.leisureFacilitiesTree = quadTreeCreator.createActivitiesTree("leisure", this.scenario); 
		this.educationFacilitiesTree = quadTreeCreator.createActivitiesTree("education", this.scenario);
	}
		
	private void createPUSPersons() {
		/*
		 * For convenience and code readability store population and population factory in a local variable 
		 */
		Population population = this.scenarioPUS.getPopulation();   
		PopulationFactory populationFactory = population.getFactory();
		
		/*
		 * Read the PUS file
		 */
		try {
			BufferedReader bufferedReader = new BufferedReader(new FileReader(this.pusPersonsFile));
			String line = bufferedReader.readLine(); //skip header
			
			int index_personId = 0;
			
			while ((line = bufferedReader.readLine()) != null) {
				String parts[] = line.split("\t");
				/*
				 * Create a person and add it to the population
				 */
				Person person = populationFactory.createPerson(this.scenario.createId(parts[index_personId]));
				population.addPerson(person);
				
				((PersonImpl)person).createDesires("desired activity durations");
				/*
				 * Create a day plan and add it to the person
				 */
				Plan plan = populationFactory.createPlan();
				person.addPlan(plan);
				((PersonImpl)person).setSelectedPlan(plan);
			}
		} // end try
		catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/*
	 *  [[ 4 ]]
	 */
	private void createPUSPlans() {
		/*
		 * For convenience and code readability store population and population factory in a local variable 
		 */
		Population population = this.scenarioPUS.getPopulation();   
		PopulationFactory populationFactory = population.getFactory();
		
		/*
		 * Read the PUS trips file
		 */
		try {
			BufferedReader bufferedReader = new BufferedReader(new FileReader(this.pusTripsFile));
			String line = bufferedReader.readLine(); //skip header
			
			int index_personId = 0;
			int index_xCoordOrigin = 0;
			int index_yCoordOrigin = 0;
			int index_xCoordDestination = 0;
			int index_yCoordDestination = 0;
			int index_activityDuration = 0;
			int index_mode = 0;
			int index_activityType = 0;			
			
			Id previousPerson = null;
			while ((line = bufferedReader.readLine()) != null) {
				String parts[] = line.split("\t");

				Id personId = new IdImpl(parts[index_personId]);
				Person person = population.getPersons().get(personId);
				
				Plan plan = person.getSelectedPlan();
				/* 
				 * If a new person is read add a home activity (first origin activity)
				 * Otherwise add a leg and an activity (destination activity)
				 */
				if (!personId.equals(previousPerson)) {
					Coord coordOrigin = this.scenarioPUS.createCoord(Double.parseDouble(parts[index_xCoordOrigin]), 
							Double.parseDouble(parts[index_yCoordOrigin]));
					
					Activity activity = 
						populationFactory.createActivityFromCoord("home", coordOrigin);
					
					plan.addActivity(activity);
				}
				else {
					/*
					 * Add a leg from previous location to this location with the given mode
					 */
					String mode = parts[index_mode];
					plan.addLeg(populationFactory.createLeg(mode));

					/*
					 * Add activity given its type.
					 */
					Coord coordDestination = this.scenarioPUS.createCoord(Double.parseDouble(parts[index_xCoordDestination]), 
							Double.parseDouble(parts[index_yCoordDestination]));
										
					String activityType = parts[index_activityType];

					Activity activity = 
						populationFactory.createActivityFromCoord(activityType, coordDestination);
					
					Double duration = Double.parseDouble(parts[index_activityDuration]);					
					// store the desired duration in the persons knowledge
					((PersonImpl)person).getDesires().putActivityDuration(activityType, duration);
					plan.addActivity(activity);
					
					if (activityType.equals("work")) {
						if (!this.pusWorkers.contains(personId)) this.pusNonWorkers.add(personId);
					}
					else {
						this.pusNonWorkers.add(personId);
					}
				}
			}	
		} // end try
		catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/*
	 * [[ 5 ]]
	 */
	private void assignPUSPlansToMATSimPopulation() {
		/* Iterate through MATSim population and randomly assign a plan from the PUS population.
		 * Adapt the activity locations and the activity end times. 
		 */
		for (Person person : this.scenario.getPopulation().getPersons().values()) {			
			if (((PersonImpl)person).isEmployed()) {
				Collections.shuffle(this.pusWorkers, this.random);
				Person pusPerson = this.scenarioPUS.getPopulation().getPersons().get(this.pusWorkers.get(0));
				Plan plan = pusPerson.getSelectedPlan();
				this.adaptPlan(plan, true);	
				person.addPlan(plan);
			}
			else {
				Collections.shuffle(this.pusNonWorkers, this.random);
				Person pusPerson = this.scenarioPUS.getPopulation().getPersons().get(this.pusNonWorkers.get(0));
				Plan plan = pusPerson.getSelectedPlan();
				this.adaptPlan(plan, false);	
				person.addPlan(plan);
			}
		}
	}
	
	private void adaptPlan(Plan plan, boolean worker) {
		Person person = plan.getPerson();
		
		/*
		 * Go through plan and adapt locations and times
		 */
		int counter = 0;
		double time = 0.0;
		Activity previousActivity = null;
		for (PlanElement pe : plan.getPlanElements()) {
			if (pe instanceof Activity) {
				ActivityImpl activity = (ActivityImpl)pe;
				ActivityFacility facility;
				
				if (activity.getType().equals("home")) {
					facility = (ActivityFacility)this.personHomeAndWorkLocations.getAttribute(person.getId().toString(), "home");
				}
				else if (activity.getType().equals("work")) {
					facility = (ActivityFacility)this.personHomeAndWorkLocations.getAttribute(person.getId().toString(), "work");
				}
				else {
					facility = this.getRandomLocation(activity, previousActivity);
				}
				if (counter == 0) {
					time = this.randomizeTimes(7.0 * 3600.0);
				}
				else {
					double activityDuration = ((PersonImpl)person).getDesires().getActivityDuration(activity.getType());
					time += this.randomizeTimes(time + activityDuration);
				}
				activity.setFacilityId(facility.getId());
				activity.setLinkId(facility.getLinkId());
				activity.setCoord(facility.getCoord());
				activity.setEndTime(time);
				
				previousActivity = activity;
			}
			else {
				Leg leg = (Leg)pe;
				leg.setDepartureTime(time);
			}
			counter++;
		}
	}
	
	private ActivityFacility getRandomLocation(Activity activity, Activity previousActivity) {		
		double xCoordCenter = (activity.getCoord().getX() + previousActivity.getCoord().getX()) / 2.0;
		double yCoordCenter = (activity.getCoord().getY() + previousActivity.getCoord().getY() ) / 2.0;
		ArrayList<ActivityFacility> facilities;
		
		// actually we should check if a facility is in the circle. But for simplicity we do not do that as we know, that there is one
		if (activity.getType().equals("shop")) {
			facilities = (ArrayList<ActivityFacility>) this.shopFacilitiesTree.get(xCoordCenter, yCoordCenter, 8000);
		}
		else if (activity.getType().equals("leisure")) {
			facilities = (ArrayList<ActivityFacility>) this.leisureFacilitiesTree.get(xCoordCenter, yCoordCenter, 8000);
		}
		else {
			facilities = (ArrayList<ActivityFacility>) this.educationFacilitiesTree.get(xCoordCenter, yCoordCenter, 8000);
		}
		int randomIndex = (int)(random.nextFloat() * (facilities.size()));
		return facilities.get(randomIndex);
	}	
			
	private double randomizeTimes(double time) {
		final double sigma = 2.0;
		return random.nextGaussian() * sigma + time;
	}
	
	public Scenario getScenario() {
		return scenario;
	}
}
