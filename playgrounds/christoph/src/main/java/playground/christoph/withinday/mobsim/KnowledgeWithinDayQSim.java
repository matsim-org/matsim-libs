/* *********************************************************************** *
 * project: org.matsim.*
 * KnowledgeWithinDayQSim.java
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

package playground.christoph.withinday.mobsim;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.mobsim.framework.PersonDriverAgent;

import playground.christoph.knowledge.container.dbtools.KnowledgeDBStorageHandler;

/*
 * This extended QSim contains some methods that
 * are needed for the WithinDay Replanning Modules.
 * 
 * Some other methods are used for the Knowledge Modules. They
 * should be separated somewhen but at the moment this seems
 * to be difficult so they remain here for now...
 */
public class KnowledgeWithinDayQSim extends WithinDayQSim{

	private final static Logger log = Logger.getLogger(KnowledgeWithinDayQSim.class);
	
	protected KnowledgeDBStorageHandler knowledgeDBStorageHandler;
	
	public KnowledgeWithinDayQSim(final Scenario scenario, final EventsManager events)
	{
		super(scenario, events);
	}

	public void useKnowledgeStorageHandler(boolean value)
	{
		if (value && (knowledgeDBStorageHandler == null))
		{
			this.knowledgeDBStorageHandler = new KnowledgeDBStorageHandler(this.population);
			this.knowledgeDBStorageHandler.start();
			getEventsManager().addHandler(knowledgeDBStorageHandler);
			this.addQueueSimulationListeners(this.knowledgeDBStorageHandler);
		}
		else
		{
			if (this.knowledgeDBStorageHandler != null)
			{
				this.knowledgeDBStorageHandler.stopHandler();
				getEventsManager().removeHandler(knowledgeDBStorageHandler);
			}
		}
	}

	/*
	 * for the Knowledge Modules
	 */
	@Override
	public void scheduleActivityEnd(final PersonDriverAgent driverAgent, int planElementIndex)
	{	
		if (knowledgeDBStorageHandler != null) knowledgeDBStorageHandler.scheduleActivityEnd(driverAgent);
//		offsetActivityEndsList.add(agent);
		super.scheduleActivityEnd(driverAgent, planElementIndex);
	}
}
