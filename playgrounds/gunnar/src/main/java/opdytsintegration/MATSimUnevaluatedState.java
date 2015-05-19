package opdytsintegration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import optdyts.SimulatorState;

import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.population.PlanImpl;

/**
 * Considering the "day-to-day" iterations of MATSim as the stages of a
 * discrete-time stochastic process, this class represents its state, to the
 * extent this state is given by the plan choice set of all agents, possibly
 * including scores and information about the selected plan.
 * 
 * Concrete subclasses may keep track of additional state information.
 * 
 * TODO Switch from Person to that person's Id as a unique identifier? Need
 * something that is stable across iterations. Ask Kai.
 * 
 * @author Gunnar Flötteröd
 * 
 * @param <X>
 *            the type of the concrete subclass of this class that represents a
 *            MATSim state in the context of a concrete optimization problem
 */
public abstract class MATSimUnevaluatedState<X extends MATSimUnevaluatedState<X>>
		implements SimulatorState<X> {

	// -------------------- MEMBERS --------------------

	protected final Population population;

	protected final Random rnd;

	/**
	 * A map of lists of (deep copies of) all plans of all persons. The plan
	 * order in the lists matters. Contains an empty list for every person that
	 * does not have any plans.
	 */
	protected final Map<Person, List<Plan>> person2planList = new LinkedHashMap<Person, List<Plan>>();

	/**
	 * A map of indices pointing to the currently selected plan of every person.
	 * Contains a null value for every person that does not have a selected
	 * plan.
	 * 
	 * Uses an index instead of a reference because references do not survive
	 * deep copies and we want to be robust here.
	 */
	protected final Map<Person, Integer> person2selectedPlanIndex = new LinkedHashMap<Person, Integer>();

	// -------------------- CONSTRUCTION --------------------

	protected MATSimUnevaluatedState(final Population population,
			final Random rnd) {
		this.population = population;
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
		this.rnd = rnd;
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

	/**
	 * Provides a reference to the (deep copy stored within this state object of
	 * the) currently selected plan of person. Returns null if no plan is
	 * selected.
	 * 
	 * If one asked the person directly for its selected plan then one would get
	 * a reference to a plan instance within the simulation (and not to the deep
	 * copy stored within this state object).
	 * 
	 * @param person
	 *            the Person whose selected plan we are interested in
	 * @return a reference to the currently selected plan of person
	 */
	protected Plan getSelectedPlan(final Person person) {
		return getSelectedPlan(this.person2planList.get(person),
				this.person2selectedPlanIndex.get(person));
	}

	// -------------------- IMPLEMENTATION --------------------

	// @Override
	// @Deprecated
	// public void blendInOtherState(final X otherState, final double
	// otherWeight) {
	//
	// if (!this.person2planList.keySet().equals(
	// otherState.person2planList.keySet())) {
	// throw new RuntimeException("Cannot combine different populations.");
	// }
	// if ((otherWeight < 0.0) || (otherWeight > 1.0)) {
	// throw new RuntimeException("otherWeight is " + otherWeight
	// + ", which is not in [0, 1]");
	// }
	//
	// final List<Person> personList = new ArrayList<Person>(
	// otherState.person2planList.keySet());
	// Collections.shuffle(personList, this.rnd);
	// int n2 = MathHelpers.round(otherWeight * personList.size());
	// for (int i = 0; i < n2; i++) {
	// final Person person = personList.get(i);
	// this.person2planList.put(person,
	// newDeepCopy(otherState.person2planList.get(person)));
	// this.person2selectedPlanIndex.put(person,
	// otherState.person2selectedPlanIndex.get(person));
	// }
	// }

	@Override
	public void takeOverConvexCombination(final List<X> states,
			final List<Double> weights) {

		final List<Person> shuffledPersonList = new ArrayList<Person>(
				this.person2planList.keySet());
		Collections.shuffle(shuffledPersonList);

		this.person2planList.clear();
		this.person2selectedPlanIndex.clear();

		double weightSum = 0;
		int personIndex = 0;
		for (int stateIndex = 0; stateIndex < states.size(); stateIndex++) {
			weightSum = Math.min(1.0, weightSum + weights.get(stateIndex));
			final int nextExcludedPersonIndex = (int) Math.ceil(weightSum
					* shuffledPersonList.size());
			for (; personIndex < nextExcludedPersonIndex; personIndex++) {
				final Person person = shuffledPersonList.get(personIndex);
				this.person2planList.put(person, newDeepCopy(states
						.get(stateIndex).person2planList.get(person)));
				this.person2selectedPlanIndex.put(person, states
						.get(stateIndex).person2selectedPlanIndex.get(person));
			}
		}
	}

	@Override
	public void implementInSimulation() {
		for (Person person : this.population.getPersons().values()) {
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
