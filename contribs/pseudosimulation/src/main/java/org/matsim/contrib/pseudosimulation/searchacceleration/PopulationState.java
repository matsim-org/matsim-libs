/*
 * Copyright 2018 Gunnar Flötteröd
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * contact: gunnar.flotterod@gmail.com
 *
 */
package org.matsim.contrib.pseudosimulation.searchacceleration;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.HasPlansAndId;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.population.PopulationUtils;

/**
 * Copied and simplified from opdytsintegration.
 * 
 * @author Gunnar Flötteröd
 * 
 */
class PopulationState {

	// -------------------- MEMBERS --------------------

	/**
	 * A map of persons on lists of (deep copies of) all plans of the respective
	 * person. The plan order in the lists matters. Contains an empty but non-null
	 * list for every person that does not have any plans.
	 */
	private final Map<Id<Person>, List<? extends Plan>> personId2planList = new LinkedHashMap<>();

	/**
	 * A map of indices pointing to the currently selected plan of every person.
	 * Contains a null value for every person that does not have a selected plan.
	 * Uses an index instead of a reference because references do not survive deep
	 * copies.
	 */
	private final Map<Id<Person>, Integer> person2selectedPlanIndex = new LinkedHashMap<>();

	// -------------------- IMPLEMENTATION --------------------

	public PopulationState(final Population population) {
		for (Person person : population.getPersons().values()) {
			if (person.getSelectedPlan() == null) {
				this.person2selectedPlanIndex.put(person.getId(), null);
			} else {
				final int selectedPlanIndex = person.getPlans().indexOf(person.getSelectedPlan());
				if (selectedPlanIndex < 0) {
					throw new RuntimeException(
							"The selected plan of person " + person.getId() + " cannot be found in its plan list.");
				}
				this.person2selectedPlanIndex.put(person.getId(), selectedPlanIndex);
			}
			this.personId2planList.put(person.getId(), newDeepCopy(person.getPlans()));
		}
	}

	public void set(final Population population) {
		for (Id<Person> personId : this.personId2planList.keySet()) {
			final Person person = population.getPersons().get(personId);
			this.set(person);
		}
	}

	public void set(final HasPlansAndId<Plan, Person> person) {
		person.getPlans().clear();
		final List<? extends Plan> copiedPlans = newDeepCopy(this.personId2planList.get(person.getId()));
		for (Plan plan : copiedPlans) {
			person.addPlan(plan);
		}
		person.setSelectedPlan(getSelectedPlan(copiedPlans, this.person2selectedPlanIndex.get(person.getId())));
	}

	public Plan getSelectedPlan(Id<Person> personId) {
		return getSelectedPlan(this.personId2planList.get(personId), this.person2selectedPlanIndex.get(personId));
	}

	// -------------------- HELPERS AND INTERNALS --------------------

	private static List<? extends Plan> newDeepCopy(final List<? extends Plan> fromPlanList) {
		final List<Plan> toPlanList = new ArrayList<>(fromPlanList.size());
		for (Plan fromPlan : fromPlanList) {
			final Plan toPlan = PopulationUtils.createPlan(fromPlan.getPerson());
			PopulationUtils.copyFromTo(fromPlan, toPlan);
			toPlanList.add(toPlan);
		}
		return toPlanList;
	}

	private static Plan getSelectedPlan(final List<? extends Plan> plans, final Integer index) {
		if (index == null) {
			return null;
		} else {
			return plans.get(index);
		}
	}
}
