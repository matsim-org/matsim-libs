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
class AllSolutions<X extends SimulatorState, U extends DecisionVariable>
		implements SearchStatistic<X, U> {

	private final String separator;

	public AllSolutions(final String separator) {
		this.separator = separator;
	}

	@Override
	public String label() {
		return "Decision variable" + this.separator + "alpha" + this.separator
				+ "...";
	}

	@Override
	public String value(SurrogateSolution<X, U> surrogateSolution) {
		if (surrogateSolution.hasProperties()) {
			final StringBuffer result = new StringBuffer();
			for (U decisionVariable : surrogateSolution.getDecisionVariables()) {
				result.append(decisionVariable + "\t");
				result.append(surrogateSolution.getAlphaSum(decisionVariable)
						+ "\t");
			}
			return result.toString();
		} else {
			return "--";
		}
	}

}
