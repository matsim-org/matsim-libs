package org.matsim.core.scoring.functions;

import org.matsim.core.config.groups.ScoringConfigGroup;
import org.matsim.vehicles.CostInformation;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.Vehicles;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

final class VehicleTypeBasedScoringUtils {

	private VehicleTypeBasedScoringUtils() {
	}

	static void addVehicleTypeModeParamsToScoringConfig(ScoringConfigGroup scoringConfigGroup, Vehicles vehicles) {
		for (ScoringConfigGroup.ScoringParameterSet scoringParameterSet : scoringConfigGroup.getScoringParametersPerSubpopulation().values()) {
			for (VehicleType vehicleType : getVehicleTypesInUse(vehicles).values()) {
				if (!scoringParameterSet.getModes().containsKey(vehicleType.getId().toString())) {
					scoringParameterSet.addModeParams(createModeParams(vehicleType));
				}
			}
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

	private static Map<String, VehicleType> getVehicleTypesInUse(Vehicles vehicles) {
		Map<String, VehicleType> vehicleTypes = new LinkedHashMap<>();
		vehicles.getVehicles().values().stream()
			.map(Vehicle::getType)
			.filter(Objects::nonNull)
			.forEach(vehicleType -> vehicleTypes.putIfAbsent(vehicleType.getId().toString(), vehicleType));
		return vehicleTypes;
	}

	private static double nullToZero(Double value) {
		return value == null ? 0.0 : value;
	}
}
