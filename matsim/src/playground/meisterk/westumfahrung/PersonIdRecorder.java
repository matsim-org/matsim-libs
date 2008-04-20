package playground.meisterk.westumfahrung;

import java.util.HashSet;

import org.matsim.plans.Person;
import org.matsim.plans.Plan;
import org.matsim.plans.algorithms.PersonAlgorithm;
import org.matsim.plans.algorithms.PlanAlgorithmI;
import org.matsim.utils.identifiers.IdI;

public class PersonIdRecorder extends PersonAlgorithm implements PlanAlgorithmI {

	private HashSet<IdI> ids = new HashSet<IdI>();

	public HashSet<IdI> getIds() {
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
