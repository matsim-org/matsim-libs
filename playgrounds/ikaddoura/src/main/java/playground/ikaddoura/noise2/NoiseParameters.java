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
package playground.ikaddoura.noise2;


import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;


/**
 * @author ikaddoura
 *
 */
public class NoiseParameters {
	
	private static final Logger log = Logger.getLogger(NoiseParameters.class);

	private double annualCostRate = (85.0/(1.95583)) * (Math.pow(1.02, (2014-1995)));
	private double timeBinSizeNoiseComputation = 3600.0;
	private double scaleFactor = 1.;
	private double receiverPointGap = 250.;
	private double relevantRadius = 500.;
	private String hgvIdPrefix = "lkw";
	private String[] consideredActivitiesForDamages = {"home", "work"};
	private String transformationFactory = TransformationFactory.DHDN_GK4;

	// Setting all minimum and maximum coordinates to 0.0 means the receiver points are computed for the entire area for which any of the considered activities for the receiver point grid are found.
	private double receiverPointsGridMinX = 0.;
	private double receiverPointsGridMinY = 0.;
	private double receiverPointsGridMaxX = 0.;
	private double receiverPointsGridMaxY = 0.;
	private String[] consideredActivitiesForReceiverPointGrid = {"home", "work"};

	private List<Id<Link>> tunnelLinkIDs = new ArrayList<Id<Link>>();
	
	private boolean throwNoiseEventsAffected = true;
	private boolean computeNoiseDamages = true;
	private boolean internalizeNoiseDamages = true;
	private boolean computeCausingAgents = true; 
	private boolean throwNoiseEventsCaused = true;
		
	// ########################################################################################################
			
	public void checkForConsistency() {
		
		List<String> consideredActivitiesForDamagesList = new ArrayList<String>();
		List<String> consideredActivitiesForReceiverPointGridList = new ArrayList<String>();

		for (int i = 0; i < consideredActivitiesForDamages.length; i++) {
			consideredActivitiesForDamagesList.add(consideredActivitiesForDamages[i]);
		}
		for (int i = 0; i < this.consideredActivitiesForReceiverPointGrid.length; i++) {
			consideredActivitiesForReceiverPointGridList.add(consideredActivitiesForReceiverPointGrid[i]);
		}
		
		if (this.internalizeNoiseDamages) {
			
			// required for internalization
			if (this.computeCausingAgents == false) {
				log.warn("Inconsistent parameters will be adjusted:");
				this.setComputeCausingAgents(true);
			}
			
			// required for internalization, i.e. the scoring
			if (this.throwNoiseEventsCaused == false) {
				log.warn("Inconsistent parameters will be adjusted:");
				this.setThrowNoiseEventsCaused(true);
			}
			
		}
		
		if (this.computeCausingAgents 
				|| this.internalizeNoiseDamages 
				|| this.throwNoiseEventsAffected
				|| this.throwNoiseEventsCaused
				) {
			
			// required
			if (this.computeNoiseDamages == false) {
				log.warn("Inconsistent parameters will be adjusted:");
				this.setComputeNoiseDamages(true);
			}
		}
		
		if (this.throwNoiseEventsCaused) {
			
			// required
			if (this.computeCausingAgents == false) {
				log.warn("Inconsistent parameters will be adjusted:");
				this.setComputeCausingAgents(true);
			}
		}
		
		if (this.computeNoiseDamages) {
						
			if (consideredActivitiesForDamagesList.size() == 0) {
				log.warn("Not considering any activity type for the noise damage computation."
						+ "The computation of noise damages should be disabled.");
			}
			
			if (this.receiverPointsGridMaxX != 0.
					|| receiverPointsGridMinX != 0.
					|| receiverPointsGridMaxY != 0.
					|| receiverPointsGridMinY != 0.) {
				throw new RuntimeException("In order to keep track of the agent activities, the grid of receiver points should not be limited to a set of predefined coordinates."
						+ "For a grid covering all activity locations, set the minimum and maximum x/y parameters to 0.0. Aborting... ");
			}
						
			if (this.receiverPointsGridMinX == 0. && this.receiverPointsGridMinY == 0. && this.receiverPointsGridMaxX == 0. && receiverPointsGridMaxY == 0.) {
				for (String type : consideredActivitiesForDamagesList) {
					if (!consideredActivitiesForReceiverPointGridList.contains(type)) {
						throw new RuntimeException("An activity type which is considered for the damage calculation (" + type
								+ ") should also be considered for the minimum and maximum coordinates of the receiver point grid area. Aborting...");
					}
				}
			}
		}
		
		if (consideredActivitiesForReceiverPointGridList.size() == 0 && this.receiverPointsGridMinX == 0. && this.receiverPointsGridMinY == 0. && this.receiverPointsGridMaxX == 0. && receiverPointsGridMaxY == 0.) {
			throw new RuntimeException("NEITHER providing a considered activity type for the minimum and maximum coordinates of the receiver point grid area "
					+ "NOR providing receiver point grid minimum and maximum coordinates. Aborting...");
		}
	}
	
	// ########################################################################################################

	public boolean isThrowNoiseEventsAffected() {
		return throwNoiseEventsAffected;
	}

	public void setThrowNoiseEventsAffected(boolean throwNoiseEventsAffected) {
		log.info("Throwing noise events for the affected agents: " + throwNoiseEventsAffected);
		this.throwNoiseEventsAffected = throwNoiseEventsAffected;
	}

	public boolean isThrowNoiseEventsCaused() {
		return throwNoiseEventsCaused;
	}

	public void setThrowNoiseEventsCaused(boolean throwNoiseEventsCaused) {
		log.info("Throwing noise events for the causing agents: " + throwNoiseEventsCaused);
		this.throwNoiseEventsCaused = throwNoiseEventsCaused;
	}

	public boolean isComputeCausingAgents() {
		return computeCausingAgents;
	}
	
	public void setComputeCausingAgents(boolean computeCausingAgents) {
		log.info("Allocating the noise damages to the causing agents: " + computeCausingAgents);
		this.computeCausingAgents = computeCausingAgents;
	}
	
	public void setAnnualCostRate(double annualCostRate) {
		log.info("Setting the annual cost rate to " + annualCostRate);
		this.annualCostRate = annualCostRate;
	}

	public void setTimeBinSizeNoiseComputation(double timeBinSizeNoiseComputation) {
		log.info("Setting the time bin size for the computation of noise to " + timeBinSizeNoiseComputation);
		this.timeBinSizeNoiseComputation = timeBinSizeNoiseComputation;
	}

	public void setScaleFactor(double scaleFactor) {
		log.info("Setting the scale factor to " + scaleFactor);
		this.scaleFactor = scaleFactor;
	}

	public void setReceiverPointGap(double receiverPointGap) {
		log.info("Setting the horizontal/vertical distance between each receiver point to " + receiverPointGap);
		this.receiverPointGap = receiverPointGap;
	}

	public void setRelevantRadius(double relevantRadius) {
		log.info("Setting the radius of relevant links around each receiver point to " + relevantRadius);
		this.relevantRadius = relevantRadius;
	}
	
	public void setHgvIdPrefix(String hgvIdPrefix) {
		log.info("Setting the HGV Id Prefix to " + hgvIdPrefix);
		this.hgvIdPrefix = hgvIdPrefix;
	}

	public void setTunnelLinkIDs(List<Id<Link>> tunnelLinkIDs) {
		log.info("Setting tunnel link IDs to " + tunnelLinkIDs.toString());
		this.tunnelLinkIDs = tunnelLinkIDs;
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

	public String[] getConsideredActivitiesForDamages() {		
		return consideredActivitiesForDamages;
	}

	public void setConsideredActivitiesForDamages(String[] consideredActivities) {
		log.info("Setting considered activities to: ");
		for (int i = 0; i < consideredActivities.length; i++) {
			log.info(consideredActivities[i]);
		}
		this.consideredActivitiesForDamages = consideredActivities;
	}
	
	public double getAnnualCostRate() {
		return annualCostRate;
	}
	
	public double getTimeBinSizeNoiseComputation() {
		return timeBinSizeNoiseComputation;
	}
	
	public double getScaleFactor() {
		return scaleFactor;
	}
	
	public double getReceiverPointGap() {
		return receiverPointGap;
	}
	
	public double getRelevantRadius() {
		return relevantRadius;
	}

	public String getHgvIdPrefix() {
		return hgvIdPrefix;
	}

	public List<Id<Link>> getTunnelLinkIDs() {
		return tunnelLinkIDs;
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

	public boolean isInternalizeNoiseDamages() {
		return internalizeNoiseDamages;
	}

	public void setInternalizeNoiseDamages(boolean internalizeNoiseDamages) {
		log.info("Internalizing noise damages: " + internalizeNoiseDamages);
		this.internalizeNoiseDamages = internalizeNoiseDamages;
	}

	public boolean isComputeNoiseDamages() {
		return computeNoiseDamages;
	}

	public void setComputeNoiseDamages(boolean computeNoiseDamages) {
		log.info("Computing noise damages: " + computeNoiseDamages);
		this.computeNoiseDamages = computeNoiseDamages;
	}
	
}
