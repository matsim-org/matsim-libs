/* *********************************************************************** *
 * project: org.matsim.*
 * DijkstraWrapper.java
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
import org.matsim.interfaces.core.v01.Person;
import org.matsim.mobsim.queuesim.QueueNetwork;
import org.matsim.network.NetworkLayer;
import org.matsim.network.Node;
import org.matsim.router.Dijkstra;
import org.matsim.router.util.TravelCost;
import org.matsim.router.util.TravelTime;

import playground.christoph.router.util.KnowledgeTools;
import playground.christoph.router.util.KnowledgeTravelCost;
import playground.christoph.router.util.KnowledgeTravelTime;
import playground.christoph.router.util.PersonLeastCostPathCalculator;

/*
 * This is a Wrapper-Class that is needed to forward the currently replanned Person
 * to the Cost- and TimeCalculator.
 */

public class DijkstraWrapper extends PersonLeastCostPathCalculator {
//public class DijkstraWrapper extends PersonLeastCostPathCalculator, Dijkstra {

	private final static Logger log = Logger.getLogger(KnowledgeTools.class);
	
	protected static int errorCounter = 0;
	
	protected Dijkstra dijkstra;
	
	protected TravelCost costFunction;
	protected TravelTime timeFunction;

	public DijkstraWrapper()
	{	
	}
	
	/*
	 * The TravelCost and TravelTime objects have to be those, which were used to initialize
	 * the Dijkstra Object.
	 * To be able to hand over the person to them, PersonTravelCost and PersonTravelTime
	 * Objects should be used.
	 * If the Calculators need information about the current traffic in the System, the
	 * QueueNetwork has to be set.
	 */
	public DijkstraWrapper(Dijkstra dijkstra, TravelCost costFunction, TravelTime timeFunction)
	{
		this.dijkstra = dijkstra;
		this.costFunction = costFunction;
		this.timeFunction = timeFunction;
	}
	
	public Path calcLeastCostPath(Node fromNode, Node toNode, double startTime)
	{
/*		log.info(this);
		log.info(fromNode);
		log.info(toNode);
		log.info(time);
*/
		return dijkstra.calcLeastCostPath(fromNode, toNode, startTime);
	}
	
	/*
	 * We have to hand over the person to the Cost- and TimeCalculators of the Router.
	 */
	@Override
	public void setPerson(Person person)
	{
		this.person = person;
		
		if(costFunction instanceof KnowledgeTravelCost)
		{
			((KnowledgeTravelCost)costFunction).setPerson(person);
		}
		
		if(timeFunction instanceof KnowledgeTravelTime)
		{
			((KnowledgeTravelTime)timeFunction).setPerson(person);
		}

	}
	
	public static int getErrorCounter()
	{
		return errorCounter;
	}
	
	public static void setErrorCounter(int i)
	{
		errorCounter = i;
	}
	
	/*
	 * We have to hand over the queueNetwork to the Cost- and TimeCalculators of the Router.
	 */
	@Override
	public void setQueueNetwork(QueueNetwork queueNetwork)
	{
		this.queueNetwork = queueNetwork;
		
		if(costFunction instanceof KnowledgeTravelCost)
		{
			((KnowledgeTravelCost)costFunction).setQueueNetwork(queueNetwork);
		}
		
		if(timeFunction instanceof KnowledgeTravelTime)
		{
			((KnowledgeTravelTime)timeFunction).setQueueNetwork(queueNetwork);
		}
	}
	
	
	public Dijkstra getDijkstra()
	{
		return dijkstra;
	}
	
	public DijkstraWrapper clone()
	{
		TravelCost costFunctionClone;
		TravelTime timeFunctionClone;
		NetworkLayer networkClone = this.queueNetwork.getNetworkLayer();
		
		if(this.costFunction instanceof KnowledgeTravelCost)
		{
			costFunctionClone = ((KnowledgeTravelCost)costFunction).clone();
		}
		else
		{
			log.error("Could not clone the Cost Function - use reference to the existing Function and hope the best...");
			costFunctionClone = costFunction;
		}
		
		if(this.timeFunction instanceof KnowledgeTravelTime)
		{
			timeFunctionClone = ((KnowledgeTravelTime)timeFunction).clone();
		}
		else
		{
			log.error("Could not clone the Time Function - use reference to the existing Function and hope the best...");
			timeFunctionClone = timeFunction;
		}
		
		Dijkstra dijkstraClone = new Dijkstra(networkClone, costFunctionClone, timeFunctionClone);
		DijkstraWrapper clone = new DijkstraWrapper(dijkstraClone, costFunctionClone, timeFunctionClone);
		clone.setQueueNetwork(this.queueNetwork);
		//clone.queueNetwork = this.queueNetwork;
		
		// TODO: how to handle an A*-Algorithm???
		
		return clone; 
	}
	
}
