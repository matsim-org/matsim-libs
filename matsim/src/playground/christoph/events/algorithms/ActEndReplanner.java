/* *********************************************************************** *
 * project: org.matsim.*
 * ActEndReplanner.java
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

package playground.christoph.events.algorithms;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.mobsim.queuesim.QueueSimulation;
import org.matsim.core.mobsim.queuesim.QueueVehicle;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.population.routes.NetworkRouteWRefs;
import org.matsim.population.algorithms.PlanAlgorithm;

import playground.christoph.events.ExtendedAgentReplanEventImpl;
import playground.christoph.network.util.SubNetworkTools;

/*
 * This is a EventHandler that replans the Route of a Person every time an
 * Activity has ended. Alternatively it can also be called "by Hand".
 * 
 * MATSim Routers use Plan as Input Data. To be able to use them, we have to create
 * a new Plan from the current Position to the location of the next Activity.
 * 
 * If a Person had just ended one Activity (ActEndEvent), a new Plan is created which 
 * contains this and the next Activity and the Leg between them.
 */

public class ActEndReplanner {
	
	protected PlanAlgorithm replanner;
	protected QueueVehicle vehicle;
	protected PersonImpl person;
	protected ActivityImpl fromAct;
	protected LegImpl betweenLeg;
	protected ActivityImpl toAct;
	protected double time;
	
	protected PopulationImpl population;
	
	private static final Logger log = Logger.getLogger(ActEndReplanner.class);

	public ActEndReplanner(ActivityImpl fromAct, QueueVehicle vehicle, double time, PlanAlgorithm replanner)
	{
		this.fromAct = fromAct;
		this.vehicle = vehicle;
		this.person = (PersonImpl) vehicle.getDriver().getPerson();
		this.time = time;
		this.replanner = replanner;

		Plan plan = person.getSelectedPlan();
		this.betweenLeg = ((PlanImpl) plan).getNextLeg(fromAct);
	
		if(betweenLeg != null)
		{
			toAct = (ActivityImpl)((PlanImpl) plan).getNextActivity(betweenLeg);
		}
		else 
		{
			toAct = null;
			log.error("An agents next activity is null - this should not happen!");
		}
		
		// calculate new Route
		if(toAct != null && replanner != null)
		{	
			doReplanning();
		}
	}
	
//	// used when starting the Replanner "by hand"
//	public ActEndReplanner(ActivityImpl fromAct, PersonImpl person, double time, PlanAlgorithm replanner)
//	{
//		this.fromAct = fromAct;
//		this.person = person;
//		this.time = time;
//		this.replanner = replanner;
//
//		Plan plan = person.getSelectedPlan();
//		this.betweenLeg = ((PlanImpl) plan).getNextLeg(fromAct);
//	
//		if(betweenLeg != null)
//		{
//			toAct = ((PlanImpl) plan).getNextActivity(betweenLeg);
//		}
//		else 
//		{
//			toAct = null;
//			log.error("An agents next activity is null - this should not happen!");
//		}
//		
//		// calculate new Route
//		if(toAct != null && replanner != null)
//		{	
//			doReplanning();
//		}
//	}

	/*
	 * Idea:
	 * MATSim Routers create Routes for complete plans.
	 * We just want to replan the Route from one Activity to another one, so we create
	 * a new Plan that contains only this Route. This Plan is replanned by sending it
	 * to the Router.
	 *
	 * Attention! The Replanner is started when the Activity of a Person ends and the Vehicle 
	 * is added to the Waiting List of its QueueLink. That means that a Person replans 
	 * his Route at time A but enters the QueueLink at time B.
	 * A short example: If all Persons of a network end their Activities at the same time 
	 * and have the same Start- and Endpoints of their Routes they will all use the same
	 * route (if they use the same router). If they would replan their routes when they really
	 * Enter the QueueLink this would not happen because the enter times would be different
	 * due to the limited number of possible vehicles on a link at a time. An implementation
	 * of such a functionality would be a problem due to the structure of MATSim...
	 */
//	protected void Routing(Act fromAct, Leg nextLeg)
	protected void doReplanning()
	{	
		// Replace the EndTime with the current time - do we really need this?
//		fromAct.setEndTime(time);
		
		// save currently selected plan
		Plan currentPlan = person.getSelectedPlan();
		
		// Create new Plan and select it.
		// This Plan contains only the just ended and the next Activity.
		// -> That's the only Part of the Route that we want to replan.
		PlanImpl newPlan = new PlanImpl(person);
		person.addPlan(newPlan);
		person.setSelectedPlan(newPlan);
		
		// Here we are at the moment.
		newPlan.addActivity(fromAct);
		
		Route originalRoute = betweenLeg.getRoute();
		
		// Current Route between fromAct and toAct - this Route shall be replanned.
		newPlan.addLeg(betweenLeg);
		
		// We still want to go there...
		newPlan.addActivity(toAct);
					
//		NodeImpl startNode = fromAct.getLink().getToNode();	// start at the end of the "current" link
//		NodeImpl endNode = toAct.getLink().getFromNode(); // the target is the start of the link
//		
//		SubNetwork subNetwork = new SubNetworkTools().getSubNetwork(person);
//		
//		if(subNetwork.person.getId() != person.getId())
//		{
//			log.error("Wrong SubNetwork!");
//		}
//		if(!subNetwork.getNodes().containsKey(startNode.getId()))
//		{
//			log.error("Key not Contained!");
//		}
//		if(!subNetwork.getNodes().containsKey(startNode.getId()))
//		{
//			log.error("Key not Contained!");
//		}
//		
//		if(!new KnowledgeTools().getNodeKnowledge(person).getKnownNodes().containsKey(startNode.getId()))
//		{
//			log.error("Key never was Contained! " + person.getId() + " " + startNode.getId());
//			log.error("Size: " + new KnowledgeTools().getNodeKnowledge(person).getKnownNodes().size());
//
//			for(NodeImpl node : new KnowledgeTools().getNodeKnowledge(person).getKnownNodes().values())
//			{
//				log.info(node.getId());
//			}
//		}
//		NodeKnowledge nodeKnowledge = new KnowledgeTools().getNodeKnowledge(person);
//		if(!new KnowledgeTools().getNodeKnowledge(person).getKnownNodes().containsKey(endNode.getId()))
//		{
//			log.error("Key never was Contained! " + person.getId() + " " + startNode.getId());
//		}
		
		// By doing the replanning the "betweenLeg" is replanned, so the changes are
		// included in the previously selected plan, too!
		replanner.run(newPlan);
		
		new SubNetworkTools().resetSubNetwork(person);
		
		// reactivate previously selected, replanned plan
		person.setSelectedPlan(currentPlan);
		
		// remove previously added new Plan
		person.removePlan(newPlan);
//		System.out.println("Do Replanning...");
		
		double tt = betweenLeg.getTravelTime();
		if(tt > 2147483640)
		{
			log.warn("To long TravelTime??? " + ((long)tt));
		}

		Route alternativeRoute = betweenLeg.getRoute();

		// set VehicleId in original Route as well as in the alternative Route
		((NetworkRouteWRefs)originalRoute).setVehicleId(this.vehicle.getId());
		((NetworkRouteWRefs)alternativeRoute).setVehicleId(this.vehicle.getId());
		
		// create ReplanningEvent
		QueueSimulation.getEvents().processEvent(new ExtendedAgentReplanEventImpl(time, person.getId(), (NetworkRouteWRefs)alternativeRoute, (NetworkRouteWRefs)originalRoute));
		

//		String newRouteString = "PersonId: " + person.getId();
//		newRouteString = newRouteString + "; LinkCount: " + ((NetworkRoute)betweenLeg.getRoute()).getLinks().size() + ";";
//		for (Link link : ((NetworkRoute)betweenLeg.getRoute()).getLinks())
//		{
//			newRouteString = newRouteString + " " + link.getId();
//		}
//		
//		log.info(newRouteString);
	}
	
}