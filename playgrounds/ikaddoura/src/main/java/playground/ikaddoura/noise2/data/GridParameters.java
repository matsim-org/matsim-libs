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
package playground.ikaddoura.noise2.data;


import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;


/**
 * 
 * Provides the parameters required to build a simple grid with some basic spatial functionality.
 * 
 * @author ikaddoura
 *
 */
public class GridParameters {
	
	private static final Logger log = Logger.getLogger(GridParameters.class);

	private double receiverPointGap = 250.;
	private String transformationFactory = TransformationFactory.DHDN_GK4;

	private String[] consideredActivitiesForReceiverPointGrid = {"home", "work"};
	private String[] consideredActivitiesForSpatialFunctionality = {"home", "work"};
	
	// Setting all minimum and maximum coordinates to 0.0 means the receiver points are computed for the entire area for which any of the considered activities for the receiver point grid are found.
	private double receiverPointsGridMinX = 0.;
	private double receiverPointsGridMinY = 0.;
	private double receiverPointsGridMaxX = 0.;
	private double receiverPointsGridMaxY = 0.;

	// ########################################################################################################
			
	public void checkForConsistency() {
		
		List<String> consideredActivitiesForReceiverPointGridList = new ArrayList<String>();
		List<String> consideredActivitiesForDamagesList = new ArrayList<String>();

		for (int i = 0; i < consideredActivitiesForSpatialFunctionality.length; i++) {
			consideredActivitiesForDamagesList.add(consideredActivitiesForSpatialFunctionality[i]);
		}

		for (int i = 0; i < this.consideredActivitiesForReceiverPointGrid.length; i++) {
			consideredActivitiesForReceiverPointGridList.add(consideredActivitiesForReceiverPointGrid[i]);
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

	public void setReceiverPointGap(double receiverPointGap) {
		log.info("Setting the horizontal/vertical distance between each receiver point to " + receiverPointGap);
		this.receiverPointGap = receiverPointGap;
	}

	public double getReceiverPointsGridMinX() {
		return receiverPointsGridMinX;
	}

	public void setReceiverPointsGridMinX(double receiverPointsGridMinX) {
		log.info("Setting receiverPoints grid MinX Coordinate to " + receiverPointsGridMinX);
		this.receiverPointsGridMinX = receiverPointsGridMinX;
	}

	public double getReceiverPointsGridMinY() {
		return receiverPointsGridMinY;
	}

	public void setReceiverPointsGridMinY(double receiverPointsGridMinY) {
		log.info("Setting receiverPoints grid MinY Coordinate to " + receiverPointsGridMinY);
		this.receiverPointsGridMinY = receiverPointsGridMinY;
	}

	public double getReceiverPointsGridMaxX() {
		return receiverPointsGridMaxX;
	}

	public void setReceiverPointsGridMaxX(double receiverPointsGridMaxX) {
		log.info("Setting receiverPoints grid MaxX Coordinate to " + receiverPointsGridMaxX);
		this.receiverPointsGridMaxX = receiverPointsGridMaxX;
	}

	public double getReceiverPointsGridMaxY() {
		return receiverPointsGridMaxY;
	}

	public void setReceiverPointsGridMaxY(double receiverPointsGridMaxY) {
		log.info("Setting receiverPoints grid MaxY Coordinate to " + receiverPointsGridMaxY);
		this.receiverPointsGridMaxY = receiverPointsGridMaxY;
	}
	
	public double getReceiverPointGap() {
		return receiverPointGap;
	}

	public String getTransformationFactory() {
		return transformationFactory;
	}

	public void setTransformationFactory(String transformationFactory) {
		this.transformationFactory = transformationFactory;
	}

	public String[] getConsideredActivitiesForReceiverPointGrid() {
		return consideredActivitiesForReceiverPointGrid;
	}

	public void setConsideredActivitiesForReceiverPointGrid(
			String[] consideredActivitiesForReceiverPointGrid) {
		this.consideredActivitiesForReceiverPointGrid = consideredActivitiesForReceiverPointGrid;
	}
	
	public String[] getConsideredActivitiesForSpatialFunctionality() {		
		return consideredActivitiesForSpatialFunctionality;
	}

	public void setConsideredActivitiesForSpatialFunctionality(String[] consideredActivities) {
		log.info("Setting considered activities to: ");
		for (int i = 0; i < consideredActivities.length; i++) {
			log.info(consideredActivities[i]);
		}
		this.consideredActivitiesForSpatialFunctionality = consideredActivities;
	}
	
}
