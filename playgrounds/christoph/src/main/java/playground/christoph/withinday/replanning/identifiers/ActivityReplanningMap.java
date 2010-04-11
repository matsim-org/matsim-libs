/* *********************************************************************** *
 * project: org.matsim.*
 * ActivityReplanningMap
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
package playground.christoph.withinday.replanning.identifiers;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.core.api.experimental.events.ActivityEndEvent;
import org.matsim.core.api.experimental.events.ActivityStartEvent;
import org.matsim.core.api.experimental.events.AgentStuckEvent;
import org.matsim.core.api.experimental.events.handler.ActivityEndEventHandler;
import org.matsim.core.api.experimental.events.handler.ActivityStartEventHandler;
import org.matsim.core.api.experimental.events.handler.AgentStuckEventHandler;
import org.matsim.ptproject.qsim.QLink;
import org.matsim.ptproject.qsim.QNetwork;
import org.matsim.ptproject.qsim.QSim;
import org.matsim.ptproject.qsim.QVehicle;
import org.matsim.core.mobsim.framework.PersonDriverAgent;
import org.matsim.core.mobsim.framework.events.SimulationAfterSimStepEvent;
import org.matsim.core.mobsim.framework.events.SimulationInitializedEvent;
import org.matsim.core.mobsim.framework.listeners.SimulationAfterSimStepListener;
import org.matsim.core.mobsim.framework.listeners.SimulationInitializedListener;

/*
 * This Module is used by a NextLegReplanner. It calculates the time
 * when an agent should do NextLegReplanning.
 * 
 * When an ActivityStartEvent is thrown the Replanning Time is set to
 * the scheduled departure Time of the Activity.
 */
public class ActivityReplanningMap implements AgentStuckEventHandler, 
		ActivityStartEventHandler, ActivityEndEventHandler,
		SimulationInitializedListener, SimulationAfterSimStepListener {
		
	private QNetwork qNetwork;
	
	private static final Logger log = Logger.getLogger(ActivityReplanningMap.class);

	/*
	 * Agents that have started an Activity in the current Time Step.
	 */
	private Map<Id, PersonDriverAgent> startingAgents;	// PersonId, PersonDriverAgent
	
	/*
	 * Contains the Agents that are currently performing an Activity
	 * as well as the replanning time.
	 */
	private Map<PersonDriverAgent, Double> replanningMap;	// PersonDriverAgent, ReplanningTime	

	/*
	 * Mapping between the PersonDriverAgents and the PersonIds.
	 * The events only contain a PersonId.
	 */
	private Map<Id, PersonDriverAgent> personAgentMapping;	// PersonId, PersonDriverAgent
	
	public ActivityReplanningMap(QSim qSim)
	{
		this.qNetwork = qSim.getQNetwork();
		
		// add ActivityReplanningMap to the QueueSimulation's EventsManager
		qSim.getEventsManager().addHandler(this);
		
		// add ActivityReplanningMap to the QueueSimulation's SimulationListeners
		qSim.addQueueSimulationListeners(this);
		
		// use a list that is ordered by the departure time
		this.replanningMap = new TreeMap<PersonDriverAgent, Double>(new DriverAgentDepartureTimeComparator());
		
		this.startingAgents = new TreeMap<Id, PersonDriverAgent>();
	}
	
	/*
	 * When the simulation starts the agents are all performing an activity.
	 * There is no activity start event so we have to collect by hand by
	 * iterating over all links and vehicles.
	 * 
	 * Additionally we create the Mapping between the PersonIds and the
	 * PersonDriverAgents.
	 */
	public void notifySimulationInitialized(SimulationInitializedEvent e)
	{	
		personAgentMapping = new TreeMap<Id, PersonDriverAgent>();
		
		for (QLink qLink : qNetwork.getLinks().values())
		{		
			for (QVehicle vehicle : qLink.getAllVehicles())
			{
				PersonDriverAgent personDriverAgent = vehicle.getDriver();
				double departureTime = personDriverAgent.getDepartureTime();				

				replanningMap.put(personDriverAgent, departureTime);
				
				personAgentMapping.put(personDriverAgent.getPerson().getId(), personDriverAgent);
			}
		}
	}
	
	/*
	 * The Activity Start Events are thrown before the Activity Start has been handled
	 * by the Simulation. As a result the Departure Time is not set at that time.
	 * We have to wait until the SimStep has been fully simulated and retrieve
	 * the Activity DepatureTimes then. 
	 */
	public void notifySimulationAfterSimStep(SimulationAfterSimStepEvent e)
	{
		for (PersonDriverAgent personDriverAgent : startingAgents.values())
		{
			double now = e.getSimulationTime();
			double departureTime = personDriverAgent.getDepartureTime();
					
			/*
			 * If it is the last scheduled Activity the departureTime is -infinity.
			 * Otherwise we select the agent for a replanning.
			 */
			if (departureTime >= now)
			{
				replanningMap.put(personDriverAgent, departureTime);
			}
			else
			{
				log.warn("Departure time is in the past - ignoring activity!");
			}
		}
		startingAgents.clear();
	}
	
	/*
	 * Collect the Agents that are starting an Activity in
	 * the current Time Step.
	 */
	public void handleEvent(ActivityStartEvent event)
	{
		Id id = event.getPersonId();
		PersonDriverAgent personDriverAgent = personAgentMapping.get(id);
//		if (startingAgents.containsKey(id)) System.out.println("already contained???");
		this.startingAgents.put(id, personDriverAgent);
	}
	
	/*
	 * Nothing to do here?
	 * Agents should be removed from the map if their
	 * replanning time has come.
	 */
	public void handleEvent(ActivityEndEvent event)
	{	
//		if (startingAgents.containsKey(event.getPersonId())) System.out.println("Zero Activity found!");
		replanningMap.remove(personAgentMapping.get(event.getPersonId()));
	}

	/*
	 * Nothing to do here?
	 * Agents should not be removed from the simulation
	 * if they are performing an activity.
	 */
	public void handleEvent(AgentStuckEvent event)
	{
//		replanningMap.remove(personAgentMapping.get(event.getPersonId()));
	}
	
	public synchronized List<PersonDriverAgent> getReplanningDriverAgents(double time)
	{
		ArrayList<PersonDriverAgent> personDriverAgentsToReplanActivityEnd = new ArrayList<PersonDriverAgent>();

		Iterator<Entry<PersonDriverAgent, Double>> entries = replanningMap.entrySet().iterator();
		while (entries.hasNext())
		{
			Entry<PersonDriverAgent, Double> entry = entries.next();
			PersonDriverAgent personDriverAgent = entry.getKey();
          	
			double replanningTime = entry.getValue();
	       
			if (time >= replanningTime)
			{
				entries.remove();
				personDriverAgentsToReplanActivityEnd.add(personDriverAgent);
			}
			else break;
		}
		
		return personDriverAgentsToReplanActivityEnd;
	}

	public void reset(int iteration)
	{
		replanningMap.clear();
		personAgentMapping.clear();
	}
	
	private class DriverAgentDepartureTimeComparator implements Comparator<PersonDriverAgent>, Serializable {

		private static final long serialVersionUID = 1L;

		public int compare(PersonDriverAgent agent1, PersonDriverAgent agent2) {
			int cmp = Double.compare(agent1.getDepartureTime(), agent2.getDepartureTime());
			if (cmp == 0) {
				// Both depart at the same time -> let the one with the larger id be first (=smaller)
				return agent2.getPerson().getId().compareTo(agent1.getPerson().getId());
			}
			return cmp;
		}
	}
	
}
