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
import java.util.List;
import java.util.Map;

import javax.inject.Provider;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.accessibility.AccessibilityConfigGroup.AreaOfAccesssibilityComputation;
import org.matsim.contrib.accessibility.gis.GridUtils;
import org.matsim.contrib.accessibility.interfaces.FacilityDataExchangeInterface;
import org.matsim.contrib.accessibility.interfaces.SpatialGridDataExchangeInterface;
import org.matsim.contrib.accessibility.utils.AccessibilityUtils;
import org.matsim.contrib.accessibility.utils.GeoserverUpdater;
import org.matsim.contrib.matrixbasedptrouter.PtMatrix;
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
import org.matsim.facilities.FacilitiesReaderMatsimV1;

import com.google.inject.Inject;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;

/**
 * @author dziemke
 */
public final class AccessibilityModule extends AbstractModule {
	private static final Logger LOG = Logger.getLogger(AccessibilityModule.class);

	private List<SpatialGridDataExchangeInterface> spatialGridDataListeners = new ArrayList<>() ;
	private List<FacilityDataExchangeInterface> facilityDataListeners = new ArrayList<>() ; 
	private ActivityFacilities measuringPoints;
	private List<ActivityFacilities> additionalFacs = new ArrayList<>() ;
	private String activityType;
	private boolean pushing2Geoserver;
	private String crs;
	
	/**
	 * If this class does not provide you with enough flexibility, do your own new AbstractModule(){...}, copy the install part from this class
	 * into that, and go from there. 
	 */
	public AccessibilityModule() {
	}
	
	
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
				double cellSize_m = acg.getCellSizeCellBasedAccessibility();
				if (cellSize_m <= 0) {
					LOG.error("Cell Size needs to be assigned a value greater than zero.");
				}
				crs = acg.getOutputCrs() ;
				
				ActivityFacilities opportunities = AccessibilityUtils.collectActivityFacilitiesWithOptionOfType(scenario, activityType) ;
				
				final BoundingBox boundingBox;
//				final ActivityFacilities measuringPoints;
				
				if (acg.getAreaOfAccessibilityComputation() == AreaOfAccesssibilityComputation.fromShapeFile) {
					Geometry boundary = GridUtils.getBoundary(acg.getShapeFileCellBasedAccessibility());
					Envelope envelope = boundary.getEnvelopeInternal();
					boundingBox = BoundingBox.createBoundingBox(envelope.getMinX(), envelope.getMinY(), envelope.getMaxX(), envelope.getMaxY());
					if (measuringPoints != null) {LOG.warn("Measuring points had already been set directly. Now overwriting...");}
					measuringPoints = GridUtils.createGridLayerByGridSizeByShapeFileV2(boundary, cellSize_m);
					LOG.info("Using shape file to determine the area for accessibility computation.");
				} else if (acg.getAreaOfAccessibilityComputation() == AreaOfAccesssibilityComputation.fromBoundingBox) {
					boundingBox = BoundingBox.createBoundingBox(acg.getBoundingBoxLeft(), acg.getBoundingBoxBottom(), acg.getBoundingBoxRight(), acg.getBoundingBoxTop());
					if (measuringPoints != null) {LOG.warn("Measuring points had already been set directly. Now overwriting...");}
					measuringPoints = GridUtils.createGridLayerByGridSizeByBoundingBoxV2(boundingBox, cellSize_m);
					LOG.info("Using custom bounding box to determine the area for accessibility computation.");
				} else if (acg.getAreaOfAccessibilityComputation() == AreaOfAccesssibilityComputation.fromFacilitiesFile) {
					boundingBox = BoundingBox.createBoundingBox(scenario.getNetwork());
					LOG.info("Using the boundary of the network file to determine the area for accessibility computation.");
					LOG.warn("This can lead to memory issues when the network is large and/or the cell size is too fine!");
					Scenario measuringPointsSc = ScenarioUtils.createScenario(ConfigUtils.createConfig());
					String measuringPointsFile = ConfigUtils.addOrGetModule(config, AccessibilityConfigGroup.class ).getMeasuringPointsFile() ;
					new FacilitiesReaderMatsimV1(measuringPointsSc).readFile(measuringPointsFile);
					if (measuringPoints != null) {LOG.warn("Measuring points had already been set directly. Now overwriting...");}
					measuringPoints = (ActivityFacilitiesImpl) AccessibilityUtils.collectActivityFacilitiesWithOptionOfType(measuringPointsSc, activityType);
					LOG.info("Using measuring points from file: " + measuringPointsFile);
				} else if (acg.getAreaOfAccessibilityComputation() == AreaOfAccesssibilityComputation.fromFacilitiesObject) {
//					boundingBox = null; // TODO
					boundingBox = BoundingBox.createBoundingBox(scenario.getNetwork());
					measuringPoints = ConfigUtils.addOrGetModule(config, AccessibilityConfigGroup.class ).getMeasuringPointsFacilities() ;
					if (measuringPoints == null) {
						throw new RuntimeException("Measuring points should have been set direclty if from-facilities-object mode is used.");
					}
					LOG.info("Using measuring points from facilities object.");
				} else {
					boundingBox = BoundingBox.createBoundingBox(scenario.getNetwork());
					measuringPoints = GridUtils.createGridLayerByGridSizeByBoundingBoxV2(boundingBox, cellSize_m) ;
					LOG.info("Using the boundary of the network file to determine the area for accessibility computation.");
					LOG.warn("This can lead to memory issues when the network is large and/or the cell size is too fine!");
				}
				
				LOG.warn("boundingBox = " + boundingBox);
				
				AccessibilityCalculator accessibilityCalculator = new AccessibilityCalculator(scenario, measuringPoints);
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
						calculator = new NetworkModeAccessibilityExpContributionCalculator(travelTime, travelDisutilityFactory, scenario) ;
						break; }
					case freespeed: {
						final TravelDisutilityFactory travelDisutilityFactory = travelDisutilityFactories.get(TransportMode.car);
						Gbl.assertNotNull(travelDisutilityFactory);
						calculator = new NetworkModeAccessibilityExpContributionCalculator(new FreeSpeedTravelTime(), travelDisutilityFactory, scenario) ;
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
				
				if (pushing2Geoserver == true) {
					accessibilityCalculator.addFacilityDataExchangeListener(new GeoserverUpdater(crs,
							config.controler().getRunId() + "_" + activityType, acg.getCellSizeCellBasedAccessibility()));
				}

				GridBasedAccessibilityShutdownListenerV3 gbasl = new GridBasedAccessibilityShutdownListenerV3(accessibilityCalculator, 
						opportunities, ptMatrix, scenario, boundingBox, cellSize_m);
				
				for (ActivityFacilities fac : additionalFacs) {
					gbasl.addAdditionalFacilityData(fac);
				}

				for (SpatialGridDataExchangeInterface listener : spatialGridDataListeners) {
					gbasl.addSpatialGridDataExchangeListener(listener) ;
				}
				
				for (FacilityDataExchangeInterface listener : facilityDataListeners) {
					gbasl.addFacilityDataExchangeListener(listener);
				}
				
				gbasl.writeToSubdirectoryWithName(activityType);
				
				return gbasl;
			}
		});
	}
	
	public final void setPushing2Geoserver( boolean pushing2Geoserver ) {
		this.pushing2Geoserver = pushing2Geoserver ;
	}
	
	@Deprecated
	public final void addSpatialGridDataExchangeListener(SpatialGridDataExchangeInterface listener) {
		spatialGridDataListeners.add(listener) ;
	}
	
	public final void addFacilityDataExchangeListener(FacilityDataExchangeInterface listener) {
		this.facilityDataListeners.add(listener) ;
	}

	/**
	 * Add additional facility data that will generate an additional column for each (x,y,t)-Entry. The facilities are aggregated to
	 * the measurement points in downstream code.
	 */
	public void addAdditionalFacilityData(ActivityFacilities facilities) {
		additionalFacs.add(facilities) ;
	}
	
	public void setConsideredActivityType(String activityType) {
		// yyyy could be done via config (list of activities)
		this.activityType = activityType ;
	}
	
	public void setMeasuringPoints(ActivityFacilities measuringPoints) {
		this.measuringPoints = measuringPoints ;
	}
}