package floetteroed.opdyts.logging;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import floetteroed.opdyts.DecisionVariable;
import floetteroed.opdyts.trajectorysampling.SamplingStage;
import floetteroed.utilities.statisticslogging.Statistic;

/**
 * 
 * @author Gunnar Flötteröd
 *
 */
public class AllSolutions implements Statistic<SamplingStage> {

	// -------------------- MEMBERS --------------------

	private final List<? extends DecisionVariable> allDecisionVariables;

	private final String separator;

	// -------------------- CONSTRUCTION --------------------

	public AllSolutions(
			final Set<? extends DecisionVariable> allDecisionVariables,
			final String separator) {
		this.allDecisionVariables = new ArrayList<>(allDecisionVariables);
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
	public String value(final SamplingStage samplingStage) {
		final StringBuffer result = new StringBuffer();
		result.append(this.allDecisionVariables.get(0));
		for (int i = 1; i < this.allDecisionVariables.size(); i++) {
			result.append(this.separator);
			result.append(samplingStage.getAlphaSum(this.allDecisionVariables
					.get(i)));
		}
		return result.toString();
	}
}
