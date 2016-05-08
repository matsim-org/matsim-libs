package org.matsim.integration.daily.accessibility;

import java.util.List;
import java.util.Map;

import javax.inject.Provider;

import com.google.inject.Inject;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.accessibility.GridBasedAccessibilityControlerListenerV3;
import org.matsim.contrib.accessibility.utils.AccessibilityRunUtils;
import org.matsim.contrib.matrixbasedptrouter.PtMatrix;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.listener.ControlerListener;
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
		for (final String activityType : activityTypes) {
			addControlerListenerBinding().toProvider(new Provider<ControlerListener>() {
				@Inject Scenario scenario;
				@Inject(optional = true) PtMatrix ptMatrix = null; // Downstream code knows how to handle a null PtMatrix
				@Inject Map<String, TravelTime> travelTimes;
				@Inject Map<String, TravelDisutilityFactory> travelDisutilityFactories;
				@Override
				public ControlerListener get() {
					GridBasedAccessibilityControlerListenerV3 listener =
							new GridBasedAccessibilityControlerListenerV3(AccessibilityRunUtils.collectActivityFacilitiesWithOptionOfType(scenario, activityType), ptMatrix, getConfig(), scenario, travelTimes, travelDisutilityFactories);
					listener.addAdditionalFacilityData(networkDensityFacilities);
					listener.generateGridsAndMeasuringPointsByNetwork(cellSize);
					listener.writeToSubdirectoryWithName(activityType);
					// for push to geoserver
					listener.addFacilityDataExchangeListener(new GeoserverUpdater(crs, name));
					listener.setUrbansimMode(false); // avoid writing some (eventually: all) files that related to matsim4urbansim
					return listener;
				}
			});
			System.out.println("here-10");
		}
	}

}
