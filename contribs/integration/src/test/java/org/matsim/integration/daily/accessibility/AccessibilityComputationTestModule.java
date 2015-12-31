package org.matsim.integration.daily.accessibility;

import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.accessibility.FacilityTypes;
import org.matsim.contrib.accessibility.GridBasedAccessibilityControlerListenerV3;
import org.matsim.contrib.accessibility.Modes4Accessibility;
import org.matsim.contrib.accessibility.utils.AccessibilityRunUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.listener.ControlerListener;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.util.TravelTime;
import org.matsim.facilities.ActivityFacilities;

import javax.inject.Inject;
import javax.inject.Provider;
import java.util.List;
import java.util.Map;

class AccessibilityComputationTestModule extends AbstractModule {
	private final List<String> activityTypes;
	private final ActivityFacilities networkDensityFacilities;
	private final String crs;
	private final String name;
	private Double cellSize;

	public AccessibilityComputationTestModule(List<String> activityTypes, ActivityFacilities networkDensityFacilities, String crs, String name, Double cellSize) {
		this.activityTypes = activityTypes;
		this.networkDensityFacilities = networkDensityFacilities;
		this.crs = crs;
		this.name = name;
		this.cellSize = cellSize;
	}

	@Override
	public void install() {
		// loop over activity types to add one GridBasedAccessibilityControlerListenerV3 for each combination
		for (final String actType : activityTypes) {
//			if ( !actType.equals("w") ) {
			if (!actType.equals(FacilityTypes.WORK)) {
				AccessibilityComputationNairobiTest.log.error("skipping everything except work for debugging purposes; remove in production code. kai, feb'14");
				continue;
			}

			addControlerListenerBinding().toProvider(new Provider<ControlerListener>() {
				@Inject Scenario scenario;
				@Inject Map<String, TravelTime> travelTimes;
				@Inject Map<String, TravelDisutilityFactory> travelDisutilityFactories;
				@Override
				public ControlerListener get() {
					GridBasedAccessibilityControlerListenerV3 listener =
							new GridBasedAccessibilityControlerListenerV3(AccessibilityRunUtils.collectActivityFacilitiesOfType(scenario, actType), null, getConfig(), scenario, travelTimes, travelDisutilityFactories);
					listener.setComputingAccessibilityForMode(Modes4Accessibility.freeSpeed, true);
					listener.setComputingAccessibilityForMode(Modes4Accessibility.car, true);
					listener.setComputingAccessibilityForMode(Modes4Accessibility.walk, true);
					listener.setComputingAccessibilityForMode(Modes4Accessibility.bike, true);
//			listener.setComputingAccessibilityForMode(Modes4Accessibility.pt, true);

					listener.addAdditionalFacilityData(networkDensityFacilities);
					listener.generateGridsAndMeasuringPointsByNetwork(cellSize);


					listener.writeToSubdirectoryWithName(actType);

					// for push to geoserver
					listener.addFacilityDataExchangeListener(new GeoserverUpdater(crs, name));

					listener.setUrbansimMode(false); // avoid writing some (eventually: all) files that related to matsim4urbansim
					return listener;
				}
			});
		}

	}
}
