package org.matsim.smallScaleCommercialTrafficGeneration;

import org.apache.commons.math3.distribution.EnumeratedDistribution;
import org.matsim.smallScaleCommercialTrafficGeneration.SmallScaleCommercialTrafficUtils.ZoneAttribute;

import java.util.List;
/**
 * Provides the OD matrix entry information needed by {@link GenerateSmallScaleCommercialTrafficDemand}.
 * Standard implementation is {@link DefaultOdMatrixEntryInformationProvider}.
 * Any configuration settings and external data-sources should be saved as attributes during initialization in the constructor of the class.
 */
public interface OdMatrixEntryInformationProvider{

	class OdMatrixEntryInformation {
		double occupancyRate;
		String[] possibleVehicleTypes;
		EnumeratedDistribution<ZoneAttribute> startCategoryDistribution;
		EnumeratedDistribution<ZoneAttribute> stopCategoryDistribution;
	}

	/**
	 * @return all possible stop/start-categories.
	 */
	List<ZoneAttribute> getAllCategories();

	/**
	 * @param purpose entry from {@link TripDistributionMatrix#getListOfPurposes()}
	 * @param modeORvehType entry from {@link TripDistributionMatrix#getListOfModesOrVehTypes()}
	 * @param smallScaleCommercialTrafficSegment Selected traffic types. Options: commercialPersonTraffic, goodsTraffic
	 * @return class holding the information that is specified by the given entry.
	 */
	OdMatrixEntryInformation getOdMatrixEntryInformation(int purpose, String modeORvehType, GenerateSmallScaleCommercialTrafficDemand.SmallScaleCommercialTrafficSegment smallScaleCommercialTrafficSegment );
}
