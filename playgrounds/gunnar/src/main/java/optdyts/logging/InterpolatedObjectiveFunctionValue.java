package optdyts.logging;

import optdyts.DecisionVariable;
import optdyts.SimulatorState;
import optdyts.surrogatesolutions.SurrogateSolution;

/**
 * 
 * @author Gunnar Flötteröd
 *
 * @param <X>
 * @param <U>
 */
public class InterpolatedObjectiveFunctionValue<X extends SimulatorState, U extends DecisionVariable>
		implements SearchStatistic<X, U> {

	@Override
	public String label() {
		return "Interpolated Objective Function Value";
	}

	@Override
	public String value(final SurrogateSolution<X, U> surrogateSolution) {
		if (surrogateSolution.hasProperties()) {
			return Double.toString(surrogateSolution
					.getInterpolatedObjectiveFunctionValue());
		} else {
			return "--";
		}
	}

}
