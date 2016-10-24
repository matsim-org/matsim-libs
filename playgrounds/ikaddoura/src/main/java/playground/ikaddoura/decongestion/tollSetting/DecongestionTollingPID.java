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

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;

import playground.ikaddoura.decongestion.data.DecongestionInfo;
import playground.ikaddoura.decongestion.data.LinkInfo;

/**
 * 
 * PDI-based toll adjustment, where e(t) = average delay
 * 
 * @author ikaddoura
 */

public class DecongestionTollingPID implements DecongestionTollSetting {
	private static final Logger log = Logger.getLogger(DecongestionTollingPID.class);
	
	private final DecongestionInfo congestionInfo;
	private Map<Id<Link>, LinkInfo> linkId2infoPreviousTollComputation = new HashMap<>();
	
	private double K_p = Double.NaN;
	private double K_i = Double.NaN;
	private double K_d = Double.NaN;
	
	public DecongestionTollingPID(DecongestionInfo congestionInfo) {
		this.congestionInfo = congestionInfo;
		K_p = congestionInfo.getDecongestionConfigGroup().getKp();
		K_i = congestionInfo.getDecongestionConfigGroup().getKi();
		K_d = congestionInfo.getDecongestionConfigGroup().getKd();
	}

	@Override
	public void updateTolls() {
	
		for (Id<Link> linkId : this.congestionInfo.getlinkInfos().keySet()) {
			for (Integer intervalNr : this.congestionInfo.getlinkInfos().get(linkId).getTime2avgDelay().keySet()) {
				
				// 0) compute e(t) = average delay
				double averageDelay = this.congestionInfo.getlinkInfos().get(linkId).getTime2avgDelay().get(intervalNr);	
				if (averageDelay <= this.congestionInfo.getDecongestionConfigGroup().getTOLERATED_AVERAGE_DELAY_SEC()) {
					averageDelay = 0.0;
				}
								
				double toll = 0.;
				
				// 1) proportional term
				toll += this.K_p * averageDelay;
		
				// 2) integral term
				double totalDelay = 0.;
				if (this.congestionInfo.getlinkInfos().get(linkId).getTime2value().containsKey(intervalNr)) {
					// artificial reset to zero if congestion is eliminated
					if (averageDelay <= congestionInfo.getDecongestionConfigGroup().getTOLERATED_AVERAGE_DELAY_SEC()) {
						totalDelay = 0.0;
					} else {
						totalDelay = this.congestionInfo.getlinkInfos().get(linkId).getTime2value().get(intervalNr) + averageDelay;
					}
					this.congestionInfo.getlinkInfos().get(linkId).getTime2value().put(intervalNr, totalDelay);
				} else {
					totalDelay = averageDelay;
					this.congestionInfo.getlinkInfos().get(linkId).getTime2value().put(intervalNr, totalDelay);
				}
				toll += this.K_i * totalDelay;

				// 3) derivative term
				double previousDelay = 0.;
				if (this.linkId2infoPreviousTollComputation.containsKey(linkId) && this.linkId2infoPreviousTollComputation.get(linkId).getTime2avgDelay().containsKey(intervalNr)) {
					previousDelay = this.linkId2infoPreviousTollComputation.get(linkId).getTime2avgDelay().get(intervalNr);
				}
			
				double deltaDelay = averageDelay - previousDelay;
				toll += this.K_d * deltaDelay;
				
				// 4) prevent negative tolls
				if (toll < 0) {
					toll = 0;
				}
				
				// 5) smoothen the tolls
				double previousToll = Double.NEGATIVE_INFINITY;
				if (this.congestionInfo.getlinkInfos().get(linkId).getTime2toll().containsKey(intervalNr)) {
					previousToll = this.congestionInfo.getlinkInfos().get(linkId).getTime2toll().get(intervalNr);
				}
				double smoothedToll = Double.NEGATIVE_INFINITY;
				if (previousToll >= 0.) {
					smoothedToll = toll * this.congestionInfo.getDecongestionConfigGroup().getTOLL_BLEND_FACTOR() + previousToll * (1 - this.congestionInfo.getDecongestionConfigGroup().getTOLL_BLEND_FACTOR());
				} else {
					smoothedToll = toll;
				}
				
				// 6) store the updated toll
				Map<Integer, Double> time2toll = this.congestionInfo.getlinkInfos().get(linkId).getTime2toll();
				time2toll.put(intervalNr, smoothedToll);
				
//				if (intervalNr == 117) {
//					log.warn("link: " + linkId + " / time interval: " + intervalNr);
//					log.warn("average delay: " + averageDelay);
//					log.warn("total delay: " + totalDelay);
//					log.warn("previous delay: " + previousDelay);			
//					log.warn("delta delay: " + deltaDelay);		
//					log.warn("toll: " + smoothedToll);
//				}
			}
		}
		
		log.info("Updating tolls completed.");
		
		// store the current link information for the next toll computation
		
		linkId2infoPreviousTollComputation = new HashMap<>();
		for (Id<Link> linkId : this.congestionInfo.getlinkInfos().keySet()) {

			Map<Integer, Double> time2previousDelay = new HashMap<>();
			for (Integer intervalNr : this.congestionInfo.getlinkInfos().get(linkId).getTime2avgDelay().keySet()) {
				time2previousDelay.put(intervalNr, this.congestionInfo.getlinkInfos().get(linkId).getTime2avgDelay().get(intervalNr));
			}
			
			LinkInfo linkInfoPreviousTollComputation = new LinkInfo(linkId);
			linkInfoPreviousTollComputation.setTime2avgDelay(time2previousDelay);
			linkId2infoPreviousTollComputation.put(linkId, linkInfoPreviousTollComputation);
		}
	}
}

