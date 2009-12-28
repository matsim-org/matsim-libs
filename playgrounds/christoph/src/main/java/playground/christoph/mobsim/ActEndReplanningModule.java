/* *********************************************************************** *
 * project: org.matsim.*
 * ActEndReplanningModule.java
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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.PriorityBlockingQueue;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.mobsim.queuesim.DriverAgent;
import org.matsim.core.mobsim.queuesim.PersonAgent;
import org.matsim.core.mobsim.queuesim.QueueVehicle;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.PersonImpl;

import playground.christoph.events.algorithms.ParallelActEndReplanner;

public class ActEndReplanningModule {

	private final static Logger log = Logger.getLogger(ActEndReplanningModule.class);
	
	protected ReplanningQueueSimulation simulation;
	protected ParallelActEndReplanner parallelActEndReplanner;
	
	public static int replanningCounter = 0;
	
	public ActEndReplanningModule(ParallelActEndReplanner parallelActEndReplanner, ReplanningQueueSimulation simulation)
	{
		this.simulation = simulation;
		this.parallelActEndReplanner = parallelActEndReplanner;
	}
	
	public void doActEndReplanning(double time)
	{		
		// Act End Replanning Objects
		List<QueueVehicle> vehiclesToReplanActEnd = new ArrayList<QueueVehicle>();
		List<PersonImpl> personsToReplanActEnd = new ArrayList<PersonImpl>();
		List<ActivityImpl> fromActActEnd = new ArrayList<ActivityImpl>();
		/*
		 * Checking only Links that leed to active Nodes is not allowed here!
		 * If a Person enters an inative Link, this Link is reactivated - but
		 * this is done when simulating the SimStep, what is to late for us.
		 * Checking if the current QueueNode is active is not allowed here!
		 * So we check every Link within the QueueNetwork.
		 */
		PriorityBlockingQueue<DriverAgent> queue = simulation.getActivityEndsList();
		
		for (DriverAgent driverAgent : queue)
		{			
			// If the Agent will depart
			if (driverAgent.getDepartureTime() <= time)
			{
				// Skip Agent if Replanning Flag is not set
				boolean replanning = (Boolean)driverAgent.getPerson().getCustomAttributes().get("endActivityReplanning");
				if(!replanning) continue; 
				
				PersonImpl person = (PersonImpl) driverAgent.getPerson();
				personsToReplanActEnd.add(person);
				
				PersonAgent pa = (PersonAgent) driverAgent;
						
				// New approach using non deprecated Methods
				// The Person is currently at an Activity and is going to leave it.
				// The Person's CurrentLeg should point to the leg that leads to that Activity...
				List<PlanElement> planElements = person.getSelectedPlan().getPlanElements();
				
				Leg leg = pa.getCurrentLeg();
				
				ActivityImpl fromAct = null;
				
				// first Activity is running - there is no previous Leg
				if (leg == null)
				{
					fromAct = (ActivityImpl)planElements.get(0);
				}
				else
				{
					int index = planElements.indexOf(leg);
					// If the leg is part of the Person's plan
					if (index >= 0)
					{
						fromAct = (ActivityImpl)planElements.get(index + 1);
					}
				}
								
				if (fromAct == null)
				{
					log.error("Found fromAct that is null!");
				}
				else
				{
					fromActActEnd.add(fromAct);
				}
				
				vehiclesToReplanActEnd.add(pa.getVehicle());
			}
			
			// it's a priority Queue -> no further Agents will be found
			else break;
		}
			
		if (vehiclesToReplanActEnd.size() > 0)
		{	
			parallelActEndReplanner.run(fromActActEnd, vehiclesToReplanActEnd, time);
					
			replanningCounter = replanningCounter + vehiclesToReplanActEnd.size();
		}
	}	// actEndReplanning
}
