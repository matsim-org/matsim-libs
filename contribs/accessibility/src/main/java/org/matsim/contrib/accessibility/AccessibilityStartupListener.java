package org.matsim.contrib.accessibility;

import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.accessibility.gis.GridUtils;
import org.matsim.contrib.accessibility.utils.AccessibilityRunUtils;
import org.matsim.contrib.accessibility.utils.GeoserverUpdater;
import org.matsim.contrib.matrixbasedptrouter.PtMatrix;
import org.matsim.core.config.Config;
import org.matsim.core.controler.ControlerListenerManager;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.util.TravelTime;
import org.matsim.facilities.ActivityFacilities;

import com.google.inject.Inject;
import com.vividsolutions.jts.geom.Envelope;

public final class AccessibilityStartupListener implements StartupListener {
	
		@Inject Scenario scenario;
		@Inject(optional = true) PtMatrix ptMatrix = null; // Downstream code knows how to handle a null PtMatrix
		@Inject Map<String, TravelTime> travelTimes;
		@Inject Map<String, TravelDisutilityFactory> travelDisutilityFactories;
		@Inject ControlerListenerManager controlerListenerManager;
		
		final List<String> activityTypes;
		final ActivityFacilities densityFacilities;
		private final String crs;
		private final String name;
		Envelope envelope;
		Double cellSize;
		
		public AccessibilityStartupListener(List<String> activityTypes, ActivityFacilities densityFacilities, String crs, String name, Envelope envelope, Double cellSize) {
			this.activityTypes = activityTypes;
			this.densityFacilities = densityFacilities;
			this.crs = crs;
			this.name = name;
			this.envelope = envelope;
			this.cellSize = cellSize;
		}
		
		@Override
		public void notifyStartup(StartupEvent arg0) {
			for (final String activityType : activityTypes) {
				Config config = scenario.getConfig();
				if (cellSize <= 0) {
					throw new RuntimeException("Cell Size needs to be assigned a value greater than zero.");
				}
				AccessibilityCalculator accessibilityCalculator = new AccessibilityCalculator(travelTimes, travelDisutilityFactories, scenario);
				accessibilityCalculator.setMeasuringPoints(GridUtils.createGridLayerByGridSizeByBoundingBoxV2(envelope.getMinX(), envelope.getMinY(), envelope.getMaxX(), envelope.getMaxY(), cellSize));
				GridBasedAccessibilityShutdownListenerV3 listener = new GridBasedAccessibilityShutdownListenerV3(accessibilityCalculator, AccessibilityRunUtils.collectActivityFacilitiesWithOptionOfType(scenario, activityType), 
						ptMatrix, config, scenario, travelTimes, travelDisutilityFactories, envelope.getMinX(), envelope.getMinY(), envelope.getMaxX(), envelope.getMaxY(), cellSize);
				listener.addAdditionalFacilityData(densityFacilities);
				listener.writeToSubdirectoryWithName(activityType);
				// for push to geoserver
//				accessibilityCalculator.addFacilityDataExchangeListener(new GeoserverUpdater(crs, name));
				listener.setUrbansimMode(false); // avoid writing some (eventually: all) files that related to matsim4urbansim
				controlerListenerManager.addControlerListener(listener);
			}
		}
	}