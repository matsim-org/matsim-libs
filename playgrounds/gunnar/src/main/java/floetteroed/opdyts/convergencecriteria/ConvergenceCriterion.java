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
package floetteroed.opdyts.convergencecriteria;

import java.util.List;

import floetteroed.opdyts.trajectorysampling.TransitionSequence;

/**
 * 
 * @author Gunnar Flötteröd
 *
 */
public interface ConvergenceCriterion {

	/**
	 * Evaluates the ConvergenceCriterion for the given TransitionSequence.
	 * 
	 * TODO The only public information currently available through
	 * TransitionSequence is the number of transitions and the objective
	 * function value sequence. But at least the to-state vectors should also be
	 * available.
	 * 
	 * @param transitionSequence
	 *            convergence is evaluated for this sequence
	 */
	public void evaluate(final TransitionSequence<?> transitionSequence);

	/**
	 * @return if the most recently evaluated TransitionSequence is converged
	 */
	public boolean isConverged();

	/**
	 * @return the final (possibly post-processed, e.g. averaged) objective
	 *         function value of the most recently evaluated TransitionSequence
	 */
	public Double getFinalObjectiveFunctionValue();
	
	public void reset();

	// TODO NEW
	public List<Double> getFinalWeights();
	
}
