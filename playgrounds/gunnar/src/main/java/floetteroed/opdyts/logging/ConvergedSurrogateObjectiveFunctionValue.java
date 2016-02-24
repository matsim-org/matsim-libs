package floetteroed.opdyts.logging;

import floetteroed.opdyts.DecisionVariable;
import floetteroed.opdyts.trajectorysampling.SamplingStage;
import floetteroed.utilities.statisticslogging.Statistic;

/**
 * 
 * @author Gunnar Flötteröd
 *
 */
public class ConvergedSurrogateObjectiveFunctionValue<U extends DecisionVariable>
		implements Statistic<SamplingStage<U>> {

	public static final String LABEL = "Converged Surrogate Objective Function Value";

	@Override
	public String label() {
		return LABEL;
	}

	@Override
	public String value(final SamplingStage<U> samplingStage) {
		final Double value = samplingStage.getConvergedSurrogateObjectiveFunctionValue();
		if (value == null) {
			return "";
		} else {
			return Double.toString(value);
		}
	}
}
