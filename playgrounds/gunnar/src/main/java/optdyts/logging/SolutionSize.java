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
public class SolutionSize<X extends SimulatorState, U extends DecisionVariable>
		implements SearchStatistic<X, U> {

	@Override
	public String label() {
		return "Solution Size";
	}

	@Override
	public String value(final SurrogateSolution<X, U> surrogateSolution) {
		return Integer.toString(surrogateSolution.size());
	}

}
