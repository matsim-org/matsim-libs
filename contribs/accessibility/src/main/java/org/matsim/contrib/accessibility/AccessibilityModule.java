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

import com.google.inject.Inject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.accessibility.AccessibilityConfigGroup.AreaOfAccesssibilityComputation;
import org.matsim.contrib.accessibility.AccessibilityConfigGroup.MeasurePointGeometryProvision;
import org.matsim.contrib.accessibility.utils.GeoserverUpdater;
import org.matsim.contrib.matrixbasedptrouter.utils.BoundingBox;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.listener.ControlerListener;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.router.TripRouter;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.trafficmonitoring.FreeSpeedTravelTime;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.ActivityFacilitiesImpl;
import org.matsim.facilities.ActivityFacility;
import org.matsim.facilities.MatsimFacilitiesReader;

import javax.inject.Provider;
import java.util.*;

/**
 * @author dziemke
 */
public final class AccessibilityModule extends AbstractModule {
	private static final Logger LOG = LogManager.getLogger(AccessibilityModule.class);

	private List<FacilityDataExchangeInterface> facilityDataListeners = new ArrayList<>() ; 
	private ActivityFacilities measuringPoints;
	private Map<String, ActivityFacilities> additionalFacs = new TreeMap<>() ;
	private String activityType;
	private boolean pushing2Geoserver = false;
	private boolean createQGisOutput = false;

	@Override
	public void install() {
		addControlerListenerBinding().toProvider(new Provider<ControlerListener>() {
			// yy not sure if this truly needs to be a provider.  kai, dec'16
			
			@Inject private Config config ;
			@Inject private Network network ;
			@Inject private Scenario scenario;

//			@Inject (optional = true) PtMatrix ptMatrix = null; // Downstream code knows how to handle a null PtMatrix
			// not consistent with guice-grapher, but also a result of garbled design: There should rather be a routing-mode that uses PtMatrix.
			// Commenting it out for the time being.  kai, sep'19

			@Inject private Map<String,TravelDisutilityFactory> travelDisutilityFactories ;
			@Inject private Map<String,TravelTime> travelTimes ;
			
			@Inject TripRouter tripRouter ;
			
			@Override
			public ControlerListener get() {
				AccessibilityConfigGroup acg = ConfigUtils.addOrGetModule(scenario.getConfig(), AccessibilityConfigGroup.class);
				ActivityFacilities opportunities = AccessibilityUtils.collectActivityFacilitiesWithOptionOfType(scenario, activityType);
				final BoundingBox boundingBox;
				
				int tileSize_m = acg.getTileSize();
				if (tileSize_m <= 0) { LOG.error("Tile Size must be assigned a value greater than zero."); }
				
				if (acg.getAreaOfAccessibilityComputation() == AreaOfAccesssibilityComputation.fromShapeFile) {
					Geometry boundary = GridUtils.getBoundary(acg.getShapeFileCellBasedAccessibility());
					Envelope envelope = boundary.getEnvelopeInternal();
					boundingBox = BoundingBox.createBoundingBox(envelope.getMinX(), envelope.getMinY(), envelope.getMaxX(), envelope.getMaxY());
					measuringPoints = GridUtils.createGridLayerByGridSizeByShapeFileV2(boundary, tileSize_m);
					LOG.info("Using shape file to determine the area for accessibility computation.");
					
				} else if (acg.getAreaOfAccessibilityComputation() == AreaOfAccesssibilityComputation.fromBoundingBox) {
					boundingBox = BoundingBox.createBoundingBox(acg.getBoundingBoxLeft(), acg.getBoundingBoxBottom(), acg.getBoundingBoxRight(), acg.getBoundingBoxTop());
					measuringPoints = GridUtils.createGridLayerByGridSizeByBoundingBoxV2(boundingBox, tileSize_m);
					LOG.info("Using custom bounding box to determine the area for accessibility computation, which is resolved in squares.");
					
				} else if (acg.getAreaOfAccessibilityComputation() == AreaOfAccesssibilityComputation.fromBoundingBoxHexagons) {
					boundingBox = BoundingBox.createBoundingBox(acg.getBoundingBoxLeft(), acg.getBoundingBoxBottom(), acg.getBoundingBoxRight(), acg.getBoundingBoxTop());
					measuringPoints = GridUtils.createHexagonLayer(boundingBox, tileSize_m);
					LOG.info("Using custom bounding box to determine the area for accessibility computation, whichs is resolved in hexagons.");
					
				} else if (acg.getAreaOfAccessibilityComputation() == AreaOfAccesssibilityComputation.fromFacilitiesFile) {
					boundingBox = BoundingBox.createBoundingBox(acg.getBoundingBoxLeft(), acg.getBoundingBoxBottom(), acg.getBoundingBoxRight(), acg.getBoundingBoxTop());
					Scenario measuringPointsSc = ScenarioUtils.createScenario(ConfigUtils.createConfig());
					String measuringPointsFile = ConfigUtils.addOrGetModule(config, AccessibilityConfigGroup.class ).getMeasuringPointsFile();
					new MatsimFacilitiesReader(measuringPointsSc).readFile(measuringPointsFile);
					measuringPoints = (ActivityFacilitiesImpl) AccessibilityUtils.collectActivityFacilitiesWithOptionOfType(measuringPointsSc, null);
					LOG.info("Using measuring points from file: " + measuringPointsFile);
					
				} else if (acg.getAreaOfAccessibilityComputation() == AreaOfAccesssibilityComputation.fromFacilitiesObject) {
					boundingBox = BoundingBox.createBoundingBox(acg.getBoundingBoxLeft(), acg.getBoundingBoxBottom(), acg.getBoundingBoxRight(), acg.getBoundingBoxTop());
					measuringPoints = ConfigUtils.addOrGetModule(config, AccessibilityConfigGroup.class ).getMeasuringPointsFacilities();
					LOG.warn("Number of measuringPoints = " +  measuringPoints.getFacilities().size());
					if (measuringPoints == null) {
						throw new RuntimeException("Measuring points should have been set direclty if from-facilities-object mode is used.");
					}
					LOG.info("Using measuring points from facilities object.");
					
				} else { // This covers also the "fromNetwork" case
					LOG.info("Using the boundary of the network file to determine the area for accessibility computation.");
					LOG.warn("This can lead to memory issues when the network is large and/or the cell size is too fine!");
					boundingBox = BoundingBox.createBoundingBox(scenario.getNetwork());
					measuringPoints = GridUtils.createGridLayerByGridSizeByBoundingBoxV2(boundingBox, tileSize_m);
				}

				Map<Id<ActivityFacility>, Geometry> measurePointGeometryMap;
				if (acg.getMeasurePointGeometryProvision() == MeasurePointGeometryProvision.fromShapeFile) {
					measurePointGeometryMap = acg.getMeasurePointGeometryMap();
				} else {
					measurePointGeometryMap = VoronoiGeometryUtils.buildMeasurePointGeometryMap(measuringPoints, boundingBox, tileSize_m);
				}
				AccessibilityUtils.assignAdditionalFacilitiesDataToMeasurePoint(measuringPoints, measurePointGeometryMap, additionalFacs);
				
				String outputDirectory = scenario.getConfig().controler().getOutputDirectory() + "/" + activityType;
				AccessibilityComputationShutdownListener accessibilityShutdownListener = new AccessibilityComputationShutdownListener(scenario, measuringPoints, opportunities, outputDirectory);

//				for (Modes4Accessibility mode : acg.getIsComputingMode()) {
				for( String mode : acg.getModes() ){
					AccessibilityContributionCalculator calculator = null;
					if ( Modes4Accessibility.freespeed.name().equals( mode  ) ) {
						// freespeed car, special case
						final TravelDisutilityFactory travelDisutilityFactory = travelDisutilityFactories.get(TransportMode.car);
						Gbl.assertNotNull(travelDisutilityFactory);
						calculator = new NetworkModeAccessibilityExpContributionCalculator(mode, new FreeSpeedTravelTime(), travelDisutilityFactory, scenario);
					} else if ( config.plansCalcRoute().getNetworkModes().contains( mode ) ) {
						final TravelTime nwModeTravelTime = travelTimes.get(mode);
						Gbl.assertNotNull(nwModeTravelTime);
						final TravelDisutilityFactory nwModeTravelDisutility = travelDisutilityFactories.get(mode);
						Gbl.assertNotNull( nwModeTravelDisutility );
						calculator = new NetworkModeAccessibilityExpContributionCalculator(mode, nwModeTravelTime, nwModeTravelDisutility, scenario);
					} else if ( TransportMode.pt.equals( mode ) ){
						calculator = new SwissRailRaptorAccessibilityContributionCalculator( mode, config.planCalcScore(), scenario );
					} else if ( Modes4Accessibility.matrixBasedPt.name().equals( mode ) ) {
						throw new RuntimeException("currently not supported because implementation not consistent with guice grapher.  kai, sep'19") ;
//						calculator = new LeastCostPathCalculatorAccessibilityContributionCalculator(
//								config.planCalcScore(),	ptMatrix.asPathCalculator(config.planCalcScore()), scenario);
					} else if ( TransportMode.walk.equals( mode ) || TransportMode.bike.equals( mode ) ) {
						// special case(s), since often in the simulation this is not treated as network route
						calculator = new ConstantSpeedAccessibilityExpContributionCalculator( mode, scenario ) ;
					} else {
						// see if we find a trip router for that mode
						final TravelTime travelTime = travelTimes.get( mode );
						final TravelDisutilityFactory travelDisutilityFactory = travelDisutilityFactories.get( mode );
						if ( travelTime==null ) {
							throw new RuntimeException("mode=" + mode + "; travelTime is null!") ;
						}
						if ( travelDisutilityFactory==null ) {
							throw new RuntimeException("mode=" + mode + "; travelDisutilityFactory is null!") ;
						}
						calculator = new TripRouterAccessibilityContributionCalculator(mode, tripRouter, config.planCalcScore(), scenario,
							  travelTime, travelDisutilityFactory );
					}

					if ( calculator!=null ){
						accessibilityShutdownListener.putAccessibilityContributionCalculator( mode, calculator );
					} else {
						LOG.warn(  "accessibility contribution calculator for mode=" + mode + " was null.  Will not compute accessibility for " +
									 "that mode." );
					}


				}

				if (pushing2Geoserver || createQGisOutput) {
					if (measurePointGeometryMap == null) {
						throw new IllegalArgumentException("measure-point-to-geometry map must not be null if push to Geoserver is intended.");
					}
					Set <String> additionalFacInfo = additionalFacs.keySet();
					accessibilityShutdownListener.addFacilityDataExchangeListener(new GeoserverUpdater(acg.getOutputCrs(),
							config.controler().getRunId() + "_" + activityType, measurePointGeometryMap, additionalFacInfo,
							outputDirectory, pushing2Geoserver, createQGisOutput));
				}
				
				for (ActivityFacilities fac : additionalFacs.values()) {
					accessibilityShutdownListener.addAdditionalFacilityData(fac);
				}
				
				for (FacilityDataExchangeInterface listener : facilityDataListeners) {
					accessibilityShutdownListener.addFacilityDataExchangeListener(listener);
				}
				
				return accessibilityShutdownListener;
			}
		});
	}
	
	public final void setPushing2Geoserver(boolean pushing2Geoserver) {
		this.pushing2Geoserver = pushing2Geoserver;
	}
	
	public final void setCreateQGisOutput(boolean createQGisOutput) {
		this.createQGisOutput = createQGisOutput;
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
