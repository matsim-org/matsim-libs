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

import org.matsim.mobsim.queuesim.QueueNetwork;
import org.matsim.network.NetworkLayer;
import org.matsim.population.Person;
import org.matsim.router.PlansCalcRoute;
import org.matsim.router.util.LeastCostPathCalculator;
import org.matsim.router.util.TravelCost;
import org.matsim.router.util.TravelTime;

import playground.christoph.router.util.PersonLeastCostPathCalculator;

public class KnowledgePlansCalcRoute extends PlansCalcRoute {
	
	protected Person person;
	protected QueueNetwork queueNetwork;
	
	public KnowledgePlansCalcRoute(final NetworkLayer network, final TravelCost costCalculator, final TravelTime timeCalculator) 
	{
		super(network, costCalculator, timeCalculator);
	}
	
	public KnowledgePlansCalcRoute(final LeastCostPathCalculator router, final LeastCostPathCalculator routerFreeflow) 
	{
		super(router, routerFreeflow);
	}
	
	public void setPerson(Person person)
	{
		this.person = person;
		
		if(routeAlgo instanceof PersonLeastCostPathCalculator)
		{
			((PersonLeastCostPathCalculator)routeAlgo).setPerson(person);
		}
		
		if(routeAlgoFreeflow instanceof PersonLeastCostPathCalculator)
		{
			((PersonLeastCostPathCalculator)routeAlgoFreeflow).setPerson(person);
		}
	}
	
	public Person getPerson()
	{
		return this.person;
	}
	
	/*
	 * We have to hand over the queueNetwork to the Cost- and TimeCalculators of the Router.
	 */
	public void setQueueNetwork(QueueNetwork queueNetwork)
	{
		this.queueNetwork = queueNetwork;
		
		if(routeAlgo instanceof PersonLeastCostPathCalculator)
		{
			((PersonLeastCostPathCalculator)routeAlgo).setQueueNetwork(queueNetwork);
		}
		
		if(routeAlgoFreeflow instanceof PersonLeastCostPathCalculator)
		{
			((PersonLeastCostPathCalculator)routeAlgoFreeflow).setQueueNetwork(queueNetwork);
		}
	}
	
	public QueueNetwork QueueNetwork()
	{
		return this.queueNetwork;
	}
}
