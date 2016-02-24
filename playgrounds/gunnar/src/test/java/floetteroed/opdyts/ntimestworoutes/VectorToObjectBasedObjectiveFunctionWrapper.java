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

import floetteroed.opdyts.ObjectiveFunction;
import floetteroed.opdyts.SimulatorState;

/**
 * 
 * @author Gunnar Flötteröd
 *
 */
@Deprecated
public class VectorToObjectBasedObjectiveFunctionWrapper implements
		ObjectiveFunction {

	private final VectorBasedObjectiveFunction objectiveFunction;

	public VectorToObjectBasedObjectiveFunctionWrapper(
			final VectorBasedObjectiveFunction objectiveFunction) {
		this.objectiveFunction = objectiveFunction;
	}

	@Override
	public double value(final SimulatorState state) {
		return this.objectiveFunction.value(state
				.getReferenceToVectorRepresentation());
	}

}
