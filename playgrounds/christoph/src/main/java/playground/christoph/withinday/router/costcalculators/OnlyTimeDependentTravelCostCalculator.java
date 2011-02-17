/* *********************************************************************** *
 * project: org.matsim.*
 * OnlyTimeDependentTravelCostCalculator.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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

package playground.christoph.withinday.router.costcalculators;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.router.util.PersonalizableTravelCost;
import org.matsim.core.router.util.TravelMinCost;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.utils.misc.Time;

import playground.christoph.withinday.router.costcalculators.OnlyTimeDependentTravelCostCalculator;
import playground.christoph.withinday.trafficmonitoring.FreeSpeedTravelTimeCalculator;

/**
 *  A Travel Cost Calculator that uses the travel times as travel costs.
 * @author cdobler
 */
public class OnlyTimeDependentTravelCostCalculator implements TravelMinCost, PersonalizableTravelCost {

	private static final Logger log = Logger.getLogger(OnlyTimeDependentTravelCostCalculator.class);
	
	protected final TravelTime timeCalculator;

	public OnlyTimeDependentTravelCostCalculator(final TravelTime timeCalculator) {
		if (timeCalculator == null) {
			log.warn("TimeCalculator is null so FreeSpeedTravelTimes will be calculated!");
			this.timeCalculator = new FreeSpeedTravelTimeCalculator();
		} else this.timeCalculator = timeCalculator;
	}

	public double getLinkGeneralizedTravelCost(final Link link, final double time) {
		return this.timeCalculator.getLinkTravelTime(link, time);
	}

	public double getLinkMinimumTravelCost(final Link link) {
		return this.timeCalculator.getLinkTravelTime(link, Time.UNDEFINED_TIME);
	}
	
	@Override
	public void setPerson(Person person) {
		// nothing to do here
	}
}