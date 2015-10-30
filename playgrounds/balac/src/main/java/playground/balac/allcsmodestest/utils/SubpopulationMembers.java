package playground.balac.allcsmodestest.utils;

import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PopulationReader;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.utils.objectattributes.ObjectAttributes;
import org.matsim.utils.objectattributes.ObjectAttributesXmlReader;
import org.matsim.utils.objectattributes.ObjectAttributesXmlWriter;

public class SubpopulationMembers {

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		
		ObjectAttributes bla = new ObjectAttributes();
		
		new ObjectAttributesXmlReader(bla).parse(args[0]);		
		
		MutableScenario scenario = (MutableScenario) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		PopulationReader populationReader = new MatsimPopulationReader(scenario);
		MatsimNetworkReader networkReader = new MatsimNetworkReader(scenario);
		networkReader.readFile(args[1]);
		populationReader.readFile(args[2]);
		
		int size = scenario.getPopulation().getPersons().values().size();
		Object[] arr = scenario.getPopulation().getPersons().values().toArray();
		
		for (int i = 1; i < size; i++) {
			if (bla.getAttribute(((Person)arr[i]).getId().toString(), "RT_CARD").equals("false")) {				
				scenario.getPopulation().getPersons().remove(((Person)arr[i]).getId());
			}			
			//if (bla.getAttribute(((Person)arr[i]).getId().toString(), "RT_CARD").equals("true")) {				
		//		bla.putAttribute(((Person)arr[i]).getId().toString(), "subpopulation", "csMembers");
		//	}
			
		}
		
		new PopulationWriter(scenario.getPopulation(), scenario.getNetwork()).writeFileV4("./plans_only_rt_members_" + args[3] + ".xml.gz");		
		
	//	ObjectAttributesXmlWriter betaWriter = new ObjectAttributesXmlWriter(bla);
	//	betaWriter.writeFile("./personAttrSubpop.xml.gz");
		

	}

}
