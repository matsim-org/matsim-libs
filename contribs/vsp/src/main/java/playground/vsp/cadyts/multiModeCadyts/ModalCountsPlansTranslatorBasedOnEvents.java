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

package playground.vsp.cadyts.multiModeCadyts;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.inject.Inject;
import cadyts.demand.PlanBuilder;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.VehicleEntersTrafficEvent;
import org.matsim.api.core.v01.events.VehicleLeavesTrafficEvent;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.events.handler.VehicleEntersTrafficEventHandler;
import org.matsim.api.core.v01.events.handler.VehicleLeavesTrafficEventHandler;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.contrib.cadyts.general.PlansTranslator;
import org.matsim.core.events.algorithms.Vehicle2DriverEventHandler;

class ModalCountsPlansTranslatorBasedOnEvents implements PlansTranslator<ModalCountsLinkIdentifier>, LinkLeaveEventHandler,
VehicleEntersTrafficEventHandler, VehicleLeavesTrafficEventHandler {
	
	private static final Logger log = Logger.getLogger(ModalCountsPlansTranslatorBasedOnEvents.class);

	private final Scenario scenario;

	private final Vehicle2DriverEventHandler delegate = new Vehicle2DriverEventHandler();
	private final Map<Id<Person>,String> personId2Mode = new HashMap<>();
	
	private int iteration = -1;

	// this is _only_ there for output:
    private final Set<Plan> plansEverSeen = new HashSet<>();

	private static final String STR_PLANSTEPFACTORY = "planStepFactory";
	private static final String STR_ITERATION = "iteration";

	private final Set<Id<ModalCountsLinkIdentifier>> calibratedLinks ;
	private final Map<Id<ModalCountsLinkIdentifier>,ModalCountsLinkIdentifier> modalLinkContainer;

	@Inject
    ModalCountsPlansTranslatorBasedOnEvents(final Scenario scenario, Map<Id<ModalCountsLinkIdentifier>,ModalCountsLinkIdentifier> modalLinkContainer) {
		this.scenario = scenario;
		this.calibratedLinks = modalLinkContainer.keySet();
		this.modalLinkContainer = modalLinkContainer;
	}

	private long plansFound = 0;
	private long plansNotFound = 0;

	@Override
	public final cadyts.demand.Plan<ModalCountsLinkIdentifier> getCadytsPlan(final Plan plan) {
		@SuppressWarnings("unchecked")
		PlanBuilder<ModalCountsLinkIdentifier> planStepFactory = (PlanBuilder<ModalCountsLinkIdentifier>) plan.getCustomAttributes().get(STR_PLANSTEPFACTORY);
		if (planStepFactory == null) {
			this.plansNotFound++;
			return null;
		}
		this.plansFound++;
		return planStepFactory.getResult();
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
			delegate.handleEvent(event);
			this.personId2Mode.put(event.getPersonId(), event.getNetworkMode());
	}
	
	@Override
	public void handleEvent(VehicleLeavesTrafficEvent event) {
		delegate.handleEvent(event);
		this.personId2Mode.remove(event.getPersonId());
	}
	
	@Override
	public void handleEvent(LinkLeaveEvent event) {
		
		Id<Person> driverId = delegate.getDriverOfVehicle(event.getVehicleId());
		String mode = this.personId2Mode.get(driverId);
		// should not happen since all modes are considered now
		if (driverId == null) throw new RuntimeException("Link leave event "+ event.toString() + ". However, "+event.getVehicleId()+" is not entered traffic."); 
		
		// if only a subset of links is calibrated but the link is not contained, ignore the event

		Id<ModalCountsLinkIdentifier> mlId = ModalCountsUtils.getModalCountLinkId(mode, event.getLinkId());
		if (!calibratedLinks.contains(mlId))
			return;
		
		// get the "Person" behind the id:
		Person person = this.scenario.getPopulation().getPersons().get(driverId);
		
		// get the selected plan:
		Plan selectedPlan = person.getSelectedPlan();
		
		// get the planStepFactory for the plan (or create one):
		PlanBuilder<ModalCountsLinkIdentifier> tmpPlanStepFactory = getPlanStepFactoryForPlan(selectedPlan);
		
		if (tmpPlanStepFactory != null) {
						
			// add the "turn" to the planStepfactory
			tmpPlanStepFactory.addTurn(modalLinkContainer.get(mlId), (int) event.getTime());
		}
	}

	// ###################################################################################
	// only private functions below here (low level functionality)

	private PlanBuilder<ModalCountsLinkIdentifier> getPlanStepFactoryForPlan(final Plan selectedPlan) {
		PlanBuilder<ModalCountsLinkIdentifier> planStepFactory = null;

		planStepFactory = (PlanBuilder<ModalCountsLinkIdentifier>) selectedPlan.getCustomAttributes().get(STR_PLANSTEPFACTORY);
		Integer factoryIteration = (Integer) selectedPlan.getCustomAttributes().get(STR_ITERATION);
		if (planStepFactory == null || factoryIteration == null || factoryIteration != this.iteration) {
			// attach the iteration number to the plan:
			selectedPlan.getCustomAttributes().put(STR_ITERATION, this.iteration);

			// construct a new PlanBulder and attach it to the plan:
			planStepFactory = new PlanBuilder<>();
			selectedPlan.getCustomAttributes().put(STR_PLANSTEPFACTORY, planStepFactory);

			// memorize the plan as being seen:
			this.plansEverSeen.add(selectedPlan);
		}

		return planStepFactory;
	}

}
