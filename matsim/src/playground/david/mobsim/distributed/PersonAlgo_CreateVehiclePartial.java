/* *********************************************************************** *
 * project: org.matsim.*
 * PersonAlgo_CreateVehiclePartial.java
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

package playground.david.mobsim.distributed;

import java.util.Iterator;
import java.util.List;

import org.matsim.gbl.Gbl;
import org.matsim.mobsim.PersonAlgo_CreateVehicle;
import org.matsim.mobsim.QueueNode;
import org.matsim.mobsim.SimulationTimer;
import org.matsim.mobsim.Vehicle;
import org.matsim.plans.Act;
import org.matsim.plans.Person;
import org.matsim.plans.Plan;

public class PersonAlgo_CreateVehiclePartial extends PersonAlgo_CreateVehicle {

	public int myID = 0;
		//////////////////////////////////////////////////////////////////////
	// run Method, creates a new Vehicle for every person
	//////////////////////////////////////////////////////////////////////

	protected int findStartLink(List actslegs) {
		boolean isFirstLeg = true; //DS TODO is that right? 
		double now = SimulationTimer.getTime();
		// if we are in initSimulation then loop over activites and find 
		// next activity, that has not yet ended, else just take next activity
		// (and let that start either now+ Activity-Duration or Activity End Time)
		// leave out the final act, we will not need a veh any more than
		for (int jj = 0; jj < actslegs.size()-1; jj+=2) {
			Act act = (Act)actslegs.get(jj);
			double departure = act.getEndTime();
			
			if(isFirstLeg)
			{
				if (departure < now) continue;
				SimulationTimer.updateSimStartTime(departure);
			} else {
				// WELL, THAT'S IMPORTANT:
				// The person leaves the activity either 'actDur' later or 
				// when the end is defined of the activity,
				// Whatever comes first.
				// Check for UNDEF == MIN_VALUE first, otherwiese the Math.min will not calc correct
				if (act.getDur() == Gbl.UNDEFINED_TIME) departure = act.getEndTime();
				else if (act.getEndTime() == Gbl.UNDEFINED_TIME)departure = now + act.getDur();
				else departure = Math.min(act.getEndTime(), now + act.getDur());
			}
			
			// this is the starting point for our vehicle, so put it in the queue
			QueueNode actNode = (QueueNode)act.getLink().getToNode();
			return actNode.getPartitionID();
		}
		return -1;
	}

	/* (non-Javadoc)
	 * @see org.matsim.plans.algorithms.PersonAlgorithm#run(org.matsim.plans.Person)
	 */
	public void run(Person person) {
		// Choose Plan to follow
		List plaene = person.getPlans();
		for( Iterator i = plaene.iterator(); i.hasNext(); )
		{
			Plan actPlan = (Plan)i.next();
			if (actPlan.isSelected())
			{
				// Create vehicle
				// Add vehicle to simulation
				int startlinkpart = findStartLink(actPlan.getActsLegs());
				if (startlinkpart != myID) break;
				
				Vehicle veh = new Vehicle();
				veh.setActLegs(actPlan.getActsLegs());
				veh.setDriverID(person.getId().toString());
				veh.initVeh();
				break; // should be only ONE selected plan per person
			}
		}
	}
}
