/* *********************************************************************** *
 * project: org.matsim.*
 * ReplanningQueueSimulation.java
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

import java.util.concurrent.PriorityBlockingQueue;

import org.apache.log4j.Logger;
import org.matsim.core.events.EventsImpl;
import org.matsim.core.mobsim.queuesim.DriverAgent;
import org.matsim.core.mobsim.queuesim.QueueSimulation;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.population.PopulationImpl;

import playground.christoph.knowledge.container.dbtools.KnowledgeDBStorageHandler;

/*
 * This extended QueueSimulation contains some methods that
 * are needed for the WithinDay Replanning Modules.
 * 
 * Some other methods are used for the Knowledge Modules. They
 * should be separated somewhen but at the moment this seems
 * to be difficult so they remain here for now...
 */
public class ReplanningQueueSimulation extends QueueSimulation{

	private final static Logger log = Logger.getLogger(ReplanningQueueSimulation.class);
	
	protected KnowledgeDBStorageHandler knowledgeDBStorageHandler;
		
	public ReplanningQueueSimulation(final NetworkLayer network, final PopulationImpl population, final EventsImpl events)
	{
		super(network, population, events);
		
		// use WithinDayAgentFactory that creates WithinDayPersonAgents who can reset their chachedNextLink
		super.setAgentFactory(new WithinDayAgentFactory(this));

		// New QueueSimeEngines that do some parts of the simulation parallel
//		this.simEngine = new ParallelQueueSimEngine(this.getQueueNetwork(), MatsimRandom.getRandom());
//		this.simEngine = new ParallelQueueSimEngine2(this.getQueueNetwork(), MatsimRandom.getRandom());
//		this.simEngine = new ParallelQueueSimEngine3(this.getQueueNetwork(), MatsimRandom.getRandom());
//		this.simEngine = new ParallelQueueSimEngine4(this.getQueueNetwork(), MatsimRandom.getRandom());
	}
	
	/*
	 * Used by the Activity End Replanning Module.
	 * This contains all Agents that are going to end their Activities.
	 */
	public PriorityBlockingQueue<DriverAgent> getActivityEndsList()
	{
		return super.activityEndsList;
	}

	public void useKnowledgeStorageHandler(boolean value)
	{
		if (value && knowledgeDBStorageHandler == null)
		{
			this.knowledgeDBStorageHandler = new KnowledgeDBStorageHandler(this.plans);
			this.knowledgeDBStorageHandler.start();
			((EventsImpl)getEvents()).addHandler(knowledgeDBStorageHandler);
			this.addQueueSimulationListeners(this.knowledgeDBStorageHandler);
		}
		else
		{
			if (this.knowledgeDBStorageHandler != null)
			{
				this.knowledgeDBStorageHandler.stopHandler();
				((EventsImpl)getEvents()).removeHandler(knowledgeDBStorageHandler);
			}
		}
	}

	/*
	 * for the Knowledge Modules
	 */
	@Override
	protected void scheduleActivityEnd(final DriverAgent agent)
	{	
		if (knowledgeDBStorageHandler != null) knowledgeDBStorageHandler.scheduleActivityEnd(agent);
//		offsetActivityEndsList.add(agent);
		super.scheduleActivityEnd(agent);
	}
}
