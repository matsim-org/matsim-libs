package org.matsim.integration.daily.accessibility;

import java.util.List;
import java.util.Map;

import javax.inject.Provider;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.name.Names;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.contrib.accessibility.GridBasedAccessibilityControlerListenerV3;
import org.matsim.contrib.accessibility.utils.AccessibilityRunUtils;
import org.matsim.contrib.matrixbasedptrouter.PtMatrix;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.listener.ControlerListener;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.trafficmonitoring.TravelTimeCalculator;
import org.matsim.facilities.ActivityFacilities;

class AccessibilityComputationTestModule extends AbstractModule {
	private final List<String> activityTypes;
	private final ActivityFacilities densityInformationFacilities;
	private final String crs;
	private final String name;
	private Double cellSize;

	public AccessibilityComputationTestModule(List<String> activityTypes,
			ActivityFacilities densityInformationFacilities, String crs, String name, Double cellSize) {
		this.activityTypes = activityTypes;
		this.densityInformationFacilities = densityInformationFacilities;
		this.crs = crs;
		this.name = name;
		this.cellSize = cellSize;
	}


	@Override
	public void install() {
//		this.addTravelTimeBinding(TransportMode.car).toProvider(new Provider<TravelTime>() {
//            @Inject Injector injector;
//            @Override
//            public TravelTime get() {
//                return injector.getInstance(Key.get(TravelTimeCalculator.class, Names.named(TransportMode.car))).getLinkTravelTimes();
//            }
//        });
		for (final String activityType : activityTypes) {
			addControlerListenerBinding().toProvider(new Provider<ControlerListener>() {
				@Inject Scenario scenario;
				@Inject (optional = true) PtMatrix ptMatrix = null; // Downstream code knows how to handle a null PtMatrix
				@Inject Map<String, TravelTime> travelTimes;
				@Inject Map<String, TravelDisutilityFactory> travelDisutilityFactories;
				@Override
				public ControlerListener get() {
//					System.err.println("travelTimes = " + travelTimes);
					GridBasedAccessibilityControlerListenerV3 listener =
							new GridBasedAccessibilityControlerListenerV3(
									AccessibilityRunUtils.collectActivityFacilitiesOfType(scenario, activityType),
									ptMatrix, getConfig(), scenario, travelTimes, travelDisutilityFactories);
					listener.addAdditionalFacilityData(densityInformationFacilities);
					listener.generateGridsAndMeasuringPointsByNetwork(cellSize);
					listener.writeToSubdirectoryWithName(activityType);
					// for push to geoserver
					listener.addFacilityDataExchangeListener(new GeoserverUpdater(crs, name));
					listener.setUrbansimMode(false); // avoid writing some (eventually: all) files that related to matsim4urbansim
					return listener;
				}
			});
		}
	}
}