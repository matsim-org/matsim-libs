package floetteroed.opdyts.logging;

import floetteroed.opdyts.DecisionVariable;
import floetteroed.opdyts.trajectorysampling.SamplingStage;
import floetteroed.utilities.statisticslogging.Statistic;

/**
 * 
 * @author Gunnar Flötteröd
 *
 */
public abstract class AbstractDecisionVariableAverage<U extends DecisionVariable>
		implements Statistic<SamplingStage<U>> {

	// -------------------- CONSTRUCTION --------------------

	public AbstractDecisionVariableAverage() {
	}

	// --------------- IMPLEMENTATION OF SearchStatistic ---------------

	@Override
	public String label() {
		return "Average " + this.realValueLabel();
	}

	@Override
	public String value(final SamplingStage<U> samplingStage) {
		double average = 0;
		for (U decisionVariable : samplingStage.getDecisionVariables()) {
			average += samplingStage.getAlphaSum(decisionVariable)
					* this.realValue(decisionVariable);
		}
		return Double.toString(average);
	}

	// -------------------- INTERFACE DEFINITION --------------------

	public abstract String realValueLabel();

	public abstract double realValue(DecisionVariable decisionVariable);
}
