package org.matsim.modechoice.estimators;

import org.matsim.core.scoring.functions.ModeUtilityParameters;
import org.matsim.modechoice.EstimatorContext;
import org.matsim.modechoice.ModeAvailability;

/**
 * Uses daily constant as fix costs.
 */
public class DailyConstantFixedCosts implements FixedCostsEstimator<ModeAvailability> {

	@Override
	public double usageUtility(EstimatorContext context, String mode, ModeAvailability option) {
		ModeUtilityParameters params = context.scoring.modeParams.get(mode);
		return params.dailyMoneyConstant + context.scoring.marginalUtilityOfMoney * params.dailyMoneyConstant;
	}

	@Override
	public double fixedUtility(EstimatorContext context, String mode, ModeAvailability option) {
		return 0;
	}
}
