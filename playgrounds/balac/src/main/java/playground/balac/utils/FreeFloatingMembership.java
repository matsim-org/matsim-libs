package playground.balac.utils;

import java.util.Random;

import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PopulationReader;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.utils.objectattributes.ObjectAttributes;
import org.matsim.utils.objectattributes.ObjectAttributesXmlReader;
import org.matsim.utils.objectattributes.ObjectAttributesXmlWriter;

public class FreeFloatingMembership {
	
	
	
	public static void main(String[] args) {

		ObjectAttributes bla = new ObjectAttributes();
		
		new ObjectAttributesXmlReader(bla).parse(args[0]);
		
		ScenarioImpl scenario = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		PopulationReader populationReader = new MatsimPopulationReader(scenario);
		populationReader.readFile(args[1]);
		int goal = 6250;
		int number = 0;
		int x = 0;
		int size = scenario.getPopulation().getPersons().size();
		Object[] arr =  scenario.getPopulation().getPersons().values().toArray();
		Random r = new Random();
		
		for(Person p:scenario.getPopulation().getPersons().values()) {
			
			if (bla.getAttribute(p.getId().toString(), "CS_CARD") != null) 
				x++;
			
		}
		
		
		while (number < goal) {
			
			Person p = (Person) arr[r.nextInt(size)];
			
			if (PersonImpl.hasLicense(p)) {
				x++;
				if (bla.getAttribute(p.getId().toString(), "CS_CARD") == null) {
					
					bla.putAttribute(p.getId().toString(), "CS_CARD", true);
					bla.putAttribute(p.getId().toString(), "CS_FLEX_ONLY", true);
					number++;
					
				}
				
			}
		
		
			
		}
		ObjectAttributesXmlWriter betaWriter = new ObjectAttributesXmlWriter(bla);
		betaWriter.writeFile("C:/Users/balacm/Desktop" + "/added_members.xml");
		
	}

}
