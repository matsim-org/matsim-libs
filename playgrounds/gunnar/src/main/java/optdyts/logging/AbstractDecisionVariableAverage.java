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
public abstract class AbstractDecisionVariableAverage<X extends SimulatorState, U extends DecisionVariable>
		implements SearchStatistic<X, U> {

	// -------------------- CONSTRUCTION --------------------

	public AbstractDecisionVariableAverage() {
	}

	// --------------- IMPLEMENTATION OF SearchStatistic ---------------

	@Override
	public String label() {
		return "Average " + this.realValueLabel();
	}

	@Override
	public String value(final SurrogateSolution<X, U> surrogateSolution) {
		if (surrogateSolution.hasProperties()) {
			double average = 0;
			for (U decisionVariable : surrogateSolution.getDecisionVariables()) {
				average += surrogateSolution.getAlphaSum(decisionVariable)
						* this.realValue(decisionVariable);
			}
			return Double.toString(average);
		} else {
			return "--";
		}
	}

	// -------------------- INTERFACE DEFINITION --------------------

	public abstract String realValueLabel();

	public abstract double realValue(U decisionVariable);
}
