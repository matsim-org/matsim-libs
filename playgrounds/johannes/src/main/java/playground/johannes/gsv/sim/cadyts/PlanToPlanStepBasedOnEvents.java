/* *********************************************************************** *
 * project: org.matsim.*
 * PlanToPlanStepBasedOnEvents.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package playground.johannes.gsv.sim.cadyts;

import cadyts.demand.PlanBuilder;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.contrib.cadyts.general.PlansTranslator;

import java.util.HashSet;
import java.util.Set;

public class PlanToPlanStepBasedOnEvents implements PlansTranslator<Link>, LinkLeaveEventHandler, 
		PersonDepartureEventHandler, PersonArrivalEventHandler {
	
	private static final Logger log = Logger.getLogger(PlanToPlanStepBasedOnEvents.class);

	private final Scenario scenario;

	private final Set<Id<Person>> driverAgents;
	
	private int iteration = -1;

	// this is _only_ there for output:
	Set<Plan> plansEverSeen = new HashSet<Plan>();

	private static final String STR_PLANSTEPFACTORY = "planStepFactory";
	private static final String STR_ITERATION = "iteration";

	private final Set<Id<Link>> calibratedLinks = new HashSet<>() ;

	PlanToPlanStepBasedOnEvents(final Scenario scenario, final Set<String> calibratedLinks) {
		this.scenario = scenario;
		for ( String str : calibratedLinks ) {
			this.calibratedLinks.add( Id.createLinkId( str ) ) ;
		}
		this.driverAgents = new HashSet<>();
	}

	void addCalibratedItem(Id<Link> link) {
		calibratedLinks.add(link);
	}
	
	private long plansFound = 0;
	private long plansNotFound = 0;

	@Override
	public final cadyts.demand.Plan<Link> getCadytsPlan(final Plan plan) {
		PlanBuilder<Link> planStepFactory = (PlanBuilder<Link>) plan.getCustomAttributes().get(STR_PLANSTEPFACTORY);
		if (planStepFactory == null) {
			this.plansNotFound++;
			return null;
		}
		this.plansFound++;
		final cadyts.demand.Plan<Link> planSteps = planStepFactory.getResult();
		return planSteps;
	}

	@Override
	public void reset(final int iteration) {
		this.iteration = iteration;

		log.warn("found " + this.plansFound + " out of " + (this.plansFound + this.plansNotFound) + " ("
				+ (100. * this.plansFound / (this.plansFound + this.plansNotFound)) + "%)");
		log.warn("(above values may both be at zero for a couple of iterations if multiple plans per agent all have no score)");

		this.driverAgents.clear();
	}

	@Override
	public void handleEvent(PersonDepartureEvent event) {
		if (event.getLegMode().equals(TransportMode.car)) this.driverAgents.add(event.getPersonId());
	}
	
	@Override
	public void handleEvent(PersonArrivalEvent event) {
		this.driverAgents.remove(event.getPersonId());
	}
	
	@Override
	public void handleEvent(LinkLeaveEvent event) {
		
		// if it is not a driver, ignore the event
		Id<Person> personId = Id.createPersonId(event.getVehicleId());
		if (!driverAgents.contains(personId)) return;
		
		// if only a subset of links is calibrated but the link is not contained, ignore the event
		if (calibratedLinks != null && !calibratedLinks.contains(event.getLinkId())) return;
		
		// get the "Person" behind the id:
		Person person = this.scenario.getPopulation().getPersons().get(personId);
		
		// get the selected plan:
		Plan selectedPlan = person.getSelectedPlan();
		
		// get the planStepFactory for the plan (or create one):
		PlanBuilder<Link> tmpPlanStepFactory = getPlanStepFactoryForPlan(selectedPlan);
		
		if (tmpPlanStepFactory != null) {
						
			Link link = this.scenario.getNetwork().getLinks().get(event.getLinkId());
					
			// add the "turn" to the planStepfactory
			tmpPlanStepFactory.addTurn(link, (int) event.getTime());
		}
	}

	// ###################################################################################
	// only private functions below here (low level functionality)

	private PlanBuilder<Link> getPlanStepFactoryForPlan(final Plan selectedPlan) {
		PlanBuilder<Link> planStepFactory = null;

		planStepFactory = (PlanBuilder<Link>) selectedPlan.getCustomAttributes().get(STR_PLANSTEPFACTORY);
		Integer factoryIteration = (Integer) selectedPlan.getCustomAttributes().get(STR_ITERATION);
		if (planStepFactory == null || factoryIteration == null || factoryIteration != this.iteration) {
			// attach the iteration number to the plan:
			selectedPlan.getCustomAttributes().put(STR_ITERATION, this.iteration);

			// construct a new PlanBulder and attach it to the plan:
			planStepFactory = new PlanBuilder<Link>();
			selectedPlan.getCustomAttributes().put(STR_PLANSTEPFACTORY, planStepFactory);

			// memorize the plan as being seen:
			this.plansEverSeen.add(selectedPlan);
		}

		return planStepFactory;
	}

}
