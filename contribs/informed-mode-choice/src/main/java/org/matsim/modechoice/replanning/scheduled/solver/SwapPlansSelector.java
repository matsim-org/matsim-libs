package org.matsim.modechoice.replanning.scheduled.solver;

import org.optaplanner.core.api.score.director.ScoreDirector;
import org.optaplanner.core.impl.heuristic.selector.move.factory.MoveIteratorFactory;

import java.util.Iterator;
import java.util.List;
import java.util.Random;

/**
 * Shuffle plan elements of a person
 */
public class SwapPlansSelector implements MoveIteratorFactory<ModeSchedulingProblem, SwapPlanMove> {

	@Override
	public long getSize(ScoreDirector<ModeSchedulingProblem> scoreDirector) {
		ModeSchedulingProblem problem = scoreDirector.getWorkingSolution();

		// all have same size available
		return (long) problem.getAgents().size() * problem.getWindowSize() * problem.getNPlans();
	}

	@Override
	public Iterator<SwapPlanMove> createOriginalMoveIterator(ScoreDirector<ModeSchedulingProblem> scoreDirector) {
		return createRandomMoveIterator(scoreDirector, new Random(0));
	}

	@Override
	public Iterator<SwapPlanMove> createRandomMoveIterator(ScoreDirector<ModeSchedulingProblem> scoreDirector, Random workingRandom) {
		return new It(workingRandom, scoreDirector.getWorkingSolution().getAgents(), getSize(scoreDirector));
	}

	private static final class It implements Iterator<SwapPlanMove> {

		private final Random random;
		private final List<AgentSchedule> agents;
		private final long size;

		private long done = 0;

		It(Random random, List<AgentSchedule> agents, long size) {
			this.random = random;
			this.agents = agents;
			this.size = size;
		}

		@Override
		public boolean hasNext() {
			return done < size;
		}

		@Override
		public SwapPlanMove next() {

			done++;

			AgentSchedule a1 = agents.get(random.nextInt(agents.size()));
			AgentSchedule a2 = agents.get(random.nextInt(agents.size()));

			int idx1 = random.nextInt(ModeSchedulingSolver.WINDOW_SIZE);
			int k1 = a1.availablePlans.getInt(random.nextInt(a1.availablePlans.size()));

			int idx2 = random.nextInt(ModeSchedulingSolver.WINDOW_SIZE);
			int k2 = a2.availablePlans.getInt(random.nextInt(a2.availablePlans.size()));

			return new SwapPlanMove(a1, idx1, k1, a2, idx2, k2);
		}
	}

}
