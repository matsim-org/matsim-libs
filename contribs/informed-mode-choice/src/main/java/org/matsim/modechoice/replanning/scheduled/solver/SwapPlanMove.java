package org.matsim.modechoice.replanning.scheduled.solver;

import org.optaplanner.core.api.score.director.ScoreDirector;
import org.optaplanner.core.impl.heuristic.move.AbstractMove;

/**
 * Switch multiple plan assignments at once.
 */
public class SwapPlanMove extends AbstractMove<ModeSchedulingProblem> {


	private final AgentSchedule a1;
	private final int idx1;
	private final int k1;
	private final AgentSchedule a2;
	private final int idx2;
	private final int k2;

	public SwapPlanMove(AgentSchedule a1, int idx1, int k1, AgentSchedule a2, int idx2, int k2) {
		this.a1 = a1;
		this.idx1 = idx1;
		this.k1 = k1;
		this.a2 = a2;
		this.idx2 = idx2;
		this.k2 = k2;
	}


	@Override
	protected SwapPlanMove createUndoMove(ScoreDirector<ModeSchedulingProblem> scoreDirector) {
		return new SwapPlanMove(a1, idx1, a1.indices.getInt(idx1), a2, idx2, a2.indices.getInt(idx2));
	}

	@Override
	protected void doMoveOnGenuineVariables(ScoreDirector<ModeSchedulingProblem> scoreDirector) {

		scoreDirector.beforeVariableChanged(a1, "indices");
		AssignPlanMove.assign(a1, idx1, k1);
		scoreDirector.afterVariableChanged(a1, "indices");

		scoreDirector.beforeVariableChanged(a2, "indices");
		AssignPlanMove.assign(a2, idx2, k2);
		scoreDirector.afterVariableChanged(a2, "indices");

	}

	@Override
	public SwapPlanMove rebase(ScoreDirector<ModeSchedulingProblem> destinationScoreDirector) {
		AgentSchedule o1 = destinationScoreDirector.lookUpWorkingObject(a1);
		AgentSchedule o2 = destinationScoreDirector.lookUpWorkingObject(a2);
		return new SwapPlanMove(o1, idx1, k1, o2, idx2, k2);
	}

	@Override
	public boolean isMoveDoable(ScoreDirector<ModeSchedulingProblem> scoreDirector) {
		return a1 != a2;
	}
}
