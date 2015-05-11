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
package optdyts;

import java.util.List;

import floetteroed.utilities.math.Vector;

/**
 * Represents a simulator state.
 * 
 * @author Gunnar Flötteröd
 * 
 * @param <X>
 *            the simulator state type
 */
public interface SimulatorState<X extends SimulatorState<X>> {

	/**
	 * Returns a copy of this simulator. This copy must be "deep" for (i) all
	 * referenced objects that may be evaluated by an ObjectiveFunction and the
	 * state's vector representation as returned by
	 * getReferenceToVectorRepresentation().
	 * 
	 * @return a deep copy of this simulator state
	 */
	public X deepCopy();

	/**
	 * Fills this state with a convex combination of the states-List, according
	 * to the values in the weights-List, which need to be non-negative and sum
	 * up to one.
	 * 
	 * @param states
	 *            the states to be combined
	 * @param weights
	 *            the weights according to which the states are combined
	 */
	public void takeOverConvexCombination(List<X> states, List<Double> weights);

	/**
	 * Returns a reference to a real-valued, fixed-dimensional vector
	 * representation of this state.
	 * 
	 * @return a reference to a real-valued, fixed-dimensional vector
	 *         representation of this state
	 */
	public Vector getReferenceToVectorRepresentation();

}
