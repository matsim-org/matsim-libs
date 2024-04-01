package org.matsim.modechoice.replanning.scheduled.solver;


import it.unimi.dsi.fastutil.ints.IntIntPair;
import it.unimi.dsi.fastutil.ints.IntList;
import org.optaplanner.core.api.score.buildin.hardsoftlong.HardSoftLongScore;
import org.optaplanner.core.api.score.calculator.IncrementalScoreCalculator;

import java.util.Map;

/**
 * Score calculator.
 */
public final class ScoreCalculator implements IncrementalScoreCalculator<ModeSchedulingProblem, HardSoftLongScore> {

	private ModeSchedulingProblem problem;

	/**
	 * Targets per window.
	 */
	private int[] observed;
	private int[] switches;
	private int targets;

	@Override
	public void resetWorkingSolution(ModeSchedulingProblem problem) {

		this.targets = problem.getTargets().size();
		this.observed = new int[problem.getWindowSize() * targets];
		this.problem = problem;
		this.switches = new int[problem.getWindowSize()];

		calcScoreInternal();
	}

	private void calcScoreInternal() {

		for (AgentSchedule agent : problem.getAgents()) {
			updateAgentPlan(agent, 1);
		}
	}

	@SuppressWarnings("StringEquality")
	private void updateAgentPlan(AgentSchedule agent, int diff) {

		int prevK = agent.currentPlan;

		// i is the index within the window
		for (int i = 0; i < agent.indices.size(); i++) {
			int k = agent.indices.getInt(i);

			for (int j = 0; j < targets; j++) {
				observed[i * targets + j] += diff * agent.weights[k * targets + j];
			}

			// iterate all trips in the plan
			for (int j = 0; j < agent.length; j++) {
				if (prevK >= 0) {
					String type = agent.planCategories[k * agent.length + j];
					String prevType = agent.planCategories[prevK * agent.length + j];
					// All String are internal
					if (prevType != type)
						switches[i] += diff;
				}
			}
			prevK = k;
		}
	}

	/**
	 * Improve an agent schedule and update the internal state.
	 */
	public void applyImprovement(AgentSchedule agent, int idx, IntList avail, int[] targets) {

		// new plan index
		int bestK = agent.indices.getInt(idx);
		int[] bestDiff = new int[targets.length];

		// Weight of the current chosen plan
		int[] weight = new int[targets.length];

		// positive diff means there are too many trips (per target)
		int[] diff = new int[targets.length];

		int bestError = 0;
		for (int i = 0; i < targets.length; i++) {
			weight[i] = agent.weights[bestK * targets.length + i];
			diff[i] = observed[idx * targets.length + i] - targets[i];
			bestError += Math.abs(diff[i]);
		}

		for (int j = 0; j < avail.size(); j++) {

			int k = avail.getInt(j);

			// calc the improvement for each plan
			int[] d = new int[targets.length];
			int sum = 0;
			for (int i = 0; i < targets.length; i++) {
				byte w = agent.weights[k * targets.length + i];
				d[i] = w - weight[i];
				sum += Math.abs(diff[i] + d[i]);
			}

			if (sum < bestError) {
				bestError = sum;
				bestDiff = d;
				bestK = k;
			}
		}

		// update if index has changed
		if (bestK != agent.indices.getInt(idx)) {
			avail.rem(bestK);
			agent.indices.set(idx, bestK);

			for (int i = 0; i < targets.length; i++) {
				observed[idx * targets.length + i] += bestDiff[i];
			}
		}
	}

	@Override
	public void beforeEntityAdded(Object entity) {
	}

	@Override
	public void afterEntityAdded(Object entity) {
	}

	@Override
	public void beforeVariableChanged(Object entity, String variableName) {

		assert variableName.equals("indices");
		AgentSchedule agent = (AgentSchedule) entity;
		updateAgentPlan(agent, -1);
	}

	@Override
	public void afterVariableChanged(Object entity, String variableName) {

		assert variableName.equals("indices");
		AgentSchedule agent = (AgentSchedule) entity;
		updateAgentPlan(agent, 1);

	}

	@Override
	public void beforeEntityRemoved(Object entity) {
	}

	@Override
	public void afterEntityRemoved(Object entity) {

	}

	@Override
	public HardSoftLongScore calculateScore() {
		long hardScore = 0;
		long softScore = 0;

		long prevHard = -1;
		long prevSoft = -1;

		for (int i = 0; i < problem.getWindowSize(); i++) {
			long score = 0;
			int k = 0;
			for (Map.Entry<String, IntIntPair> kv : problem.getTargets().entrySet()) {

				IntIntPair bounds = kv.getValue();
				int obs = observed[i * targets + (k++)];

				// check against bounds
				if (obs > bounds.rightInt())
					score += obs - bounds.rightInt();
				else if (obs < bounds.leftInt())
					score += bounds.leftInt() - obs;
			}

			// Difference between score levels is added to penalize uneven distributed variations
			if (prevHard != -1) {
				hardScore += Math.abs(score - prevHard) / 2;
			}

			// there is no current plan in very first iteration
			long soft = 0;
			if (!problem.isFirstIteration() || i != 0) {
				soft = Math.abs(switches[i] - problem.getSwitchTarget());
				if (prevSoft != -1) {
					softScore += Math.abs(prevSoft - soft) / 2;
				}

				prevSoft = soft;
			}

			prevHard = score;

			hardScore += prevHard;
			softScore += soft;
		}

		return HardSoftLongScore.of(-hardScore, -softScore);
	}

	public int[] getObserved() {
		return observed;
	}

	public int[] getSwitches() {
		return switches;
	}
}
