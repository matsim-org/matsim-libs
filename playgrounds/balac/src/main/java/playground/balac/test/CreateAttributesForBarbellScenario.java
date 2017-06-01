package playground.balac.test;

import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.internal.MatsimReader;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.utils.objectattributes.ObjectAttributes;
import org.matsim.utils.objectattributes.ObjectAttributesXmlWriter;

public class CreateAttributesForBarbellScenario {

	public static void main(String[] args) {
		MutableScenario sc = (MutableScenario) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		MatsimReader populationReader = new PopulationReader(sc);
		MatsimNetworkReader networkReader = new MatsimNetworkReader(sc.getNetwork());
		networkReader.readFile(args[0]);
		populationReader.readFile(args[1]);
		
		ObjectAttributes members = new ObjectAttributes();
		
		for (Person p : sc.getPopulation().getPersons().values()) {
			
			
				
				members.putAttribute(p.getId().toString(), "home" , 13*3600.0);
			
			
				members.putAttribute(p.getId().toString(), "work" , 10*3600.0);

				members.putAttribute(p.getId().toString(), "leisure" , 3600.0);

			
				
			

						
		
		}
		ObjectAttributesXmlWriter betaWriter = new ObjectAttributesXmlWriter(members);
		betaWriter.writeFile("C:/Users/balacm/Documents/TestScenario/1000desiresAttributes.xml.gz");	

	}

}
