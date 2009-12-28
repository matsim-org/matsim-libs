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

import java.lang.reflect.Method;

import org.apache.log4j.Logger;

import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.router.Dijkstra;
import org.matsim.core.router.util.TravelCost;
import org.matsim.core.router.util.TravelTime;

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
 * 
 * Can be maybe removed once if the Time- and CostCalculators have access to the
 * Person they are calculating for (BackPointer, etc.).
 */

public class DijkstraWrapper extends PersonLeastCostPathCalculator {

	private final static Logger log = Logger.getLogger(DijkstraWrapper.class);
	
	protected static int errorCounter = 0;

	protected Network network;
	
	protected Dijkstra dijkstra;
	
	protected TravelCost costCalculator;
	protected TravelTime timeCalculator;

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
	public DijkstraWrapper(Dijkstra dijkstra, TravelCost costFunction, TravelTime timeFunction, Network network)
	{
		this.dijkstra = dijkstra;
		this.costCalculator = costFunction;
		this.timeCalculator = timeFunction;
		this.network = network;
		
		subNetworkCreator = new SubNetworkCreator(network);
		subNetworkTools = new SubNetworkTools();
		knowledgeTools = new KnowledgeTools();
	}
	
	public Path calcLeastCostPath(Node fromNode, Node toNode, double startTime)
	{
		return dijkstra.calcLeastCostPath(fromNode, toNode, startTime);
	}
	
	/*
	 * We have to hand over the person to the Cost- and TimeCalculators of the Router.
	 */
	@Override
	public void setPerson(Person person)
	{
		this.person = person;
		
		if(costCalculator instanceof KnowledgeTravelCost)
		{
			((KnowledgeTravelCost)costCalculator).setPerson(person);
		}
		
		if(timeCalculator instanceof KnowledgeTravelTime)
		{
			((KnowledgeTravelTime)timeCalculator).setPerson(person);
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
	
//				knowledgeTools.removeKnowledge(person);
				
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
		return costCalculator;
	}
	
	public TravelTime getTravelTimeCalculator()
	{
		return timeCalculator;
	}
		
	public Dijkstra getDijkstra()
	{
		return dijkstra;
	}
	
	@Override
	public DijkstraWrapper clone()
	{
//		TravelCost travelCostClone;
//		if(this.costFunction instanceof KnowledgeTravelCost)
//		{
//			travelCostClone = ((KnowledgeTravelCost)costCalculator).clone();
//		}
//		else
//		{
//			log.error("Could not clone the Cost Function - use reference to the existing Function and hope the best...");
//			travelCostClone = costCalculator;
//		}
		
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
		
//		TravelTime travelTimeClone;
//		if(this.timeCalculator instanceof KnowledgeTravelTime)
//		{
//			travelTimeClone = ((KnowledgeTravelTime)timeCalculator).clone();
//		}
//		else
//		{
//			log.error("Could not clone the Time Function - use reference to the existing Function and hope the best...");
//			travelTimeClone = timeCalculator;
//		}
				
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
		
		Dijkstra dijkstraClone;
		if (this.dijkstra instanceof MyDijkstra) dijkstraClone = new MyDijkstra(network, travelCostClone, travelTimeClone);
		else dijkstraClone = new Dijkstra(network, travelCostClone, travelTimeClone);
				
		DijkstraWrapper clone = new DijkstraWrapper(dijkstraClone, travelCostClone, travelTimeClone, network);
		
		// TODO: how to handle an A*-Algorithm???
		
		return clone; 
	}
	
}
