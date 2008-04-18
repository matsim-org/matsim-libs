package playground.meisterk.westumfahrung;

import java.util.List;
import java.util.HashSet;

import org.matsim.plans.Person;
import org.matsim.plans.Plan;
import org.matsim.plans.algorithms.PersonAlgorithm;
import org.matsim.plans.algorithms.PlanAlgorithmI;
import org.matsim.utils.identifiers.IdI;

public class PersonIds extends PersonAlgorithm implements PlanAlgorithmI {

	private HashSet<IdI> ids = new HashSet<IdI>();

	public HashSet<IdI> getIds() {
		return ids;
	}

	@Override
	public void run(Person person) {
		// TODO Auto-generated method stub
		
	}

	public void run(Plan plan) {
		ids.add(plan.getPerson().getId());
	}

}
