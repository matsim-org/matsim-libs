package org.matsim.smallScaleCommercialTrafficGeneration;

import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;

import java.util.Map;

public interface IntegrateExistingTrafficToSmallScaleCommercial {

	void readExistingCarriersFromFolder(Scenario scenario, double sampleScenario,
										Map<String, Map<Id<Link>, Link>> linksPerZone) throws Exception;

	void reduceDemandBasedOnExistingCarriers(Scenario scenario,
											 Map<String, Map<Id<Link>, Link>> linksPerZone, String smallScaleCommercialTrafficType,
											 Map<TrafficVolumeGeneration.TrafficVolumeKey, Object2DoubleMap<Integer>> trafficVolumePerTypeAndZone_start,
											 Map<TrafficVolumeGeneration.TrafficVolumeKey, Object2DoubleMap<Integer>> trafficVolumePerTypeAndZone_stop);
}
