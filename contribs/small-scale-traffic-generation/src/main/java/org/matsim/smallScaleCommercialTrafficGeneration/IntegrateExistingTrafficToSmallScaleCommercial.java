package org.matsim.smallScaleCommercialTrafficGeneration;

import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import org.matsim.api.core.v01.Scenario;
import org.matsim.application.options.ShpOptions;

import java.util.Map;

public interface IntegrateExistingTrafficToSmallScaleCommercial {

	void readExistingCarriersFromFolder(Scenario scenario, double sampleScenario,
										ShpOptions.Index indexZones) throws Exception;

	void reduceDemandBasedOnExistingCarriers(Scenario scenario,
											 ShpOptions.Index linksPerZone, GenerateSmallScaleCommercialTrafficDemand.SmallScaleCommercialTrafficType smallScaleCommercialTrafficType,
											 Map<TrafficVolumeGeneration.TrafficVolumeKey, Object2DoubleMap<Integer>> trafficVolumePerTypeAndZone_start,
											 Map<TrafficVolumeGeneration.TrafficVolumeKey, Object2DoubleMap<Integer>> trafficVolumePerTypeAndZone_stop);
}
