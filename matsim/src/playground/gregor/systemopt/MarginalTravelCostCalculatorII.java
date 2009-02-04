/* *********************************************************************** *
 * project: org.matsim.*
 * MarginalTravelCostCalculatorII.java
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

import org.matsim.network.Link;
import org.matsim.router.util.TravelCost;
import org.matsim.trafficmonitoring.TravelTimeCalculator;

public class MarginalTravelCostCalculatorII implements TravelCost {

	


	private final SocialCostCalculator sc;
	private final TravelTimeCalculator tc;

	public MarginalTravelCostCalculatorII(final TravelTimeCalculator tc, final SocialCostCalculator sc) {
		this.tc = tc;
		this.sc = sc;
	}
	

	public double getLinkTravelCost(final Link link, final double time) {
		double t = this.tc.getLinkTravelTime(link, time);
		double s = this.sc.getSocialCost(link, time);
		return t + s;
	}
	

}