package playground.meisterk.westumfahrung;

import java.util.HashSet;

import org.matsim.basic.v01.Id;
import org.matsim.plans.Person;
import org.matsim.plans.Plan;
import org.matsim.plans.algorithms.PersonAlgorithm;
import org.matsim.plans.algorithms.PlanAlgorithmI;

public class PersonIdRecorder extends PersonAlgorithm implements PlanAlgorithmI {

	private HashSet<Id> ids = new HashSet<Id>();

	public HashSet<Id> getIds() {
		return ids;
	}

	@Override
	public void run(Person person) {
		ids.add(person.getId());	
}

	public void run(Plan plan) {
		this.run(plan.getPerson());
	}

}
