/* *********************************************************************** *
 * project: org.matsim.*
 * WithinDayQueueSimulation.java
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
import org.matsim.core.events.EventsManagerImpl;
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
public class WithinDayQueueSimulation extends QueueSimulation{

	private final static Logger log = Logger.getLogger(WithinDayQueueSimulation.class);
	
	protected KnowledgeDBStorageHandler knowledgeDBStorageHandler;
	

	public WithinDayQueueSimulation(final NetworkLayer network, final PopulationImpl population, final EventsManagerImpl events)
	{
		super(network, population, events);
		
		// use WithinDayAgentFactory that creates WithinDayPersonAgents who can reset their chachedNextLink
		super.setAgentFactory(new WithinDayAgentFactory(this));
	}
			
	public PriorityBlockingQueue<DriverAgent> getActivityEndsList()
	{
		return super.activityEndsList;
	}
}
