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
public class AllSolutions<X extends SimulatorState, U extends DecisionVariable>
		implements SearchStatistic<X, U> {

	// -------------------- MEMBERS --------------------

	private final List<U> allDecisionVariables;

	private final String separator;

	// -------------------- CONSTRUCTION --------------------

	public AllSolutions(final Set<U> allDecisionVariables,
			final String separator) {
		this.allDecisionVariables = new ArrayList<U>(allDecisionVariables);
		this.separator = separator;
	}

	// --------------- IMPLEMENTATION OF SearchStatistic ---------------

	@Override
	public String label() {
		final StringBuffer result = new StringBuffer();
		result.append(this.allDecisionVariables.get(0));
		for (int i = 1; i < this.allDecisionVariables.size(); i++) {
			result.append(this.separator);
			result.append("alpha(" + this.allDecisionVariables.get(i) + ")");
		}
		return result.toString();
	}

	@Override
	public String value(final SurrogateSolution<X, U> surrogateSolution) {
		final StringBuffer result = new StringBuffer();
		if (surrogateSolution.hasProperties()) {
			result.append(this.allDecisionVariables.get(0));
			for (int i = 1; i < this.allDecisionVariables.size(); i++) {
				result.append(this.separator);
				result.append(surrogateSolution
						.getAlphaSum(this.allDecisionVariables.get(i)));
			}
		} else {
			for (int i = 0; i < this.allDecisionVariables.size(); i++) {
				result.append(this.separator);
				result.append("--"); // no alpha
			}
		}
		return result.toString();
	}
}
