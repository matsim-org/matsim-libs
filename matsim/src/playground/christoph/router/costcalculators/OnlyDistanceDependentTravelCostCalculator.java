/* *********************************************************************** *
 * project: org.matsim.*
 * OnlyDistanceDependentTravelCostCalculator.java
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

import org.matsim.core.network.LinkImpl;
import org.matsim.core.router.util.TravelMinCost;
import org.matsim.core.router.util.TravelTime;

public class OnlyDistanceDependentTravelCostCalculator implements TravelMinCost {
	
	public OnlyDistanceDependentTravelCostCalculator(final TravelTime timeCalculator)
	{
	}

	public double getLinkTravelCost(final LinkImpl link, final double time) 
	{
		return link.getLength();
	}

	public double getLinkMinimumTravelCost(final LinkImpl link) 
	{
		return link.getLength();
	}
}