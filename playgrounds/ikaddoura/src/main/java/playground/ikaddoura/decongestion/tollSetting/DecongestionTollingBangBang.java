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

package playground.ikaddoura.decongestion.tollSetting;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;

import com.google.inject.Inject;

import playground.ikaddoura.decongestion.data.DecongestionInfo;

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
	
		for (Id<Link> linkId : this.congestionInfo.getlinkInfos().keySet()) {
			for (Integer intervalNr : this.congestionInfo.getlinkInfos().get(linkId).getTime2avgDelay().keySet()) {

				double averageDelay = this.congestionInfo.getlinkInfos().get(linkId).getTime2avgDelay().get(intervalNr);	
				
				double toll = 0.;
				
				// 1) increase / decrease the toll per link and time bin
				
				if (averageDelay <= this.congestionInfo.getDecongestionConfigGroup().getTOLERATED_AVERAGE_DELAY_SEC()) {	
					if (this.congestionInfo.getlinkInfos().get(linkId).getTime2toll().get(intervalNr) != null) {
						toll = this.congestionInfo.getlinkInfos().get(linkId).getTime2toll().get(intervalNr) 
								- this.congestionInfo.getDecongestionConfigGroup().getTOLL_ADJUSTMENT();
					}					
				} else {
					if (this.congestionInfo.getlinkInfos().get(linkId).getTime2toll().get(intervalNr) != null) {
						toll = this.congestionInfo.getlinkInfos().get(linkId).getTime2toll().get(intervalNr) 
								+ this.congestionInfo.getDecongestionConfigGroup().getTOLL_ADJUSTMENT();
					} else {
						toll = this.congestionInfo.getDecongestionConfigGroup().getINITIAL_TOLL();
					}
				}

				// 2) prevent negative tolls
				
				if (toll < 0.) {
					toll = 0.;
				}
				
				// 3) smoothen the tolls
				
				double previousToll = Double.NEGATIVE_INFINITY;
				if (this.congestionInfo.getlinkInfos().get(linkId).getTime2toll().get(intervalNr) != null) {
					previousToll = this.congestionInfo.getlinkInfos().get(linkId).getTime2toll().get(intervalNr);
				}
				
				double blendFactor = Double.NEGATIVE_INFINITY;
				if (this.congestionInfo.getDecongestionConfigGroup().isMsa()) {
					if (this.tollUpdateCounter > 0) {
						blendFactor = 1.0 / (double) this.tollUpdateCounter;
					} else {
						blendFactor = 1.0;
					}
				} else {
					blendFactor = this.congestionInfo.getDecongestionConfigGroup().getTOLL_BLEND_FACTOR();
				}
				
				double smoothedToll = Double.NEGATIVE_INFINITY;
				if (previousToll >= 0.) {
					smoothedToll = toll * blendFactor + previousToll * (1 - blendFactor);
				} else {
					smoothedToll = toll;
				}
				
				// 4) store the updated toll
				
				if (toll > 0) this.congestionInfo.getlinkInfos().get(linkId).getTime2toll().put(intervalNr, smoothedToll);
			}
		}
		
		log.info("Updating tolls completed.");
		this.tollUpdateCounter++;
	}
}

