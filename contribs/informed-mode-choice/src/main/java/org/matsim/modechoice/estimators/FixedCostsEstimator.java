package org.matsim.modechoice.estimators;

import org.matsim.core.scoring.functions.ModeUtilityParameters;
import org.matsim.modechoice.EstimatorContext;
import org.matsim.modechoice.ModeAvailability;

public interface FixedCostsEstimator {

	/**
	 * The usage utility is added to the total score estimate, if this mode was used at least one tine.
	 */
	double usageUtility(EstimatorContext context, String mode, ModeAvailability option);

	/**
	 * The fixed utility is always added to the score estimate, regardless of usage.
	 */
	default double fixedUtility(EstimatorContext context, String mode, ModeAvailability option) {
		return 0;
	}


	/**
	 * Default implementation that uses the daily constant as fixed costs.
	 */
	final class DailyConstant implements FixedCostsEstimator {

		@Override
		public double usageUtility(EstimatorContext context, String mode, ModeAvailability option) {

			if (option.isModeAvailable()) {
				ModeUtilityParameters params = context.scoring.modeParams.get(mode);
				if (params == null)
					throw new IllegalStateException("Scoring parameter for mode " + mode + " not configured.");

				return params.dailyUtilityConstant + context.scoring.marginalUtilityOfMoney * params.dailyMoneyConstant;
			}

			return 0;
		}

		@Override
		public double fixedUtility(EstimatorContext context, String mode, ModeAvailability option) {
			return 0;
		}
	}
}
