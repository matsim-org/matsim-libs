/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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

/**
 * 
 */
package org.matsim.contrib.noise.data;


import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.core.config.ReflectiveConfigGroup;
import org.matsim.core.utils.collections.CollectionUtils;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;


/**
 * 
 * Provides the parameters required to build a simple grid with some basic spatial functionality.
 * 
 * @author ikaddoura
 *
 */
public class GridParameters extends ReflectiveConfigGroup {
	
	public static final String GROUP_NAME = "noiseGrid";
	
	public GridParameters() {
		super(GROUP_NAME);
	}

	private static final Logger log = Logger.getLogger(GridParameters.class);

	private double receiverPointGap = 250.;
	private String transformationFactory = TransformationFactory.DHDN_GK4;

	private String[] consideredActivitiesForReceiverPointGridArray = {"home", "work"};
	private String[] consideredActivitiesForSpatialFunctionalityArray = {"home", "work"};
	
	// Setting all minimum and maximum coordinates to 0.0 means the receiver points are computed for the entire area for which any of the considered activities for the receiver point grid are found.
	private double receiverPointsGridMinX = 0.;
	private double receiverPointsGridMinY = 0.;
	private double receiverPointsGridMaxX = 0.;
	private double receiverPointsGridMaxY = 0.;
	
	private String consideredActivitiesForReceiverPointGrid = null;
	private String consideredActivitiesForSpatialFunctionality = null;

	// ########################################################################################################
			
	public void checkForConsistency() {
		
		List<String> consideredActivitiesForReceiverPointGridList = new ArrayList<String>();
		List<String> consideredActivitiesForDamagesList = new ArrayList<String>();

		for (int i = 0; i < consideredActivitiesForSpatialFunctionalityArray.length; i++) {
			consideredActivitiesForDamagesList.add(consideredActivitiesForSpatialFunctionalityArray[i]);
		}

		for (int i = 0; i < this.consideredActivitiesForReceiverPointGridArray.length; i++) {
			consideredActivitiesForReceiverPointGridList.add(consideredActivitiesForReceiverPointGridArray[i]);
		}
		
		if (this.receiverPointGap == 0.) {
			throw new RuntimeException("The receiver point gap is 0. Aborting...");
		}
				
		if (consideredActivitiesForReceiverPointGridList.size() == 0 && this.receiverPointsGridMinX == 0. && this.receiverPointsGridMinY == 0. && this.receiverPointsGridMaxX == 0. && receiverPointsGridMaxY == 0.) {
			throw new RuntimeException("NEITHER providing a considered activity type for the minimum and maximum coordinates of the receiver point grid area "
					+ "NOR providing receiver point grid minimum and maximum coordinates. Aborting...");
		}
	}
	
	// ########################################################################################################

	@StringGetter( "receiverPointGap" )
	public double getReceiverPointGap() {
		return receiverPointGap;
	}
	
	@StringSetter( "receiverPointGap" )
	public void setReceiverPointGap(double receiverPointGap) {
		log.info("setting the horizontal/vertical distance between each receiver point to " + receiverPointGap);
		this.receiverPointGap = receiverPointGap;
	}

	@StringGetter( "receiverPointsGridMinX" )
	public double getReceiverPointsGridMinX() {
		return receiverPointsGridMinX;
	}

	@StringSetter( "receiverPointsGridMinX" )
	public void setReceiverPointsGridMinX(double receiverPointsGridMinX) {
		log.info("setting receiverPoints grid MinX Coordinate to " + receiverPointsGridMinX);
		this.receiverPointsGridMinX = receiverPointsGridMinX;
	}

	@StringGetter( "receiverPointsGridMinY" )
	public double getReceiverPointsGridMinY() {
		return receiverPointsGridMinY;
	}

	@StringSetter( "receiverPointsGridMinY" )
	public void setReceiverPointsGridMinY(double receiverPointsGridMinY) {
		log.info("setting receiverPoints grid MinY Coordinate to " + receiverPointsGridMinY);
		this.receiverPointsGridMinY = receiverPointsGridMinY;
	}

	@StringGetter( "receiverPointsGridMaxX" )
	public double getReceiverPointsGridMaxX() {
		return receiverPointsGridMaxX;
	}

	@StringSetter( "receiverPointsGridMaxX" )
	public void setReceiverPointsGridMaxX(double receiverPointsGridMaxX) {
		log.info("setting receiverPoints grid MaxX Coordinate to " + receiverPointsGridMaxX);
		this.receiverPointsGridMaxX = receiverPointsGridMaxX;
	}

	@StringGetter( "receiverPointsGridMaxY" )
	public double getReceiverPointsGridMaxY() {
		return receiverPointsGridMaxY;
	}

	@StringSetter( "receiverPointsGridMaxY" )
	public void setReceiverPointsGridMaxY(double receiverPointsGridMaxY) {
		log.info("setting receiverPoints grid MaxY Coordinate to " + receiverPointsGridMaxY);
		this.receiverPointsGridMaxY = receiverPointsGridMaxY;
	}

	@StringGetter( "transformationFactory" )
	public String getTransformationFactory() {
		return transformationFactory;
	}

	@StringSetter( "transformationFactory" )
	public void setTransformationFactory(String transformationFactory) {
		this.transformationFactory = transformationFactory;
	}

	public String[] getConsideredActivitiesForReceiverPointGridArray() {
		return consideredActivitiesForReceiverPointGridArray;
	}

	public void setConsideredActivitiesForReceiverPointGridArray(String[] consideredActivitiesForReceiverPointGrid) {
		log.info("setting considered activities for receiver point grid to: ");
		for (int i = 0; i < consideredActivitiesForReceiverPointGrid.length; i++) {
			log.info(consideredActivitiesForReceiverPointGrid[i]);
		}
		this.consideredActivitiesForReceiverPointGridArray = consideredActivitiesForReceiverPointGrid;		
	}
	
	public String[] getConsideredActivitiesForSpatialFunctionalityArray() {		
		return consideredActivitiesForSpatialFunctionalityArray;
	}

	public void setConsideredActivitiesForSpatialFunctionalityArray(String[] consideredActivitiesForSpatialFunctionality) {
		log.info("setting considered activities for spatial functionality to: ");
		for (int i = 0; i < consideredActivitiesForSpatialFunctionality.length; i++) {
			log.info(consideredActivitiesForSpatialFunctionality[i]);
		}
		this.consideredActivitiesForSpatialFunctionalityArray = consideredActivitiesForSpatialFunctionality;
	}

	@StringGetter( "consideredActivitiesForReceiverPointGrid" )
	private String getConsideredActivitiesForReceiverPointGrid() {
		return consideredActivitiesForReceiverPointGrid;
	}

	@StringSetter( "consideredActivitiesForReceiverPointGrid" )
	public void setConsideredActivitiesForReceiverPointGrid(String consideredActivitiesForReceiverPointGridString) {
		this.consideredActivitiesForReceiverPointGrid = consideredActivitiesForReceiverPointGridString;
		
		this.setConsideredActivitiesForReceiverPointGridArray(CollectionUtils.stringToArray(consideredActivitiesForReceiverPointGridString));
	}

	@StringGetter( "consideredActivitiesForSpatialFunctionality" )
	public String getConsideredActivitiesForSpatialFunctionality() {
		return consideredActivitiesForSpatialFunctionality;
	}

	@StringSetter( "consideredActivitiesForSpatialFunctionality" )
	public void setConsideredActivitiesForSpatialFunctionality(String consideredActivitiesForSpatialFunctionalityString) {
		this.consideredActivitiesForSpatialFunctionality = consideredActivitiesForSpatialFunctionalityString;
		
		this.setConsideredActivitiesForSpatialFunctionalityArray(CollectionUtils.stringToArray(consideredActivitiesForSpatialFunctionalityString));
	}
	
}
