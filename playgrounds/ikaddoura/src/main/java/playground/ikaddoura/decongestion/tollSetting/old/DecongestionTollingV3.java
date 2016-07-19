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

package playground.ikaddoura.decongestion.tollSetting.old;

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;

import playground.ikaddoura.decongestion.data.DecongestionInfo;
import playground.ikaddoura.decongestion.data.LinkInfo;
import playground.ikaddoura.decongestion.tollSetting.DecongestionTollSetting;

/**
 * 
 * Initial tolls
 * ... are _not_ set based on the average delay per link and time bin (= d), but set to the adjustment value.
 * 
 * Tolls in all further iterations
 * ... are recomputed
 * - If d > threshold: Compare the current delay d(t) with the delay when previously computing the tolls d(t-1).
 * 			- If d(t) >= d(t-1): Increase the toll by the adjustment value.
 * 			- If d(t) < d(t-1): Decrease the toll by the adjustment value.
 *   
 * - If d <= threshold: Keep the tolls as they are.
 * 
 * @author ikaddoura
 */

@Deprecated
public class DecongestionTollingV3 implements DecongestionTollSetting {
	
	private static final Logger log = Logger.getLogger(DecongestionTollingV3.class);

	private final DecongestionInfo congestionInfo;
	private final double vtts_hour;

	private Map<Id<Link>, LinkInfo> linkId2infoPreviousTollComputation = new HashMap<>();
	
	public DecongestionTollingV3(DecongestionInfo congestionInfo) {
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
					
					// do nothing
					
				} else {
					
					if (this.congestionInfo.getlinkInfos().get(linkId).getTime2toll().containsKey(intervalNr)) {
						
						double previousDelay = linkId2infoPreviousTollComputation.get(linkId).getTime2avgDelay().get(intervalNr);
						
						log.info("Previous delay: " + previousDelay + " --- Current delay: " + averageDelay);
						
						if (averageDelay >= previousDelay) {
							
							Map<Integer, Double> time2toll = this.congestionInfo.getlinkInfos().get(linkId).getTime2toll();
							double updatedToll = time2toll.get(intervalNr) + (time2toll.get(intervalNr) * this.congestionInfo.getDecongestionConfigGroup().getTOLL_ADJUSTMENT());
							time2toll.put(intervalNr, updatedToll);
							
						} else {
							
							Map<Integer, Double> time2toll = this.congestionInfo.getlinkInfos().get(linkId).getTime2toll();
							double updatedToll = time2toll.get(intervalNr) - (time2toll.get(intervalNr) * this.congestionInfo.getDecongestionConfigGroup().getTOLL_ADJUSTMENT());
							time2toll.put(intervalNr, updatedToll);							
						}
											
					} else {
						
						// start with the adjustment value
						double toll = this.congestionInfo.getDecongestionConfigGroup().getTOLL_ADJUSTMENT();
						this.congestionInfo.getlinkInfos().get(linkId).getTime2toll().put(intervalNr, toll);		
					}
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

