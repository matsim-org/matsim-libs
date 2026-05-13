package org.matsim.smallScaleCommercialTrafficGeneration;

import org.junit.jupiter.api.Test;
import org.matsim.smallScaleCommercialTrafficGeneration.GenerateSmallScaleCommercialTrafficDemand.SmallScaleCommercialTrafficType;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class DefaultOdMatrixEntryInformationProviderTest {

	@Test
	void usesConfiguredVehicleTypeSelection() {
		VehicleTypeSelection vehicleTypeSelection = (purpose, modeOrVehType, smallScaleCommercialTrafficType) ->
			new VehicleTypeSelection.VehicleTypeInformation(new String[]{"customVehicle"}, 2.5);

		DefaultOdMatrixEntryInformationProvider provider = new DefaultOdMatrixEntryInformationProvider(vehicleTypeSelection);

		OdMatrixEntryInformationProvider.OdMatrixEntryInformation information = provider.getOdMatrixEntryInformation(
			1, "total", SmallScaleCommercialTrafficType.commercialPersonTraffic);

		assertArrayEquals(new String[]{"customVehicle"}, information.possibleVehicleTypes);
		assertEquals(2.5, information.occupancyRate);
		assertNotNull(information.startCategoryDistribution);
		assertNotNull(information.stopCategoryDistribution);
	}
}
