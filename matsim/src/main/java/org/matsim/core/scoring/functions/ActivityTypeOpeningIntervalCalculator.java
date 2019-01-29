package org.matsim.core.scoring.functions;

import org.matsim.api.core.v01.population.Activity;

public class ActivityTypeOpeningIntervalCalculator implements OpeningIntervalCalculator {
	private final ScoringParameters params;

	public ActivityTypeOpeningIntervalCalculator(ScoringParameters params) {
		this.params = params;
	}

	@Override
	public double[] getOpeningInterval(final Activity act) {

		ActivityUtilityParameters actParams = this.params.utilParams.get(act.getType());
		if (actParams == null) {
			throw new IllegalArgumentException("acttype \"" + act.getType() + "\" is not known in utility parameters " +
					"(module name=\"planCalcScore\" in the config file).");
		}

		double openingTime = actParams.getOpeningTime();
		double closingTime = actParams.getClosingTime();

		// openInterval has two values
		// openInterval[0] will be the opening time
		// openInterval[1] will be the closing time

		return new double[]{openingTime, closingTime};
	}
}
