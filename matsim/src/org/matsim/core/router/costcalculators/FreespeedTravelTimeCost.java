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

package org.matsim.core.router.costcalculators;

import org.matsim.api.core.v01.network.Link;
import org.matsim.core.config.groups.CharyparNagelScoringConfigGroup;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.router.util.TravelMinCost;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.utils.misc.Time;

/**<p>
 * CostCalculator and TravelTimeCalculator for Links based on freespeed on links and
 * distance costs if set.  It sets the <em> function </em> that is to be used with calls
 * <tt>getLinkTravelTime( link, time)</tt> and <tt>getLinkTravelCost( link, time )</tt>.
 * </p><p>
 * The unit of "cost" is defined by the input: if the marginal utilities are given in "utils per second", then 
 * cost is in "utils"; if the marginal utilities are given in "euros per second", then cost is in "euros".
 * When the CharyparNagelScoringFunction is used, the values come from the config file, where one is also free to 
 * interpret the units. 
 * </p>
 * @author mrieser
 * @author dgrether
 */
public class FreespeedTravelTimeCost implements TravelMinCost, TravelTime {

	private final double travelCostFactor;
	private final double marginalUtlOfDistance;
	/**
	 * 
	 * @param scaledMarginalUtilityOfTraveling. Must be scaled, i.e. per second.  Usually negative.
	 * @param scaledMarginalUtilityOfPerforming. Must be scaled, i.e. per second.  Usually positive.
	 * @param scaledMarginalUtilityOfDistance. Must be scaled, i.e. per meter.  Usually negative.
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
				cnScoringGroup.getMarginalUtlOfDistanceCar());
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
