package playground.anhorni.csestimation;

import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;

public class SurveyCleaner {	
	
	private final static Logger log = Logger.getLogger(SurveyCleaner.class);
	
	public void clean(TreeMap<Id<Person>, EstimationPerson> population) {
		this.filter(population);
		this.cleanIncome(population);
	}
	
	public Population removeNonAgeNonIncomePersons(Population population) {
		log.info("clean agents...");
		Population pop = ((ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig())).getPopulation();
		
		for (Person p : population.getPersons().values()) {
			EstimationPerson person = (EstimationPerson)p;
			if (person.getAge() > 0.0 && person.getHhIncome() > 0.0) {
				pop.addPerson(person);
			}
		}
		return pop;
	}
	
	public TreeMap<Id<Person>, EstimationPerson> removeNonAgeNonIncomePersons(TreeMap<Id<Person>, EstimationPerson> population) {
		TreeMap<Id<Person>, EstimationPerson> pop = new TreeMap<Id<Person>, EstimationPerson>();
		
		for (EstimationPerson person : population.values()) {
			if (person.getAge() > 0.0 && person.getHhIncome() > 0.0) {
				pop.put(person.getId(), person);
			}
		}
		return pop;
	}
	
	public void filter(TreeMap<Id<Person>, EstimationPerson> population) { // stopped survey
		log.info("clean filter ...");
		population.remove(Id.create(1225, Person.class));
		population.remove(Id.create(1984, Person.class));	
		
		population.remove(Id.create(1897, Person.class)); // Amriswil
		population.remove(Id.create(1756, Person.class)); // Niederglatt
		population.remove(Id.create(1277, Person.class)); //Küsnacht
	}
	
	public void cleanIncome(TreeMap<Id<Person>, EstimationPerson> population) {
		population.get(Id.create(1298, Person.class)).setHhIncome(
				(int)(population.get(Id.create(1298, Person.class)).getHhIncome() / 12.0));
		population.get(Id.create(1512, Person.class)).setHhIncome(
				(int)(population.get(Id.create(1512, Person.class)).getHhIncome() / 12.0));
	}
}
