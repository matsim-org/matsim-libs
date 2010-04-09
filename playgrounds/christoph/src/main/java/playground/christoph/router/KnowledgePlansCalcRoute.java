/* *********************************************************************** *
 * project: org.matsim.*
 * KnowledgePlansCalcRoute.java
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

package playground.christoph.router;

import java.lang.reflect.Method;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.config.groups.PlansCalcRouteConfigGroup;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.router.PlansCalcRoute;
import org.matsim.core.router.util.DijkstraFactory;
import org.matsim.core.router.util.LeastCostPathCalculatorFactory;
import org.matsim.core.router.util.PersonalizableTravelCost;
import org.matsim.core.router.util.TravelCost;
import org.matsim.core.router.util.TravelTime;

import playground.christoph.router.util.PersonLeastCostPathCalculator;

public class KnowledgePlansCalcRoute extends PlansCalcRoute implements Cloneable{
	
	protected Person person;
	protected Network network;
	protected PlansCalcRouteConfigGroup configGroup;
	protected PersonalizableTravelCost costCalculator;
	protected TravelTime timeCalculator;
	protected LeastCostPathCalculatorFactory factory;
		
	private final static Logger log = Logger.getLogger(KnowledgePlansCalcRoute.class);
				
	public KnowledgePlansCalcRoute(final PlansCalcRouteConfigGroup group, final Network network, final PersonalizableTravelCost costCalculator,
			final TravelTime timeCalculator, LeastCostPathCalculatorFactory factory){
		super(group, network, costCalculator, timeCalculator, factory);
		
		this.configGroup = group;
		this.network = network;
		this.costCalculator = costCalculator;
		this.timeCalculator = timeCalculator;
		this.factory = factory;
	}
	
	/*
	 * If no LeastCostPathCalculatorFactory is given use by Default
	 * a DijkstraFactory.
	 */
	public KnowledgePlansCalcRoute(final PlansCalcRouteConfigGroup group, final Network network, final PersonalizableTravelCost costCalculator, final TravelTime timeCalculator) {
		this(group, network, costCalculator, timeCalculator, new DijkstraFactory());
	}
	
	/*
	 * Hand over the Person to the CostCalculators and then
	 * run the super method.
	 */
	@Override
	public void run(final Person person)
	{
		setPerson(person);
		
		super.run(person);
	}

	/*
	 * Hand over the Person to the CostCalculators and then
	 * run the super method.
	 */
	@Override
	public void run(final Plan plan)
	{
		setPerson(plan.getPerson());
		
		super.run(plan);
	}
	
	/*
	 * We have to hand over the person to the Cost- and TimeCalculators of the Router.
	 */
	private void setPerson(Person person)
	{
		this.person = person;
		
		if(this.getLeastCostPathCalculator() instanceof PersonLeastCostPathCalculator)
		{
			((PersonLeastCostPathCalculator)this.getLeastCostPathCalculator()).setPerson(person);
		}
		
		if(this.getPtFreeflowLeastCostPathCalculator() instanceof PersonLeastCostPathCalculator)
		{
			((PersonLeastCostPathCalculator)this.getPtFreeflowLeastCostPathCalculator()).setPerson(person);
		}
	}
	
	public Person getPerson()
	{
		return this.person;
	}
	
	@Override
	public KnowledgePlansCalcRoute clone()
	{
		TravelCost travelCostClone = null;
		if (costCalculator instanceof Cloneable)
		{
			try
			{
				Method method;
				method = costCalculator.getClass().getMethod("clone", new Class[]{});
				travelCostClone = costCalculator.getClass().cast(method.invoke(costCalculator, new Object[]{}));
			}
			catch (Exception e)
			{
				Gbl.errorMsg(e);
			} 
		}
		// not cloneable or an Exception occured
		if (travelCostClone == null)
		{
			travelCostClone = costCalculator;
			log.warn("Could not clone the Travel Cost Calculator - use reference to the existing Calculator and hope the best...");
		}
		
		TravelTime travelTimeClone = null;
		if (timeCalculator instanceof Cloneable)
		{
			try
			{
				Method method;
				method = timeCalculator.getClass().getMethod("clone", new Class[]{});
				travelTimeClone = timeCalculator.getClass().cast(method.invoke(timeCalculator, new Object[]{}));
			}
			catch (Exception e)
			{
				Gbl.errorMsg(e);
			} 
		}
		// not cloneable or an Exception occured
		if (travelTimeClone == null)
		{
			travelTimeClone = timeCalculator;
			log.warn("Could not clone the Travel Time Calculator - use reference to the existing Calculator and hope the best...");
		}
		
		KnowledgePlansCalcRoute clone;
		clone = new KnowledgePlansCalcRoute(configGroup, network, costCalculator, timeCalculator, factory);
	
		return clone;
	}
}
