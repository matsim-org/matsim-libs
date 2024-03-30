package org.matsim.modechoice.replanning.scheduled.solver;

import it.unimi.dsi.fastutil.ints.IntIntPair;
import it.unimi.dsi.fastutil.objects.Reference2ObjectMap;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.optaplanner.core.api.domain.solution.PlanningEntityCollectionProperty;
import org.optaplanner.core.api.domain.solution.PlanningScore;
import org.optaplanner.core.api.domain.solution.PlanningSolution;
import org.optaplanner.core.api.domain.solution.cloner.SolutionCloner;
import org.optaplanner.core.api.domain.valuerange.CountableValueRange;
import org.optaplanner.core.api.domain.valuerange.ValueRangeFactory;
import org.optaplanner.core.api.domain.valuerange.ValueRangeProvider;
import org.optaplanner.core.api.score.buildin.hardsoftlong.HardSoftLongScore;

import java.util.ArrayList;
import java.util.List;

/**
 * Planning problem containing all entities and information.
 */
@PlanningSolution(solutionCloner = ModeSchedulingProblem.Cloner.class)
public final class ModeSchedulingProblem {

	private static final Logger log = LogManager.getLogger(ModeSchedulingProblem.class);

	@PlanningEntityCollectionProperty
	private final List<AgentSchedule> agents;
	private final int windowSize;
	private final int nPlans;
	private final Reference2ObjectMap<String, IntIntPair> targets;
	private final int switchTarget;

	private final boolean isFirstIteration;

	@PlanningScore
	private HardSoftLongScore score;

	/**
	 * Create a new mode scheduling problem.
	 *
	 * @param windowSize   number of optimized plans per step
	 * @param agents       list of agents
	 * @param targets      per relevant mode, the min and max target value
	 * @param switchTarget number of desired trip changes between iterations
	 */
	public ModeSchedulingProblem(int windowSize, List<AgentSchedule> agents, Reference2ObjectMap<String, IntIntPair> targets,
								 int switchTarget) {
		this.windowSize = windowSize;
		this.agents = agents;
		this.nPlans = agents.get(0).availablePlans.size();
		this.targets = targets;
		this.switchTarget = switchTarget;
		this.isFirstIteration = agents.get(0).currentPlan == -1;
		this.score = HardSoftLongScore.ofUninitialized(-1, 0, 0);
	}

	/**
	 * Copy constructor.
	 */
	private ModeSchedulingProblem(List<AgentSchedule> agents, ModeSchedulingProblem other) {
		this.windowSize = other.windowSize;
		this.agents = agents;
		this.nPlans = other.nPlans;
		this.targets = other.targets;
		this.switchTarget = other.switchTarget;
		this.isFirstIteration = other.isFirstIteration;
		this.score = other.score;
	}

	public List<AgentSchedule> getAgents() {
		return agents;
	}

	public HardSoftLongScore getScore() {
		return score;
	}

	public void setScore(HardSoftLongScore score) {
		this.score = score;
	}

	public int getWindowSize() {
		return windowSize;
	}

	public int getNPlans() {
		return nPlans;
	}

	public Reference2ObjectMap<String, IntIntPair> getTargets() {
		return targets;
	}

	public int getSwitchTarget() {
		return switchTarget;
	}

	boolean isFirstIteration() {
		return isFirstIteration;
	}

	@ValueRangeProvider(id = "availablePlans")
	public CountableValueRange<Integer> getAvailablePlans() {
		// This value provider is not needed for the implemented move
		return ValueRangeFactory.createIntValueRange(0, nPlans);
	}

	void printScore(int offset) {

		ScoreCalculator scorer = new ScoreCalculator();
		scorer.resetWorkingSolution(this);

		log.info("Targets: {} | Switches: {}", targets, switchTarget);
		for (int i = 0; i < windowSize; i++) {
			log.info("Iteration: {} | Target: {} | Switches: {}", (offset + i), scorer.getObserved()[i], scorer.getSwitches()[i]);
		}
	}

	/**
	 * Create a clone of a solution.
	 */
	public static final class Cloner implements SolutionCloner<ModeSchedulingProblem> {
		@Override
		public ModeSchedulingProblem cloneSolution(ModeSchedulingProblem original) {
			List<AgentSchedule> personsCopy = new ArrayList<>();
			for (AgentSchedule person : original.agents) {
				personsCopy.add(person.copy());
			}
			return new ModeSchedulingProblem(personsCopy, original);
		}
	}

}
