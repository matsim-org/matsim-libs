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
public class MaximumAbsoluteEquilibriumGap<X extends SimulatorState, U extends DecisionVariable>
		implements SearchStatistic<X, U> {

	@Override
	public String label() {
		return "Maximum Absolute Equilibrium Gap";
	}

	@Override
	public String value(final SurrogateSolution<X, U> surrogateSolution) {
		if (surrogateSolution.hasProperties()) {
			return Double
					.toString(surrogateSolution.getMaximumRelativeGap()
							* surrogateSolution
									.getInterpolatedFromStateEuclideanNorm());
		} else {
			return "--";
		}
	}

}
