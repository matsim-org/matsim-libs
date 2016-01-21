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
package floetteroed.opdyts;

import java.util.Collection;

/**
 * Creates random decision variables that are used as "innovations" in an
 * iterative, stochastic optimization process.
 * 
 * @param U
 *            the decision variable type
 * 
 * @author Gunnar Flötteröd
 *
 */
public interface DecisionVariableRandomizer<U extends DecisionVariable> {

	// /**
	// * @return a completely random decision variable (uniform to the extent
	// * possible)
	// */
	// public U newRandomDecisionVariable();

	/**
	 * @return two random variations of decisionVariable; these are symmetric
	 *         ("positive and negative") to the extent possible
	 */
	public Collection<U> newRandomVariations(final U decisionVariable);

}
