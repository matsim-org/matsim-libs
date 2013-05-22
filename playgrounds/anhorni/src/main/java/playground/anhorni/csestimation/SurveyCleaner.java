package playground.anhorni.csestimation;

import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;

public class SurveyCleaner {	
	
	private final static Logger log = Logger.getLogger(SurveyCleaner.class);
	
	public void clean(TreeMap<Id, EstimationPerson> population) {
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
	
	public TreeMap<Id, EstimationPerson> removeNonAgeNonIncomePersons(TreeMap<Id, EstimationPerson> population) {
		TreeMap<Id, EstimationPerson> pop = new TreeMap<Id, EstimationPerson>();
		
		for (EstimationPerson person : population.values()) {
			if (person.getAge() > 0.0 && person.getHhIncome() > 0.0) {
				pop.put(person.getId(), person);
			}
		}
		return pop;
	}
	
	public void filter(TreeMap<Id, EstimationPerson> population) { // stopped survey
		log.info("clean filter ...");
		population.remove(new IdImpl(1225));
		population.remove(new IdImpl(1984));	
		
		population.remove(new IdImpl(1897)); // Amriswil
		population.remove(new IdImpl(1756)); // Niederglatt
		population.remove(new IdImpl(1277)); //Küsnacht
	}
	
	public void cleanIncome(TreeMap<Id, EstimationPerson> population) {
		population.get(new IdImpl(1298)).setHhIncome(
				(int)(population.get(new IdImpl(1298)).getHhIncome() / 12.0));
		population.get(new IdImpl(1512)).setHhIncome(
				(int)(population.get(new IdImpl(1512)).getHhIncome() / 12.0));
	}
}
