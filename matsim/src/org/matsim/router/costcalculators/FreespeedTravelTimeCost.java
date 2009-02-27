/* *********************************************************************** *
 * project: org.matsim.*
 * FreespeedTravelTimeCost.java
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

package org.matsim.router.costcalculators;

import org.matsim.config.groups.CharyparNagelScoringConfigGroup;
import org.matsim.gbl.Gbl;
import org.matsim.interfaces.core.v01.Link;
import org.matsim.router.util.TravelMinCost;
import org.matsim.router.util.TravelTime;
import org.matsim.utils.misc.Time;

/**
 * CostCalculator and TravelTimeCalculator for Links based on freespeed on links and
 * distance costs if set
 *
 * @author mrieser
 * @author dgrether
 */
public class FreespeedTravelTimeCost implements TravelMinCost, TravelTime {

	private final double travelCostFactor;
	private final double marginalUtlOfDistance;
	/**
	 * 
	 * @param scaledTravelFactor must be scaled, i.e. per second
	 * @param scaledPerformingFactor must be scaled, i.e. per second
	 * @param scaledMarginalUtilityOfDistance must be scaled, i.e. per meter
	 */
	public FreespeedTravelTimeCost(double scaledMarginalUtilityOfTraveling, double scaledMarginalUtilityOfPerforming, 
			double scaledMarginalUtilityOfDistance){
		// usually, the travel-utility should be negative (it's a disutility)
		// but for the cost, the cost should be positive.
		this.travelCostFactor = -scaledMarginalUtilityOfTraveling + scaledMarginalUtilityOfPerforming;
		
		if (this.travelCostFactor <= 0) {
			Gbl.errorMsg("The travel cost in " + this.getClass().getName() + " must be >= 0. " +
					"Currently, it is " + this.travelCostFactor + "." +
					"That is the sum of the costs for traveling and the opportunity costs." +
							" Please adjust the parameters" +
							"'traveling' and 'performing' in the module 'planCalcScore' in your config file to be" +
							" lower or equal than 0 when added.");
		}

		this.marginalUtlOfDistance = scaledMarginalUtilityOfDistance;
	}
	
	public FreespeedTravelTimeCost(CharyparNagelScoringConfigGroup cnScoringGroup){
		this(cnScoringGroup.getTraveling() / 3600.0, cnScoringGroup.getPerforming() / 3600.0, 
				cnScoringGroup.getMarginalUtlOfDistance());
	}
	
	/**
	 * @deprecated use constructor with explicit parameters or the one using the ConfigGroup
	 */
	@Deprecated
	public FreespeedTravelTimeCost() {
		this(Gbl.getConfig().charyparNagelScoring());
	}

	public double getLinkTravelCost(Link link, double time) {
		if (this.marginalUtlOfDistance == 0.0) {
			return (link.getLength() / link.getFreespeed(time)) * this.travelCostFactor;
		}
		return (link.getLength() / link.getFreespeed(time)) * this.travelCostFactor - this.marginalUtlOfDistance * link.getLength();
	}

	public double getLinkMinimumTravelCost(Link link) {
		if (this.marginalUtlOfDistance == 0.0) {
			return (link.getLength() / link.getFreespeed(Time.UNDEFINED_TIME)) * this.travelCostFactor;
		}
		return (link.getLength() / link.getFreespeed(Time.UNDEFINED_TIME)) * this.travelCostFactor 
		- this.marginalUtlOfDistance * link.getLength();
	}

	public double getLinkTravelTime(Link link, double time) {
		return link.getLength() / link.getFreespeed(time);
	}
}
