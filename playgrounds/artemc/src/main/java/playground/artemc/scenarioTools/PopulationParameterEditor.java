package playground.artemc.scenarioTools;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.population.PopulationReaderMatsimV5;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.utils.objectattributes.ObjectAttributesXmlReader;
import org.matsim.utils.objectattributes.ObjectAttributesXmlWriter;

import java.util.Random;

/**
 * Created by artemc on 24/4/15.
 */
public class PopulationParameterEditor {

	public static void main(String[] args){

		String populationPath = args[0];
		String personAttributePath = args[1];
		String editedPersonAttributePath = args[2];

		ScenarioImpl scenario = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());

		PopulationImpl population = (PopulationImpl) scenario.getPopulation();

		new PopulationReaderMatsimV5(scenario).readFile(populationPath);
		new ObjectAttributesXmlReader(population.getPersonAttributes()).parse(personAttributePath);

		Random generator = new Random(10830239345L);

		for(Id<Person> personId:population.getPersons().keySet()){
			/*Generate random factor  between 0.4 and 1.6 from normal distribution with mean=1 and std=0.3*/

			double randomValue = 0.0;
			do {
				randomValue = 0.3 * generator.nextGaussian() + 1.0;
			}while(randomValue<0.4 || randomValue>1.6);
				population.getPersonAttributes().putAttribute(personId.toString(),"betaFactor",randomValue);
		}

		ObjectAttributesXmlWriter attributesWriter = new ObjectAttributesXmlWriter(population.getPersonAttributes());
		attributesWriter.writeFile(editedPersonAttributePath);
	}
}
