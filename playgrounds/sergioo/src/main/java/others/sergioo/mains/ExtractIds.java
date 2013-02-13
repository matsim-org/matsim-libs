package others.sergioo.mains;

import java.io.FileNotFoundException;
import java.io.PrintWriter;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.scenario.ScenarioUtils;

public class ExtractIds {

	//Main
		/**
		 * 
		 * @param args
		 * 0-Source population file
		 * 1-Destination file
		 * @throws FileNotFoundException 
		 */
		public static void main(String[] args) throws FileNotFoundException {
			Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
			new MatsimPopulationReader(scenario).readFile(args[0]);
			PrintWriter writer = new PrintWriter(args[1]);
			for(Person person:scenario.getPopulation().getPersons().values())
				writer.println(person.getId());
			writer.close();
		}

}
