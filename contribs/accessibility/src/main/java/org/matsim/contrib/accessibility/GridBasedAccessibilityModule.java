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

import java.util.Map;
import java.util.Map.Entry;

import javax.inject.Provider;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.accessibility.AccessibilityConfigGroup.AreaOfAccesssibilityComputation;
import org.matsim.contrib.accessibility.gis.GridUtils;
import org.matsim.contrib.accessibility.interfaces.SpatialGridDataExchangeInterface;
import org.matsim.contrib.matrixbasedptrouter.PtMatrix;
import org.matsim.contrib.matrixbasedptrouter.utils.BoundingBox;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.listener.ControlerListener;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.ActivityFacilitiesImpl;

import com.google.inject.Inject;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;

/**
 * @author dziemke
 */
public class GridBasedAccessibilityModule extends AbstractModule {
	private static final Logger LOG = Logger.getLogger(GridBasedAccessibilityModule.class);
	
	public GridBasedAccessibilityModule() {
	}

	@Override
	public void install() {
		addControlerListenerBinding().toProvider(new Provider<ControlerListener>() {
			@Inject private Scenario scenario;
			@Inject private ActivityFacilities opportunities;
			@Inject private Map<String, AccessibilityContributionCalculator> calculators;
			@Inject private SpatialGridDataExchangeInterface spatialGridDataExchangeListener;
			@Inject (optional = true) PtMatrix ptMatrix = null; // Downstream code knows how to handle a null PtMatrix

			@Override
			public ControlerListener get() {
				AccessibilityConfigGroup acg = ConfigUtils.addOrGetModule(scenario.getConfig(), AccessibilityConfigGroup.class);
				double cellSize_m = acg.getCellSizeCellBasedAccessibility();
				final BoundingBox boundingBox;
				final ActivityFacilitiesImpl measuringPoints;
				if (cellSize_m <= 0) {
					throw new RuntimeException("Cell Size needs to be assigned a value greater than zero.");
				}
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
				GridBasedAccessibilityShutdownListenerV3 gbasl = new GridBasedAccessibilityShutdownListenerV3(accessibilityCalculator, opportunities, ptMatrix, scenario, 
						boundingBox, cellSize_m);
				for (Entry<String, AccessibilityContributionCalculator> entry : calculators.entrySet()) {
					accessibilityCalculator.putAccessibilityContributionCalculator(entry.getKey(), entry.getValue());
				}

				if (spatialGridDataExchangeListener != null) {
					gbasl.addSpatialGridDataExchangeListener(spatialGridDataExchangeListener);
				}
				return gbasl;
			}
		});
	}
}