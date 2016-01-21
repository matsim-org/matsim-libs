package floetteroed.opdyts.logging;

import floetteroed.opdyts.DecisionVariable;
import floetteroed.opdyts.trajectorysampling.SamplingStage;
import floetteroed.utilities.statisticslogging.Statistic;

/**
 * 
 * @author Gunnar Flötteröd
 *
 */
public class UniformityGap<U extends DecisionVariable> implements
		Statistic<SamplingStage<U>> {

	public UniformityGap() {
	}

	@Override
	public String label() {
		return "Uniformity Gap";
	}

	@Override
	public String value(final SamplingStage<U> samplingStage) {
		return Double.toString(samplingStage.getUniformityGap());
	}

}
