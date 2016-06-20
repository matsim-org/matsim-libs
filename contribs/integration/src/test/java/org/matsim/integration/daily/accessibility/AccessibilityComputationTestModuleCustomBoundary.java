package org.matsim.integration.daily.accessibility;

import java.util.List;
import java.util.Map;

import javax.inject.Provider;

import com.google.inject.Inject;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.accessibility.AccessibilityCalculator;
import org.matsim.contrib.accessibility.AccessibilityConfigGroup;
import org.matsim.contrib.accessibility.GridBasedAccessibilityShutdownListenerV3;
import org.matsim.contrib.accessibility.gis.GridUtils;
import org.matsim.contrib.accessibility.utils.AccessibilityRunUtils;
import org.matsim.contrib.accessibility.utils.GeoserverUpdater;
import org.matsim.contrib.matrixbasedptrouter.PtMatrix;
import org.matsim.contrib.matrixbasedptrouter.utils.BoundingBox;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.listener.ControlerListener;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.util.TravelTime;
import org.matsim.facilities.ActivityFacilities;

class AccessibilityComputationTestModuleCustomBoundary extends AbstractModule {
	private final List<String> activityTypes;
	private final ActivityFacilities networkDensityFacilities;
	private final String crs;
	private final String name;
	private Double cellSize;
	private BoundingBox boundingBox;

	public AccessibilityComputationTestModuleCustomBoundary(List<String> activityTypes,
			ActivityFacilities networkDensityFacilities, String crs, String name, Double cellSize,
			BoundingBox boundingBox) {
		this.activityTypes = activityTypes;
		this.networkDensityFacilities = networkDensityFacilities;
		this.crs = crs;
		this.name = name;
		this.cellSize = cellSize;
		this.boundingBox = boundingBox;
	}


	@Override
	public void install() {
		for (final String activityType : activityTypes) {
			addControlerListenerBinding().toProvider(new Provider<ControlerListener>() {
				@Inject Scenario scenario;
				@Inject(optional = true) PtMatrix ptMatrix = null; // Downstream code knows how to handle a null PtMatrix
				@Inject Map<String, TravelTime> travelTimes;
				@Inject Map<String, TravelDisutilityFactory> travelDisutilityFactories;
				@Override
				public ControlerListener get() {
					AccessibilityCalculator accessibilityCalculator = new AccessibilityCalculator(travelTimes, travelDisutilityFactories, scenario, ConfigUtils.addOrGetModule(getConfig(), AccessibilityConfigGroup.GROUP_NAME, AccessibilityConfigGroup.class));
					accessibilityCalculator.setMeasuringPoints(GridUtils.createGridLayerByGridSizeByBoundingBoxV2(boundingBox.getXMin(), boundingBox.getYMin(), boundingBox.getXMax(), boundingBox.getYMax(), cellSize));
					GridBasedAccessibilityShutdownListenerV3 listener = new GridBasedAccessibilityShutdownListenerV3(accessibilityCalculator, AccessibilityRunUtils.collectActivityFacilitiesWithOptionOfType(scenario, activityType), ptMatrix, getConfig(), scenario, travelTimes, travelDisutilityFactories, boundingBox.getXMin(), boundingBox.getYMin(), boundingBox.getXMax(), boundingBox.getYMax(), cellSize);
					listener.addAdditionalFacilityData(networkDensityFacilities);
					listener.writeToSubdirectoryWithName(activityType);
					// for push to geoserver
					accessibilityCalculator.addFacilityDataExchangeListener(new GeoserverUpdater(crs, name));
					listener.setUrbansimMode(false); // avoid writing some (eventually: all) files that related to matsim4urbansim
					return listener;
				}
			});
		}
	}
}