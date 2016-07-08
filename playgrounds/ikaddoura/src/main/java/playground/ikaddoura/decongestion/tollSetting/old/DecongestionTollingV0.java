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

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;

import playground.ikaddoura.decongestion.data.DecongestionInfo;
import playground.ikaddoura.decongestion.tollSetting.DecongestionTollSetting;

/**
 * 
 * Initial tolls
 * ... are set based on the average delay per link and time bin (= d).
 * 
 * Tolls in all further iterations
 * ... are recomputed
 * - If d > threshold: The toll is set based on the average delay increased by the adjustment rate.
 * - If d <= threshold: Set the toll to zero.
 * 
 * Set the adjustment factor to 0.0 to run basic average congestion cost pricing.
 * 
 * => Seems to produce a stable outcome. Increasing the toll by an adjustment factor > 0.0 results in lower delays.
 * 
 * @author ikaddoura
 */

@Deprecated
public class DecongestionTollingV0 implements DecongestionTollSetting {
	
	private static final Logger log = Logger.getLogger(DecongestionTollingV0.class);

	private final DecongestionInfo congestionInfo;
	private final double vtts_hour;

	public DecongestionTollingV0(DecongestionInfo congestionInfo) {
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
					
					if (this.congestionInfo.getlinkInfos().get(linkId).getTime2toll().containsKey(intervalNr)) {
						this.congestionInfo.getlinkInfos().get(linkId).getTime2toll().remove(intervalNr);
					}

				} else {
					
					double toll = (1 + this.congestionInfo.getDecongestionConfigGroup().getTOLL_ADJUSTMENT()) * averageDelay * vtts_hour / 3600.;
					this.congestionInfo.getlinkInfos().get(linkId).getTime2toll().put(intervalNr, toll);
				}
			}
		}
	}

	
}

