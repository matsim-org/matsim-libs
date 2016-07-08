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
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;

import playground.ikaddoura.decongestion.data.DecongestionInfo;
import playground.ikaddoura.decongestion.data.LinkInfo;

/**
 * 
 * Initial tolls
 * ... are set based on the average delay per link and time bin (= d).
 * 
 * Tolls in all further iterations
 * ... are recomputed
 * - If d > threshold: Compare the current delay d(t) with the delay when previously computing the tolls d(t-1).
 * 			- If d(t) >= d(t-1): Increase the link and time specific toll weight
 * 			- If d(t) < d(t-1): Decrease the link and time specific toll weight
 *   
 * - If d <= threshold: Set toll to zero.
 * 
 * @author ikaddoura
 */

public class DecongestionTollingV8 implements DecongestionTollSetting {
	
	private static final Logger log = Logger.getLogger(DecongestionTollingV8.class);

	private final DecongestionInfo congestionInfo;
	private final double vtts_hour;

	private Map<Id<Link>, LinkInfo> linkId2infoPreviousTollComputation = new HashMap<>();
	
	public DecongestionTollingV8(DecongestionInfo congestionInfo) {
		this.congestionInfo = congestionInfo;
		this.vtts_hour = (this.congestionInfo.getScenario().getConfig().planCalcScore().getPerforming_utils_hr() - this.congestionInfo.getScenario().getConfig().planCalcScore().getModes().get(TransportMode.car).getMarginalUtilityOfTraveling()) / this.congestionInfo.getScenario().getConfig().planCalcScore().getMarginalUtilityOfMoney();
		log.info("VTTS [monetary units / hour]: " + this.vtts_hour);
	}

	@Override
	public void updateTolls() {
	
		for (Id<Link> linkId : this.congestionInfo.getlinkInfos().keySet()) {
			for (Integer intervalNr : this.congestionInfo.getlinkInfos().get(linkId).getTime2avgDelay().keySet()) {

				double averageDelay = this.congestionInfo.getlinkInfos().get(linkId).getTime2avgDelay().get(intervalNr);
				if (averageDelay <= this.congestionInfo.getDecongestionConfigGroup().getTOLERATED_AVERAGE_DELAY_SEC()) {
					averageDelay = 0.;
				} 
				
				if (averageDelay > 0) {
					
					// 1) adjust the weight
					
					double weight = 0.;					
					if (this.congestionInfo.getlinkInfos().get(linkId).getTime2toll().containsKey(intervalNr)) {
													
						double previousDelay = linkId2infoPreviousTollComputation.get(linkId).getTime2avgDelay().get(intervalNr);
						log.info("Previous delay: " + previousDelay + " / Current delay: " + averageDelay);
						
						weight = this.congestionInfo.getlinkInfos().get(linkId).getTime2weight().get(intervalNr);

						if (averageDelay >= previousDelay) {
							
							// increase the cost
							
							log.info("Old weight: " + weight);
							
							weight += this.congestionInfo.getDecongestionConfigGroup().getTOLL_ADJUSTMENT();						
							this.congestionInfo.getlinkInfos().get(linkId).getTime2weight().put(intervalNr, weight);
							
							log.info("New weight: " + weight);
							
						} else {
							// do not (further) increase the weight
						}
											
					} else {
						// initial toll, no need to consider the weight						
					}
					
					this.congestionInfo.getlinkInfos().get(linkId).getTime2weight().put(intervalNr, weight);
					
					// 2) compute the toll
					
					double averageDelayBasedCostToll = (1 + weight) * averageDelay * vtts_hour / 3600.;

					double previousToll = Double.NEGATIVE_INFINITY;
					if (this.congestionInfo.getlinkInfos().get(linkId).getTime2toll().containsKey(intervalNr)) {
						previousToll = this.congestionInfo.getlinkInfos().get(linkId).getTime2toll().get(intervalNr);
					}
						
					double toll = Double.NEGATIVE_INFINITY;
					if (previousToll > 0.) {
						toll = averageDelayBasedCostToll * this.congestionInfo.getDecongestionConfigGroup().getTOLL_BLEND_FACTOR() + previousToll * (1 - this.congestionInfo.getDecongestionConfigGroup().getTOLL_BLEND_FACTOR());
					} else {
						toll = averageDelayBasedCostToll;
					}
					
					log.info("Previous toll: " + previousToll + " // Average delay based cost toll: " + averageDelayBasedCostToll + " // Next toll: " + toll);
					
					this.congestionInfo.getlinkInfos().get(linkId).getTime2toll().put(intervalNr, toll);
	
					
				} else {
					
					// average delay is below threshold
					
					// do not change the tolls vs. set tolls and weights to zero?
					this.congestionInfo.getlinkInfos().get(linkId).getTime2weight().remove(intervalNr);
					this.congestionInfo.getlinkInfos().get(linkId).getTime2toll().remove(intervalNr);

				}
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

