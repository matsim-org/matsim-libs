package org.matsim.core.scoring.functions;

import org.matsim.core.config.groups.ScoringConfigGroup;
import org.matsim.vehicles.CostInformation;
import org.matsim.vehicles.VehicleType;

final class VehicleTypeBasedScoringUtils {

	private VehicleTypeBasedScoringUtils() {
	}

	static ScoringConfigGroup.ModeParams getOrCreateModeParams(ScoringConfigGroup.ScoringParameterSet scoringParameterSet, VehicleType vehicleType) {
		synchronized (scoringParameterSet) {
			String mode = vehicleType.getId().toString();
			ScoringConfigGroup.ModeParams modeParams = scoringParameterSet.getModeParams().get(mode);
			if (modeParams != null) {
				return modeParams;
			}
			modeParams = createModeParams(vehicleType);
			scoringParameterSet.addModeParams(modeParams);
			return modeParams;
		}
	}

	static ScoringConfigGroup.ModeParams createModeParams(VehicleType vehicleType) {
		CostInformation costInformation = vehicleType.getCostInformation();

		ScoringConfigGroup.ModeParams modeParams = new ScoringConfigGroup.ModeParams(vehicleType.getId().toString());
		modeParams.setDailyMonetaryConstant(-nullToZero(costInformation.getFixedCosts()));
		modeParams.setMarginalUtilityOfDistance(-nullToZero(costInformation.getCostsPerMeter()));
		modeParams.setMarginalUtilityOfTraveling(-nullToZero(costInformation.getCostsPerSecond()) * 3600);
		return modeParams;
	}

	private static double nullToZero(Double value) {
		return value == null ? 0.0 : value;
	}
}
