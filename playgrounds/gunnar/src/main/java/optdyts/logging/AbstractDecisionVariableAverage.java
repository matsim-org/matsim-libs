package optdyts.logging;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

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

	// -------------------- MEMBERS --------------------

	private final List<U> allDecisionVariables;

	private final String separator;

	// -------------------- CONSTRUCTION --------------------

	public AbstractDecisionVariableAverage(final Set<U> allDecisionVariables,
			final String separator) {
		this.allDecisionVariables = new ArrayList<U>(allDecisionVariables);
		this.separator = separator;
	}

	// --------------- PARTIAL IMPLEMENTATION OF SearchStatistic ---------------

	@Override
	public String label() {
		final StringBuffer result = new StringBuffer();
		result.append("average(" + this.realValueLabel() + ")");
		for (U decisionVariable : this.allDecisionVariables) {
			result.append(this.separator);
			result.append("alpha(" + this.realValue(decisionVariable) + ")");
		}
		return result.toString();
	}

	@Override
	public String value(final SurrogateSolution<X, U> surrogateSolution) {
		if (surrogateSolution.hasProperties()) {
			final StringBuffer result = new StringBuffer();
			double average = 0;
			for (U decisionVariable : surrogateSolution.getDecisionVariables()) {
				average += surrogateSolution.getAlphaSum(decisionVariable)
						* this.realValue(decisionVariable);
			}
			result.append(average);
			for (U decisionVariable : this.allDecisionVariables) {
				result.append(this.separator);
				if (surrogateSolution.getDecisionVariables().contains(
						decisionVariable)) {
					result.append(surrogateSolution
							.getAlphaSum(decisionVariable));
				} else {
					result.append("0.0");
				}
			}
			return result.toString();
		} else {
			final StringBuffer result = new StringBuffer();
			result.append("--");
			for (int i = 0; i < this.allDecisionVariables.size(); i++) {
				result.append(this.separator);
				result.append("--");
			}
			return result.toString();
		}
	}

	// -------------------- INTERFACE DEFINITION --------------------

	public abstract String realValueLabel();

	public abstract double realValue(U decisionVariable);
}
