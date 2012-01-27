package playground.yu.integration.cadyts.parameterCalibration.withCarCounts.testC1LT;

import cadyts.calibrators.analytical.ChoiceParameterCalibrator;
import cadyts.demand.Plan;

public class CPC1<L> extends ChoiceParameterCalibrator<L> {

	private static final long serialVersionUID = 1L;

	public CPC1(String logFile, Long randomSeed, int timeBinSize_s,
			int parameterDimension) {
		super(logFile, randomSeed, timeBinSize_s, parameterDimension);
	}

	public double getUtilityCorrection(final Plan<L> plan) {
		return super.calcLinearPlanEffect(plan);
	}
}
