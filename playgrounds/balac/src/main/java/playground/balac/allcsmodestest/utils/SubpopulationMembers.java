package playground.balac.allcsmodestest.utils;

import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PopulationReader;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.utils.objectattributes.ObjectAttributes;
import org.matsim.utils.objectattributes.ObjectAttributesXmlReader;

public class SubpopulationMembers {

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		
		ObjectAttributes bla = new ObjectAttributes();
		
		new ObjectAttributesXmlReader(bla).parse(args[0]);		
		
		ScenarioImpl scenario = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		PopulationReader populationReader = new MatsimPopulationReader(scenario);
		MatsimNetworkReader networkReader = new MatsimNetworkReader(scenario);
		networkReader.readFile(args[1]);
		populationReader.readFile(args[2]);
		
		int size = scenario.getPopulation().getPersons().values().size();
		Object[] arr = scenario.getPopulation().getPersons().values().toArray();
		
		for (int i = 1; i < size; i++) {
			if (bla.getAttribute(((Person)arr[i]).getId().toString(), "OW_CARD").equals("false")) {				
				scenario.getPopulation().getPersons().remove(((Person)arr[i]).getId());
			}			
		}
		
		new PopulationWriter(scenario.getPopulation(), scenario.getNetwork()).writeFileV4(args[3] + "/plans_only_ow_members.xml.gz");		
		
		
		

	}

}
