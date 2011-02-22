/* *********************************************************************** *
 * project: org.matsim.*
 * Restarter.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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

package playground.christoph.qsim;

import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.core.api.experimental.events.Event;
import org.matsim.core.api.experimental.events.EventsFactory;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.events.handler.EventHandler;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.framework.PlanAgent;
import org.matsim.core.mobsim.framework.events.SimulationInitializedEvent;
import org.matsim.core.mobsim.framework.listeners.SimulationInitializedListener;
import org.matsim.ptproject.qsim.QSim;

public class Restarter implements SimulationInitializedListener {

	private static Logger log = Logger.getLogger(Restarter.class);
	
	private QSim sim;
	private String eventsFile;
	private double endTime = 0.0;
	
	private Map<Id, Integer> activityPerforming;
	private Map<Id, Integer> legPerforming;
	
	@Override
	public void notifySimulationInitialized(SimulationInitializedEvent e) {
		if (!(e.getQueueSimulation() instanceof QSim)) {
			log.error("Simulation is not from type QSim - cannot restart it!");
			return;
		}
		else sim = (QSim) e.getQueueSimulation();
		
		EventsManager eventsManager = sim.getEventsManager();
		EventsManagerWrapper eventsManagerWrapper = new EventsManagerWrapper(eventsManager, endTime);
		
		new MatsimEventsReader(eventsManager).readFile(this.eventsFile);

		
//		log.info("filtering events...");
//		EventsManager writerManager = new EventsManagerImpl();
//		EventWriterXML xmlWriter = new EventWriterXML(this.eventsFileOut);
//		writerManager.addHandler(xmlWriter);
//
//		EventsManager events = new EventsManagerImpl();
//		events.addHandler(eventsFilter);
//		xmlWriter.closeFile();
//		((EventsManagerImpl) events).printEventsCount();
		
		/*
		 * TODO:
		 * - get number of activities per agent
		 * - parse events and identify number of alive agents
		 * 
		 * - how to fill the agents in the simulation?
		 * -> if necessary, add an "ignore event" switch to the EventsManagerWrapper...
		 */
		
//		sim.getAgents();
		
//		sim.getAgentCounter().setLiving(count)
		
//		sim.getSimTimer().setSimStartTime(startTimeSec)
		
		for (MobsimAgent mobsimAgent : sim.getAgents()) {
			PlanAgent agent = (PlanAgent) mobsimAgent;
			
			if (activityPerforming.containsKey(agent.getId())) {
				int planElementIndex = activityPerforming.get(agent.getId());
				setPlanElementIndex(agent, planElementIndex);
				sim.scheduleActivityEnd(agent);
				
			} else if (legPerforming.containsKey(agent.getId())) {
				int planElementIndex = legPerforming.get(agent.getId());
				setPlanElementIndex(agent, planElementIndex);
				
				sim.getNetsimNetwork();
			}
//		sim.scheduleActivityEnd(agent)
			
		}
		
		
	}

	private void setPlanElementIndex(PlanAgent agent, int index) {
		// TODO
	}
	
	/*
	 * Hand over events until a given EndTime.
	 * After that time basically an abort Exception could be created...
	 */
	private static class EventsManagerWrapper implements EventsManager {

		private EventsManager eventsManager;
		private double endTime;
		
		public EventsManagerWrapper(EventsManager eventsManager, double endTime) {
			this.eventsManager = eventsManager;
			this.endTime = endTime;
		}
		
		@Override
		public void addHandler(EventHandler handler) {
			eventsManager.addHandler(handler);
		}

		@Override
		public EventsFactory getFactory() {
			return eventsManager.getFactory();
		}

		@Override
		public void processEvent(Event event) {
			if (event.getTime() <= endTime) eventsManager.processEvent(event);
		}

		@Override
		public void removeHandler(EventHandler handler) {
			eventsManager.removeHandler(handler);
		}
		
	}
}
