package playground.anhorni.csestimation;

import java.util.TreeMap;

import org.matsim.api.core.v01.Id;
import org.matsim.core.basic.v01.IdImpl;

public class SurveyCleaner {
	
	public TreeMap<Id, EstimationPerson> clean(TreeMap<Id, EstimationPerson> population) {
		this.filter(population);
		this.cleanIncome(population);
		return this.removeNonAgeNonIncomePersons(population);
	}
	
	private TreeMap<Id, EstimationPerson> removeNonAgeNonIncomePersons(TreeMap<Id, EstimationPerson> population) {
		TreeMap<Id, EstimationPerson> pop = new TreeMap<Id, EstimationPerson>();
		
		for (EstimationPerson person : population.values()) {
			if (person.getAge() > 0.0 && person.getHhIncome() > 0.0) {
				pop.put(person.getId(), person);
			}
		}
		return pop;
	}
	
	public void filter(TreeMap<Id, EstimationPerson> population) { // stopped survey
		population.remove(new IdImpl(1225));
		population.remove(new IdImpl(1984));		
	}
	
	public void cleanIncome(TreeMap<Id, EstimationPerson> population) {
		population.get(new IdImpl(1962)).setHhIncome((int)(population.get(new IdImpl(1962)).getHhIncome() / 12.0));
		population.get(new IdImpl(1298)).setHhIncome((int)(population.get(new IdImpl(1298)).getHhIncome() / 12.0));
		population.get(new IdImpl(1512)).setHhIncome((int)(population.get(new IdImpl(1512)).getHhIncome() / 12.0));
	}
}
