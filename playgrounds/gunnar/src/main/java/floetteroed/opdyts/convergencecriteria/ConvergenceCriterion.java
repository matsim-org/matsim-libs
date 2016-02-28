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

import floetteroed.opdyts.DecisionVariable;
import floetteroed.opdyts.trajectorysampling.Transition;

/**
 * 
 * @author Gunnar Flötteröd
 *
 */
public interface ConvergenceCriterion {

	/**
	 * Evaluates the ConvergenceCriterion for the given TransitionSequence.
	 * 
	 * @param transitionSequence
	 *            convergence is evaluated for this sequence
	 */
	public <U extends DecisionVariable> ConvergenceCriterionResult evaluate(
			final List<Transition<U>> transitionSequence);

	public double effectiveAveragingIterations();
}
