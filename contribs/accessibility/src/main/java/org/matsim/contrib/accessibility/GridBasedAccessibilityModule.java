package org.matsim.contrib.accessibility;

import java.util.Map;
import java.util.Map.Entry;

import com.google.inject.Inject;

//import javax.inject.Inject;
import javax.inject.Provider;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.accessibility.AccessibilityConfigGroup.AreaOfAccesssibilityComputation;
import org.matsim.contrib.accessibility.gis.GridUtils;
import org.matsim.contrib.accessibility.interfaces.SpatialGridDataExchangeInterface;
import org.matsim.contrib.accessibility.run.AccessibilityIntegrationTest;
import org.matsim.contrib.matrixbasedptrouter.PtMatrix;
import org.matsim.contrib.matrixbasedptrouter.utils.BoundingBox;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.listener.ControlerListener;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.ActivityFacilitiesImpl;

import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;

public class GridBasedAccessibilityModule extends AbstractModule {
	private static final Logger LOG = Logger.getLogger(AccessibilityIntegrationTest.class);
	
//	private final PtMatrix ptMatrix;

//	public GridBasedAccessibilityModule(PtMatrix ptMatrix) {
	public GridBasedAccessibilityModule() {
//		this.ptMatrix = ptMatrix;
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
				BoundingBox boundingBox;
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
					LOG.warn("This could lead to memory issues when the network is large and/or the cell size is too fine!");
				}
				AccessibilityCalculator accessibilityCalculator = new AccessibilityCalculator(scenario, measuringPoints);
				GridBasedAccessibilityShutdownListenerV3 gacl = new GridBasedAccessibilityShutdownListenerV3(accessibilityCalculator, opportunities, ptMatrix, scenario, 
						boundingBox, cellSize_m);
				for (Entry<String, AccessibilityContributionCalculator> entry : calculators.entrySet()) {
					accessibilityCalculator.putAccessibilityContributionCalculator(entry.getKey(), entry.getValue());
				}

				// this will be called by the accessibility listener after the accessibility calculations are finished
				// It checks if the SpatialGrid for activated (true) transport modes are instantiated or null if not (false)
				if (spatialGridDataExchangeListener != null) {
////					EvaluateTestResults etr = new EvaluateTestResults(true, true, true, true, true);
					gacl.addSpatialGridDataExchangeListener(spatialGridDataExchangeListener);
				}

				return gacl;
			}
		});
	}
}