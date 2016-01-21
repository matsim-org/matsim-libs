package floetteroed.opdyts.logging;

import floetteroed.opdyts.DecisionVariable;
import floetteroed.opdyts.trajectorysampling.SamplingStage;
import floetteroed.utilities.statisticslogging.Statistic;

/**
 * 
 * @author Gunnar Flötteröd
 *
 */
public class LastDecisionVariable<U extends DecisionVariable> implements
		Statistic<SamplingStage<U>> {

	@Override
	public String label() {
		return "Last Decision Variable";
	}

	@Override
	public String value(final SamplingStage<U> samplingStage) {
		return samplingStage.getLastDecisionVariable().toString();
	}

}
