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

import org.apache.log4j.Logger;
import org.matsim.mobsim.queuesim.QueueNetwork;
import org.matsim.network.NetworkLayer;
import org.matsim.population.Person;
import org.matsim.router.Dijkstra;
import org.matsim.router.PlansCalcRoute;
import org.matsim.router.util.LeastCostPathCalculator;
import org.matsim.router.util.TravelCost;
import org.matsim.router.util.TravelTime;

import playground.christoph.router.costcalculators.KnowledgeTravelCostCalculator;
import playground.christoph.router.costcalculators.KnowledgeTravelTimeCalculator;
import playground.christoph.router.util.KnowledgeTools;
import playground.christoph.router.util.PersonLeastCostPathCalculator;

public class KnowledgePlansCalcRoute extends PlansCalcRoute implements Cloneable{
	
	protected Person person;
	protected QueueNetwork queueNetwork;
	protected double time;
	
	private final static Logger log = Logger.getLogger(KnowledgePlansCalcRoute.class);
	
	public KnowledgePlansCalcRoute(final NetworkLayer network, final TravelCost costCalculator, final TravelTime timeCalculator) 
	{
		super(network, costCalculator, timeCalculator);
	}
	
	public KnowledgePlansCalcRoute(final LeastCostPathCalculator router, final LeastCostPathCalculator routerFreeflow) 
	{
		super(router, routerFreeflow);
	}
	
	/*
	 * We have to hand over the person to the Cost- and TimeCalculators of the Router.
	 */
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

	/*
	 * We have to hand over the time to the Cost- and TimeCalculators of the Router.
	 */
	public void setTime(Double time)
	{
		this.time = time;
		
		if(routeAlgo instanceof PersonLeastCostPathCalculator)
		{
			((PersonLeastCostPathCalculator)routeAlgo).setTime(time);
		}
		
		if(routeAlgoFreeflow instanceof PersonLeastCostPathCalculator)
		{
			((PersonLeastCostPathCalculator)routeAlgoFreeflow).setTime(time);
		}
	}
	
	public double getTime()
	{
		return this.time;
	}
	
	public KnowledgePlansCalcRoute clone()
	{
		LeastCostPathCalculator routeAlgoClone;
		LeastCostPathCalculator routeAlgoFreeflowClone;
		
		if(this.routeAlgo instanceof PersonLeastCostPathCalculator)
		{
			routeAlgoClone = ((PersonLeastCostPathCalculator)routeAlgo).clone();
		}
		else
		{
			log.error("Could not clone the Route Algorithm - use reference to the existing Algorithm and hope the best...");
			routeAlgoClone = routeAlgo;
		}
		
		if(this.routeAlgoFreeflow instanceof PersonLeastCostPathCalculator)
		{
			routeAlgoFreeflowClone = ((PersonLeastCostPathCalculator)routeAlgoFreeflow).clone();
		}
		else
		{
			log.error("Could not clone the Freeflow Route Algorithm - use reference to the existing Algorithm and hope the best...");
			routeAlgoFreeflowClone = routeAlgoFreeflow;
		}
		
		/* 
		 * routeAlgo and routeAlgoFreeflow may be the references to the same object.
		 * In this case -> use only one clone.
		 */
		KnowledgePlansCalcRoute clone;
		if(routeAlgo == routeAlgoFreeflow)
		{
			clone = new KnowledgePlansCalcRoute(routeAlgoClone, routeAlgoClone);
		}
		else
		{
			clone = new KnowledgePlansCalcRoute(routeAlgoClone, routeAlgoFreeflowClone);
		}
		
		clone.setQueueNetwork(this.queueNetwork);
		//clone.queueNetwork = this.queueNetwork;
/*		
		log.info(routeAlgo.getClass());
		log.info("org   " + this);
		log.info("clone " + clone);
		
		log.info("org wrapper   " + this.routeAlgo);
		log.info("clone wrapper " + clone.routeAlgo);
*/		
		return clone;
	}
}
