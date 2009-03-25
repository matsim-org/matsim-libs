/* *********************************************************************** *
 * project: org.matsim.*
 * BKickTravelCostCalculator
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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
package playground.benjamin;

import org.matsim.core.api.network.Link;
import org.matsim.core.config.groups.CharyparNagelScoringConfigGroup;
import org.matsim.core.router.util.TravelMinCost;
import org.matsim.core.router.util.TravelTime;
import org.matsim.utils.misc.Time;


/**
 * @author dgrether
 *
 */
public class BKickTravelCostCalculator implements TravelMinCost {

	private double marginalUtilityOfFuel = Double.NaN;
	private double marginalUtilityOfTraveling = Double.NaN;

	
	private CharyparNagelScoringConfigGroup scoringConfig;
	private TravelTime travelTimeCalculator;

	public BKickTravelCostCalculator(
			CharyparNagelScoringConfigGroup charyparNagelScoring) {
		this.scoringConfig = charyparNagelScoring;
		this.marginalUtilityOfFuel = -scoringConfig.getMarginalUtlOfDistance();
		this.marginalUtilityOfTraveling = -scoringConfig.getTraveling() / 3600.0;
	}

	/**
	 * @see org.matsim.core.router.util.TravelMinCost#getLinkMinimumTravelCost(org.matsim.core.api.network.Link)
	 */
	public double getLinkMinimumTravelCost(Link link) {
		return (link.getLength() / link.getFreespeed(Time.UNDEFINED_TIME)) * marginalUtilityOfTraveling + marginalUtilityOfFuel * 0.12d/1000.0d * link.getLength();
	}

	/**
	 * @see org.matsim.core.router.util.TravelCost#getLinkTravelCost(org.matsim.core.api.network.Link, double)
	 */
	public double getLinkTravelCost(Link link, double time) {
		return travelTimeCalculator.getLinkTravelTime(link, time) * marginalUtilityOfTraveling + marginalUtilityOfFuel * 0.12d/1000.0d * link.getLength();
	}

	
	
	public void setTravelTimeCalculator(TravelTime travelTimeCalculator) {
		this.travelTimeCalculator = travelTimeCalculator;
	}

}
