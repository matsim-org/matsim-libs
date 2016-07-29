package sharedmobility;

import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.internal.MatsimReader;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.utils.objectattributes.ObjectAttributes;
import org.matsim.utils.objectattributes.ObjectAttributesXmlReader;
import org.matsim.utils.objectattributes.ObjectAttributesXmlWriter;

public class CSMembershipAllocator {

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		
		ObjectAttributes bla = new ObjectAttributes();
		
		new ObjectAttributesXmlReader(bla).readFile(args[0]);		
			
		MutableScenario scenario = (MutableScenario) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		MatsimReader populationReader = new PopulationReader(scenario);
		MatsimNetworkReader networkReader = new MatsimNetworkReader(scenario.getNetwork());
		networkReader.readFile(args[1]);
		populationReader.readFile(args[2]);
		
		int size = scenario.getPopulation().getPersons().values().size();
		Object[] arr = scenario.getPopulation().getPersons().values().toArray();
		
		for (int i = 1; i < size; i++) {
			
			
			// Allocate RT-membership
			if ( (i % 10) == 0 ){
				bla.putAttribute(((Person)arr[i]).getId().toString(), "RT_CARD", "true");
			}
			else {
				bla.putAttribute(((Person)arr[i]).getId().toString(), "RT_CARD", "false");
			}
			
			// Allocate OW-membership
			if ( (i % 11) == 0 ){
				bla.putAttribute(((Person)arr[i]).getId().toString(), "OW_CARD", "true");
			}
			else {
				bla.putAttribute(((Person)arr[i]).getId().toString(), "OW_CARD", "false");
			}			

			// Allocate FFC-membership
			if ( (i % 6) == 0 ){
				bla.putAttribute(((Person)arr[i]).getId().toString(), "FF_CARD", "true");
			}
			else {
				bla.putAttribute(((Person)arr[i]).getId().toString(), "FF_CARD", "false");
			}
		
			
		}
		
//		new PopulationWriter(scenario.getPopulation(), scenario.getNetwork()).writeFileV4("./plans_with_CS_membership_" + args[3] + ".xml.gz");		
		
		ObjectAttributesXmlWriter betaWriter = new ObjectAttributesXmlWriter(bla);
		betaWriter.writeFile(args[3]);
		

	}

}
