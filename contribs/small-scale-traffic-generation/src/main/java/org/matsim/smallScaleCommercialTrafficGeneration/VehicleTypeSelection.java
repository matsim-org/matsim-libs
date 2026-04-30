package org.matsim.smallScaleCommercialTrafficGeneration;

import org.matsim.smallScaleCommercialTrafficGeneration.GenerateSmallScaleCommercialTrafficDemand.SmallScaleCommercialTrafficType;

/**
 * Strategy to determine vehicle types and occupancy rates for an OD matrix entry.
 */
public interface VehicleTypeSelection {

	/**
	 * @param purpose entry from {@link TripDistributionMatrix#getListOfPurposes()}
	 * @param modeOrVehType entry from {@link TripDistributionMatrix#getListOfModesOrVehTypes()}
	 * @param smallScaleCommercialTrafficType selected traffic type
	 * @return vehicle types and occupancy rate for the given OD matrix entry
	 */
	VehicleTypeInformation getVehicleTypeInformation(
		int purpose,
		String modeOrVehType,
		SmallScaleCommercialTrafficType smallScaleCommercialTrafficType
	);

	record VehicleTypeInformation(String[] possibleVehicleTypes, double occupancyRate) {}
}
