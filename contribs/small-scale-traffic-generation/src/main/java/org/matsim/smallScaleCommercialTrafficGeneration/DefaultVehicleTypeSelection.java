package org.matsim.smallScaleCommercialTrafficGeneration;

import org.matsim.smallScaleCommercialTrafficGeneration.GenerateSmallScaleCommercialTrafficDemand.SmallScaleCommercialTrafficType;

/**
 * Default vehicle type and occupancy selection for small-scale commercial traffic.
 */
public class DefaultVehicleTypeSelection implements VehicleTypeSelection {

	@Override
	public VehicleTypeInformation getVehicleTypeInformation(
		int purpose,
		String modeOrVehType,
		SmallScaleCommercialTrafficType smallScaleCommercialTrafficType
	) {
		if (smallScaleCommercialTrafficType.equals(SmallScaleCommercialTrafficType.commercialPersonTraffic)) {
			if (purpose == 1) {
				return new VehicleTypeInformation(new String[]{"vwCaddy", "e_SpaceTourer"}, 1.5);
			} else if (purpose == 2) {
				return new VehicleTypeInformation(new String[]{"vwCaddy", "e_SpaceTourer"}, 1.6);
			} else if (purpose == 3) {
				return new VehicleTypeInformation(new String[]{"golf1.4", "c_zero"}, 1.2);
			} else if (purpose == 4) {
				return new VehicleTypeInformation(new String[]{"golf1.4", "c_zero"}, 1.2);
			} else if (purpose == 5) {
				return new VehicleTypeInformation(new String[]{"mercedes313", "e_SpaceTourer"}, 1.7);
			}
		} else if (smallScaleCommercialTrafficType.equals(SmallScaleCommercialTrafficType.goodsTraffic)) {
			return switch (modeOrVehType) {
				case "vehTyp1" -> new VehicleTypeInformation(
					new String[]{"vwCaddy", "e_SpaceTourer"}, 1.); // possible to add more types, see source
				case "vehTyp2" -> new VehicleTypeInformation(new String[]{"mercedes313", "e_SpaceTourer"}, 1.);
				case "vehTyp3", "vehTyp4" -> new VehicleTypeInformation(
					new String[]{"light8t", "truck8t", "light8t_electro", "truck8t_electro"}, 1.);
				case "vehTyp5" -> new VehicleTypeInformation(
					new String[]{"medium18t", "medium18t_electro", "truck18t", "truck18t_electro"}, 1.);
				default -> null;
			};
		}

		return null;
	}
}
