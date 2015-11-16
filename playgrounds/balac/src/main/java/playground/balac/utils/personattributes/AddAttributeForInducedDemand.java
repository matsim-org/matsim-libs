package playground.balac.utils.personattributes;

import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PopulationReader;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.utils.objectattributes.ObjectAttributes;
import org.matsim.utils.objectattributes.ObjectAttributesXmlReader;
import org.matsim.utils.objectattributes.ObjectAttributesXmlWriter;

public class AddAttributeForInducedDemand {

	public static void main(String[] args) {

		MutableScenario scenario = (MutableScenario) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		PopulationReader populationReader = new MatsimPopulationReader(scenario);
		MatsimNetworkReader networkReader = new MatsimNetworkReader(scenario);
		networkReader.readFile(args[1]);
		populationReader.readFile(args[2]);
		
		ObjectAttributes bla = new ObjectAttributes();
		
		new ObjectAttributesXmlReader(bla).parse(args[0]);
		
		for(Person p : scenario.getPopulation().getPersons().values()) {
			String act = "home";
			if (bla.getAttribute(p.getId().toString(), "earliestEndTime_leisure") != null)
				act = act + ",leisure";
			if (bla.getAttribute(p.getId().toString(), "earliestEndTime_work") != null)
				act = act + ",work";
			if (bla.getAttribute(p.getId().toString(), "earliestEndTime_shopping") != null)
				act = act + ",shopping";
			
			bla.putAttribute(p.getId().toString(), "activities", act);
			
		}
		
		ObjectAttributesXmlWriter betaWriter = new ObjectAttributesXmlWriter(bla);
		betaWriter.writeFile(args[3]);		
		
		
	}

}
