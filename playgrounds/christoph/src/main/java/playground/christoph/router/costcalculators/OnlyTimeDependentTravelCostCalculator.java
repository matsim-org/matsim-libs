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

package playground.christoph.router.costcalculators;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.router.util.PersonalizableTravelCost;
import org.matsim.core.router.util.TravelMinCost;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.utils.misc.Time;

import playground.christoph.router.util.KnowledgeTravelTime;

public class OnlyTimeDependentTravelCostCalculator implements TravelMinCost, Cloneable, PersonalizableTravelCost {

	private static final Logger log = Logger.getLogger(OnlyTimeDependentTravelCostCalculator.class);
	
	protected final TravelTime timeCalculator;

	public OnlyTimeDependentTravelCostCalculator(final TravelTime timeCalculator)
	{
		this.timeCalculator = timeCalculator;
		if (timeCalculator == null) log.warn("TimeCalculator is null so FreeSpeedTravelTimes will be calculated!");
	}

	public double getLinkTravelCost(final Link link, final double time) 
	{
		if (timeCalculator != null)
		{
			double travelTime = this.timeCalculator.getLinkTravelTime(link, time);
			return travelTime;
		}
		else
		{
			return link.getLength()/link.getFreespeed(time);
		}
	}

	public double getLinkMinimumTravelCost(final Link link) 
	{
		double TravelTime = this.timeCalculator.getLinkTravelTime(link, Time.UNDEFINED_TIME);
		return TravelTime;
	}
	
	@Override
	public OnlyTimeDependentTravelCostCalculator clone()
	{
		OnlyTimeDependentTravelCostCalculator clone;
		
		if (timeCalculator instanceof KnowledgeTravelTime)
		{
			KnowledgeTravelTime timeCalculatorClone = ((KnowledgeTravelTime)timeCalculator).clone();
			clone = new OnlyTimeDependentTravelCostCalculator(timeCalculatorClone);
		}
		else
		{
			clone = new OnlyTimeDependentTravelCostCalculator(timeCalculator);
		}
		
		return clone;
	}

	@Override
	public void setPerson(Person person) {
		// TODO Auto-generated method stub
		
	}
	
	
}