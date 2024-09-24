package org.matsim.smallScaleCommercialTrafficGeneration;

import java.util.ArrayList;
import java.util.List;
/**
 * Interface to set the categories needed by {@link GenerateSmallScaleCommercialTrafficDemand}.
 * Standard implementation is {@link DefaultVehicleSelection}.
 * Any configuration settings and external data-sources should be saved as attributes during initialization in the constructor of the class.
 */
public interface VehicleSelection{

	class OdMatrixEntryInformation {
		double occupancyRate;
		String[] possibleVehicleTypes;
		List<String> possibleStartCategories = new ArrayList<>();
		List<String> possibleStopCategories = new ArrayList<>();
	}

	/**
	 * @return all possible stop/start-categories.
	 */
	List<String> getAllCategories();

	/**
	 * @param purpose entry from {@link TripDistributionMatrix#getListOfPurposes()}
	 * @param modeORvehType entry from {@link TripDistributionMatrix#getListOfModesOrVehTypes()}
	 * @param smallScaleCommercialTrafficType Selected traffic types. Options: commercialPersonTraffic, goodsTraffic
	 * @return class holding the information that is specified by the given entry.
	 */
	OdMatrixEntryInformation getOdMatrixEntryInformation(int purpose, String modeORvehType, String smallScaleCommercialTrafficType);
}
