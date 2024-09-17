package org.matsim.smallScaleCommercialTrafficGeneration;


import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

public class DefaultVehicleSelection implements VehicleSelection{
	private static final Logger log = LogManager.getLogger(GenerateSmallScaleCommercialTrafficDemand.class);

	@Override
	public List<String> getAllCategories() {
		return null;
	}

	@Override
	public PurposeInformation getPurposeInformation(int purpose) {
		return null;
	}

	@Override
	public String[] getPossibleVehicleTypes(String modeORvehType) {
		return new String[0];
	}
}
