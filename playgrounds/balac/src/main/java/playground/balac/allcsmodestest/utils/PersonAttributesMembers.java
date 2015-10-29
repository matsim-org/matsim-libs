package playground.balac.allcsmodestest.utils;

import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PersonUtils;
import org.matsim.core.population.PopulationReader;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.utils.objectattributes.ObjectAttributes;
import org.matsim.utils.objectattributes.ObjectAttributesXmlWriter;

public class PersonAttributesMembers {

	public static void main(String[] args) {
		MutableScenario sc = (MutableScenario) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		PopulationReader populationReader = new MatsimPopulationReader(sc);
		MatsimNetworkReader networkReader = new MatsimNetworkReader(sc);
		networkReader.readFile(args[0]);
		populationReader.readFile(args[1]);
		
		ObjectAttributes members = new ObjectAttributes();
		
		for (Person p : sc.getPopulation().getPersons().values()) {
			
			if (PersonUtils.getTravelcards(p) != null && PersonUtils.getTravelcards(p).contains("ffProgram")) {
				
				members.putAttribute(p.getId().toString(), "CS_CARD" , "freefloating");
			}
		}
		
		ObjectAttributesXmlWriter betaWriter = new ObjectAttributesXmlWriter(members);
		betaWriter.writeFile("C:/Users/balacm/Desktop/members.xml");	
		

	}

}
