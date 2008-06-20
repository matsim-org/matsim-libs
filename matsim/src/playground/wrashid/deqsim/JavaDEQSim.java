/* *********************************************************************** *
 * project: org.matsim.*
 * JavaDEQSim.java
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

package playground.wrashid.deqsim;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

import org.matsim.basic.v01.Id;
import org.matsim.events.Events;
import org.matsim.network.Link;
import org.matsim.network.NetworkLayer;
import org.matsim.plans.Act;
import org.matsim.plans.Leg;
import org.matsim.plans.Person;
import org.matsim.plans.Plan;
import org.matsim.plans.Plans;

import playground.wrashid.PDES.Road;
import playground.wrashid.PDES.Scheduler;
import playground.wrashid.PDES.Vehicle;

public class JavaDEQSim {

	final Plans population;
	final NetworkLayer network;
	
	public JavaDEQSim(final NetworkLayer network, final Plans population, final Events events) {
		// constructor
		this.population = population;
		this.network = network;
		
	}
	
	public void run() {
		Scheduler scheduler=new Scheduler();
		
		// initialize network
		Road road=null;
		for (Link link: network.getLinks().values()){
			road= new Road(scheduler,link);
		}
		
		
		
		// initialize vehicles
		
		
		Vehicle vehicle=null;
		for (Person person : this.population.getPersons().values()) {
			vehicle =new Vehicle(scheduler,person);
			/*
			Plan plan = person.getSelectedPlan(); // that's the plan the person will execute
			ArrayList<Object> actsLegs = plan.getActsLegs();
			for (int i = 0; i < actsLegs.size(); i++) {
				if (i % 2 == 0) {
					Act act = (Act)actsLegs.get(i);
					// the activity the agent performs
					double departureTime = act.getEndTime(); // the time the agent departs at this activity
				} else {
					Leg leg = (Leg)actsLegs.get(i);
					// the leg the agent performs
					if ("car".equals(leg.getMode())) { // we only simulate car traffic
						Link[] route = leg.getRoute().getLinkRoute(); // these are the links the agent will drive along one after the other.
					}
				}
			}
			*/
		}
		
		scheduler.startSimulation();
	}
}
