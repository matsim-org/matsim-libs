package opdytsintegration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.population.PlanImpl;

import floetteroed.opdyts.SimulatorState;
import floetteroed.utilities.math.Vector;

/**
 * Considering the "day-to-day" iterations of MATSim as the stages of a
 * discrete-time stochastic process, this class represents the state of this
 * process. This state consists of the plan choice sets of all agents, including
 * scores and information about the selected plan.
 * 
 * @author Gunnar Flötteröd
 * 
 * @see SimulatorState
 */
public class MATSimState implements SimulatorState {

	// -------------------- MEMBERS --------------------

	/**
	 * A map of persons on lists of (deep copies of) all plans of the respective
	 * person. The plan order in the lists matters. Contains an empty list but
	 * non-null list for every person that does not have any plans.
	 */
	private final Map<Id<Person>, List<? extends Plan>> person2planList = new LinkedHashMap<>();

	/**
	 * A map of indices pointing to the currently selected plan of every person.
	 * Contains a null value for every person that does not have a selected
	 * plan.
	 * <p>
	 * Uses an index instead of a reference because references do not survive
	 * deep copies and we want to be robust here.
	 */
	private final Map<Id<Person>, Integer> person2selectedPlanIndex = new LinkedHashMap<>();

	private final Vector vectorRepresentation;

	// Michael, ich glaube, dass dies von Dir kommt. Warum brauchen wir hier die
	// Population?
	private Population population;

	// -------------------- CONSTRUCTION --------------------

	/**
	 * Takes over a <em>deep copy</em> of the population and a
	 * <em>reference</em> to vectorRepresentation.
	 * 
	 * @param population
	 *            the current MATSim population
	 * @param vectorRepresentation
	 *            a real-valued vector representation of the current MATSim
	 *            state.
	 */
	public MATSimState(final Population population,
			final Vector vectorRepresentation) {
		this.population = population;
		for (Person person : population.getPersons().values()) {
			if (person.getSelectedPlan() == null) {
				this.person2selectedPlanIndex.put(person.getId(), null);
			} else {
				final int selectedPlanIndex = person.getPlans().indexOf(
						person.getSelectedPlan());
				if (selectedPlanIndex < 0) {
					throw new RuntimeException("The selected plan of person "
							+ person.getId()
							+ " cannot be found in its plan list.");
				}
				this.person2selectedPlanIndex.put(person.getId(),
						selectedPlanIndex);
			}
			this.person2planList.put(person.getId(),
					newDeepCopy(person.getPlans()));
		}

		this.vectorRepresentation = vectorRepresentation;
	}

	// -------------------- HELPERS AND INTERNALS --------------------

	private static List<? extends Plan> newDeepCopy(
			final List<? extends Plan> fromPlanList) {
		final List<Plan> toPlanList = new ArrayList<>(fromPlanList.size());
		for (Plan fromPlan : fromPlanList) {
			final PlanImpl toPlan = new PlanImpl(fromPlan.getPerson());
			toPlan.copyFrom(fromPlan);
			toPlanList.add(toPlan);
		}
		return toPlanList;
	}

	private static Plan getSelectedPlan(final List<? extends Plan> plans,
			final Integer index) {
		if (index == null) {
			return null;
		} else {
			return plans.get(index);
		}
	}

	public Set<Id<Person>> getPersonIdView() {
		return Collections.unmodifiableSet(this.person2planList.keySet());
	}

	public List<? extends Plan> getPlansView(final Id<Person> personId) {
		return Collections.unmodifiableList(this.person2planList.get(personId));
	}

	public Plan getSelectedPlan(final Id<Person> personId) {
		return getSelectedPlan(this.person2planList.get(personId),
				this.person2selectedPlanIndex.get(personId));
	}

	// --------------- IMPLEMENTATION OF SimulatorState ---------------

	@Override
	public Vector getReferenceToVectorRepresentation() {
		return this.vectorRepresentation;
	}

	@Override
	public void implementInSimulation() {
		for (Id<Person> personId : this.person2planList.keySet()) {
			Person person = population.getPersons().get(personId);
			person.getPlans().clear();
			final List<? extends Plan> copiedPlans = newDeepCopy(this.person2planList
					.get(personId));
			for (Plan plan : copiedPlans) {
				person.addPlan(plan);
			}
			person.setSelectedPlan(getSelectedPlan(copiedPlans,
					this.person2selectedPlanIndex.get(personId)));
		}
	}

	// TODO What for?
	public void setPopulation(Population population) {
		this.population = population;
	}
}
