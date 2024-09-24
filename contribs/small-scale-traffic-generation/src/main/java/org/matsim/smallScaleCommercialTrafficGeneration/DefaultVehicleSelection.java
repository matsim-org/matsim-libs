package org.matsim.smallScaleCommercialTrafficGeneration;

import java.util.ArrayList;
import java.util.List;

public class DefaultVehicleSelection implements VehicleSelection{
	@Override
	public List<String> getAllCategories() {
		ArrayList<String> categories = new ArrayList<>(7);
		categories.add("Employee Primary Sector");
		categories.add("Employee Construction");
		categories.add("Employee Secondary Sector Rest");
		categories.add("Employee Retail");
		categories.add("Employee Traffic/Parcels");
		categories.add("Employee Tertiary Sector Rest");
		categories.add("Inhabitants");
		return categories;
	}


	@Override
	public OdMatrixEntryInformation getOdMatrixEntryInformation(int purpose, String modeORvehType, String smallScaleCommercialTrafficType) {
		VehicleSelection.OdMatrixEntryInformation information = new OdMatrixEntryInformation();
		information.occupancyRate = 0;
		information.possibleVehicleTypes = null;
		information.possibleStartCategories = new ArrayList<>();
		information.possibleStopCategories = new ArrayList<>(getAllCategories());

		if (purpose == 1) {
			if (smallScaleCommercialTrafficType.equals("commercialPersonTraffic")) {
				information.possibleVehicleTypes = new String[]{"vwCaddy", "e_SpaceTourer"};
				information.occupancyRate = 1.5;
			}
			information.possibleStartCategories.add("Employee Secondary Sector Rest");
			information.possibleStopCategories.clear();
			information.possibleStopCategories.add("Employee Secondary Sector Rest");
		} else if (purpose == 2) {
			if (smallScaleCommercialTrafficType.equals("commercialPersonTraffic")) {
				information.possibleVehicleTypes = new String[]{"vwCaddy", "e_SpaceTourer"};
				information.occupancyRate = 1.6;
			}
			information.possibleStartCategories.add("Employee Secondary Sector Rest");
		} else if (purpose == 3) {
			if (smallScaleCommercialTrafficType.equals("commercialPersonTraffic")) {
				information.possibleVehicleTypes = new String[]{"golf1.4", "c_zero"};
				information.occupancyRate = 1.2;
			}
			information.possibleStartCategories.add("Employee Retail");
			information.possibleStartCategories.add("Employee Tertiary Sector Rest");
		} else if (purpose == 4) {
			if (smallScaleCommercialTrafficType.equals("commercialPersonTraffic")) {
				information.possibleVehicleTypes = new String[]{"golf1.4", "c_zero"};
				information.occupancyRate = 1.2;
			}
			information.possibleStartCategories.add("Employee Traffic/Parcels");
		} else if (purpose == 5) {
			if (smallScaleCommercialTrafficType.equals("commercialPersonTraffic")) {
				information.possibleVehicleTypes = new String[]{"mercedes313", "e_SpaceTourer"};
				information.occupancyRate = 1.7;
			}
			information.possibleStartCategories.add("Employee Construction");
		} else if (purpose == 6) {
			information.possibleStartCategories.add("Inhabitants");
		}

		if (smallScaleCommercialTrafficType.equals("goodsTraffic")) {
			information.occupancyRate = 1.;
			switch (modeORvehType) {
				case "vehTyp1" ->
					information.possibleVehicleTypes = new String[]{"vwCaddy", "e_SpaceTourer"}; // possible to add more types, see source
				case "vehTyp2" ->
					information.possibleVehicleTypes = new String[]{"mercedes313", "e_SpaceTourer"};
				case "vehTyp3", "vehTyp4" ->
					information.possibleVehicleTypes = new String[]{"light8t", "light8t_electro"};
				case "vehTyp5" ->
					information.possibleVehicleTypes = new String[]{"medium18t", "medium18t_electro", "heavy40t", "heavy40t_electro"};
			}
		}

		return information;
	}
}
