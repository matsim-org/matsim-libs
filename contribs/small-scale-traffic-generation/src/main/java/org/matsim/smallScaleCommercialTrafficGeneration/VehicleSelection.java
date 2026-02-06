package org.matsim.smallScaleCommercialTrafficGeneration;

import org.matsim.smallScaleCommercialTrafficGeneration.SmallScaleCommercialTrafficUtils.StructuralAttribute;

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
		List<StructuralAttribute> possibleStartCategories = new ArrayList<>();
		List<StructuralAttribute> possibleStopCategories = new ArrayList<>();
	}

	/**
	 * @return all possible stop/start-categories.
	 */
	List<StructuralAttribute> getAllCategories();

	/**
	 * @param purpose entry from {@link TripDistributionMatrix#getListOfPurposes()}
	 * @param modeORvehType entry from {@link TripDistributionMatrix#getListOfModesOrVehTypes()}
	 * @param smallScaleCommercialTrafficType Selected traffic types. Options: commercialPersonTraffic, goodsTraffic
	 * @return class holding the information that is specified by the given entry.
	 */
	OdMatrixEntryInformation getOdMatrixEntryInformation(int purpose, String modeORvehType, GenerateSmallScaleCommercialTrafficDemand.SmallScaleCommercialTrafficType smallScaleCommercialTrafficType);
}
