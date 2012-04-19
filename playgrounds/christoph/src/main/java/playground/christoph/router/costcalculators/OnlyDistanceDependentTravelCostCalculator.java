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

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.vehicles.Vehicle;

public class OnlyDistanceDependentTravelCostCalculator implements TravelDisutility, Cloneable {
	
	public OnlyDistanceDependentTravelCostCalculator()
	{
	}

	@Override
	public double getLinkTravelDisutility(final Link link, final double time, final Person person, final Vehicle vehicle) 
	{
		return link.getLength();
	}

	@Override
	public double getLinkMinimumTravelDisutility(final Link link) 
	{
		return link.getLength();
	}
	
	@Override
	public OnlyDistanceDependentTravelCostCalculator clone()
	{
		OnlyDistanceDependentTravelCostCalculator clone;

		clone = new OnlyDistanceDependentTravelCostCalculator();
		
		return clone;
	}
}