package playground.balac.utils.membership;

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

public class CopyMembershipFromRT {

	public static void main(String[] args) {
		// TODO Auto-generated method stub

		MutableScenario scenario = (MutableScenario) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		PopulationReader populationReader = new MatsimPopulationReader(scenario);
		MatsimNetworkReader networkReader = new MatsimNetworkReader(scenario);
		networkReader.readFile(args[1]);
	//	new FacilitiesReaderMatsimV1(scenario).readFile(facilitiesfilePath);
		populationReader.readFile(args[2]);
		ObjectAttributes bla = new ObjectAttributes();
		
		new ObjectAttributesXmlReader(bla).parse(args[0]);
		
		for(Person p : scenario.getPopulation().getPersons().values()) {
			
			if ( ((String) bla.getAttribute(p.getId().toString(), "RT_CARD")).equals("true")) {
				bla.putAttribute(p.getId().toString(), "OW_CARD", "true");
				bla.putAttribute(p.getId().toString(), "FF_CARD", "true");

			}
			else {
				bla.putAttribute(p.getId().toString(), "OW_CARD", "false");
				bla.putAttribute(p.getId().toString(), "FF_CARD", "false");

			}

		}
		
		ObjectAttributesXmlWriter betaWriter = new ObjectAttributesXmlWriter(bla);
		betaWriter.writeFile("./desires_memb_rt_ow_ff_100perc.xml.gz");
	}

}
