/* *********************************************************************** *
 * project: org.matsim.*
 * BKickFreespeedTravelTimeCost
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

import org.matsim.config.groups.CharyparNagelScoringConfigGroup;
import org.matsim.interfaces.core.v01.Link;
import org.matsim.router.costcalculators.FreespeedTravelTimeCost;
import org.matsim.utils.misc.Time;


/**
 * @author dgrether
 *
 */
public class BKickFreespeedTravelTimeCost extends FreespeedTravelTimeCost {

	
	private CharyparNagelScoringConfigGroup scoringConfig;
	private double marginalUtilityOfFuel;
	private double marginalUtilityOfTraveling;

	public BKickFreespeedTravelTimeCost(
			CharyparNagelScoringConfigGroup charyparNagelScoring) {
		this.scoringConfig = charyparNagelScoring;
		this.marginalUtilityOfFuel = -scoringConfig.getMarginalUtlOfDistance();
		this.marginalUtilityOfTraveling = -scoringConfig.getTraveling()/3600.0;
	}
	
	/**
	 * @see org.matsim.router.util.TravelMinCost#getLinkMinimumTravelCost(org.matsim.interfaces.core.v01.Link)
	 */
	@Override
	public double getLinkMinimumTravelCost(Link link) {
		return (link.getLength() / link.getFreespeed(Time.UNDEFINED_TIME)) * marginalUtilityOfTraveling 
		+ marginalUtilityOfFuel * 0.12d/1000.0d * link.getLength();
	}

	/**
	 * @see org.matsim.router.util.TravelCost#getLinkTravelCost(org.matsim.interfaces.core.v01.Link, double)
	 */
	@Override
	public double getLinkTravelCost(Link link, double time) {
		return (link.getLength() / link.getFreespeed(time)) * marginalUtilityOfTraveling 
		+ marginalUtilityOfFuel * 0.12d/1000.0d * link.getLength();
	}
	
}
