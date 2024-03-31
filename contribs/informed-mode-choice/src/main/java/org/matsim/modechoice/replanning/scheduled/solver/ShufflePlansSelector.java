package org.matsim.modechoice.replanning.scheduled.solver;

import org.optaplanner.core.api.score.director.ScoreDirector;
import org.optaplanner.core.impl.heuristic.selector.move.factory.MoveIteratorFactory;

import java.util.Iterator;
import java.util.List;
import java.util.Random;

/**
 * Shuffle plan elements of a person
 */
public class ShufflePlansSelector implements MoveIteratorFactory<ModeSchedulingProblem, AssignPlanMove> {

	@Override
	public long getSize(ScoreDirector<ModeSchedulingProblem> scoreDirector) {
		ModeSchedulingProblem problem = scoreDirector.getWorkingSolution();

		// all have same size available
		return (long) problem.getAgents().size() * problem.getWindowSize() * problem.getNPlans();
	}

	@Override
	public Iterator<AssignPlanMove> createOriginalMoveIterator(ScoreDirector<ModeSchedulingProblem> scoreDirector) {
		return createRandomMoveIterator(scoreDirector, new Random(0));
	}

	@Override
	public Iterator<AssignPlanMove> createRandomMoveIterator(ScoreDirector<ModeSchedulingProblem> scoreDirector, Random workingRandom) {
		return new It(workingRandom, scoreDirector.getWorkingSolution().getAgents(), getSize(scoreDirector));
	}

	private static final class It implements Iterator<AssignPlanMove> {

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
		public AssignPlanMove next() {

			done++;

			AgentSchedule agent = agents.get(random.nextInt(agents.size()));

			// TODO: chose plan in the right direction (using score director)

			int idx = random.nextInt(ModeSchedulingSolver.WINDOW_SIZE);
			int k = agent.availablePlans.getInt(random.nextInt(agent.availablePlans.size()));

			return new AssignPlanMove(agent, idx, k);
		}
	}

}
