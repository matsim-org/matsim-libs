/* *********************************************************************** *
 * project: org.matsim.*
 * WithindayAgent.java
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

package org.matsim.withinday;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.mobsim.queuesim.SimulationTimer;
import org.matsim.network.Link;
import org.matsim.network.Node;
import org.matsim.population.Act;
import org.matsim.population.Leg;
import org.matsim.population.Person;
import org.matsim.population.Plan;
import org.matsim.population.Route;
import org.matsim.scoring.PlanScorer;
import org.matsim.scoring.ScoringFunctionFactory;
import org.matsim.utils.collections.Tuple;
import org.matsim.withinday.beliefs.AgentBeliefs;
import org.matsim.withinday.contentment.AgentContentment;
import org.matsim.withinday.mobsim.OccupiedVehicle;
import org.matsim.withinday.percepts.AgentPercepts;
import org.matsim.withinday.routeprovider.RouteProvider;

/**
 * @author dgrether
 *
 */
public class WithindayAgent {

	private static final Logger log = Logger.getLogger(WithindayAgent.class);

	private Person person;

	private OccupiedVehicle vehicle;

	private AgentBeliefs beliefs;

	private RouteProvider desireGenerationFunction;

	private AgentContentment contentment;

	private ScoringFunctionFactory scoringFunctionFactory;

	private PlanScorer planScorer;

	private double replanningInterval;

	private double lastReplaningTimeStep;

	private double replanningThreshold;

	private List<AgentPercepts> percepts;

	public WithindayAgent(final Person person, final OccupiedVehicle v, final int sightDistance, final WithindayAgentLogicFactory factory) {
		this.person = person;
		this.vehicle = v;
		this.lastReplaningTimeStep = 0.0d;
	//place the agent in the vehicle
		this.vehicle.setAgent(this);
	//set the agents desire generation module
		this.desireGenerationFunction = factory.createRouteProvider();
	//set agent's contentment
		this.contentment = factory.createAgentContentment(this);
		//set the scoring function factory
		this.scoringFunctionFactory = factory.createScoringFunctionFactory();
		this.planScorer = new PlanScorer(this.scoringFunctionFactory);
		//set the agent's beliefs
		Tuple<AgentBeliefs, List<AgentPercepts>> tuple = factory.createAgentPerceptsBeliefs(sightDistance);
		this.beliefs = tuple.getFirst();
		this.percepts = tuple.getSecond();
	}

	private void revisePercepts() {
		for (AgentPercepts p : this.percepts) {
			p.updatedPercepts(this.vehicle.getCurrentNode());
		}
	}


	public void replan() {
		//check if replanning is allowed
		if (SimulationTimer.getTime() >= (this.replanningInterval + this.lastReplaningTimeStep)) {
			/*if (log.isTraceEnabled()) {
				log.trace("Agent " + this.person.getId() + " requested to replan...");
			}*/
			//let the agent look out of his window if he is able to do this
			this.revisePercepts();
			double replanningNeed = this.getReplanningNeed();
			if (replanningNeed >= this.replanningThreshold) {
				Link currentLink = this.vehicle.getCurrentLink();
				Node currentToNode = currentLink.getToNode();
				Node currentDestinationNode = this.vehicle.getDestinationLink().getFromNode();
				//as replanning is rerouting agents will only replan if they are on the road and not on the link of the next activity
				if (isEnRoute()) {
					//only reroute if the RouteProvider provides a route
					Route subRoute = this.vehicle.getCurrentRoute().getSubRoute(currentToNode, currentDestinationNode);
					if (this.desireGenerationFunction.providesRoute(currentLink, subRoute)) {
						this.reroute();
					}
					/*else if (log.isTraceEnabled()) {
						log.trace("...but his desireGenerationFunction doesn't generate an appropriate option.");
					}*/
				}
			}
			else if (log.isTraceEnabled()) {
				log.trace("...but has no need to replan.");
			}
		}
	}

	private void reroute() {
		if (log.isTraceEnabled()) {
			log.trace("");
			log.trace("Starting agent's rerouting...");
			log.trace("agent nr.: " + this.getPerson().getId());
			log.trace("agentposition link: " + this.vehicle.getCurrentLink());
			int hours = (int)SimulationTimer.getTime() / 3600;
			int min = (int) ((SimulationTimer.getTime() - (hours * 60)) / 60);
			log.trace("time: " + hours + ":" + min);
		}
		Link currentLink = this.vehicle.getCurrentLink();
		Act nextAct = this.person.getSelectedPlan().getNextActivity(this.vehicle.getCurrentLeg());
		Link destinationLink = nextAct.getLink();
		Route alternativeRoute = this.desireGenerationFunction.requestRoute(currentLink, destinationLink, SimulationTimer.getTime());
		Plan oldPlan = this.person.getSelectedPlan();
		Leg currentLeg = this.vehicle.getCurrentLeg();

		//create Route of already passed Nodes
		//TODO use Route.getSubroute method
		Node lastPassedNode = currentLink.getFromNode();
		List<Node> oldRouteNodes = currentLeg.getRoute().getRoute();
		Route alreadyPassedNodes = new Route();
		int lastPassedNodeIndex = oldRouteNodes.indexOf(lastPassedNode);
		//this in fact a bit sophisticated construction is needed because Route.setRoute(..) doesn't use the List interface and
		//is bound to a ArrayList instead
		ArrayList<Node> passedNodesList = new ArrayList<Node>();
		if (lastPassedNodeIndex != -1) {
			passedNodesList.addAll(oldRouteNodes.subList(0, lastPassedNodeIndex+1));
			alreadyPassedNodes.setRoute(passedNodesList);
		}
		alreadyPassedNodes.setRoute(passedNodesList);
		//create new plan
		Plan newPlan = new Plan(this.person);
		newPlan.copyPlan(oldPlan);
		//put new route into the new plan
		//first determine index of current leg in the plan
		int currentLegIndex = 0;
		for (int i = 1; i < oldPlan.getActsLegs().size(); i = i + 2)	{
			if (oldPlan.getActsLegs().get(i).equals(currentLeg)) {
				currentLegIndex = i;
				break;
			}
		}
		//remove the old leg in the plan and replace it with the new one
    Leg oldLeg = (Leg) newPlan.getActsLegs().remove(currentLegIndex);
    Leg newLeg = new Leg(oldLeg);
    //concat the Route of already passed nodes with the new route
    ArrayList<Node> newRouteConcatedList = new ArrayList<Node>(alreadyPassedNodes.getRoute().size() + alternativeRoute.getRoute().size());
    newRouteConcatedList.addAll(alreadyPassedNodes.getRoute());
    newRouteConcatedList.addAll(alternativeRoute.getRoute());
    Route newRoute = new Route();
    newRoute.setRoute(newRouteConcatedList);
    //put the new route in the leg and the leg in the plan
    newLeg.setRoute(newRoute);
    newPlan.getActsLegs().add(currentLegIndex, newLeg);
    //score plans and select best
    double currentScore = this.planScorer.getScore(oldPlan);
    double newScore = this.planScorer.getScore(newPlan);
    if (log.isTraceEnabled()) {
    	log.trace("Score of old plan: " + currentScore);
    	log.trace("Score of new plan: " + newScore);
    }
    //put it in vehicle
    //TODO dg remove true  when model of environment provides sufficient information to
    //create different plan scores
    if (/*newScore > currentScore*/ true) {
    	//TODO dg remove
    	if (log.isTraceEnabled()) {
				log.trace("rerouting agent " + this.person.getId() + " with ...");
				StringBuffer buffer = new StringBuffer();
				for (Node n : newRoute.getRoute()) {
					buffer.append(n.getId().toString());
					buffer.append(" ");
				}
	    	log.trace("  new route: " + newRoute + " nodes: " + buffer.toString());
    	}
    	this.person.exchangeSelectedPlan(newPlan, false);
    	this.vehicle.exchangeActsLegs(newPlan.getActsLegs());
    }
	}

	// methods from AgentBrain

	/**
	 * Returns the replanning need. It is the value returned by the agent
	 * contentment with an altered sign an neglecting the negative part.
	 *
	 * @return the replanning need.
	 */
	public double getReplanningNeed() {
		if (this.contentment != null) {
			// TODO make this human readable
			// this was the implementation of AgentBrain.getReplanningNeed()
			double brainContentment = Math.min(1, this.contentment.getContentment()
					* -1);
			// this was the implementation of Agent.getReplanningNeed()
			return Math.max(0, brainContentment);
		}
		return 0;
	}

	/**
	 * Returns if the agent is currently performing a leg. Actually checks if the
	 * departure time is later than the current simulation time
	 *
	 * @return <tt>true</tt> if the agent is currently performing a leg,
	 *         <tt>false</tt> otherwise.
	 */
	public boolean isEnRoute() {
		if (this.vehicle.getDepartureTime_s() > SimulationTimer.getTime()) {
			//TODO remove if exception never thrown (dg oct2007)
			throw new RuntimeException("This should never happen in the new implementation!");
		}
		return true;
	}
	/**
	 * @return the vehicle
	 */
	public OccupiedVehicle getVehicle() {
		return this.vehicle;
	}
	/**
	 * @return the person
	 */
	public Person getPerson() {
		return this.person;
	}

	public void setAgentContentment(final AgentContentment contentment) {
		this.contentment = contentment;
	}

	public AgentBeliefs getBeliefs() {
		return this.beliefs;
	}

	public void setBeliefs(final AgentBeliefs beliefs) {
		this.beliefs = beliefs;
	}

	public void setDesireGenerationModule(final RouteProvider routeProvider) {
		this.desireGenerationFunction = routeProvider;
	}
	/**
	 * @param scoringFunctionFactory the scoringFunctionFactory to set
	 */
	public void setScoringFunctionFactory(
			final ScoringFunctionFactory scoringFunctionFactory) {
		this.scoringFunctionFactory = scoringFunctionFactory;
		this.planScorer = new PlanScorer(this.scoringFunctionFactory);
	}

	public void setReplanningInterval(final double replanningInterval) {
		this.replanningInterval = replanningInterval;
	}
	/**
	 * @param replanningThreshold the replanningThreshold to set
	 */
	public void setReplanningThreshold(final double replanningThreshold) {
		this.replanningThreshold = replanningThreshold;
	}

}
