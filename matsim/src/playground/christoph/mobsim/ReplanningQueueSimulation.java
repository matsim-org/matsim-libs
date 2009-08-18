/* *********************************************************************** *
 * project: org.matsim.*
 * KnowledgeQueueSimulation.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package playground.christoph.mobsim;

import java.io.Serializable;
import java.util.Comparator;
import java.util.concurrent.PriorityBlockingQueue;

import org.apache.log4j.Logger;
import org.matsim.core.controler.Controler;
import org.matsim.core.events.Events;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.mobsim.queuesim.DriverAgent;
import org.matsim.core.mobsim.queuesim.QueueSimulation;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.population.PopulationImpl;

import playground.christoph.events.algorithms.ParallelReplanner;
import playground.christoph.knowledge.container.dbtools.KnowledgeDBStorageHandler;

public class ReplanningQueueSimulation extends QueueSimulation{

	private final static Logger log = Logger.getLogger(ReplanningQueueSimulation.class);
	
	protected Controler controler;
	protected KnowledgeDBStorageHandler knowledgeDBStorageHandler;
	
	/*
	 * Basically a 1:1 copy of the activityEndsList of the QueueSimulation.
	 * The difference is, that this implementation uses a Time Offset. That means
	 * that we are informed that an activity will end a few Time Steps before it
	 * really ends. This allows us for example to read the known nodes of a Person
	 * from a Database before they are needed what should speed up the Simulation.
	 */
	protected final PriorityBlockingQueue<DriverAgent> offsetActivityEndsList = new PriorityBlockingQueue<DriverAgent>(500, new DriverAgentDepartureTimeComparator());
	protected final double timeOffset = 120.0;
	
	public ReplanningQueueSimulation(final NetworkLayer network, final PopulationImpl population, final Events events)
	{
		super(network, population, events);
		
		/*
		 * Use a MyQueueNetwork with MyQueueNodes - we need it for our Replanning! 
		 */
		this.network = new MyQueueNetwork(network);
		this.networkLayer = network;
		
		// replace the QueueSimEngine with a MyQueueSimEngine
		this.simEngine = new MyQueueSimEngine(this.network, MatsimRandom.getRandom());
		
		// set the QueueSimulation in the SimEngine
		((MyQueueSimEngine)this.simEngine).setQueueSimulation(this);
		
		//setAgentFactory(new MyAgentFactory(this));
		
		this.knowledgeDBStorageHandler = new KnowledgeDBStorageHandler(population);
		this.knowledgeDBStorageHandler.start();
		getEvents().addHandler(knowledgeDBStorageHandler);
	}

	public MyQueueNetwork getMyQueueNetwork()
	{
		return (MyQueueNetwork) this.getQueueNetwork();
	}
	
	public PriorityBlockingQueue<DriverAgent> getActivityEndsList()
	{
		return super.activityEndsList;
	}

	@Override
	protected boolean doSimStep(final double time) 
	{
		/*
		 * Update the LookupTables for the LinkTravelTimes and LinkTravelCosts.
		 * Update the LinkTravelTimes first because the LinkTravelCosts may use
		 * them already!
		 */
//		log.info("Updating LookupTable...");
		ParallelReplanner.updateLinkTravelTimesLookupTables(time);
		ParallelReplanner.updateLinkTravelCostsLookupTables(time);
//		log.info("done");
		
		if (MyQueueSimEngine.isActEndReplanning())
		{
			((MyQueueSimEngine)this.simEngine).actEndReplanning(time);
		}
		
		handleOffsetActivityEnds(time);
		
		return super.doSimStep(time);		
	}

/*	
	public QueueNetwork getQueueNetwork()
	{
		return this.network;
	}
*/
	public void setControler(final Controler controler)
	{
		this.controler = controler;

		// Referenz auf den Controler mit dem Replanning Algorithmus hinterlegen
		((MyQueueNetwork)this.network).setControler(controler);
	}

	public Controler getControler()
	{
		return this.controler;
	}

	/**
	 * Registers this agent as performing an activity and makes sure that the
	 * agent will be informed once his departure time has come.
	 * 
	 * @param agent
	 * 
	 * @see DriverAgent#getDepartureTime()
	 */
	@Override
	protected void scheduleActivityEnd(final DriverAgent agent)
	{	
		offsetActivityEndsList.add(agent);
		super.scheduleActivityEnd(agent);
	}
	
	private void handleOffsetActivityEnds(final double time)
	{		
		while (this.offsetActivityEndsList.peek() != null)
		{
			DriverAgent agent = this.offsetActivityEndsList.peek();
			if (agent.getDepartureTime() <= time + timeOffset)
			{
				this.offsetActivityEndsList.poll();
				knowledgeDBStorageHandler.addPerson(agent.getPerson());
			} 
			else
			{
				return;
			}
		} 
	}
	
	/*package*/ class DriverAgentDepartureTimeComparator implements Comparator<DriverAgent>, Serializable {

		private static final long serialVersionUID = 1L;

		public int compare(DriverAgent agent1, DriverAgent agent2) {
			int cmp = Double.compare(agent1.getDepartureTime(), agent2.getDepartureTime());
			if (cmp == 0) {
				// Both depart at the same time -> let the one with the larger id be first (=smaller)
				return agent2.getPerson().getId().compareTo(agent1.getPerson().getId());
			}
			return cmp;
		}
	}
}
