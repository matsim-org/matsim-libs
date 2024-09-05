package org.matsim.smallScaleCommercialTrafficGeneration;

import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.facilities.ActivityFacility;
import org.matsim.smallScaleCommercialTrafficGeneration.data.GetCommercialTourSpecifications;

import java.util.List;
import java.util.Map;

/**
 * Interface to generate carriers and demand needed by {@link GenerateSmallScaleCommercialTrafficDemand}.
 * Standard implementation is {@link DefaultVehicleSelection}.
 * Any configuration settings and external data-sources should be saved as attributes during initialization in the constructor of the class.
 */
public interface VehicleSelection{

	/**
	 * Creates the carriers and the related demand.
	 * @param scenario Scenario (loaded from your config), where the carriers will be put into
	 */
	void createCarriers(Scenario scenario,
						GetCommercialTourSpecifications getCommercialTourSpecifications,
						Map<String, Map<String, List<ActivityFacility>>> facilitiesPerZone,
						int jspritIterations,
						TripDistributionMatrix odMatrix,
						String smallScaleCommercialTrafficType,
						Map<String, Object2DoubleMap<String>> resultingDataPerZone,
						Map<String, Map<Id<Link>, Link>> linksPerZone);
}
