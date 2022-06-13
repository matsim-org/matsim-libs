package org.matsim.modechoice.estimators;

import org.matsim.core.scoring.functions.ModeUtilityParameters;
import org.matsim.modechoice.EstimatorContext;
import org.matsim.modechoice.ModeAvailability;

public interface FixedCostEstimator<T extends Enum<?>> {

	/**
	 * The usage utility is added to the total score estimate, if this mode was used at least one tine.
	 */
	double usageUtility(EstimatorContext context, String mode, T option);

	/**
	 * The fixed utility is always added to the score estimate, regardless of usage.
	 */
	double fixedUtility(EstimatorContext context, String mode, T option);


	static final class DailyConstant implements FixedCostEstimator<ModeAvailability> {

		@Override
		public double usageUtility(EstimatorContext context, String mode, ModeAvailability option) {

			if (option == ModeAvailability.YES) {
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
