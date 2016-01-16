package floetteroed.opdyts.logging;

import floetteroed.opdyts.DecisionVariable;
import floetteroed.opdyts.trajectorysampling.SamplingStage;
import floetteroed.utilities.statisticslogging.Statistic;

/**
 * 
 * @author Gunnar Flötteröd
 *
 */
public class ConvergedObjectiveFunctionValue<U extends DecisionVariable>
		implements Statistic<SamplingStage<U>> {

	public static final String LABEL = "Converged Objective Function Value";

	@Override
	public String label() {
		return LABEL;
	}

	@Override
	public String value(final SamplingStage<U> samplingStage) {
		final Double value = samplingStage.getConvergedObjectiveFunctionValue();
		if (value == null) {
			return "";
		} else {
			return Double.toString(value);
		}
	}
}
