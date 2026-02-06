package org.matsim.smallScaleCommercialTrafficGeneration;

import org.matsim.smallScaleCommercialTrafficGeneration.SmallScaleCommercialTrafficUtils.StructuralAttribute;

import java.util.ArrayList;
import java.util.List;

public class DefaultVehicleSelection implements VehicleSelection{
	@Override
	public List<StructuralAttribute> getAllCategories() {
		return List.of(
			StructuralAttribute.EMPLOYEE_PRIMARY,
			StructuralAttribute.EMPLOYEE_CONSTRUCTION,
			StructuralAttribute.EMPLOYEE_SECONDARY,
			StructuralAttribute.EMPLOYEE_RETAIL,
			StructuralAttribute.EMPLOYEE_TRAFFIC,
			StructuralAttribute.EMPLOYEE_TERTIARY,
			StructuralAttribute.INHABITANTS
		);
	}


	@Override
	public OdMatrixEntryInformation getOdMatrixEntryInformation(int purpose, String modeORvehType, GenerateSmallScaleCommercialTrafficDemand.SmallScaleCommercialTrafficType smallScaleCommercialTrafficType) {
		VehicleSelection.OdMatrixEntryInformation information = new OdMatrixEntryInformation();
		information.occupancyRate = 0;
		information.possibleVehicleTypes = null;
		information.possibleStartCategories = new ArrayList<>();
		information.possibleStopCategories = new ArrayList<>(getAllCategories());

		if (purpose == 1) {
			if (smallScaleCommercialTrafficType.equals(GenerateSmallScaleCommercialTrafficDemand.SmallScaleCommercialTrafficType.commercialPersonTraffic)) {
				information.possibleVehicleTypes = new String[]{"vwCaddy", "e_SpaceTourer"};
				information.occupancyRate = 1.5;
			}
			information.possibleStartCategories.add(StructuralAttribute.EMPLOYEE_SECONDARY);
			information.possibleStopCategories.clear();
			information.possibleStopCategories.add(StructuralAttribute.EMPLOYEE_SECONDARY);
		} else if (purpose == 2) {
			if (smallScaleCommercialTrafficType.equals(GenerateSmallScaleCommercialTrafficDemand.SmallScaleCommercialTrafficType.commercialPersonTraffic)) {
				information.possibleVehicleTypes = new String[]{"vwCaddy", "e_SpaceTourer"};
				information.occupancyRate = 1.6;
			}
			information.possibleStartCategories.add(StructuralAttribute.EMPLOYEE_SECONDARY);
		} else if (purpose == 3) {
			if (smallScaleCommercialTrafficType.equals(GenerateSmallScaleCommercialTrafficDemand.SmallScaleCommercialTrafficType.commercialPersonTraffic)) {
				information.possibleVehicleTypes = new String[]{"golf1.4", "c_zero"};
				information.occupancyRate = 1.2;
			}
			information.possibleStartCategories.add(StructuralAttribute.EMPLOYEE_RETAIL);
			information.possibleStartCategories.add(StructuralAttribute.EMPLOYEE_TERTIARY);
		} else if (purpose == 4) {
			if (smallScaleCommercialTrafficType.equals(GenerateSmallScaleCommercialTrafficDemand.SmallScaleCommercialTrafficType.commercialPersonTraffic)) {
				information.possibleVehicleTypes = new String[]{"golf1.4", "c_zero"};
				information.occupancyRate = 1.2;
			}
			information.possibleStartCategories.add(StructuralAttribute.EMPLOYEE_TRAFFIC);
		} else if (purpose == 5) {
			if (smallScaleCommercialTrafficType.equals(GenerateSmallScaleCommercialTrafficDemand.SmallScaleCommercialTrafficType.commercialPersonTraffic)) {
				information.possibleVehicleTypes = new String[]{"mercedes313", "e_SpaceTourer"};
				information.occupancyRate = 1.7;
			}
			information.possibleStartCategories.add(StructuralAttribute.EMPLOYEE_CONSTRUCTION);
		} else if (purpose == 6) {
			information.possibleStartCategories.add(StructuralAttribute.INHABITANTS);
		}

		if (smallScaleCommercialTrafficType.equals(GenerateSmallScaleCommercialTrafficDemand.SmallScaleCommercialTrafficType.goodsTraffic)) {
			information.occupancyRate = 1.;
			switch (modeORvehType) {
				case "vehTyp1" ->
					information.possibleVehicleTypes = new String[]{"vwCaddy", "e_SpaceTourer"}; // possible to add more types, see source
				case "vehTyp2" ->
					information.possibleVehicleTypes = new String[]{"mercedes313", "e_SpaceTourer"};
				case "vehTyp3", "vehTyp4" ->
					information.possibleVehicleTypes = new String[]{"light8t", "truck8t", "light8t_electro", "truck8t_electro"};
				case "vehTyp5" ->
					information.possibleVehicleTypes = new String[]{"medium18t", "medium18t_electro", "truck18t", "truck18t_electro"};
			}
		}

		return information;
	}
}
