/* *********************************************************************** *
 * project: org.matsim.*
 * RiskAversTravelCost.java
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

package playground.gregor.sims.riskaversion;

import org.matsim.core.api.network.Link;
import org.matsim.router.util.TravelCost;
import org.matsim.trafficmonitoring.TravelTimeCalculator;

public class RiskAverseTravelCostCalculator implements TravelCost {

	private final TravelTimeCalculator tc;
	private final RiskCostCalculator rc;

	public RiskAverseTravelCostCalculator(final TravelTimeCalculator tc, final RiskCostCalculator rc) {
		this.tc = tc;
		this.rc = rc;
	}
	
	public double getLinkTravelCost(final Link link, final double time) {
		return this.tc.getLinkTravelTime(link, time) + this.rc.getLinkRisk(link,time);
	}

}
