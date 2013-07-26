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
		String inputPlansFile = "H:/data/cvs/ivt/studies/switzerland/plans/teleatlas-ivtcheu/census2000v2_dilZh30km_10pct/plans.xml.gz";
		String inputNetworkFile = "H:/data/cvs/ivt/studies/switzerland/networks/teleatlas-ivtcheu/network.xml.gz";
		String inputFacilitiesFile = "H:/data/cvs/ivt/studies/switzerland/facilities/facilities.xml.gz";
		int populationExpansionFactor = 3;

		String outputPlansFile = "H:/data/experiments/msimoni/26July2013/plans_zurich_30pct.xml.gz";
		Scenario scenario = GeneralLib.readScenario(inputPlansFile,
				inputNetworkFile,inputFacilitiesFile);

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
