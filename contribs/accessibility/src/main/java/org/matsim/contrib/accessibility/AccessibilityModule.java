/* *********************************************************************** *
 * project: org.matsim.*
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
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.trafficmonitoring.FreeSpeedTravelTime;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.ActivityFacilitiesImpl;

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
	
	public final void setPushing2Geoserver( boolean pushing2Geoserver ) {
		this.pushing2Geoserver = pushing2Geoserver ;
	}
	
	@Deprecated
	public final void addSpatialGridDataExchangeListener( SpatialGridDataExchangeInterface listener ) {
		spatialGridDataListeners.add( listener ) ;
	}
	
	public final void addFacilityDataExchangeListener( FacilityDataExchangeInterface listener ) {
		this.facilityDataListeners.add( listener ) ;
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
			
			@Override
			public ControlerListener get() {
				AccessibilityConfigGroup acg = ConfigUtils.addOrGetModule(scenario.getConfig(), AccessibilityConfigGroup.class);
				double cellSize_m = acg.getCellSizeCellBasedAccessibility();
				if (cellSize_m <= 0) {
					throw new RuntimeException("Cell Size needs to be assigned a value greater than zero.");
				}
				crs = acg.getOutputCrs() ;
				
				ActivityFacilities opportunities = AccessibilityUtils.collectActivityFacilitiesWithOptionOfType(scenario, activityType) ;
				
				final BoundingBox boundingBox;
				final ActivityFacilitiesImpl measuringPoints;
				if(acg.getAreaOfAccessibilityComputation().equals(AreaOfAccesssibilityComputation.fromShapeFile.toString())) {
					Geometry boundary = GridUtils.getBoundary(acg.getShapeFileCellBasedAccessibility());
					Envelope envelope = boundary.getEnvelopeInternal();
					boundingBox = BoundingBox.createBoundingBox(envelope.getMinX(), envelope.getMinY(), envelope.getMaxX(), envelope.getMaxY());
					measuringPoints = GridUtils.createGridLayerByGridSizeByShapeFileV2(boundary, cellSize_m);
					LOG.info("Using shape file to determine the area for accessibility computation.");
				} else if(acg.getAreaOfAccessibilityComputation().equals(AreaOfAccesssibilityComputation.fromBoundingBox.toString())) {
					boundingBox = BoundingBox.createBoundingBox(acg.getBoundingBoxLeft(), acg.getBoundingBoxBottom(), acg.getBoundingBoxRight(), acg.getBoundingBoxTop());
					measuringPoints = GridUtils.createGridLayerByGridSizeByBoundingBoxV2(boundingBox, cellSize_m);
					LOG.info("Using custom bounding box to determine the area for accessibility computation.");
				} else {
					boundingBox = BoundingBox.createBoundingBox(scenario.getNetwork());
					measuringPoints = GridUtils.createGridLayerByGridSizeByBoundingBoxV2(boundingBox, cellSize_m) ;
					LOG.info("Using the boundary of the network file to determine the area for accessibility computation.");
					LOG.warn("This can lead to memory issues when the network is large and/or the cell size is too fine!");
				}
				AccessibilityCalculator accessibilityCalculator = new AccessibilityCalculator(scenario, measuringPoints);
				for ( Modes4Accessibility mode : acg.getIsComputingMode() ) {
					AccessibilityContributionCalculator calc = null ;
					switch( mode ) {
					case bike:
						calc = new ConstantSpeedAccessibilityExpContributionCalculator( mode.name(), config, network);
						break;
					case car: {
						final TravelTime travelTime = travelTimes.get(mode.name());
						Gbl.assertNotNull(travelTime);
						final TravelDisutilityFactory travelDisutilityFactory = travelDisutilityFactories.get(mode.name());
						calc = new NetworkModeAccessibilityExpContributionCalculator(travelTime, travelDisutilityFactory, scenario) ;
						break; }
					case freespeed: {
						final TravelDisutilityFactory travelDisutilityFactory = travelDisutilityFactories.get(TransportMode.car);
						Gbl.assertNotNull(travelDisutilityFactory);
						calc = new NetworkModeAccessibilityExpContributionCalculator( new FreeSpeedTravelTime(), travelDisutilityFactory, scenario) ;
						break; }
					case pt:
						throw new RuntimeException("currently not implemented") ;
					case walk:
						calc = new ConstantSpeedAccessibilityExpContributionCalculator( mode.name(), config, network);
						break;
					default:
						throw new RuntimeException("not implemented") ;
					}
					accessibilityCalculator.putAccessibilityContributionCalculator(mode.name(), calc ) ;
				}
				
				if (pushing2Geoserver == true) {
					accessibilityCalculator.addFacilityDataExchangeListener(new GeoserverUpdater(crs, config.controler().getRunId() + "_" + activityType));
				}

				GridBasedAccessibilityShutdownListenerV3 gbasl = new GridBasedAccessibilityShutdownListenerV3(accessibilityCalculator, 
						opportunities, ptMatrix, scenario, boundingBox, cellSize_m);

				for ( SpatialGridDataExchangeInterface listener : spatialGridDataListeners ) {
					gbasl.addSpatialGridDataExchangeListener(listener) ;
				}
				for ( ActivityFacilities fac : additionalFacs ) {
					gbasl.addAdditionalFacilityData(fac);
				}
				for ( FacilityDataExchangeInterface listener : facilityDataListeners ) {
					gbasl.addFacilityDataExchangeListener(listener);
				}
				
				gbasl.writeToSubdirectoryWithName(activityType);
				
				return gbasl;
			}
		});
	}

	/**
	 * Add additional facility data that will generate an additional column for each (x,y,t)-Entry.  The facilities are aggreated to
	 * the measurement points in downstream code.
	 */
	public void addAdditionalFacilityData(ActivityFacilities facilities) {
		additionalFacs.add(facilities) ;
	}
	public void setConsideredActivityType(String activityType) {
		// yyyy could be done via config (list of activities)
		this.activityType = activityType ;
	}
}