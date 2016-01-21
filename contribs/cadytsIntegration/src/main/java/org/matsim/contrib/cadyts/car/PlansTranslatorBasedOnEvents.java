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

package org.matsim.contrib.cadyts.car;

import java.util.HashSet;
import java.util.Set;

import javax.inject.Inject;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.VehicleLeavesTrafficEvent;
import org.matsim.api.core.v01.events.VehicleEntersTrafficEvent;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.events.handler.VehicleLeavesTrafficEventHandler;
import org.matsim.api.core.v01.events.handler.VehicleEntersTrafficEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.contrib.cadyts.general.CadytsConfigGroup;
import org.matsim.contrib.cadyts.general.PlansTranslator;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.algorithms.Vehicle2DriverEventHandler;

import cadyts.demand.PlanBuilder;

public class PlansTranslatorBasedOnEvents implements PlansTranslator<Link>, LinkLeaveEventHandler, 
VehicleEntersTrafficEventHandler, VehicleLeavesTrafficEventHandler {
	
	private static final Logger log = Logger.getLogger(PlansTranslatorBasedOnEvents.class);

	private final Scenario scenario;

	private Vehicle2DriverEventHandler delegate = new Vehicle2DriverEventHandler();
	
	private int iteration = -1;

	// this is _only_ there for output:
	Set<Plan> plansEverSeen = new HashSet<>();

	private static final String STR_PLANSTEPFACTORY = "planStepFactory";
	private static final String STR_ITERATION = "iteration";

	private final Set<Id<Link>> calibratedLinks = new HashSet<>() ;

	@Inject
	PlansTranslatorBasedOnEvents(final Scenario scenario) {
		this.scenario = scenario;
		Set<String> abc = ConfigUtils.addOrGetModule(scenario.getConfig(), CadytsConfigGroup.GROUP_NAME, CadytsConfigGroup.class).getCalibratedItems();
		for ( String str : abc ) {
			this.calibratedLinks.add( Id.createLinkId(str) ) ;
		}
	}

	private long plansFound = 0;
	private long plansNotFound = 0;

	@Override
	public final cadyts.demand.Plan<Link> getCadytsPlan(final Plan plan) {
		@SuppressWarnings("unchecked")
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
		
		delegate.reset(iteration);
	}

	@Override
	public void handleEvent(VehicleEntersTrafficEvent event) {
		if (event.getNetworkMode().equals(TransportMode.car))
			delegate.handleEvent(event);
	}
	
	@Override
	public void handleEvent(VehicleLeavesTrafficEvent event) {
		delegate.handleEvent(event);
	}
	
	@Override
	public void handleEvent(LinkLeaveEvent event) {
		
		Id<Person> driverId = delegate.getDriverOfVehicle(event.getVehicleId());
		
		// if it is not a car, ignore the event
		if (driverId == null) 
			return;
		
		// if only a subset of links is calibrated but the link is not contained, ignore the event
		if (!calibratedLinks.contains(event.getLinkId())) 
			return;
		
		// get the "Person" behind the id:
		Person person = this.scenario.getPopulation().getPersons().get(driverId);
		
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
