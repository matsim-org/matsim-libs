/**
 * 
 */
package playground.dgrether.koehlerstrehlersignal.braessscenario;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.scenario.ScenarioUtils;

import playground.dgrether.DgPaths;

/**
 * Class to create a population for the braess scenario.
 * Choose the number of persons you like to simulate before starting this class.
 * 
 * @author tthunig
 */
public class TtCreateBraessPopulation {

	private Population population;
	private Network network;

	public TtCreateBraessPopulation(Population pop, Network net) {
		this.population = pop;
		this.network = net;
	}

	/**
	 * @param args not used
	 */
	public static void main(String[] args) {
		
		int numberOfPersons = 3600;
		String outputDir = DgPaths.SHAREDSVN
				+ "/projects/cottbus/data/scenarios/braess_scenario/";
		String popOutputFile = outputDir + "plans" + numberOfPersons + ".xml";

		Config config = ConfigUtils.createConfig();
		config.network().setInputFile(outputDir + "network_8640_5s.xml");
		Scenario scenario = ScenarioUtils.loadScenario(config);
		Network network = scenario.getNetwork();
		Population pop = PopulationUtils.createPopulation(config, network);

		TtCreateBraessPopulation creator = new TtCreateBraessPopulation(pop, network);
		creator.createPersons(numberOfPersons);
		creator.writePersons(popOutputFile);
	}

	private void writePersons(String popOutputFile) {
		new PopulationWriter(population, network).write(popOutputFile);
	}

	/**
	 * Fills a population container with the given number of persons. All
	 * persons travel from the left to the right through the network as in
	 * Braess's original paradox.
	 * 
	 * @param numberOfPersons
	 */
	private void createPersons(int numberOfPersons) {

		for (int i = 0; i < numberOfPersons; i++) {

			// create a person and a plan container
			Person person = population.getFactory().createPerson(
					Id.create(i, Person.class));
			Plan plan = population.getFactory().createPlan();

			// add a start activity at link 1
			Activity homeAct = population.getFactory()
					.createActivityFromLinkId("dummy", Id.create(1, Link.class));
			homeAct.setEndTime(8*3600 + i); // 8:00 am. plus i seconds
			plan.addActivity(homeAct);
			
			// add a leg
			plan.addLeg(population.getFactory().createLeg(TransportMode.car));
			
			// add a drain activity at link 7
			plan.addActivity(population.getFactory()
					.createActivityFromLinkId("dummy", Id.create(7, Link.class)));

			// store information in population
			person.addPlan(plan);
			population.addPerson(person);

		}

	}

}
