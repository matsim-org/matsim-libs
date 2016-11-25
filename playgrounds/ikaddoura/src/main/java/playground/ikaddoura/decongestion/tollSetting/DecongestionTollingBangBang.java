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

import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;

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
	private final DecongestionInfo congestionInfo;
	
	public DecongestionTollingBangBang(DecongestionInfo congestionInfo) {
		this.congestionInfo = congestionInfo;		
	}

	@Override
	public void updateTolls(int iteration) {
	
		for (Id<Link> linkId : this.congestionInfo.getlinkInfos().keySet()) {
			for (Integer intervalNr : this.congestionInfo.getlinkInfos().get(linkId).getTime2avgDelay().keySet()) {

				double averageDelay = this.congestionInfo.getlinkInfos().get(linkId).getTime2avgDelay().get(intervalNr);	
				
				if (averageDelay <= this.congestionInfo.getDecongestionConfigGroup().getTOLERATED_AVERAGE_DELAY_SEC()) {
					
					// no delay --> decrease the toll
										
					if (this.congestionInfo.getlinkInfos().get(linkId).getTime2toll().containsKey(intervalNr)) {
						Map<Integer, Double> time2toll = this.congestionInfo.getlinkInfos().get(linkId).getTime2toll();

						double updatedToll = this.congestionInfo.getlinkInfos().get(linkId).getTime2toll().get(intervalNr) 
								- this.congestionInfo.getDecongestionConfigGroup().getTOLL_ADJUSTMENT();

						if (updatedToll < 0.) {
							updatedToll = 0.;
							time2toll.remove(intervalNr);
						}
						
						if (updatedToll > 0) {
							time2toll.put(intervalNr, updatedToll);
						}
						
					}
					
				} else {
					
					// delay --> increase the toll
										
					if (this.congestionInfo.getlinkInfos().get(linkId).getTime2toll().containsKey(intervalNr)) {
						
						// not the initial toll
						double updatedToll = this.congestionInfo.getlinkInfos().get(linkId).getTime2toll().get(intervalNr) 
								+ this.congestionInfo.getDecongestionConfigGroup().getTOLL_ADJUSTMENT();
						Map<Integer, Double> time2toll = this.congestionInfo.getlinkInfos().get(linkId).getTime2toll();
						time2toll.put(intervalNr, updatedToll);	
						
					} else {
						
						// initial toll
						double updatedToll = this.congestionInfo.getDecongestionConfigGroup().getINITIAL_TOLL();
						Map<Integer, Double> time2toll = this.congestionInfo.getlinkInfos().get(linkId).getTime2toll();
						time2toll.put(intervalNr, updatedToll);	
					}
				}
			}
		}
		
		log.info("Updating tolls completed.");
	}
}

