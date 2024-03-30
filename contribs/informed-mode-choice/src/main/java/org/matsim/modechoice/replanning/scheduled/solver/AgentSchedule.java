package org.matsim.modechoice.replanning.scheduled.solver;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.optaplanner.core.api.domain.entity.PlanningEntity;
import org.optaplanner.core.api.domain.lookup.PlanningId;
import org.optaplanner.core.api.domain.variable.PlanningListVariable;

/**
 * The schedule of modes for one agent
 */
@PlanningEntity
public final class AgentSchedule {

	@PlanningId
	private final Id<Person> id;

	@PlanningListVariable(valueRangeProviderRefs = "availablePlans")
	final IntList indices = new IntArrayList();

	final IntList availablePlans = new IntArrayList();

	/**
	 * Array of plan categories for each index.
	 */
	final String[] planCategories;

	/**
	 * Number of trips in each plan.
	 */
	final int length;

	/**
	 * Optional current plan, to minimize number of changes.
	 */
	int currentPlan = -1;

	AgentSchedule(Id<Person> id, String[] planCategories, int length) {
        this.id = id;
		this.planCategories = planCategories;
		this.length = length;
	}

	private AgentSchedule(AgentSchedule other) {
		this.id = other.id;
		this.planCategories = other.planCategories;
		this.availablePlans.addAll(other.availablePlans);
		this.indices.addAll(other.indices);
		this.currentPlan = other.currentPlan;
		this.length = other.length;
	}

	public Id<Person> getId() {
		return id;
	}

	AgentSchedule copy() {
		return new AgentSchedule(this);
	}

}
