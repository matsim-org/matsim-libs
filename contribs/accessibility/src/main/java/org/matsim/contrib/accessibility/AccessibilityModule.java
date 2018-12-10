/* *********************************************************************** *
 * project: org.matsim.*                                                   *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

package org.matsim.contrib.accessibility;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import javax.inject.Provider;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.accessibility.AccessibilityConfigGroup.AreaOfAccesssibilityComputation;
import org.matsim.contrib.accessibility.gis.GridUtils;
import org.matsim.contrib.accessibility.interfaces.FacilityDataExchangeInterface;
import org.matsim.contrib.accessibility.utils.AccessibilityUtils;
import org.matsim.contrib.accessibility.utils.GeoserverUpdater;
import org.matsim.contrib.matrixbasedptrouter.PtMatrix;
import org.matsim.contrib.matrixbasedptrouter.utils.BoundingBox;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.listener.ControlerListener;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.algorithms.TransportModeNetworkFilter;
import org.matsim.core.router.TripRouter;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.trafficmonitoring.FreeSpeedTravelTime;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.ActivityFacilitiesImpl;
import org.matsim.facilities.ActivityFacility;
import org.matsim.facilities.MatsimFacilitiesReader;

import com.google.inject.Inject;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;

/**
 * @author dziemke
 */
public final class AccessibilityModule extends AbstractModule {
	private static final Logger LOG = Logger.getLogger(AccessibilityModule.class);

	private List<FacilityDataExchangeInterface> facilityDataListeners = new ArrayList<>() ; 
	private ActivityFacilities measuringPoints;
	private Map<String, ActivityFacilities> additionalFacs = new TreeMap<>() ;
	private String activityType;
	private boolean pushing2Geoserver;
	private String crs;

	@Override
	public void install() {
		addControlerListenerBinding().toProvider(new Provider<ControlerListener>() {
			// yy not sure if this truly needs to be a provider.  kai, dec'16
			
			@Inject private Config config ;
			@Inject private Network network ;
			@Inject private Scenario scenario;

			@Inject (optional = true) PtMatrix ptMatrix = null; // Downstream code knows how to handle a null PtMatrix
			@Inject private Map<String,TravelDisutilityFactory> travelDisutilityFactories ;
			@Inject private Map<String,TravelTime> travelTimes ;
			
			@Inject TripRouter tripRouter ;
			
			@Override
			public ControlerListener get() {
				AccessibilityConfigGroup acg = ConfigUtils.addOrGetModule(scenario.getConfig(), AccessibilityConfigGroup.class);
				crs = acg.getOutputCrs() ;
				
				ActivityFacilities opportunities = AccessibilityUtils.collectActivityFacilitiesWithOptionOfType(scenario, activityType);
				Map<Id<ActivityFacility>, Geometry> measurePointGeometryMap = null;
				final BoundingBox boundingBox;
				
				if (acg.getAreaOfAccessibilityComputation() == AreaOfAccesssibilityComputation.fromShapeFile) {
					int tileSize_m = acg.getTileSize();
					if (tileSize_m <= 0) {
						LOG.error("Tile Size needs to be assigned a value greater than zero.");
					}
					Geometry boundary = GridUtils.getBoundary(acg.getShapeFileCellBasedAccessibility());
					Envelope envelope = boundary.getEnvelopeInternal();
					boundingBox = BoundingBox.createBoundingBox(envelope.getMinX(), envelope.getMinY(), envelope.getMaxX(), envelope.getMaxY());
					measuringPoints = GridUtils.createGridLayerByGridSizeByShapeFileV2(boundary, tileSize_m);
					measurePointGeometryMap = VoronoiGeometryUtils.buildMeasurePointGeometryMap(measuringPoints, boundingBox, tileSize_m);
					LOG.info("Using shape file to determine the area for accessibility computation.");
					
				} else if (acg.getAreaOfAccessibilityComputation() == AreaOfAccesssibilityComputation.fromBoundingBox) {
					int tileSize_m = acg.getTileSize();
					if (tileSize_m <= 0) {
						LOG.error("Tile Size needs to be assigned a value greater than zero.");
					}
					boundingBox = BoundingBox.createBoundingBox(acg.getBoundingBoxLeft(), acg.getBoundingBoxBottom(), acg.getBoundingBoxRight(), acg.getBoundingBoxTop());
					measuringPoints = GridUtils.createGridLayerByGridSizeByBoundingBoxV2(boundingBox, tileSize_m);
					measurePointGeometryMap = VoronoiGeometryUtils.buildMeasurePointGeometryMap(measuringPoints, boundingBox, tileSize_m);
					LOG.info("Using custom bounding box to determine the area for accessibility computation.");
					
				} else if (acg.getAreaOfAccessibilityComputation() == AreaOfAccesssibilityComputation.fromBoundingBoxHexagons) {
					int tileSize_m = acg.getTileSize();
					if (tileSize_m <= 0) {
						LOG.error("Tile Size needs to be assigned a value greater than zero.");
					}
					boundingBox = BoundingBox.createBoundingBox(acg.getBoundingBoxLeft(), acg.getBoundingBoxBottom(), acg.getBoundingBoxRight(), acg.getBoundingBoxTop());
					measuringPoints = GridUtils.createHexagonLayer(boundingBox, tileSize_m);
					measurePointGeometryMap = VoronoiGeometryUtils.buildMeasurePointGeometryMap(measuringPoints, boundingBox, tileSize_m);
					LOG.info("Using custom bounding box to determine the area for accessibility computation.");
					
				} else if (acg.getAreaOfAccessibilityComputation() == AreaOfAccesssibilityComputation.fromFacilitiesFile) {
					boundingBox = BoundingBox.createBoundingBox(acg.getBoundingBoxLeft(), acg.getBoundingBoxBottom(), acg.getBoundingBoxRight(), acg.getBoundingBoxTop());
					Scenario measuringPointsSc = ScenarioUtils.createScenario(ConfigUtils.createConfig());
					String measuringPointsFile = ConfigUtils.addOrGetModule(config, AccessibilityConfigGroup.class ).getMeasuringPointsFile();
					new MatsimFacilitiesReader(measuringPointsSc).readFile(measuringPointsFile);
					measuringPoints = (ActivityFacilitiesImpl) AccessibilityUtils.collectActivityFacilitiesWithOptionOfType(measuringPointsSc, null);
//					measurePointGeometryMap
					LOG.info("Using measuring points from file: " + measuringPointsFile);
					
				} else if (acg.getAreaOfAccessibilityComputation() == AreaOfAccesssibilityComputation.fromFacilitiesObject) {
					boundingBox = BoundingBox.createBoundingBox(acg.getBoundingBoxLeft(), acg.getBoundingBoxBottom(), acg.getBoundingBoxRight(), acg.getBoundingBoxTop());
					measuringPoints = ConfigUtils.addOrGetModule(config, AccessibilityConfigGroup.class ).getMeasuringPointsFacilities();
//					measurePointGeometryMap
					LOG.warn("Number of measuringPoints = " +  measuringPoints.getFacilities().size());
					if (measuringPoints == null) {
						throw new RuntimeException("Measuring points should have been set direclty if from-facilities-object mode is used.");
					}
					LOG.info("Using measuring points from facilities object.");
					
				} else { // This covers also the "fromNetwork" case
					LOG.info("Using the boundary of the network file to determine the area for accessibility computation.");
					LOG.warn("This can lead to memory issues when the network is large and/or the cell size is too fine!");

					int tileSize_m = acg.getTileSize();
					if (tileSize_m <= 0) {
						LOG.error("Tile Size needs to be assigned a value greater than zero.");
					}
					boundingBox = BoundingBox.createBoundingBox(scenario.getNetwork());
					measuringPoints = GridUtils.createGridLayerByGridSizeByBoundingBoxV2(boundingBox, tileSize_m);
					measurePointGeometryMap = VoronoiGeometryUtils.buildMeasurePointGeometryMap(measuringPoints, boundingBox, tileSize_m);
					LOG.info("Using the boundary of the network file to determine the area for accessibility computation.");
					LOG.warn("This can lead to memory issues when the network is large and/or the cell size is too fine!");
				}
				
				AccessibilityUtils.assignAdditionalFacilitiesDataToMeasurePoint(measuringPoints, measurePointGeometryMap, additionalFacs);
				
				// TODO Need to find a stable way for multi-modal networks
				// AV stuff -------------------------------------------------------------
				TransportModeNetworkFilter filter = new TransportModeNetworkFilter(scenario.getNetwork());
				LOG.warn("Full network has " + network.getNodes().size() + " nodes.");
				Network carNetwork = NetworkUtils.createNetwork();
				Set<String> modeSet = new HashSet<>();
				modeSet.add("car");
				filter.filter(carNetwork, modeSet);
				LOG.warn("Pure car network now has " + carNetwork.getNodes().size() + " nodes.");
				// End AV stuff -------------------------------------------------------------
				
				AccessibilityCalculator accessibilityCalculator = new AccessibilityCalculator(scenario, measuringPoints, network);
//				AccessibilityCalculator accessibilityCalculator = new AccessibilityCalculator(scenario, measuringPoints, carNetwork);
				for (Modes4Accessibility mode : acg.getIsComputingMode()) {
					AccessibilityContributionCalculator calculator = null ;
					switch(mode) {
					case bike:
						calculator = new ConstantSpeedAccessibilityExpContributionCalculator(mode.name(), config, network);
						break;
					case car: {
						final TravelTime travelTime = travelTimes.get(mode.name());
						Gbl.assertNotNull(travelTime);
						final TravelDisutilityFactory travelDisutilityFactory = travelDisutilityFactories.get(mode.name());
						calculator = new NetworkModeAccessibilityExpContributionCalculator(travelTime, travelDisutilityFactory, scenario, network);
//						calculator = new NetworkModeAccessibilityExpContributionCalculator(travelTime, travelDisutilityFactory, scenario, carNetwork);
						break; }
					case freespeed: {
						final TravelDisutilityFactory travelDisutilityFactory = travelDisutilityFactories.get(TransportMode.car);
						Gbl.assertNotNull(travelDisutilityFactory);
						calculator = new NetworkModeAccessibilityExpContributionCalculator(new FreeSpeedTravelTime(), travelDisutilityFactory, scenario, network);
//						calculator = new NetworkModeAccessibilityExpContributionCalculator(new FreeSpeedTravelTime(), travelDisutilityFactory, scenario, carNetwork);
						break; }
					case walk:
						calculator = new ConstantSpeedAccessibilityExpContributionCalculator(mode.name(), config, network);
						break;
					case matrixBasedPt:
						calculator = new LeastCostPathCalculatorAccessibilityContributionCalculator(
								config.planCalcScore(),	ptMatrix.asPathCalculator(config.planCalcScore()));
						break;
						//$CASES-OMITTED$
					default:
//						TravelTime timeCalculator = this.travelTimes.get( mode.toString() ) ;
//						TravelDisutility travelDisutility = this.travelDisutilityFactories.get(mode.toString()).createTravelDisutility(timeCalculator) ;
						calculator = new TripRouterAccessibilityContributionCalculator(mode.toString(), tripRouter, config.planCalcScore());
					}
					accessibilityCalculator.putAccessibilityContributionCalculator(mode.name(), calculator);
				}
				
				String outputDirectory = scenario.getConfig().controler().getOutputDirectory() + "/" + activityType;
				
				if (pushing2Geoserver == true) {
					if (measurePointGeometryMap == null) {
						throw new IllegalArgumentException("measure-point-to-geometry map must not be null if push to Geoserver is intended.");
					}
					Set <String> additionalFacInfo = additionalFacs.keySet();
					accessibilityCalculator.addFacilityDataExchangeListener(new GeoserverUpdater(crs,
							config.controler().getRunId() + "_" + activityType, measurePointGeometryMap, additionalFacInfo, outputDirectory));
				}
				

				AccessibilityShutdownListenerV4 accessibilityShutdownListener = new AccessibilityShutdownListenerV4(accessibilityCalculator, 
						opportunities, outputDirectory, acg);
				
				for (FacilityDataExchangeInterface listener : facilityDataListeners) {
					accessibilityShutdownListener.addFacilityDataExchangeListener(listener);
				}
				
				return accessibilityShutdownListener;
			}
		});
	}
	
	public final void setPushing2Geoserver( boolean pushing2Geoserver ) {
		this.pushing2Geoserver = pushing2Geoserver ;
	}
	
	public final void addFacilityDataExchangeListener(FacilityDataExchangeInterface listener) {
		this.facilityDataListeners.add(listener) ;
	}

	/**
	 * Add additional facility data that will generate an additional column for each (x,y,t)-Entry. The facilities are aggregated to
	 * the measurement points in downstream code.
	 */
	public void addAdditionalFacilityData(ActivityFacilities facilities) { // TDO cleanu up this method
		if (facilities.getName() == null || facilities.getName().equals("")) {
			throw new RuntimeException("Cannot add unnamed facility containers here. A key is required to identify them.") ;
		}
		for (ActivityFacilities existingFacilities : this.additionalFacs.values()) {
			if (existingFacilities.getName().equals(facilities.getName())) {
				throw new RuntimeException("Additional facilities under the name of + " + facilities.getName() + 
						" already exist. Cannot add additional facilities under the same name twice.") ;
			}
		}
		this.additionalFacs.put(facilities.getName(), facilities);
	}
	
	public void setConsideredActivityType(String activityType) {
		this.activityType = activityType ;
	}
}