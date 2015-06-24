package opdytsintegration;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import optdyts.SimulatorState;

import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.population.PlanImpl;

import floetteroed.utilities.math.Vector;

/**
 * Considering the "day-to-day" iterations of MATSim as the stages of a
 * discrete-time stochastic process, this class represents its state, to the
 * extent this state is given by the plan choice set of all agents, possibly
 * including scores and information about the selected plan.
 * 
 * TODO Switch from Person to that person's Id as a unique identifier? Need
 * something that is stable across iterations.
 * 
 * @author Gunnar Flötteröd
 * 
 * @see SimulatorState
 */
public abstract class MATSimState implements SimulatorState {

	// -------------------- MEMBERS --------------------

	/**
	 * A map of lists of (deep copies of) all plans of all persons. The plan
	 * order in the lists matters. Contains an empty list for every person that
	 * does not have any plans.
	 */
	private final Map<Person, List<Plan>> person2planList = new LinkedHashMap<Person, List<Plan>>();

	/**
	 * A map of indices pointing to the currently selected plan of every person.
	 * Contains a null value for every person that does not have a selected
	 * plan.
	 * 
	 * Uses an index instead of a reference because references do not survive
	 * deep copies and we want to be robust here.
	 */
	private final Map<Person, Integer> person2selectedPlanIndex = new LinkedHashMap<Person, Integer>();

	private final Vector vectorRepresentation;

	// -------------------- CONSTRUCTION --------------------

	public MATSimState(final Population population,
			final Vector vectorRepresentation) {

		for (Person person : population.getPersons().values()) {
			if (person.getSelectedPlan() == null) {
				this.person2selectedPlanIndex.put(person, null);
			} else {
				final int selectedPlanIndex = person.getPlans().indexOf(
						person.getSelectedPlan());
				if (selectedPlanIndex < 0) {
					throw new RuntimeException("The selected plan of person "
							+ person.getId()
							+ " cannot be found in its plan list.");
				}
				this.person2selectedPlanIndex.put(person, selectedPlanIndex);
			}
			this.person2planList.put(person, newDeepCopy(person.getPlans()));
		}

		this.vectorRepresentation = vectorRepresentation.copy();
	}

	// -------------------- HELPERS AND INTERNALS --------------------

	private static List<Plan> newDeepCopy(
			final List<? extends Plan> fromPlanList) {
		final List<Plan> toPlanList = new ArrayList<Plan>(fromPlanList.size());
		for (Plan fromPlan : fromPlanList) {
			// TODO Kai says that this is not ideal but the only existing way.
			final PlanImpl toPlan = new PlanImpl(fromPlan.getPerson());
			toPlan.copyFrom(fromPlan);
			toPlanList.add(toPlan);
		}
		return toPlanList;
	}

	private static Plan getSelectedPlan(final List<Plan> plans,
			final Integer index) {
		if (index == null) {
			return null;
		} else {
			return plans.get(index);
		}
	}

	// --------------- IMPLEMENTATION OF SimulatorState ---------------

	@Override
	public Vector getReferenceToVectorRepresentation() {
		return this.vectorRepresentation;
	}

	@Override
	public void implementInSimulation() {
		for (Person person : this.person2planList.keySet()) {
			person.getPlans().clear();
			final List<Plan> copiedPlans = newDeepCopy(this.person2planList
					.get(person));
			for (Plan plan : copiedPlans) {
				person.addPlan(plan);
			}
			person.setSelectedPlan(getSelectedPlan(copiedPlans,
					this.person2selectedPlanIndex.get(person)));
		}
	}
}
