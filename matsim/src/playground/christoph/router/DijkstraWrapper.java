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

import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.router.Dijkstra;
import org.matsim.core.router.util.TravelCost;
import org.matsim.core.router.util.TravelTime;

import playground.christoph.mobsim.MyQueueNetwork;
import playground.christoph.network.SubNetwork;
import playground.christoph.network.util.SubNetworkCreator;
import playground.christoph.network.util.SubNetworkTools;
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

	protected NetworkLayer network;
	
	protected Dijkstra dijkstra;
	
	protected TravelCost costFunction;
	protected TravelTime timeFunction;

	protected SubNetworkCreator subNetworkCreator;
	protected SubNetworkTools subNetworkTools;
	protected KnowledgeTools knowledgeTools;
		
	/*
	 * The TravelCost and TravelTime objects have to be those, which were used to initialize
	 * the Dijkstra Object.
	 * To be able to hand over the person to them, PersonTravelCost and PersonTravelTime
	 * Objects should be used.
	 * If the Calculators need information about the current traffic in the System, the
	 * QueueNetwork has to be set.
	 */
	public DijkstraWrapper(Dijkstra dijkstra, TravelCost costFunction, TravelTime timeFunction, NetworkLayer network)
	{
		this.dijkstra = dijkstra;
		this.costFunction = costFunction;
		this.timeFunction = timeFunction;
		this.network = network;
		
		subNetworkCreator = new SubNetworkCreator(network);
		subNetworkTools = new SubNetworkTools();
		knowledgeTools = new KnowledgeTools();
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
	public void setPerson(PersonImpl person)
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
				
		if (dijkstra instanceof MyDijkstra)
		{
			SubNetwork subNetwork = subNetworkTools.getSubNetwork(person);
			
			synchronized(subNetwork)
			{
				if (!subNetwork.isInitialized())
				{
					subNetworkCreator.createSubNetwork(knowledgeTools.getNodeKnowledge(person), subNetwork);
	//				log.info("Set Network for Person " + person.getId());
				}
				
				knowledgeTools.removeKnowledge(person);
				
				((MyDijkstra)dijkstra).setNetwork(subNetwork);
			}
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
	
	public TravelCost getTravelCostCalculator()
	{
		return costFunction;
	}
	
	public TravelTime getTravelTimeCalculator()
	{
		return timeFunction;
	}
	
	/*
	 * We have to hand over the queueNetwork to the Cost- and TimeCalculators of the Router.
	 */
	@Override
	public void setMyQueueNetwork(MyQueueNetwork myQueueNetwork)
	{
		this.myQueueNetwork = myQueueNetwork;
		
		if(costFunction instanceof KnowledgeTravelCost)
		{
			((KnowledgeTravelCost)costFunction).setMyQueueNetwork(myQueueNetwork);
		}
		
		if(timeFunction instanceof KnowledgeTravelTime)
		{
			((KnowledgeTravelTime)timeFunction).setMyQueueNetwork(myQueueNetwork);
		}
	}
	
	
	public Dijkstra getDijkstra()
	{
		return dijkstra;
	}
	
	@Override
	public DijkstraWrapper clone()
	{
		TravelCost costFunctionClone;
		TravelTime timeFunctionClone;
//		NetworkLayer networkClone = this.myQueueNetwork.getNetworkLayer();
		
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
		
		Dijkstra dijkstraClone;
		if (this.dijkstra instanceof MyDijkstra) dijkstraClone = new MyDijkstra(network, costFunctionClone, timeFunctionClone);
		else dijkstraClone = new Dijkstra(network, costFunctionClone, timeFunctionClone);
				
		DijkstraWrapper clone = new DijkstraWrapper(dijkstraClone, costFunctionClone, timeFunctionClone, network);
		clone.setMyQueueNetwork(this.myQueueNetwork);
		//clone.queueNetwork = this.queueNetwork;
		
		// TODO: how to handle an A*-Algorithm???
		
		return clone; 
	}
	
}
