package playground.balac.allcsmodestest.utils.attributes;

import java.util.Random;

import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PopulationReader;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.utils.objectattributes.ObjectAttributes;
import org.matsim.utils.objectattributes.ObjectAttributesXmlReader;
import org.matsim.utils.objectattributes.ObjectAttributesXmlWriter;

public class AddRandomShoppingUtility {

	private static double beta = 2.0;
	
	private static double standarddev = 2.0;
	
	public static void main(String[] args) {
		
		ObjectAttributes bla = new ObjectAttributes();
		
		new ObjectAttributesXmlReader(bla).parse(args[0]);	
		
		MutableScenario scenario = (MutableScenario) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		PopulationReader populationReader = new MatsimPopulationReader(scenario);
		MatsimNetworkReader networkReader = new MatsimNetworkReader(scenario);
		networkReader.readFile(args[1]);
		populationReader.readFile(args[2]);
		
		Random rand = MatsimRandom.getRandom();
		
		double maxgaus = 0.0;
		
		int size = scenario.getPopulation().getPersons().values().size();
		Object[] arr = scenario.getPopulation().getPersons().values().toArray();
		
		
		for (int i = 1; i < size; i++) {
			/*if (bla.getAttribute(((Person)arr[i]).getId().toString(), "RT_CARD").equals("false")) {				
				scenario.getPopulation().getPersons().remove(((Person)arr[i]).getId());
			}*/			
			double gaus = rand.nextGaussian() * standarddev;
	
			if (gaus < 0.0)
				gaus = -gaus;
			if (maxgaus < gaus)
				maxgaus = gaus;
			bla.putAttribute(((Person)arr[i]).getId().toString(), "randValue", gaus);
			
			
		}
		for (int i = 1; i < size; i++) {
			/*if (bla.getAttribute(((Person)arr[i]).getId().toString(), "RT_CARD").equals("false")) {				
				scenario.getPopulation().getPersons().remove(((Person)arr[i]).getId());
			}*/			
			
			bla.putAttribute(((Person)arr[i]).getId().toString(), "randValue", beta * (double)bla.getAttribute(((Person)arr[i]).getId().toString(), "randValue") / maxgaus);
			
			
		}
		
		ObjectAttributesXmlWriter betaWriter = new ObjectAttributesXmlWriter(bla);
		betaWriter.writeFile("./personAttrWithAddedUtility_beta2.xml.gz");

	}

}
