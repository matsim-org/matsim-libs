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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.groups.ScoringConfigGroup;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.router.util.LinkToLinkTravelTime;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.vehicles.Vehicle;

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
public class FreespeedTravelTimeAndDisutility implements TravelDisutility, TravelTime, LinkToLinkTravelTime {

	private static final Logger log = LogManager.getLogger(FreespeedTravelTimeAndDisutility.class);

	private final double travelCostFactor;
	private final double marginalUtlOfDistance;
	private static int wrnCnt = 0 ;
	/**
	 *
	 * @param scaledMarginalUtilityOfTraveling Must be scaled, i.e. per second.  Usually negative.
	 * @param scaledMarginalUtilityOfPerforming Must be scaled, i.e. per second.  Usually positive.
	 * @param scaledMarginalUtilityOfDistance Must be scaled, i.e. per meter.  Usually negative.
	 */
	public FreespeedTravelTimeAndDisutility(double scaledMarginalUtilityOfTraveling, double scaledMarginalUtilityOfPerforming,
			double scaledMarginalUtilityOfDistance){
		// usually, the travel-utility should be negative (it's a disutility)
		// but for the cost, the cost should be positive.
		this.travelCostFactor = -scaledMarginalUtilityOfTraveling + scaledMarginalUtilityOfPerforming;

		if ( wrnCnt < 1 ) {
			wrnCnt++ ;
			if (this.travelCostFactor <= 0) {
				log.warn("The travel cost in " + this.getClass().getName() + " under normal circumstances should be > 0. " +
						"Currently, it is " + this.travelCostFactor + "." +
						"That is the sum of the costs for traveling and the opportunity costs." +
						" Please adjust the parameters" +
						"'traveling' and 'performing' in the module 'planCalcScore' in your config file to be" +
				" lower or equal than 0 when added.");
				log.warn(Gbl.ONLYONCE) ;
			}
		}

		this.marginalUtlOfDistance = scaledMarginalUtilityOfDistance;
	}

	public FreespeedTravelTimeAndDisutility(ScoringConfigGroup cnScoringGroup){
		this(cnScoringGroup.getModes().get(TransportMode.car).getMarginalUtilityOfTraveling() / 3600.0, cnScoringGroup.getPerforming_utils_hr() / 3600.0,
//				cnScoringGroup.getMarginalUtlOfDistanceCar());
				cnScoringGroup.getModes().get(TransportMode.car).getMonetaryDistanceRate() *cnScoringGroup.getMarginalUtilityOfMoney());
	}

	@Override
	public double getLinkTravelDisutility(final Link link, final double time, final Person person, final Vehicle vehicle) {
		if (this.marginalUtlOfDistance == 0.0) {
			return (link.getLength() / link.getFreespeed(time)) * this.travelCostFactor;
		}
		return (link.getLength() / link.getFreespeed(time)) * this.travelCostFactor - this.marginalUtlOfDistance * link.getLength();
	}

	@Override
	public double getLinkMinimumTravelDisutility(Link link) {
		if (this.marginalUtlOfDistance == 0.0) {
			return (link.getLength() / link.getFreespeed()) * this.travelCostFactor;
		}
		return (link.getLength() / link.getFreespeed()) * this.travelCostFactor
		- this.marginalUtlOfDistance * link.getLength();
	}

	@Override
	public double getLinkTravelTime(Link link, double time, Person person, Vehicle vehicle) {
		return link.getLength() / link.getFreespeed(time);
	}

	/**
	 * If travelling freespeed the turning move travel time is not relevant
	 */
	@Override
	public double getLinkToLinkTravelTime(Link fromLink, Link toLink, double time, Person person, Vehicle vehicle) {
		return this.getLinkTravelTime(fromLink, time, null, null);
	}

}
