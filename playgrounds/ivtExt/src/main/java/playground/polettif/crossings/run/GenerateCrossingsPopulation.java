package playground.polettif.crossings.run;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import org.matsim.api.core.v01.*;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.api.internal.MatsimWriter;
import org.matsim.core.config.*;
import org.matsim.core.scenario.*;
import org.matsim.core.utils.geometry.CoordUtils;


public class GenerateCrossingsPopulation {

	public static void main(String[] args) throws IOException {
		Config config = ConfigUtils.createConfig();
		Scenario sc = ScenarioUtils.createScenario(config);
		
		Network network = sc.getNetwork();
		Population population = sc.getPopulation();
		PopulationFactory populationFactory = population.getFactory();


		for(int i=1; i < 1000; i+=5) {
			// Create a person with the id i and add it to the population
			Person person = populationFactory.createPerson(Id.createPersonId(Integer.toString(i)));
			population.addPerson(person);
			
			// Create a plan for the person
			Plan plan = populationFactory.createPlan();
			person.addPlan(plan);

			Activity activity1 = populationFactory.createActivityFromLinkId("h",Id.createLinkId("01"));
			activity1.setEndTime(21000+i);
			plan.addActivity(activity1);
			plan.addLeg(populationFactory.createLeg("car"));

			Activity activity2 = populationFactory.createActivityFromLinkId("w",Id.createLinkId("67"));
			activity2.setEndTime(57600+i);
			plan.addActivity(activity2);
			plan.addLeg(populationFactory.createLeg("car"));

			Activity activity3 = populationFactory.createActivityFromLinkId("h", Id.createLinkId("10"));
			plan.addActivity(activity3);
		}

		MatsimWriter popWriter = new PopulationWriter(population, network);
		popWriter.write("C:/Users/polettif/Desktop/crossings/input/small/population.xml");


	}
}




