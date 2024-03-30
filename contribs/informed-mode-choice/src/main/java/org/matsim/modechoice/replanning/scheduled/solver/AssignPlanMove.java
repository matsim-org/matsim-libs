package org.matsim.modechoice.replanning.scheduled.solver;

import org.optaplanner.core.api.score.director.ScoreDirector;
import org.optaplanner.core.impl.heuristic.move.AbstractMove;

/**
 * Switch multiple plan assignments at once.
 */
public class AssignPlanMove extends AbstractMove<ModeSchedulingProblem> {


	private final AgentSchedule agent;
	private final int idx;
	private final int k;

	/**
	 * Assign plan k at index idx.
	 */
	public AssignPlanMove(AgentSchedule agent, int idx, int k) {
		this.agent = agent;
		this.idx = idx;
		this.k = k;
	}

	@Override
	protected AssignPlanMove createUndoMove(ScoreDirector<ModeSchedulingProblem> scoreDirector) {
		return new AssignPlanMove(agent, idx, agent.indices.getInt(idx));
	}

	static void assign(AgentSchedule agent, int idx, int k) {
		int current = agent.indices.getInt(idx);

		// Swap k, if some other idx is assigned to the same k
		for (int i = 0; i < agent.indices.size(); i++) {
			if (agent.indices.getInt(i) == k) {
				agent.indices.set(i, current);
			}
		}

		agent.indices.set(idx, k);
	}

	@Override
	protected void doMoveOnGenuineVariables(ScoreDirector<ModeSchedulingProblem> scoreDirector) {

		scoreDirector.beforeVariableChanged(agent, "indices");

		assign(agent, idx, k);

		scoreDirector.afterVariableChanged(agent, "indices");
	}

	@Override
	public AssignPlanMove rebase(ScoreDirector<ModeSchedulingProblem> destinationScoreDirector) {

		AgentSchedule other = destinationScoreDirector.lookUpWorkingObject(agent);
		return new AssignPlanMove(other, idx, k);
	}

	@Override
	public boolean isMoveDoable(ScoreDirector<ModeSchedulingProblem> scoreDirector) {
		return true;
	}
}
