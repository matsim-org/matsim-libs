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

package playground.ikaddoura.decongestion;

import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;

import playground.ikaddoura.decongestion.data.DecongestionInfo;

/**
 * Computes the tolls per link and time bin.
 * 
 * @author ikaddoura
 */

public class DecongestionTollComputation {
	
	private static final Logger log = Logger.getLogger(DecongestionTollComputation.class);

	private final DecongestionInfo congestionInfo;
	private final double vtts_hour;
	private final boolean setTollsToZero = true;

	public DecongestionTollComputation(DecongestionInfo congestionInfo) {
		this.congestionInfo = congestionInfo;
		this.vtts_hour = (this.congestionInfo.getScenario().getConfig().planCalcScore().getPerforming_utils_hr() - this.congestionInfo.getScenario().getConfig().planCalcScore().getModes().get(TransportMode.car).getMarginalUtilityOfTraveling()) / this.congestionInfo.getScenario().getConfig().planCalcScore().getMarginalUtilityOfMoney();
		log.info("VTTS [monetary units / hour]: " + this.vtts_hour);
	}

	public void updateTolls() {
		
		for (Id<Link> linkId : this.congestionInfo.getlinkInfos().keySet()) {
						
			for (Integer intervalNr : this.congestionInfo.getlinkInfos().get(linkId).getTime2avgDelay().keySet()) {

				double averageDelay = this.congestionInfo.getlinkInfos().get(linkId).getTime2avgDelay().get(intervalNr);
								
				if (averageDelay <= this.congestionInfo.getTOLERATED_AVERAGE_DELAY_SEC()) {
					
					if (setTollsToZero) {
						if (this.congestionInfo.getlinkInfos().get(linkId).getTime2toll().containsKey(intervalNr)) {
							this.congestionInfo.getlinkInfos().get(linkId).getTime2toll().remove(intervalNr);
						}
					}

				} else {
					
					if (this.congestionInfo.getlinkInfos().get(linkId).getTime2toll().containsKey(intervalNr)) {
						
						Map<Integer, Double> time2toll = this.congestionInfo.getlinkInfos().get(linkId).getTime2toll();
						double updatedToll = time2toll.get(intervalNr) * (1. + this.congestionInfo.getTOLL_ADJUSTMENT_RATE());
						time2toll.put(intervalNr, updatedToll);
											
					} else {
						
						// start with an average delay based toll setting
						double toll = averageDelay * vtts_hour / 3600.;
						this.congestionInfo.getlinkInfos().get(linkId).getTime2toll().put(intervalNr, toll);		
					}
				}
			}
		}
	}
}

