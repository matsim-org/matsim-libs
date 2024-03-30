package org.matsim.modechoice.replanning.scheduled.solver;


import it.unimi.dsi.fastutil.ints.IntIntPair;
import it.unimi.dsi.fastutil.objects.Reference2IntMap;
import it.unimi.dsi.fastutil.objects.Reference2IntOpenHashMap;
import org.optaplanner.core.api.score.buildin.hardsoftlong.HardSoftLongScore;
import org.optaplanner.core.api.score.calculator.IncrementalScoreCalculator;

import java.util.Map;

/**
 * Score calculator.
 */
public final class ScoreCalculator implements IncrementalScoreCalculator<ModeSchedulingProblem, HardSoftLongScore> {

	private ModeSchedulingProblem problem;
	private Reference2IntMap<String>[] observed;
	private int[] switches;

	@Override
	public void resetWorkingSolution(ModeSchedulingProblem problem) {

		observed = new Reference2IntMap[problem.getWindowSize()];
		this.problem = problem;
		for (int i = 0; i < problem.getWindowSize(); i++) {
			observed[i] = new Reference2IntOpenHashMap<>();
		}

		switches = new int[problem.getWindowSize()];

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

			// iterate all trips in the plan
			for (int j = 0; j < agent.length; j++) {
				String type = agent.planCategories[k * agent.length + j];
				if (type != null)
					observed[i].merge(type, diff, Integer::sum);

				if (prevK >= 0) {
					String prevType = agent.planCategories[prevK * agent.length + j];
					// All String are internal
					if (prevType != type)
						switches[i] += diff;
				}
			}
			prevK = k;
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
			for (Map.Entry<String, IntIntPair> kv : problem.getTargets().entrySet()) {

				IntIntPair bounds = kv.getValue();
				int obs = observed[i].getInt(kv.getKey());

				// check against bounds
				if (obs > bounds.rightInt())
					score += obs - bounds.rightInt();
				else if (obs < bounds.leftInt())
					score += bounds.leftInt() - obs;
			}

			// Difference between score levels is added to penalize uneven distributed variations
			if (prevHard != -1) {
				hardScore += Math.abs(score - prevHard) / 3;
			}

			// there is no current plan in very first iteration
			long soft = 0;
			if (!problem.isFirstIteration() || i != 0) {
				soft = Math.abs(switches[i] - problem.getSwitchTarget());
				if (prevSoft != -1) {
					softScore += Math.abs(prevSoft - soft) / 3;
				}

				prevSoft = soft;
			}

			prevHard = score;

			hardScore += prevHard;
			softScore += soft;
		}

		return HardSoftLongScore.of(-hardScore, -softScore);
	}

	public Reference2IntMap<String>[] getObserved() {
		return observed;
	}

	public int[] getSwitches() {
		return switches;
	}
}
