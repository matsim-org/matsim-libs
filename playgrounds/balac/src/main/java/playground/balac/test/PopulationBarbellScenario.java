package playground.balac.test;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.api.internal.MatsimWriter;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;

public class PopulationBarbellScenario {

	public static void main(String[] args) {

		/*
		 * First, create a new Config and a new Scenario.
		 */
		Config config = ConfigUtils.createConfig();
		Scenario sc = ScenarioUtils.createScenario(config);

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

		/*
		 * Create a Person designated "1" and add it to the Population.
		 */
	
		Coord[] homeCoordinates = new Coord[3];
		homeCoordinates[0] = new Coord(-200.0,0.0);
		homeCoordinates[1] = new Coord(-0.0,200.0);
		homeCoordinates[2] = new Coord(-0.0,-200.0);
		
		Coord[] workCoordinates = new Coord[3];
		workCoordinates[0] = new Coord(1000.0,200.0);
		workCoordinates[1] = new Coord(1200.0,0.0);
		workCoordinates[2] = new Coord(1000.0,-200.0);
		
		Coord[] leisureCoordinates = new Coord[2];
		leisureCoordinates[0] = new Coord(0.0,0.0);
		leisureCoordinates[1] = new Coord(1000.0,0.0);
		for (int i = 0; i < 600; i++) {
			Person person = populationFactory.createPerson(Id.create(Integer.toString(i), Person.class));
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
			Activity activity1 = populationFactory.createActivityFromCoord("home", homeCoordinates[i % 3]);
			activity1.setEndTime(21600); // leave at 6 o'clock
			plan.addActivity(activity1); // add the Activity to the Plan
			
			/*
			 * Create a Leg. A Leg initially hasn't got many attributes. It just says that a car will be used.
			 */
			plan.addLeg(populationFactory.createLeg("car"));
			
			/*
			 * Create a "work" Activity, at a different location.
			 */
			Activity activity2 = populationFactory.createActivityFromCoord("work", workCoordinates[i % 3]);
			activity2.setEndTime(57600); // leave at 4 p.m.
			plan.addActivity(activity2);
			
			/*
			 * Create another car Leg.
			 */
			plan.addLeg(populationFactory.createLeg("car"));
			
			Activity activity3 = populationFactory.createActivityFromCoord("leisure", leisureCoordinates[i % 2]);
			activity3.setEndTime(61200); // leave at 4 p.m.
			plan.addActivity(activity3);
			
			
			plan.addLeg(populationFactory.createLeg("car"));

			/*
			 * End the day with another Activity at home. Note that it gets the same coordinates as the first activity.
			 */
			Activity activity4 = populationFactory.createActivityFromCoord("home", homeCoordinates[i % 3]);
			plan.addActivity(activity4);
			person.addPlan(plan);
		}

		/*
		 * Write the population (of 1 Person) to a file.
		 */
		MatsimWriter popWriter = new PopulationWriter(population, network);
		popWriter.write("C:\\LocalDocuments\\Scenarios\\Barbel\\population.xml");
		
		
		
		
	}

}
