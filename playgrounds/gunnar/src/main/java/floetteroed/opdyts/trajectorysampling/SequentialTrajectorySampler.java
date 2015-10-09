/*
 * Opdyts - Optimization of dynamic traffic simulations
 *
 * Copyright 2015 Gunnar Flötteröd
 * 
 *
 * This file is part of Opdyts.
 *
 * Opdyts is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Opdyts is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Opdyts.  If not, see <http://www.gnu.org/licenses/>.
 *
 * contact: gunnar.floetteroed@abe.kth.se
 *
 */
package floetteroed.opdyts.trajectorysampling;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import floetteroed.opdyts.DecisionVariable;
import floetteroed.opdyts.SimulatorState;

/**
 * 
 * @author Gunnar Flötteröd
 *
 */
public class SequentialTrajectorySampler implements TrajectorySampler {

	// -------------------- MEMBERS --------------------

	private final LinkedList<SingleTrajectorySampler> untriedSingleTrajectorySamplers;

	private final LinkedList<SingleTrajectorySampler> processedSingleTrajectorySamplers;

	// -------------------- CONSTRUCTION --------------------

	public SequentialTrajectorySampler(
			final List<? extends SingleTrajectorySampler> singleTrajectorySamplers) {
		this.untriedSingleTrajectorySamplers = new LinkedList<SingleTrajectorySampler>(
				singleTrajectorySamplers);
		this.processedSingleTrajectorySamplers = new LinkedList<SingleTrajectorySampler>();
	}

	// -------------------- GETTERS --------------------

	public List<SingleTrajectorySampler> getProcessedSingleTrajectorySamplersView() {
		return Collections
				.unmodifiableList(this.processedSingleTrajectorySamplers);
	}

	public int getUntriedTrajectorySamplerCnt() {
		return this.untriedSingleTrajectorySamplers.size();
	}

	// --------------- IMPLEMENTATION OF TrajectorySampler ---------------

	@Override
	public void initialize() {
		this.processedSingleTrajectorySamplers
				.addFirst(this.untriedSingleTrajectorySamplers.removeLast());
	}

	@Override
	public boolean foundSolution() {
		return ((this.untriedSingleTrajectorySamplers.size() == 0) && this.processedSingleTrajectorySamplers
				.getFirst().foundSolution());
	}

	@Override
	public DecisionVariable getCurrentDecisionVariable() {
		return this.processedSingleTrajectorySamplers.getFirst()
				.getCurrentDecisionVariable();
	}

	@Override
	public void afterIteration(final SimulatorState newState) {
		this.processedSingleTrajectorySamplers.getFirst().afterIteration(
				newState);
		if ((this.untriedSingleTrajectorySamplers.size() > 0)
				&& this.processedSingleTrajectorySamplers.getFirst()
						.foundSolution()) {
			this.processedSingleTrajectorySamplers
					.addFirst(this.untriedSingleTrajectorySamplers.removeLast());
		}
	}

	@Override
	public Map<DecisionVariable, Double> getDecisionVariable2finalObjectiveFunctionValue() {
		final Map<DecisionVariable, Double> result = new LinkedHashMap<DecisionVariable, Double>();
		for (SingleTrajectorySampler singleSampler : this.processedSingleTrajectorySamplers) {
			result.putAll(singleSampler
					.getDecisionVariable2finalObjectiveFunctionValue());
		}
		return result;
	}

	@Override
	public int getTotalTransitionCnt() {
		int result = 0;
		for (SingleTrajectorySampler singleSampler : this.processedSingleTrajectorySamplers) {
			result += singleSampler.getTotalTransitionCnt();
		}
		return result;
	}
}
