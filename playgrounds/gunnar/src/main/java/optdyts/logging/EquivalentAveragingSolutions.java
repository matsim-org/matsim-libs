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
public class EquivalentAveragingSolutions<X extends SimulatorState, U extends DecisionVariable>
		implements SearchStatistic<X, U> {

	@Override
	public String label() {
		return "Equivalent Averaging Solutions";
	}

	@Override
	public String value(final SurrogateSolution<X, U> surrogateSolution) {
		if (surrogateSolution.hasProperties()) {
			return Double.toString(surrogateSolution
					.getEquivalentAveragingIterations());
		} else {
			return "--";
		}
	}

}
