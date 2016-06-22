package org.matsim.contrib.accessibility;

import java.util.List;
import java.util.Map;

import javax.inject.Provider;

import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.accessibility.gis.GridUtils;
import org.matsim.contrib.accessibility.utils.AccessibilityRunUtils;
import org.matsim.contrib.matrixbasedptrouter.PtMatrix;
import org.matsim.contrib.matrixbasedptrouter.utils.BoundingBox;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.ControlerListenerManager;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.ControlerListener;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.util.TravelTime;
import org.matsim.facilities.ActivityFacilities;

import com.google.inject.Inject;

public final class AccessibilityStartupListener implements StartupListener {
	
		@Inject Scenario scenario;
		@Inject(optional = true) PtMatrix ptMatrix = null; // Downstream code knows how to handle a null PtMatrix
		@Inject Map<String, TravelTime> travelTimes;
		@Inject Map<String, TravelDisutilityFactory> travelDisutilityFactories;
		@Inject ControlerListenerManager controlerListenerManager;
		
		final List<String> activityTypes;
		final ActivityFacilities networkDensityFacilities;
		private final String crs;
		private final String name;
		Double cellSize;
		
		public AccessibilityStartupListener(List<String> activityTypes, ActivityFacilities networkDensityFacilities, String crs, String name, Double cellSize) {
			this.activityTypes = activityTypes;
			this.networkDensityFacilities = networkDensityFacilities;
			this.crs = crs;
			this.name = name;
			this.cellSize = cellSize;
		}
		
		@Override
		public void notifyStartup(StartupEvent arg0) {
			for (final String activityType : activityTypes) {
				Config config = scenario.getConfig();
				if (cellSize <= 0) {
					throw new RuntimeException("Cell Size needs to be assigned a value greater than zero.");
				}
				BoundingBox bb = BoundingBox.createBoundingBox(scenario.getNetwork());
				AccessibilityCalculator accessibilityCalculator = new AccessibilityCalculator(travelTimes, travelDisutilityFactories, scenario, ConfigUtils.addOrGetModule(config, AccessibilityConfigGroup.GROUP_NAME, AccessibilityConfigGroup.class));
				accessibilityCalculator.setMeasuringPoints(GridUtils.createGridLayerByGridSizeByBoundingBoxV2(bb.getXMin(), bb.getYMin(), bb.getXMax(), bb.getYMax(), cellSize));
				GridBasedAccessibilityShutdownListenerV3 listener = new GridBasedAccessibilityShutdownListenerV3(accessibilityCalculator, AccessibilityRunUtils.collectActivityFacilitiesWithOptionOfType(scenario, activityType), ptMatrix, config, scenario, travelTimes, travelDisutilityFactories,bb.getXMin(), bb.getYMin(), bb.getXMax(), bb.getYMax(), cellSize);
				listener.addAdditionalFacilityData(networkDensityFacilities);
				listener.writeToSubdirectoryWithName(activityType);
				// for push to geoserver
//							listener.addFacilityDataExchangeListener(new GeoserverUpdater(crs, name));
				listener.setUrbansimMode(false); // avoid writing some (eventually: all) files that related to matsim4urbansim
				controlerListenerManager.addControlerListener(listener);
			}
		}
	}