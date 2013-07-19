package playground.wrashid.lib.tools.plan;

import java.util.LinkedList;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.contrib.parking.lib.GeneralLib;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.scenario.ScenarioUtils;

public class PlanExpander {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		String inputNetworkFile = "C:/data/workspace3/matsim/src/test/resources/test/scenarios/berlin/network.xml.gz";
		String inputPlansFile = "C:/data/workspace3/matsim/src/test/resources/test/scenarios/berlin/plans_hwh_1pct.xml.gz";
		int populationExpansionFactor = 100;

		String outputPlansFile = "C:/tmp/plans_hwh_100pct.xml.gz";
		Scenario scenario = GeneralLib.readScenario(inputPlansFile,
				inputNetworkFile);

		Population population = scenario.getPopulation();

		PopulationFactory factory = scenario.getPopulation().getFactory();

		LinkedList<Person> originalAgents = new LinkedList<Person>();

		for (Person p : population.getPersons().values()) {
			originalAgents.add(p);
		}

		for (Person p : originalAgents) {
			scenario.getPopulation().getPersons().remove(p.getId());
		}

		int pCounter = 1;

		for (int i = 0; i < populationExpansionFactor; i++) {
			for (Person origPerson : originalAgents) {
				PersonImpl originPersonImpl=(PersonImpl) origPerson;
				
				Person newPerson = factory.createPerson(scenario.createId(String
						.valueOf(pCounter++)));
				newPerson.addPlan(originPersonImpl.copySelectedPlan());
				scenario.getPopulation().addPerson(newPerson);
			}
		}

		new PopulationWriter(scenario.getPopulation(), scenario.getNetwork(),
				1.0).write(outputPlansFile);

	}

}
