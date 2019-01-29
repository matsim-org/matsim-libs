/* *********************************************************************** *
 * project: org.matsim.*												   *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.accessibility.gis.GridUtils;
import org.matsim.contrib.accessibility.gis.SpatialGrid;
import org.matsim.contrib.accessibility.interfaces.FacilityDataExchangeInterface;
import org.matsim.contrib.accessibility.interfaces.SpatialGridDataExchangeInterface;
import org.matsim.contrib.accessibility.utils.AccessibilityUtils;
import org.matsim.contrib.matrixbasedptrouter.PtMatrix;
import org.matsim.contrib.matrixbasedptrouter.utils.BoundingBox;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.events.ShutdownEvent;
import org.matsim.core.controler.listener.ShutdownListener;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.facilities.ActivityFacilities;

/**
 * @author thomas, dziemke
 */
@Deprecated
public final class GridBasedAccessibilityShutdownListenerV3 implements ShutdownListener {
	// yyyy The zone based and the grid based accessibility controler listeners should be combined, since the coordinate points on which this is
	// computed are now external anyways.  There is probably one or the other grid dependency in the grid based accessibility controler
	// listener, but we wanted to remove that anyways.  kai, dec'16

	private static final Logger log = Logger.getLogger(GridBasedAccessibilityShutdownListenerV3.class);


	private final AccessibilityCalculator accessibilityCalculator;
	private final List<SpatialGridDataExchangeInterface> spatialGridDataExchangeListener = new ArrayList<>();

	private final Scenario scenario;
	private double time;

	// for consideration of different activity types or different modes (or both) subdirectories are
	// required in order not to confuse the output
	private String outputSubdirectory;

	private boolean	calculateAggregateValues;
	private Map<String, Double> accessibilitySums = new HashMap<>();
	private Map<String, Double> accessibilityGiniCoefficients = new HashMap<>();

	private final double xMin, yMin, xMax, yMax, cellSize;

	private SpatialGridAggregator spatialGridAggregator;


	private PtMatrix ptMatrix;


	private ActivityFacilities opportunities;

	public GridBasedAccessibilityShutdownListenerV3(AccessibilityCalculator accessibilityCalculator, ActivityFacilities opportunities, 
			PtMatrix ptMatrix, Scenario scenario, BoundingBox box, double cellSize) {
		this( accessibilityCalculator, opportunities, ptMatrix, scenario, box.getXMin(), box.getYMin(), box.getXMax(), box.getYMax(), cellSize ) ;
	}

	/**
	 * constructor
	 * @param opportunities represented by ActivityFacilitiesImpl
	 * @param ptMatrix matrix with travel times and distances for any pair of pt stops
	 * @param scenario MATSim scenario
	 */
	public GridBasedAccessibilityShutdownListenerV3(AccessibilityCalculator accessibilityCalculator, ActivityFacilities opportunities, 
			PtMatrix ptMatrix, Scenario scenario, double xMin, double yMin, double xMax, double yMax, double cellSize) {
		this.xMin = xMin;
		this.yMin = yMin;
		this.xMax = xMax;
		this.yMax = yMax;
		this.cellSize = cellSize;
		this.ptMatrix = ptMatrix;
		this.opportunities = opportunities;
		this.accessibilityCalculator = accessibilityCalculator;
		this.scenario = scenario;
	}

	private List<ActivityFacilities> additionalFacilityData = new ArrayList<>() ;
	private Map<String,Tuple<SpatialGrid,SpatialGrid>> additionalSpatialGrids = new TreeMap<>() ;
	//(not sure if this is a bit odd ... but I always need TWO spatial grids. kai, mar'14)
	private boolean lockedForAdditionalFacilityData = false;


	@Override
	public void notifyShutdown(ShutdownEvent event) {
		if (event.isUnexpected()) {
			return;
		}

		// I thought about changing the type of opportunities to Map<Id,Facility> or even Collection<Facility>, but in the end
		// one can also use FacilitiesUtils.createActivitiesFacilities(), put everything in there, and give that to this constructor. kai, feb'14

		log.info("Initializing  ...");
		spatialGridAggregator = new SpatialGridAggregator();
		this.accessibilityCalculator.addFacilityDataExchangeListener(spatialGridAggregator);

		if (ptMatrix != null) {
			log.warn("Add pt-matrix accessibility calculator.");
			accessibilityCalculator.putAccessibilityContributionCalculator(
					Modes4Accessibility.matrixBasedPt.toString(),
					PtMatrixAccessibilityUtils.createPtMatrixAccessibilityCalculator(
							ptMatrix,
							scenario.getConfig()));	// this could be zero if no input files for pseudo pt are given ...
		}
		// yyyyyy todo: get rid of the above special case. kai, nov'16
		// TODO I think when using AccessibilityModule it is created twice and overwritten. So, the above could be deleted. dz, apr'17

		log.info(".. done initializing CellBasedAccessibilityControlerListenerV3");
		for (String mode : accessibilityCalculator.getModes() ) {
			spatialGridAggregator.getAccessibilityGrids().put(mode, new SpatialGrid(xMin, yMin, xMax, yMax, cellSize, Double.NaN));
		}
		
		// always put the free speed grid (yyyy may not be necessary in the long run)
		// this is required; otherwise the not-null assertion in linke 221 will fail
		log.warn("Spatial grid for freespeed added (again) to make sure it exists in any case.");
		spatialGridAggregator.getAccessibilityGrids().put(Modes4Accessibility.freespeed.name(),
				new SpatialGrid(xMin, yMin, xMax, yMax, cellSize, Double.NaN));

		lockedForAdditionalFacilityData  = true ;
		for ( ActivityFacilities facilities : this.additionalFacilityData ) {
			if ( this.additionalSpatialGrids.get( facilities.getName() ) != null ) {
				throw new RuntimeException("this should not yet exist ...") ;
			}
			Tuple<SpatialGrid,SpatialGrid> spatialGrids = new Tuple<>(
					new SpatialGrid(xMin, yMin, xMax, yMax, cellSize, 0.), new SpatialGrid(xMin, yMin, xMax, yMax, cellSize, 0.)) ;
			this.additionalSpatialGrids.put( facilities.getName(), spatialGrids ) ;
		}

		if (outputSubdirectory != null) {
			File file = new File(scenario.getConfig().controler().getOutputDirectory() + "/" + outputSubdirectory);
			file.mkdirs();
		}

		// prepare the additional columns:
		for ( ActivityFacilities facilities : this.additionalFacilityData ) {
			Tuple<SpatialGrid,SpatialGrid> spatialGrids = this.additionalSpatialGrids.get( facilities.getName() ) ;
			GridUtils.aggregateFacilitiesIntoSpatialGrid(facilities, spatialGrids.getFirst(), spatialGrids.getSecond(), null);
		}

		log.info("Computing and writing cell based accessibility measures ...");
		// printParameterSettings(); // use only for debugging (settings are printed as part of config dump)

		AccessibilityConfigGroup acg = ConfigUtils.addOrGetModule(scenario.getConfig(), AccessibilityConfigGroup.class);
		accessibilityCalculator.computeAccessibilities(acg.getTimeOfDay(), opportunities);

		// do calculation of aggregate index values, e.g. gini coefficient
		if (calculateAggregateValues) {
			performAggregateValueCalculations();
		}

		// as for the other writer above: In case multiple AccessibilityControlerListeners are added to the controller, e.g. if 
		// various calculations are done for different activity types or different modes (or both) subdirectories are required
		// in order not to confuse the output
		if (outputSubdirectory == null) {
			writePlottingData(scenario.getConfig().controler().getOutputDirectory());
		} else {
			writePlottingData(scenario.getConfig().controler().getOutputDirectory() + "/" + outputSubdirectory);
		}

		log.info("Triggering " + spatialGridDataExchangeListener.size() + " SpatialGridDataExchangeListener(s) ...");
		for (SpatialGridDataExchangeInterface spatialGridDataExchangeInterface : spatialGridDataExchangeListener) {
			try {
				spatialGridDataExchangeInterface.setAndProcessSpatialGrids(spatialGridAggregator.getAccessibilityGrids());
			} catch ( Exception ee ) {
				log.warn("Had a problem here; printing stack trace but then continuing anyways") ;
				ee.printStackTrace(); 
			}
		}
	}


	/**
	 * This writes the accessibility grid data into the MATSim output directory
	 */
	private void writePlottingData(String adaptedOutputDirectory) {

		// in the following, the data used for gnuplot or QGis is written. dz, feb'15
		// different separators have to be used to make this output useable by gnuplot or QGis, respectively
		log.info("Writing plotting data for other analyis into " + adaptedOutputDirectory + " ...");

		final CSVWriter writer = new CSVWriter(adaptedOutputDirectory + "/" + CSVWriter.FILE_NAME ) ;

		// Write header
		writer.writeField(Labels.X_COORDINATE);
		writer.writeField(Labels.Y_COORDINATE);
		//		writer.writeField(Labels.TIME); // TODO
		for (String mode : accessibilityCalculator.getModes() ) {
			writer.writeField(mode + "_accessibility");
		}
		writer.writeField(Labels.DENSITIY); // TODO I think this has to be made adjustable
		writer.writeField(Labels.DENSITIY);
		writer.writeNewLine();

		final SpatialGrid spatialGrid = spatialGridAggregator.getAccessibilityGrids().get(Modes4Accessibility.freespeed.name() ) ;
		Gbl.assertNotNull( spatialGrid );
		// yy for time being, have to assume that this is always there
		for(double y = spatialGrid.getYmin(); y < spatialGrid.getYmax(); y += spatialGrid.getResolution()) {
			for(double x = spatialGrid.getXmin(); x < spatialGrid.getXmax(); x += spatialGrid.getResolution()) {
				
				// new
//				log.error("This is only here temporarily! Think about long-term solution"); // TODO
//				if (this.additionalSpatialGrids.get(FacilityTypes.HOME).getFirst().getValue(x, y) <= 50.) {
//					continue;
//				}
				// end new

				writer.writeField( x + 0.5*spatialGrid.getResolution());
				writer.writeField( y + 0.5*spatialGrid.getResolution());

				//				writer.writeField(time); // TODO

				for (String mode : accessibilityCalculator.getModes() ) {
					final SpatialGrid spatialGridOfMode = spatialGridAggregator.getAccessibilityGrids().get(mode);
					final double value = spatialGridOfMode.getValue(x, y);
					if (!Double.isNaN(value)) { 
						writer.writeField(value) ;
					} else {
						writer.writeField(Double.NaN) ;
					}
				}
				for ( Tuple<SpatialGrid,SpatialGrid> additionalGrids : this.additionalSpatialGrids.values() ) {
					writer.writeField( additionalGrids.getFirst().getValue(x, y) ) ;
					writer.writeField( additionalGrids.getSecond().getValue(x, y) ) ;
				}
				writer.writeNewLine(); 
			}
		}
		writer.close() ;

		log.info("Writing plotting data for other analysis done!");
	}


	// new idea
	/**
	 * perform aggregate value calculations
	 */
	private void performAggregateValueCalculations() {
		// yyyy Dominik, I have issues with such average values since they are strongly affected by MAUP.  E.g. imagine that you add
		// 5km of desert around the NMB scenario: then all these aggregated values significantly change, without any changes in the reality.
		// In consequence, such values may be useful to track progress within a scenario, but not to compare between scenarios.  
		// However, people always end up using it for the latter.  kai, nov'16
		
		log.info("Starting to caluclating aggregate values!");
		final SpatialGrid spatialGrid = spatialGridAggregator.getAccessibilityGrids().get(Modes4Accessibility.freespeed) ;
		// yy for time being, have to assume that this is always there

		for (String mode : accessibilityCalculator.getModes() ) {
			List<Double> valueList = new ArrayList<>();

			for(double y = spatialGrid.getYmin(); y <= spatialGrid.getYmax() ; y += spatialGrid.getResolution()) {
				for(double x = spatialGrid.getXmin(); x <= spatialGrid.getXmax(); x += spatialGrid.getResolution()) {
					final SpatialGrid spatialGridOfMode = spatialGridAggregator.getAccessibilityGrids().get(mode);
					Gbl.assertNotNull(spatialGridOfMode);
					final double value = spatialGridOfMode.getValue(x, y);
					if ( !Double.isNaN(value ) ) {
						valueList.add(value);
					} else {
						new RuntimeException("Don't know how to calculate aggregate values properly if some are missing!");
					}
				}
			}

			double accessibilityValueSum = AccessibilityUtils.calculateSum(valueList);
			double giniCoefficient = AccessibilityUtils.calculateGiniCoefficient(valueList);

			log.warn("mode = " + mode  + " -- accessibilityValueSum = " + accessibilityValueSum);
			accessibilitySums.put(mode, accessibilityValueSum);
			log.warn("accessibilitySum = " + accessibilitySums);
			accessibilityGiniCoefficients.put(mode, giniCoefficient);
		}
		log.info("Done with caluclating aggregate values!");
	}
	//

	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	// GridBasedAccessibilityControlerListenerV3 specific methods that do not apply to zone-based accessibility measures
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////


	/**
	 * I wanted to plot something like (max(acc)-acc)*population.  For that, I needed "population" at the x/y coordinates.
	 * This is the mechanics via which I inserted that. (The computation is then done in postprocessing.)
	 * <p></p>
	 * You can add arbitrary ActivityFacilities containers here.  They will be aggregated to the grid points, and then written to
	 * file as additional column.
	 */
	public void addAdditionalFacilityData(ActivityFacilities facilities ) {
		log.warn("changed this data flow (by adding the _cnt_ column) but did not test.  If it works, please remove this warning. kai, mar'14") ;

		if ( this.lockedForAdditionalFacilityData ) {
			throw new RuntimeException("too late for adding additional facility data; spatial grids have already been generated.  Needs"
					+ " to be called before generating the spatial grids.  (This design should be improved ..)") ;
		}
		if ( facilities.getName()==null || facilities.getName().equals("") ) {
			throw new RuntimeException("cannot add unnamed facility containers here since we need a key to find them again") ;
		}
		for ( ActivityFacilities existingFacilities : this.additionalFacilityData ) {
			if ( existingFacilities.getName().equals( facilities.getName() ) ) {
				throw new RuntimeException("additional facilities under the name of + " + facilities.getName() + 
						" already exist; cannot add additional facilities under the same name twice.") ;
			}
		}

		this.additionalFacilityData.add( facilities ) ;
	}


	/**
	 * Using this method changes the folder structure of the output. The output of the calculation will be written into the
	 * subfolder. This is needed if more than one ContolerListener is added since otherwise the output would be overwritten
	 * and not be available for analyses anymore.
	 */
	public void writeToSubdirectoryWithName(String subdirectory) {
		this.outputSubdirectory = subdirectory;
	}

	@Deprecated // use FacilityDataExchangeInterface instead
	public void addSpatialGridDataExchangeListener(SpatialGridDataExchangeInterface l) {
		this.spatialGridDataExchangeListener.add(l);
	}

	public void addFacilityDataExchangeListener( FacilityDataExchangeInterface facilityDataExchangeListener ) {
		this.accessibilityCalculator.addFacilityDataExchangeListener(facilityDataExchangeListener);
	}


	public void setTime(double time) {
		this.time = time;
	}


	public void setCalculateAggregateValues(boolean calculateAggregateValues) {
		this.calculateAggregateValues = calculateAggregateValues;
	}


	public Map<String, Double> getAccessibilitySums() {
		return this.accessibilitySums;
	}


	public Map<String, Double> getAccessibilityGiniCoefficients() {
		return this.accessibilityGiniCoefficients;
	}
}