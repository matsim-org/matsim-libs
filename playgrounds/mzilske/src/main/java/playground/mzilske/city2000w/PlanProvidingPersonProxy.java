package playground.mzilske.city2000w;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;

public class PlanProvidingPersonProxy implements Person {

	private Person person;

	public PlanProvidingPersonProxy(Person value) {
		this.person = value;
	}

	@Override
	public boolean addPlan(Plan p) {
		throw new RuntimeException();
	}

	@Override
	public List<? extends Plan> getPlans() {
		return Arrays.asList(person.getSelectedPlan());
	}

	@Override
	public Plan getSelectedPlan() {
		return person.getSelectedPlan();
	}

	@Override
	public void setId(Id id) {
		throw new RuntimeException();
	}

	@Override
	public Id getId() {
		return person.getId();
	}

	@Override
	public Map<String, Object> getCustomAttributes() {
		throw new RuntimeException();
	}

}
