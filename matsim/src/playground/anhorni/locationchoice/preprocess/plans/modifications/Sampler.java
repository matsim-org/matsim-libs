package playground.anhorni.locationchoice.preprocess.plans.modifications;

import java.util.Iterator;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PopulationImpl;

public class Sampler {
	
	private final static Logger log = Logger.getLogger(Sampler.class);
	
	public PopulationImpl sample(PopulationImpl plans) {
		PopulationImpl sampledPopulation = new PopulationImpl();
		
		Iterator<PersonImpl> person_it = plans.getPersons().values().iterator();
		while (person_it.hasNext()) {
			Person person = person_it.next();
			double r = MatsimRandom.getRandom().nextDouble();
			
			if (r > 0.9) {
				sampledPopulation.addPerson(person);
			}
		}	
		log.info("Population size after sampling: " + sampledPopulation.getPersons().size());
		return sampledPopulation;
	}

}
