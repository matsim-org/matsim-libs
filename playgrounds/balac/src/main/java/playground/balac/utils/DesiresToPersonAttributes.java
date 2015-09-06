package playground.balac.utils;

import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PopulationReader;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.utils.objectattributes.ObjectAttributes;
import org.matsim.utils.objectattributes.ObjectAttributesXmlWriter;

public class DesiresToPersonAttributes {

	public static void main(String[] args) {
		ScenarioImpl sc = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		PopulationReader populationReader = new MatsimPopulationReader(sc);
		MatsimNetworkReader networkReader = new MatsimNetworkReader(sc);
		networkReader.readFile(args[0]);
		populationReader.readFile(args[1]);
		
		ObjectAttributes members = new ObjectAttributes();
		
		for (Person p : sc.getPopulation().getPersons().values()) {
			
			for (String s : ((PersonImpl)p).getDesires().getActivityDurations().keySet()) {
				
				members.putAttribute(p.getId().toString(), s , ((PersonImpl)p).getDesires().getActivityDurations().get(s));
			}
			
			if (PersonImpl.getTravelcards(p) != null && PersonImpl.getTravelcards(p).contains("ffProgram")) {
				
				members.putAttribute(p.getId().toString(), "FF_CARD" , "true");
			}
			else
				members.putAttribute(p.getId().toString(), "FF_CARD" , "false");

			
			if (PersonImpl.getTravelcards(p) != null && PersonImpl.getTravelcards(p).contains("ch-HT-mobility")) {
				
				members.putAttribute(p.getId().toString(), "RT_CARD" , "true");
			}
			else
				members.putAttribute(p.getId().toString(), "RT_CARD" , "false");

						
		}
		
		ObjectAttributesXmlWriter betaWriter = new ObjectAttributesXmlWriter(members);
		betaWriter.writeFile("C:/Users/balacm/Desktop/desiresAttributes_25perc.xml.gz");	
		

	}
}
