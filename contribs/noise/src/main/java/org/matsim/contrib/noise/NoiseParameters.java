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
package org.matsim.contrib.noise;


import java.io.BufferedReader;
import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.noise.data.NoiseAllocationApproach;
import org.matsim.core.config.ReflectiveConfigGroup;
import org.matsim.core.utils.collections.CollectionUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.StringUtils;


/**
 * Provides the parameters required to compute noise emissions, immissions and damages.
 * 
 * @author ikaddoura
 *
 */
public class NoiseParameters extends ReflectiveConfigGroup {
	
	public static final String GROUP_NAME = "noise";
	
	public NoiseParameters() {
		super(GROUP_NAME);
		
		// default
		this.hgvIdPrefixesSet.add("lkw");
	}
	
	private static final Logger log = Logger.getLogger(NoiseParameters.class);

	private double annualCostRate = (85.0/(1.95583)) * (Math.pow(1.02, (2014-1995)));
	private double timeBinSizeNoiseComputation = 3600.0;
	private double scaleFactor = 1.;
	private double relevantRadius = 500.;
	private String tunnelLinkIdFile = null;
	private int writeOutputIteration = 10;
	private boolean useActualSpeedLevel = true;
	private boolean allowForSpeedsOutsideTheValidRange = false;
	
	private boolean throwNoiseEventsAffected = true;
	private boolean computeNoiseDamages = true;
	private boolean internalizeNoiseDamages = true;
	private boolean computeCausingAgents = true; 
	private boolean throwNoiseEventsCaused = true;
	private boolean computePopulationUnits = true;
	
	private NoiseAllocationApproach noiseAllocationApproach = NoiseAllocationApproach.AverageCost;
		
	private Set<String> hgvIdPrefixesSet = new HashSet<String>();
	private Set<String> busIdPrefixesSet = new HashSet<String>();
	private Set<Id<Link>> tunnelLinkIDsSet = new HashSet<Id<Link>>();

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
			
			if (this.tunnelLinkIDsSet.size() > 0) {
				log.warn("Loading the tunnel link IDs from a file. Deleting the existing tunnel link IDs that are added manually.");
				this.tunnelLinkIDsSet.clear();
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
					this.tunnelLinkIDsSet.add(linkId);
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

	// ########################################################################################################

	@StringGetter( "throwNoiseEventsAffected" )
	public boolean isThrowNoiseEventsAffected() {
		return throwNoiseEventsAffected;
	}

	@StringSetter( "throwNoiseEventsAffected" )
	public void setThrowNoiseEventsAffected(boolean throwNoiseEventsAffected) {
		log.info("Throwing noise events for the affected agents: " + throwNoiseEventsAffected);
		this.throwNoiseEventsAffected = throwNoiseEventsAffected;
	}

	@StringGetter( "throwNoiseEventsCaused" )
	public boolean isThrowNoiseEventsCaused() {
		return throwNoiseEventsCaused;
	}

	@StringSetter( "throwNoiseEventsCaused" )
	public void setThrowNoiseEventsCaused(boolean throwNoiseEventsCaused) {
		log.info("Throwing noise events for the causing agents: " + throwNoiseEventsCaused);
		this.throwNoiseEventsCaused = throwNoiseEventsCaused;
	}

	@StringGetter( "computeCausingAgents" )
	public boolean isComputeCausingAgents() {
		return computeCausingAgents;
	}
	
	@StringSetter( "computeCausingAgents" )
	public void setComputeCausingAgents(boolean computeCausingAgents) {
		log.info("Allocating the noise damages to the causing agents: " + computeCausingAgents);
		this.computeCausingAgents = computeCausingAgents;
	}
	
	@StringSetter( "annualCostRate" )
	public void setAnnualCostRate(double annualCostRate) {
		log.info("Setting the annual cost rate to " + annualCostRate);
		this.annualCostRate = annualCostRate;
	}

	@StringSetter( "timeBinSizeNoiseComputation" )
	public void setTimeBinSizeNoiseComputation(double timeBinSizeNoiseComputation) {
		log.info("Setting the time bin size for the computation of noise to " + timeBinSizeNoiseComputation);
		this.timeBinSizeNoiseComputation = timeBinSizeNoiseComputation;
	}

	@StringSetter( "scaleFactor" )
	public void setScaleFactor(double scaleFactor) {
		log.info("Setting the scale factor to " + scaleFactor);
		this.scaleFactor = scaleFactor;
	}

	@StringSetter( "relevantRadius" )
	public void setRelevantRadius(double relevantRadius) {
		log.info("Setting the radius of relevant links around each receiver point to " + relevantRadius);
		this.relevantRadius = relevantRadius;
	}
	
	@StringGetter( "annualCostRate" )
	public double getAnnualCostRate() {
		return annualCostRate;
	}
	
	@StringGetter( "timeBinSizeNoiseComputation" )
	public double getTimeBinSizeNoiseComputation() {
		return timeBinSizeNoiseComputation;
	}
	
	@StringGetter( "scaleFactor" )
	public double getScaleFactor() {
		return scaleFactor;
	}
	
	@StringGetter( "relevantRadius" )
	public double getRelevantRadius() {
		return relevantRadius;
	}

	@StringGetter( "internalizeNoiseDamages" )
	public boolean isInternalizeNoiseDamages() {
		return internalizeNoiseDamages;
	}

	@StringSetter( "internalizeNoiseDamages" )
	public void setInternalizeNoiseDamages(boolean internalizeNoiseDamages) {
		log.info("Internalizing noise damages: " + internalizeNoiseDamages);
		this.internalizeNoiseDamages = internalizeNoiseDamages;
	}

	@StringGetter( "computeNoiseDamages" )
	public boolean isComputeNoiseDamages() {
		return computeNoiseDamages;
	}

	@StringSetter( "computeNoiseDamages" )
	public void setComputeNoiseDamages(boolean computeNoiseDamages) {
		log.info("Computing noise damages: " + computeNoiseDamages);
		this.computeNoiseDamages = computeNoiseDamages;
	}

	@StringGetter( "noiseAllocationApproach" )
	public NoiseAllocationApproach getNoiseAllocationApproach() {
		return noiseAllocationApproach;
	}

	@StringSetter( "noiseAllocationApproach" )
	public void setNoiseAllocationApproach(NoiseAllocationApproach noiseAllocationApproach) {
		log.info("Noise allocation approach: " + noiseAllocationApproach);
		this.noiseAllocationApproach = noiseAllocationApproach;
	}

	@StringGetter( "writeOutputIteration" )
	public int getWriteOutputIteration() {
		return writeOutputIteration;
	}

	@StringSetter( "writeOutputIteration" )
	public void setWriteOutputIteration(int writeOutputIteration) {
		log.info("Writing output every " + writeOutputIteration + " iteration.");
		this.writeOutputIteration = writeOutputIteration;
	}

	@StringSetter( "tunnelLinkIdFile" )
	public void setTunnelLinkIdFile(String tunnelLinkIdFile) {
		log.info("Setting file which contains the tunnel link Ids to " + tunnelLinkIdFile + ".");
		this.tunnelLinkIdFile = tunnelLinkIdFile;
	}
	
	@StringGetter( "tunnelLinkIdFile" )
	private String getTunnelLinkIdFile() {
		return tunnelLinkIdFile;
	}

	@StringGetter( "useActualSpeedLevel" )
	public boolean isUseActualSpeedLevel() {
		return useActualSpeedLevel;
	}
	
	@StringSetter( "useActualSpeedLevel" )
	public void setUseActualSpeedLevel(boolean useActualSpeedLevel) {
		log.info("Using the actual speed level for noise calculation: " + useActualSpeedLevel);
		this.useActualSpeedLevel = useActualSpeedLevel;
	}

	@StringGetter( "computePopulationUnits" )
	public boolean isComputePopulationUnits() {
		return computePopulationUnits;
	}

	@StringSetter( "computePopulationUnits" )
	public void setComputePopulationUnits(boolean computePopulationUnits) {
		log.info("Computing population units: " + computePopulationUnits);
		this.computePopulationUnits = computePopulationUnits;
	}

	@StringGetter( "allowForSpeedsOutsideTheValidRange" )
	public boolean isAllowForSpeedsOutsideTheValidRange() {
		return allowForSpeedsOutsideTheValidRange;
	}
	
	@StringSetter( "allowForSpeedsOutsideTheValidRange" )
	public void setAllowForSpeedsOutsideTheValidRange(boolean allowForSpeedsOutsideTheValidRange) {
		log.info("Allowing for speeds above or below the valid range (cars: 30-130 km/h; HGV: 30-80 km/h): " + allowForSpeedsOutsideTheValidRange);
		this.allowForSpeedsOutsideTheValidRange = allowForSpeedsOutsideTheValidRange;
	}

	// #######
	
	@StringGetter( "hgvIdPrefixes" )
	private String getHgvIdPrefixes() {
		return CollectionUtils.setToString(hgvIdPrefixesSet);
	}

	@StringSetter( "hgvIdPrefixes" )
	public void setHgvIdPrefixes(String hgvIdPrefixes) {		
		this.setHgvIdPrefixesSet(CollectionUtils.stringToSet(hgvIdPrefixes));
	}

	@StringGetter( "busIdPrefixes" )
	private String getBusIdPrefixes() {
		return CollectionUtils.setToString(busIdPrefixesSet);
	}

	@StringSetter( "busIdPrefixes" )
	public void setBusIdPrefixes(String busIdPrefixes) {		
		this.setBusIdPrefixesSet(CollectionUtils.stringToSet(busIdPrefixes));
	}

	@StringGetter( "tunnelLinkIDs" )
	private String getTunnelLinkIDs() {
		return this.linkIdSetToString(tunnelLinkIDsSet);
	}

	@StringSetter( "tunnelLinkIDs" )
	public void setTunnelLinkIDs(String tunnelLinkIDs) {		
		this.setTunnelLinkIDsSet(stringToLinkIdSet(tunnelLinkIDs));
	}

	public void setHgvIdPrefixesSet(Set<String> hgvIdPrefix) {
		log.info("Setting the HGV Id Prefixes to " + hgvIdPrefix.toString());
		this.hgvIdPrefixesSet = hgvIdPrefix;
	}

	public void setTunnelLinkIDsSet(Set<Id<Link>> tunnelLinkIDs) {
		log.info("Setting tunnel link IDs to " + tunnelLinkIDs.toString());
		this.tunnelLinkIDsSet = tunnelLinkIDs;
	}
	
	public Set<String> getHgvIdPrefixesSet() {
		return hgvIdPrefixesSet;
	}

	public Set<Id<Link>> getTunnelLinkIDsSet() {
		return tunnelLinkIDsSet;
	}
	
	public Set<String> getBusIdPrefixesSet() {
		return busIdPrefixesSet;
	}

	public void setBusIdPrefixesSet(Set<String> busIdPrefixes) {
		log.info("Setting the bus Id prefixes to : " + busIdPrefixes.toString());
		this.busIdPrefixesSet = busIdPrefixes;
	}
	
	private String linkIdSetToString (Set<Id<Link>> linkIds) {
		String linkIdsString = null;
		boolean first = true;
		for (Id<Link> id : linkIds) {
			if (first) {
				linkIdsString = id.toString();
				first = false;
			} else {
				linkIdsString = linkIdsString + "," + id;
			}
		}
		return linkIdsString;
	}
	
	private Set<Id<Link>> stringToLinkIdSet(String linkIds) {
		if (linkIds == null) {
			return Collections.emptySet();
		}
		String[] parts = StringUtils.explode(linkIds, ',');
		Set<Id<Link>> tmp = new LinkedHashSet<Id<Link>>();
		for (String part : parts) {
			String trimmed = part.trim();
			if (trimmed.length() > 0) {
				tmp.add(Id.createLinkId(trimmed.intern()));
			}
		}
		return tmp;
	}
}
