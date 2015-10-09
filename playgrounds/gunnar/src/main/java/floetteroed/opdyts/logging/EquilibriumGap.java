package floetteroed.opdyts.logging;

import floetteroed.opdyts.trajectorysampling.SamplingStage;
import floetteroed.utilities.statisticslogging.Statistic;

/**
 * 
 * @author Gunnar Flötteröd
 *
 */
public class EquilibriumGap implements Statistic<SamplingStage> {

	@Override
	public String label() {
		return "Equilibrium Gap";
	}

	@Override
	public String value(final SamplingStage samplingStage) {
		return Double.toString(samplingStage.getEquilibriumGap());
	}

}
