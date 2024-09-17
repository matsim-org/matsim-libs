package org.matsim.smallScaleCommercialTrafficGeneration;

import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.facilities.ActivityFacility;
import org.matsim.smallScaleCommercialTrafficGeneration.data.GetCommercialTourSpecifications;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Interface to set the categories needed by {@link GenerateSmallScaleCommercialTrafficDemand}.
 * Standard implementation is {@link DefaultVehicleSelection}.
 * Any configuration settings and external data-sources should be saved as attributes during initialization in the constructor of the class.
 */
public interface VehicleSelection{

	class PurposeInformation {
		double occupancyRate;
		String[] possibleVehicleTypes;
		List<String> startCategory = new ArrayList<>();
		List<String> stopCategory = new ArrayList<>();
	}

	/**
	 * @return all possible stop/start-categories.
	 */
	List<String> getAllCategories();

	/**
	 * @param purpose entry from {@link TripDistributionMatrix#getListOfPurposes()}
	 * @return class holding the information that is specified by the purpose.
	 */
	PurposeInformation getPurposeInformation(int purpose);

	/**
	 * @param modeORvehType mode- or vehicle-type from the {@link TripDistributionMatrix}
	 * @return possible CarrierVehicleTypes for given attribute
	 */
	String[] getPossibleVehicleTypes(String modeORvehType);
}
