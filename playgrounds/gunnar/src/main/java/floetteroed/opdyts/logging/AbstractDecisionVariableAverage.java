package floetteroed.opdyts.logging;

import floetteroed.opdyts.DecisionVariable;
import floetteroed.opdyts.trajectorysampling.SamplingStage;
import floetteroed.utilities.statisticslogging.Statistic;

/**
 * 
 * @author Gunnar Flötteröd
 *
 */
public abstract class AbstractDecisionVariableAverage implements
		Statistic<SamplingStage> {

	// -------------------- CONSTRUCTION --------------------

	public AbstractDecisionVariableAverage() {
	}

	// --------------- IMPLEMENTATION OF SearchStatistic ---------------

	@Override
	public String label() {
		return "Average " + this.realValueLabel();
	}

	@Override
	public String value(final SamplingStage samplingStage) {
		double average = 0;
		for (DecisionVariable decisionVariable : samplingStage
				.getDecisionVariables()) {
			average += samplingStage.getAlphaSum(decisionVariable)
					* this.realValue(decisionVariable);
		}
		return Double.toString(average);
	}

	// -------------------- INTERFACE DEFINITION --------------------

	public abstract String realValueLabel();

	public abstract double realValue(DecisionVariable decisionVariable);
}
