package playground.dziemke.other;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.api.internal.MatsimWriter;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.scenario.ScenarioUtils;

public class CreatePopulation {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Config config = ConfigUtils.createConfig();
		Scenario scenario = ScenarioUtils.createScenario(config);
		Population population = scenario.getPopulation();   
		new MatsimNetworkReader(scenario.getNetwork()).readFile("D:/Workspace/container/examples/equil/input/network.xml");
		Network network = scenario.getNetwork();
		
		for(Integer i = 1; i <= 5; i++) {
			Person person = population.getFactory().createPerson(Id.create(i.toString(), Person.class));
			Plan plan = population.getFactory().createPlan();
			
			Activity homeActivity = population.getFactory().createActivityFromLinkId("h", Id.create(1, Link.class));
			homeActivity.setEndTime(6*60*60);
			plan.addActivity(homeActivity);
			
			Leg home2workLeg = population.getFactory().createLeg("car");
			plan.addLeg(home2workLeg);
			
			Activity workActivity = population.getFactory().createActivityFromLinkId("w", Id.create(20, Link.class));
			workActivity.setMaximumDuration(30*60);
			plan.addActivity(workActivity);
			
			Leg work2homeLeg = population.getFactory().createLeg("car");
			plan.addLeg(work2homeLeg);
			
			Activity homeActivity2 = population.getFactory().createActivityFromLinkId("h", Id.create(1, Link.class));
			plan.addActivity(homeActivity2);
			
			person.addPlan(plan);
			
			population.addPerson(person);
		}
		
		MatsimWriter popWriter = new PopulationWriter(population, network);
		popWriter.write("D:/Workspace/container/examples/equil-test/input/plans5.xml");
	}
}
