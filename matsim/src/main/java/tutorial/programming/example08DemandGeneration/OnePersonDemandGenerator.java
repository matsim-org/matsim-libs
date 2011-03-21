package tutorial.programming.example08DemandGeneration;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.core.api.internal.MatsimWriter;
import org.matsim.core.config.Config;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.misc.ConfigUtils;


public class OnePersonDemandGenerator {

	public static void main(String[] args) {
		CoordinateTransformation ct = 
			 TransformationFactory.getCoordinateTransformation(TransformationFactory.WGS84, TransformationFactory.WGS84_UTM35S);
		Config config = ConfigUtils.createConfig();
		Scenario sc = ScenarioUtils.createScenario(config);

		Network network = sc.getNetwork();
		Population population = sc.getPopulation();

		PopulationFactory populationFactory = population.getFactory(); // (*)

		Person person = populationFactory.createPerson(sc.createId("1"));
		population.addPerson(person) ;

		Plan plan = populationFactory.createPlan();
		Activity activity1 = populationFactory.createActivityFromCoord("home", ct.transform(sc.createCoord(14.31377, 51.76948)));
		activity1.setEndTime(21600);
		plan.addActivity(activity1);
		plan.addLeg(populationFactory.createLeg("car"));
		Activity activity2 = populationFactory.createActivityFromCoord("work", ct.transform(sc.createCoord(14.34024, 51.75649)));
		activity2.setEndTime(57600);
		plan.addActivity(activity2);
		plan.addLeg(populationFactory.createLeg("car"));
		Activity activity3 = populationFactory.createActivityFromCoord("home", ct.transform(sc.createCoord(14.31377, 51.76948)));
		plan.addActivity(activity3);
		person.addPlan(plan);

		
		
		MatsimWriter popWriter = new org.matsim.api.core.v01.population.PopulationWriter(population, network);
		popWriter.write("./input/population.xml");
	}
	
}
