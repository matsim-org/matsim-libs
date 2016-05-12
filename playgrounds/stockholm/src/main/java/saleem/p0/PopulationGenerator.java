package saleem.p0;


import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.core.api.internal.MatsimWriter;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;

/**
 * 
 * Creates a sample Population of one Person and writes it to a file.
 * 
 * @author michaz
 *
 */
public class PopulationGenerator {
	
	public void generatePopulationForTwoJunctionNetwork(){
		/*
		 * We enter coordinates in the WGS84 reference system, but we want them to appear in the population file
		 * projected to UTM33N, because we also generated the network in UTM33N.
		 */
		CoordinateTransformation ct = 
			 TransformationFactory.getCoordinateTransformation(TransformationFactory.WGS84, TransformationFactory.WGS84_UTM33N);
		
		/*
		 * First, create a new Config and a new Scenario.
		 */
		Config config = ConfigUtils.createConfig();
		Scenario sc = ScenarioUtils.createScenario(config);
		generatePopulationForTwoLinkPaperJunction();
		/*
		 * Pick the Network and the Population out of the Scenario for convenience. 
		 */
		
		Network network = sc.getNetwork();
		Population population = sc.getPopulation();

		/*
		 * Pick the PopulationFactory out of the Population for convenience.
		 * It contains methods to create new Population items.
		 */
		PopulationFactory populationFactory = population.getFactory();

		int key =1;
		for(int i=1; i<=5000;i++){
			key=i;
			Person person = populationFactory.createPerson(Id.createPersonId(key));
			population.addPerson(person);

			/*
			 * Create a Plan for the Person
			 */
			Plan plan = populationFactory.createPlan();
			
			/*
			 * Create a "home" Activity for the Person. In order to have the Person end its day at the same location,
			 * we keep the home coordinates for later use (see below).
			 * Note that we use the CoordinateTransformation created above.
			 */
			Coord homeCoordinates = new Coord(686661.13571, 4827510.51845);
			Activity activity1 = populationFactory.createActivityFromCoord("home", homeCoordinates);
			activity1.setEndTime(21600 + i*0.3); // leave at 6 o'clock, one vehicle entering after other in a short while so that there is no peak at one second. 
			//activity1.setEndTime(21600);
			plan.addActivity(activity1); // add the Activity to the Plan
			
			/*
			 * Create a Leg. A Leg initially hasn't got many attributes. It just says that a car will be used.
			 */
			plan.addLeg(populationFactory.createLeg("car"));
			
			/*
			 * Create a "work" Activity, at a different location.
			 */
			Activity activity2 = populationFactory.createActivityFromCoord("work", new Coord(691410.67204, 4826700.65288));
			activity2.setEndTime(57600); // leave at 4 p.m.
			plan.addActivity(activity2);
			/*
			 * Create another car Leg.
			 */
			person.addPlan(plan);

		}
		for(int i=1; i<=3000;i++){
			key=i+5000;
			Person person = populationFactory.createPerson(Id.createPersonId(key));
			population.addPerson(person);

			/*
			 * Create a Plan for the Person
			 */
			Plan plan = populationFactory.createPlan();
			
			/*
			 * Create a "home" Activity for the Person. In order to have the Person end its day at the same location,
			 * we keep the home coordinates for later use (see below).
			 * Note that we use the CoordinateTransformation created above.
			 */
			Coord homeCoordinates = new Coord(686661.13571, 4826063.88649);
			Activity activity1 = populationFactory.createActivityFromCoord("home", homeCoordinates);
			activity1.setEndTime(21600 + i*0.4); // leave at 6 o'clock, one vehicle entering after other in a short while so that there is no peak at one second. 
			//activity1.setEndTime(21600);
			plan.addActivity(activity1); // add the Activity to the Plan
			
			/*
			 * Create a Leg. A Leg initially hasn't got many attributes. It just says that a car will be used.
			 */
			plan.addLeg(populationFactory.createLeg("car"));
			
			/*
			 * Create a "work" Activity, at a different location.
			 */
			Activity activity2 = populationFactory.createActivityFromCoord("work", new Coord(691410.67204, 4826700.65288));
			activity2.setEndTime(57600); // leave at 4 p.m.
			plan.addActivity(activity2);
			/*
			 * Create another car Leg.
			 */
			person.addPlan(plan);

		}
		for(int i=1; i<=4000;i++){
			key=i+8000;
			Person person = populationFactory.createPerson(Id.createPersonId(key));
			population.addPerson(person);

			/*
			 * Create a Plan for the Person
			 */
			Plan plan = populationFactory.createPlan();
			
			/*
			 * Create a "home" Activity for the Person. In order to have the Person end its day at the same location,
			 * we keep the home coordinates for later use (see below).
			 * Note that we use the CoordinateTransformation created above.
			 */
			Coord homeCoordinates = new Coord(687874.55573,4827510.51845);
			Activity activity1 = populationFactory.createActivityFromCoord("home", homeCoordinates);
			activity1.setEndTime(25500 + i*0.4); // leave at 6 o'clock, one vehicle entering after other in a short while so that there is no peak at one second. 
			//activity1.setEndTime(21600);
			plan.addActivity(activity1); // add the Activity to the Plan
			
			/*
			 * Create a Leg. A Leg initially hasn't got many attributes. It just says that a car will be used.
			 */
			plan.addLeg(populationFactory.createLeg("car"));
			
			/*
			 * Create a "work" Activity, at a different location.
			 */
			Activity activity2 = populationFactory.createActivityFromCoord("work", new Coord(691410.67204, 4826700.65288));
			activity2.setEndTime(57600); // leave at 4 p.m.
			plan.addActivity(activity2);
			/*
			 * Create another car Leg.
			 */
			person.addPlan(plan);

		}
		for(int i=1; i<=4000;i++){
			key=i+12000;
			Person person = populationFactory.createPerson(Id.createPersonId(key));
			population.addPerson(person);

			/*
			 * Create a Plan for the Person
			 */
			Plan plan = populationFactory.createPlan();
			
			/*
			 * Create a "home" Activity for the Person. In order to have the Person end its day at the same location,
			 * we keep the home coordinates for later use (see below).
			 * Note that we use the CoordinateTransformation created above.
			 */
			Coord homeCoordinates = new Coord(687874.55573,4826063.88649);
			Activity activity1 = populationFactory.createActivityFromCoord("home", homeCoordinates);
			activity1.setEndTime(25500 + i*0.4); // leave at 6 o'clock, one vehicle entering after other in a short while so that there is no peak at one second. 
			//activity1.setEndTime(21600);
			plan.addActivity(activity1); // add the Activity to the Plan
			
			/*
			 * Create a Leg. A Leg initially hasn't got many attributes. It just says that a car will be used.
			 */
			plan.addLeg(populationFactory.createLeg("car"));
			
			/*
			 * Create a "work" Activity, at a different location.
			 */
			Activity activity2 = populationFactory.createActivityFromCoord("work", new Coord(691410.67204, 4826700.65288));
			activity2.setEndTime(57600); // leave at 4 p.m.
			plan.addActivity(activity2);
			/*
			 * Create another car Leg.
			 */
			person.addPlan(plan);

		}
		/*
		 * Write the population (of 1 Person) to a file.
		 */
		MatsimWriter popWriter = new org.matsim.api.core.v01.population.PopulationWriter(population, network);
		popWriter.write("H:\\Mike Work\\input\\population-2junctions.xml");

	}
	public void generatePopulationForTwoLinkPaperJunction(){
		
	}
	public static void main(String[] args) {
		PopulationGenerator pgen = new PopulationGenerator();
		pgen.generatePopulationForTwoJunctionNetwork();
	}
	public static String claculateTime(double timeInSeconds){
		int hours = (int) timeInSeconds / 3600;
	    int remainder = (int) timeInSeconds - hours * 3600;
	    int mins = remainder / 60;
	    remainder = remainder - mins * 60;
	    int secs = remainder;
	    String departureoffset = (hours<10)?"0"+hours+":":""+hours+":";//Hours, Mins and Secs in format "00:00:00"
	    departureoffset = (mins<10)?departureoffset+"0"+mins+":":departureoffset+mins+":";
	    departureoffset = (secs<10)?departureoffset+"0"+secs:departureoffset+secs;

		return departureoffset;
	}
	
}
