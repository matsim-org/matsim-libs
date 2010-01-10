package playground.anhorni.locationchoice.preprocess.plans.modifications;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.population.PopulationImpl;

public class Sampler {
	
	private final static Logger log = Logger.getLogger(Sampler.class);
	
	public PopulationImpl sample(PopulationImpl plans) {
		PopulationImpl sampledPopulation = new ScenarioImpl().getPopulation();
		
		for (Person person : plans.getPersons().values()) {
			double r = MatsimRandom.getRandom().nextDouble();
			
			if (r > 0.9) {
				sampledPopulation.addPerson(person);
			}
		}	
		log.info("Population size after sampling: " + sampledPopulation.getPersons().size());
		return sampledPopulation;
	}

}
