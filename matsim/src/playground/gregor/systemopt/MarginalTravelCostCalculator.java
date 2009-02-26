/* *********************************************************************** *
 * project: org.matsim.*
 * MarginalTravelCostCalculator.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package playground.gregor.systemopt;

import org.matsim.gbl.Gbl;
import org.matsim.interfaces.core.v01.Link;
import org.matsim.router.util.TravelCost;


public class MarginalTravelCostCalculator implements TravelCost {

	
	
	private final TravelTimeAndSocialCostCalculator timeCostCalculator;
	private final double travelCostFactor;
//	private final SocialCostCalculator sc;
//	private final TravelTimeCalculator tc;

	public MarginalTravelCostCalculator(final TravelTimeAndSocialCostCalculator t) {
		this.timeCostCalculator = t;
//		this.tc = tc;
//		this.sc = sc;
		/* Usually, the travel-utility should be negative (it's a disutility)
		 * but the cost should be positive. Thus negate the utility.
		 */
		this.travelCostFactor = -Gbl.getConfig().charyparNagelScoring().getTraveling() / 3600.0;
	}
	

	public double getLinkTravelCost(final Link link, final double time) {
		double t = this.timeCostCalculator.getLinkTravelTime(link, time);
		double s = this.timeCostCalculator.getSocialCost(link, time);
//		double t = this.tc.getLinkTravelTime(link, time);
//		double s = this.sc.getSocialCost(link, time);
		return t + s;
	}
	

}
