/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
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

package org.matsim.contrib.decongestion.tollSetting;

import org.apache.log4j.Logger;

import com.google.inject.Inject;

import org.matsim.contrib.decongestion.data.DecongestionInfo;
import org.matsim.contrib.decongestion.data.LinkInfo;

/**
 * 
 * - If d > threshold: Increase the toll: + adjustment value  
 * - If d <= threshold: Decrease the toll: - adjustment value
 * 
 * @author ikaddoura
 */

public class DecongestionTollingBangBang implements DecongestionTollSetting {
	
	private static final Logger log = Logger.getLogger(DecongestionTollingBangBang.class);
	private int tollUpdateCounter = 0;

	@Inject
	private DecongestionInfo congestionInfo;

	@Override
	public void updateTolls() {
	
		log.info("Number of links: " + this.congestionInfo.getlinkInfos().size());
		
		final double toleratedAverageDelay = this.congestionInfo.getDecongestionConfigGroup().getToleratedAverageDelaySec();
		final double tollAdjustment = this.congestionInfo.getDecongestionConfigGroup().getTollAdjustment();
		final double initialToll = this.congestionInfo.getDecongestionConfigGroup().getInitialToll();
		final boolean msa = this.congestionInfo.getDecongestionConfigGroup().isMsa();
		final double blendFactorFromConfig = this.congestionInfo.getDecongestionConfigGroup().getTollBlendFactor();
		
		for (LinkInfo linkInfo : this.congestionInfo.getlinkInfos().values()) {
			for (Integer intervalNr : linkInfo.getTime2avgDelay().keySet()) {

				Double previousToll = linkInfo.getTime2toll().get(intervalNr);
				double toll = 0.;
				
				// 1) increase / decrease the toll per link and time bin
				
				if (linkInfo.getTime2avgDelay().get(intervalNr) <= toleratedAverageDelay ) {	
					if (previousToll != null) {
						toll = previousToll - tollAdjustment * linkInfo.getLink().getLength()/1000. ;
					}		
					
				} else {
					if (previousToll != null) {
						toll = previousToll + tollAdjustment;
					} else {
						toll = initialToll * linkInfo.getLink().getLength()/1000. ;
					}
				}

				// 2) prevent negative tolls
				
				if (toll < 0.) {
					toll = 0.;
				}
				
				// 3) smoothen the tolls
				
				double blendFactor;
				if (msa) {
					if (this.tollUpdateCounter > 0) {
						blendFactor = 1.0 / (double) this.tollUpdateCounter;
					} else {
						blendFactor = 1.0;
					}
				} else {
					blendFactor = blendFactorFromConfig;
				}
				
				double smoothedToll;
				if (previousToll != null) {
					smoothedToll = toll * blendFactor + previousToll * (1 - blendFactor);
				} else {
					smoothedToll = toll;
				}
				
				// 4) store the updated toll
				
				linkInfo.getTime2toll().put(intervalNr, smoothedToll);
			}
		}
		
		log.info("Updating tolls completed.");
		this.tollUpdateCounter++;
	}
}

