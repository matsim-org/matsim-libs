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

import playground.ikaddoura.noise2.data.NoiseAllocationApproach;


/**
 * Provides the parameters required to compute noise emissions, immissions and damages.
 * 
 * @author ikaddoura
 *
 */
public class NoiseParameters {
	
	private static final Logger log = Logger.getLogger(NoiseParameters.class);

	private double annualCostRate = (85.0/(1.95583)) * (Math.pow(1.02, (2014-1995)));
	private double timeBinSizeNoiseComputation = 3600.0;
	private double scaleFactor = 1.;
	private double relevantRadius = 500.;
	private String hgvIdPrefix = "lkw";
	private List<Id<Link>> tunnelLinkIDs = new ArrayList<Id<Link>>();
	
	private boolean throwNoiseEventsAffected = true;
	private boolean computeNoiseDamages = true;
	private boolean internalizeNoiseDamages = true;
	private boolean computeCausingAgents = true; 
	private boolean throwNoiseEventsCaused = true;
	
	private NoiseAllocationApproach noiseAllocationApproach = NoiseAllocationApproach.AverageCost;
		
	// ########################################################################################################
			
	public void checkForConsistency() {
		
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
	
	public double getAnnualCostRate() {
		return annualCostRate;
	}
	
	public double getTimeBinSizeNoiseComputation() {
		return timeBinSizeNoiseComputation;
	}
	
	public double getScaleFactor() {
		return scaleFactor;
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

	public NoiseAllocationApproach getNoiseAllocationApproach() {
		return noiseAllocationApproach;
	}

	public void setNoiseAllocationApproach(NoiseAllocationApproach noiseAllocationApproach) {
		log.info("Noise allocation approach: " + noiseAllocationApproach);
		this.noiseAllocationApproach = noiseAllocationApproach;
	}
	
}
