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
import java.util.Map;
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
public class NoiseConfigGroup extends ReflectiveConfigGroup {
	
	public static final String GROUP_NAME = "noise";
	
	public NoiseConfigGroup() {
		super(GROUP_NAME);
	}
	
	private static final Logger log = Logger.getLogger(NoiseConfigGroup.class);

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
		
	private String[] hgvIdPrefixes = { "lkw" };
	private Set<String> busIdIdentifier = new HashSet<String>();
	private Set<Id<Link>> tunnelLinkIDs = new HashSet<Id<Link>>();
	
	// ########################################################################################################
	
	@Override
	public Map<String, String> getComments() {
		Map<String, String> comments = super.getComments();
		comments.put("annualCostRate", "annual noise cost rate [in EUR per exposed pulation unit]; following the German EWS approach" ) ;
		comments.put("timeBinSizeNoiseComputation", "Specifies the temporal resolution, i.e. the time bin size [in seconds] to compute noise levels." ) ;
		comments.put("scaleFactor", "Set to '1.' for a 100 percent sample size. Set to '10.' for a 10 percent sample size. Set to '100.' for a 1 percent sample size." ) ;
		comments.put("relevantRadius", "Specifies the radius [in coordinate units] around each receiver point links are taken into account." ) ;
		comments.put("tunnelLinkIdFile", "Specifies a csv file which contains all tunnel link IDs." ) ;
		comments.put("tunnelLinkIDs", "Specifies a list of comma-seperated tunnel link IDs. Will be ignored in case a the tunnel link IDs are provided as file (see parameter 'tunnelLinkIdFile')." ) ;

		comments.put("writeOutputIteration", "Specifies how often the noise-specific output is written out." ) ;
		comments.put("useActualSpeedLevel", "Set to 'true' if the actual speed level should be used to compute noise levels. Set to 'false' if the freespeed level should be used to compute noise levels." ) ;
		comments.put("allowForSpeedsOutsideTheValidRange", "Set to 'true' if speed levels below 30 km/h or above 80 km/h (HGV) / 130 km/h (car) should be used to compute noise levels. Set to 'false' if speed levels outside of the valid range should not be used to compute noise levels (recommended)." ) ;
		
		comments.put("throwNoiseEventsAffected", "Set to 'true' if noise events (providing information about the affected agent) should be thrown. Otherwise set to 'false'." ) ;
		comments.put("computeNoiseDamages", "Set to 'true' if noise damages should be computed. Otherwise set to 'false'." ) ;
		comments.put("computeCausingAgents", "Set to 'true' if the noise damages should be traced back and a causing agent should be identified. Otherwise set to 'false'." ) ;
		comments.put("throwNoiseEventsCaused", "Set to 'true' if noise events (providing information about the causing agent) should be thrown. Otherwise set to 'false'." ) ;
		comments.put("computePopulationUnits", "Set to 'true' if population densities should be computed. Otherwise set to 'false'." ) ;

		comments.put("hgvIdPrefixes", "Specifies the HGV (heavy goods vehicles, trucks) ID prefix." ) ;
		comments.put("busIdIdentifier", "Specifies the public transit vehicle ID identifiers. Buses are treated as HGV, other public transit vehicles are neglected." ) ;

		
		return comments;
	}

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
		log.info("setting the annual cost rate to " + annualCostRate);
		this.annualCostRate = annualCostRate;
	}

	@StringSetter( "timeBinSizeNoiseComputation" )
	public void setTimeBinSizeNoiseComputation(double timeBinSizeNoiseComputation) {
		log.info("setting the time bin size for the computation of noise to " + timeBinSizeNoiseComputation);
		this.timeBinSizeNoiseComputation = timeBinSizeNoiseComputation;
	}

	@StringSetter( "scaleFactor" )
	public void setScaleFactor(double scaleFactor) {
		log.info("setting the scale factor to " + scaleFactor);
		this.scaleFactor = scaleFactor;
	}

	@StringSetter( "relevantRadius" )
	public void setRelevantRadius(double relevantRadius) {
		log.info("setting the radius of relevant links around each receiver point to " + relevantRadius);
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
		log.info("setting file which contains the tunnel link Ids to " + tunnelLinkIdFile + ".");
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
		return CollectionUtils.arrayToString(hgvIdPrefixes);
	}

	@StringSetter( "hgvIdPrefixes" )
	public void setHgvIdPrefixes(String hgvIdPrefixes) {		
		this.setHgvIdPrefixesArray(CollectionUtils.stringToArray(hgvIdPrefixes));
	}

	@StringGetter( "busIdIdentifier" )
	private String getBusIdPrefixes() {
		return CollectionUtils.setToString(busIdIdentifier);
	}

	@StringSetter( "busIdIdentifier" )
	public void setBusIdIdentifiers(String busIdPrefixes) {		
		this.setBusIdIdentifierSet(CollectionUtils.stringToSet(busIdPrefixes));
	}

	@StringGetter( "tunnelLinkIDs" )
	private String getTunnelLinkIDs() {
		return this.linkIdSetToString(tunnelLinkIDs);
	}

	@StringSetter( "tunnelLinkIDs" )
	public void setTunnelLinkIDs(String tunnelLinkIDs) {		
		this.setTunnelLinkIDsSet(stringToLinkIdSet(tunnelLinkIDs));
	}

	public void setHgvIdPrefixesArray(String[] hgvIdPrefix) {
		log.info("setting the HGV Id Prefixes to " + hgvIdPrefix.toString());
		this.hgvIdPrefixes = hgvIdPrefix;
	}

	public void setTunnelLinkIDsSet(Set<Id<Link>> tunnelLinkIDs) {
		log.info("setting tunnel link IDs to " + tunnelLinkIDs.toString());
		this.tunnelLinkIDs = tunnelLinkIDs;
	}
	
	public String[] getHgvIdPrefixesArray() {
		return hgvIdPrefixes;
	}

	public Set<Id<Link>> getTunnelLinkIDsSet() {
		return tunnelLinkIDs;
	}
	
	public Set<String> getBusIdIdentifierSet() {
		return busIdIdentifier;
	}

	public void setBusIdIdentifierSet(Set<String> busIdPrefixes) {
		log.info("setting the bus Id identifiers to : " + busIdPrefixes.toString());
		this.busIdIdentifier = busIdPrefixes;
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
