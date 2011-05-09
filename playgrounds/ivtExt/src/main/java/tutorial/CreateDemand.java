package tutorial;


import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.Vector;

import org.apache.log4j.Logger;
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
import org.matsim.core.population.PlanImpl;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.core.utils.misc.ConfigUtils;
import org.matsim.utils.objectattributes.ObjectAttributes;

import utils.BuildTrees;


public class CreateDemand {
	private Scenario scenario;
	
	// We need another population, the PUS population
	private Scenario scenarioPUS;
	
	// [[ 3 ]] here you have to fill in the path of the pus files
	private String pusTripsFile = "./input/travelsurvey_trips.txt";		// [[ 3 ]] = "";
	private String pusPersonsFile = "./input/travelsurvey_persons.txt";
	
	private ObjectAttributes personHomeAndWorkLocations;
	private Random random = new Random(3838494); 
	
	private List<Id> pusWorkers = new Vector<Id>();
	private List<Id> pusNonWorkers = new Vector<Id>();
	
	private QuadTree<ActivityFacility> shopFacilitiesTree;
	private QuadTree<ActivityFacility> leisureFacilitiesTree;
	private QuadTree<ActivityFacility> educationFacilitiesTree;
	
	private final static Logger log = Logger.getLogger(CreateDemand.class);
	
	
	public void run(Scenario scenario, ObjectAttributes personHomeAndWorkLocations) {
		this.scenario = scenario;
		this.scenarioPUS = ScenarioUtils.createScenario(ConfigUtils.createConfig());
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
				Person person = populationFactory.createPerson(this.scenario.createId(parts[index_personId].trim()));
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
			int index_xCoordOrigin = 2;
			int index_yCoordOrigin = 3;
			int index_xCoordDestination = 4;
			int index_yCoordDestination = 5;
			int index_activityDuration = 6;
			int index_mode = 7;
			int index_activityType = 8;			
			
			Id previousPerson = null;
			boolean worker = false;
			
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
					
					// define if previous person is a worker or not
					if (previousPerson != null) {
						if (worker) {
							this.pusWorkers.add(previousPerson);
						}
						else {
							this.pusNonWorkers.add(previousPerson);
						}
					}
					worker = false;
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
										
					String activityType = parts[index_activityType].trim();
					if (activityType.startsWith("w")) worker = true;

					Activity activity = 
						populationFactory.createActivityFromCoord(activityType, coordDestination);
					
					Double duration = Double.parseDouble(parts[index_activityDuration]);		
					// store the desired duration in the persons knowledge
					((PersonImpl)person).getDesires().putActivityDuration(activityType, duration);
					plan.addActivity(activity);
				}
				previousPerson = personId;
			}
			log.info("Number of workers: " + this.pusWorkers.size());
			log.info("Number of non-workers: " + this.pusNonWorkers.size());
		} // end try
		catch (IOException e) {
			e.printStackTrace();
		}
		PopulationWriter populationWriter = new PopulationWriter(this.scenarioPUS.getPopulation(), this.scenario.getNetwork());
		populationWriter.write("./output/PUSplans.xml.gz");
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
				Plan plan = this.adaptAndCopyPlan(person, pusPerson.getSelectedPlan(), true);	
				person.addPlan(plan);
			}
			else {
				Collections.shuffle(this.pusNonWorkers, this.random);
				Person pusPerson = this.scenarioPUS.getPopulation().getPersons().get(this.pusNonWorkers.get(0));
				Plan plan =  this.adaptAndCopyPlan(person, pusPerson.getSelectedPlan(), false);	
				person.addPlan(plan);
			}
		}
	}
	
	private Plan adaptAndCopyPlan(Person person, Plan plan, boolean worker) {		
		PlanImpl newPlan = new PlanImpl();
		newPlan.copyPlan(plan);
		/*
		 * Go through plan and adapt locations and times
		 */
		int counter = 0;
		double time = 0.0;
		Activity previousActivity = null;
		String firstType = "";
		for (PlanElement pe : newPlan.getPlanElements()) {
			if (pe instanceof Activity) {
				ActivityImpl activity = (ActivityImpl)pe;
				ActivityFacility facility;
				
				if (activity.getType().startsWith("h")) {
					facility = (ActivityFacility)this.personHomeAndWorkLocations.getAttribute(person.getId().toString(), "home");
				}
				else if (activity.getType().startsWith("w")) {
					facility = (ActivityFacility)this.personHomeAndWorkLocations.getAttribute(person.getId().toString(), "work");
				}
				else {
					facility = this.getRandomLocation(activity, previousActivity.getCoord());
				}
				
				if (counter == 0) {
					time = 8.0 * 3600.0 + this.randomizeTimes();
					int suffix = (int)(time / 3600.0);
					activity.setType("h" + suffix);
					firstType = activity.getType();
					activity.setEndTime(time);
				}
				else if (counter == newPlan.getPlanElements().size() -1) {
					activity.setType(firstType);
				}
				else {
					Person pusPerson = plan.getPerson();
					double activityDuration = ((PersonImpl)pusPerson).getDesires().getActivityDuration(activity.getType());
					
					time += activityDuration + this.randomizeTimes();
					String dur = String.valueOf((int)(activityDuration / 3600.0));
					if (dur.equals("0")) dur = "0.5";
					activity.setType(activity.getType().substring(0, 1) + dur);
					activity.setEndTime(time);
				}								
				activity.setFacilityId(facility.getId());
				activity.setLinkId(facility.getLinkId());
				activity.setCoord(facility.getCoord());
				
				
				previousActivity = activity;
			}
			else {
				Leg leg = (Leg)pe;
				leg.setDepartureTime(time);
			}
			counter++;
		}
		return newPlan;
	}
	
	private ActivityFacility getRandomLocation(Activity activity, Coord coordPreviousActivity) {		
		double xCoordCenter = coordPreviousActivity.getX();
		double yCoordCenter = coordPreviousActivity.getY();
		ArrayList<ActivityFacility> facilities = new ArrayList<ActivityFacility>();
		
		if (activity.getType().startsWith("s")) {
			double radius = 8000.0;
			while (facilities.size() == 0) {
				facilities = (ArrayList<ActivityFacility>) this.shopFacilitiesTree.get(xCoordCenter, yCoordCenter, radius);
				radius *= 2.0;
			}
		}
		else if (activity.getType().startsWith("l")) {
			double radius = 8000.0;
			while (facilities.size() == 0) {
				facilities = (ArrayList<ActivityFacility>) this.leisureFacilitiesTree.get(xCoordCenter, yCoordCenter, radius);
				radius *= 2.0;
			}
		}
		else {
			double radius = 8000.0;
			while (facilities.size() == 0) {
				facilities = (ArrayList<ActivityFacility>) this.educationFacilitiesTree.get(xCoordCenter, yCoordCenter, radius);
				radius *= 2.0;
			}
		}
		int randomIndex = (int)(random.nextFloat() * (facilities.size()));
		return facilities.get(randomIndex);
	}	
			
	private double randomizeTimes() {
		final double sigma = 1.0;
		return random.nextGaussian() * sigma * 3600.0;
	}
	
	public Scenario getScenario() {
		return scenario;
	}
}
