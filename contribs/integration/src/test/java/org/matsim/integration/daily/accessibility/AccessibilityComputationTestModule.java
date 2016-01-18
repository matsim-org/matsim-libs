package org.matsim.integration.daily.accessibility;

import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Provider;

import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.accessibility.AccessibilityConfigGroup;
import org.matsim.contrib.accessibility.GridBasedAccessibilityControlerListenerV3;
import org.matsim.contrib.accessibility.utils.AccessibilityRunUtils;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.events.ShutdownEvent;
import org.matsim.core.controler.listener.ControlerListener;
import org.matsim.core.controler.listener.ShutdownListener;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.util.TravelTime;
import org.matsim.facilities.ActivityFacilities;

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
		final AccessibilityConfigGroup accessibilityConfigGroup = ConfigUtils.addOrGetModule(getConfig(), AccessibilityConfigGroup.GROUP_NAME, AccessibilityConfigGroup.class);

		for (final String activityType : activityTypes) {
			final GeoserverUpdater geoserverUpdater = new GeoserverUpdater(crs, name);
			
			addControlerListenerBinding().toProvider(new Provider<ControlerListener>() {
				@Inject Scenario scenario;
				@Inject Map<String, TravelTime> travelTimes;
				@Inject Map<String, TravelDisutilityFactory> travelDisutilityFactories;
				@Override
				public ControlerListener get() {
					GridBasedAccessibilityControlerListenerV3 listener =
							new GridBasedAccessibilityControlerListenerV3(AccessibilityRunUtils.collectActivityFacilitiesOfType(scenario, activityType),
									null, getConfig(), scenario, travelTimes, travelDisutilityFactories);
					listener.addAdditionalFacilityData(networkDensityFacilities);
					listener.generateGridsAndMeasuringPointsByNetwork(cellSize);
					listener.writeToSubdirectoryWithName(activityType);
					// for push to geoserver
					listener.addFacilityDataExchangeListener(geoserverUpdater);
					listener.setUrbansimMode(false); // avoid writing some (eventually: all) files that related to matsim4urbansim
					return listener;
				}
			});
			
			
			System.out.println("here-10");
			addControlerListenerBinding().toInstance(new ShutdownListener() {
				@Override
				public void notifyShutdown(ShutdownEvent event) {
					System.out.println("here-20");
					geoserverUpdater.setAndProcessSpatialGrids(accessibilityConfigGroup.getIsComputingMode().keySet());
				}
			});
		}
	}

}
