package playground.balac.allcsmodestest.utils;

import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.api.internal.MatsimReader;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;

public class PlansGenerationFromExecutedScenario {

	public static void main(String[] args) {
		// TODO Auto-generated method stub

		MutableScenario scenario1 = (MutableScenario) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		MatsimReader populationReader1 = new PopulationReader(scenario1);
		MatsimNetworkReader networkReader1 = new MatsimNetworkReader(scenario1.getNetwork());
		networkReader1.readFile(args[0]);
		populationReader1.readFile(args[1]);
		
		MutableScenario scenario2 = (MutableScenario) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		MatsimReader populationReader2 = new PopulationReader(scenario2);
		MatsimNetworkReader networkReader2 = new MatsimNetworkReader(scenario2.getNetwork());
		networkReader2.readFile(args[0]);
		populationReader2.readFile(args[2]);
		
		for (Person p : scenario1.getPopulation().getPersons().values()) {
			
			if (!scenario2.getPopulation().getPersons().containsKey(p.getId())) {
				
				Person newPerson = scenario2.getPopulation().getFactory().createPerson(p.getId());
				newPerson.addPlan(p.getSelectedPlan());
				newPerson.setSelectedPlan(p.getSelectedPlan());
				scenario2.getPopulation().addPerson(newPerson);
			}
			
		}
		
		new PopulationWriter(scenario2.getPopulation(), scenario2.getNetwork()).writeV4("./population_withsubpop.xml.gz");
		
		
	}

}
