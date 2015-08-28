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


import java.io.BufferedReader;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.utils.io.IOUtils;

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
	private Set<String> hgvIdPrefixes = new HashSet<String>();
	private Set<String> busIdPrefixes = new HashSet<String>();
	private Set<Id<Link>> tunnelLinkIDs = new HashSet<Id<Link>>();
	private String tunnelLinkIdFile = null;
	private int writeOutputIteration = 1;
	private boolean useActualSpeedLevel = true;
	private boolean allowForSpeedsOutsideTheValidRange = false;
	
	private boolean throwNoiseEventsAffected = true;
	private boolean computeNoiseDamages = true;
	private boolean internalizeNoiseDamages = true;
	private boolean computeCausingAgents = true; 
	private boolean throwNoiseEventsCaused = true;
	private boolean computePopulationUnits = true;
	
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
			
			if (this.computePopulationUnits == false) {
				log.warn("Inconsistent parameters will be adjusted:");
				this.setComputePopulationUnits(true);
			}
		}
		
		if (this.computeNoiseDamages) {
		
			// required			
			if (this.computePopulationUnits == false) {
				log.warn("Inconsistent parameters will be adjusted:");
				this.setComputePopulationUnits(true);
			}
		}
		
		if (this.throwNoiseEventsCaused) {
			
			// required
			if (this.computeCausingAgents == false) {
				log.warn("Inconsistent parameters will be adjusted:");
				this.setComputeCausingAgents(true);
			}
		}
		
		if (this.tunnelLinkIdFile != null && this.tunnelLinkIdFile != "") {
			
			if (this.tunnelLinkIDs.size() > 0) {
				log.warn("Loading the tunnel link IDs from a file. Deleting the existing tunnel link IDs that are added manually.");
				this.tunnelLinkIDs.clear();
			}
			
			// loading tunnel link IDs from file
			BufferedReader br = IOUtils.getBufferedReader(this.tunnelLinkIdFile);
			
			String line = null;
			try {
				line = br.readLine();
			} catch (IOException e) {
				e.printStackTrace();
			} // headers

			log.info("Reading tunnel link Id file...");
			try {
				int countWarning = 0;
				while ((line = br.readLine()) != null) {
					
					String[] columns = line.split(";");
					Id<Link> linkId = null;
					for (int column = 0; column < columns.length; column++) {
						if (column == 0) {
							linkId = Id.createLinkId(columns[column]);
						} else {
							if (countWarning < 1) {
								log.warn("Expecting the tunnel link Id to be in the first column. Ignoring further columns...");
							} else if (countWarning == 1) {
								log.warn("This message is only given once.");
							}
							countWarning++;
						}						
					}
					log.info("Adding tunnel link ID " + linkId);
					this.tunnelLinkIDs.add(linkId);
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
			
			log.info("Reading tunnel link Id file... Done.");
		}
		
		if (this.useActualSpeedLevel && this.allowForSpeedsOutsideTheValidRange) {
			log.warn("Using the actual vehicle speeds for the noise computation may result in very low speed levels due to congestion."
					+ " The RLS computation approach defines a range of valid speed levels: for cars: 30-130 km/h; for HGV: 30-80 km/h."
					+ " 20 km/h or 10 km/h may still result in an 'okay' estimate of the traffic noise. However, 1 km/h or lower speeds will definitly make no sense."
					+ " It is therefore recommended not to use speeds outside of the range of valid parameters!");
		}
	}
	
	public NoiseParameters() {
		this.hgvIdPrefixes.add("lkw");
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
	
	public void setHgvIdPrefixes(Set<String> hgvIdPrefix) {
		log.info("Setting the HGV Id Prefixes to " + hgvIdPrefix.toString());
		this.hgvIdPrefixes = hgvIdPrefix;
	}

	public void setTunnelLinkIDs(Set<Id<Link>> tunnelLinkIDs) {
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

	public Set<String> getHgvIdPrefixes() {
		return hgvIdPrefixes;
	}

	public Set<Id<Link>> getTunnelLinkIDs() {
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

	public int getWriteOutputIteration() {
		return writeOutputIteration;
	}

	public void setWriteOutputIteration(int writeOutputIteration) {
		log.info("Writing output every " + writeOutputIteration + " iteration.");
		this.writeOutputIteration = writeOutputIteration;
	}

	public void setTunnelLinkIdFile(String tunnelLinkIdFile) {
		log.info("Setting file which contains the tunnel link Ids to " + tunnelLinkIdFile + ".");
		this.tunnelLinkIdFile = tunnelLinkIdFile;
	}

	public boolean isUseActualSpeedLevel() {
		return useActualSpeedLevel;
	}

	public void setUseActualSpeedLevel(boolean useActualSpeedLevel) {
		log.info("Using the actual speed level for noise calculation: " + useActualSpeedLevel);
		this.useActualSpeedLevel = useActualSpeedLevel;
	}

	public Set<String> getBusIdPrefixes() {
		return busIdPrefixes;
	}

	public void setBusIdPrefixes(Set<String> busIdPrefixes) {
		log.info("Setting the bus Id prefixes to : " + busIdPrefixes.toString());
		this.busIdPrefixes = busIdPrefixes;
	}

	public boolean isComputePopulationUnits() {
		return computePopulationUnits;
	}

	public void setComputePopulationUnits(boolean computePopulationUnits) {
		log.info("Computing population units: " + computePopulationUnits);
		this.computePopulationUnits = computePopulationUnits;
	}

	public boolean isAllowForSpeedsOutsideTheValidRange() {
		return allowForSpeedsOutsideTheValidRange;
	}

	public void setAllowForSpeedsOutsideTheValidRange(boolean allowForSpeedsOutsideTheValidRange) {
		log.info("Allowing for speeds above or below the valid range (cars: 30-130 km/h; HGV: 30-80 km/h): " + allowForSpeedsOutsideTheValidRange);
		this.allowForSpeedsOutsideTheValidRange = allowForSpeedsOutsideTheValidRange;
	}
	
}
