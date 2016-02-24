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
package floetteroed.opdyts.ntimestworoutes;

import floetteroed.utilities.math.Vector;

/**
 * 
 * Am objective function that evaluates the vector-representation of a
 * SimulatorState. The smaller the objective function value, the better the
 * state.
 * 
 * @author Gunnar Flötteröd
 *
 */
@Deprecated
public interface VectorBasedObjectiveFunction {

	public double value(Vector state);

	/**
	 * @param state
	 *            a vector representation of the a simulator state
	 * @return the gradient of the objective function, evaluated at the point
	 *         given by state
	 */
	public Vector gradient(Vector state);

}
