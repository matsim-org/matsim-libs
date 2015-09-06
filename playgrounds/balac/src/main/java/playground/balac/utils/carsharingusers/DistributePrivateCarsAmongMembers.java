package playground.balac.utils.carsharingusers;

import java.util.Random;

import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PopulationReader;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;

public class DistributePrivateCarsAmongMembers {

	public static void main(String[] args) {

		ScenarioImpl scenario = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		PopulationReader populationReader = new MatsimPopulationReader(scenario);
		MatsimNetworkReader networkReader = new MatsimNetworkReader(scenario);
		networkReader.readFile(args[0]);
		populationReader.readFile(args[1]);
		
		Random rand = MatsimRandom.getRandom();
		
		double x = 650.0 / 1309.0;
		for (Person p : scenario.getPopulation().getPersons().values()) {
			
			if (rand.nextDouble() < x)
			
				PersonImpl.setCarAvail(p, "always");
			else
				PersonImpl.setCarAvail(p, "never");

		}
		
		
		new PopulationWriter(scenario.getPopulation(), scenario.getNetwork()).writeFileV4(args[2] + "/plans_memb_fixedPrivateCars.xml.gz");		

		
		
	}

}
