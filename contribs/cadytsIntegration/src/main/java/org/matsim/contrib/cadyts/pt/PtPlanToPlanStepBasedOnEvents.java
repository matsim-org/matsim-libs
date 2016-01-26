/* *********************************************************************** *
 * project: org.matsim.*
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

package org.matsim.contrib.cadyts.pt;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.PersonLeavesVehicleEvent;
import org.matsim.api.core.v01.events.TransitDriverStartsEvent;
import org.matsim.api.core.v01.events.handler.PersonEntersVehicleEventHandler;
import org.matsim.api.core.v01.events.handler.PersonLeavesVehicleEventHandler;
import org.matsim.api.core.v01.events.handler.TransitDriverStartsEventHandler;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.contrib.cadyts.general.PlansTranslator;
import org.matsim.core.api.experimental.events.VehicleDepartsAtFacilityEvent;
import org.matsim.core.api.experimental.events.handler.VehicleDepartsAtFacilityEventHandler;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

import cadyts.demand.PlanBuilder;

/*package*/ class PtPlanToPlanStepBasedOnEvents<T> implements TransitDriverStartsEventHandler, PersonEntersVehicleEventHandler,
		PersonLeavesVehicleEventHandler, VehicleDepartsAtFacilityEventHandler, PlansTranslator<T> {
	private static final Logger log = Logger.getLogger(PtPlanToPlanStepBasedOnEvents.class);

	private final Scenario sc;
	private final TransitSchedule schedule;

	private final Map<Id, Collection<Id>> personsFromVehId = new HashMap<Id, Collection<Id>>();

	private int iteration = -1;

	// this is _only_ there for output:
	Set<Plan> plansEverSeen = new HashSet<Plan>();

	private static final String STR_PLANSTEPFACTORY = "planStepFactory";
	private static final String STR_ITERATION = "iteration";

	private final Set<Id> transitDrivers = new HashSet<Id>();
	private final Set<Id> transitVehicles = new HashSet<Id>();
	private final Set<Id<TransitLine>> calibratedLines;

	PtPlanToPlanStepBasedOnEvents(final Scenario sc, final Set<Id<TransitLine>> calibratedLines) {
		this.sc = sc;
		this.schedule = ((MutableScenario) sc).getTransitSchedule();
		this.calibratedLines = calibratedLines;
	}

	private long plansFound = 0;
	private long plansNotFound = 0;

	@Override
	public final cadyts.demand.Plan<T> getCadytsPlan(final Plan plan) {
		@SuppressWarnings("unchecked") // getting stuff from custom attributes has to be untyped.  (Although I am not sure why it is necessary to put this
		// there in the first place. kai, jul'13)
		PlanBuilder<T> planStepFactory = (PlanBuilder<T>) plan.getCustomAttributes().get(STR_PLANSTEPFACTORY);
		if (planStepFactory == null) {
			this.plansNotFound++;
			return null;
		}
		this.plansFound++;
		final cadyts.demand.Plan<T> planSteps = planStepFactory.getResult();
		return planSteps;
	}

	@Override
	public void reset(final int it) {
		this.iteration = it;

		log.warn("found " + this.plansFound + " out of " + (this.plansFound + this.plansNotFound) + " ("
				+ (100. * this.plansFound / (this.plansFound + this.plansNotFound)) + "%)");
		log.warn("(above values may both be at zero for a couple of iterations if multiple plans per agent all have no score)");

		this.personsFromVehId.clear();
		this.transitDrivers.clear();
		this.transitVehicles.clear();
	}

	@Override
	public void handleEvent(final TransitDriverStartsEvent event) {
		if (this.calibratedLines.contains(event.getTransitLineId())) {
			this.transitDrivers.add(event.getDriverId());
			this.transitVehicles.add(event.getVehicleId());
		}
	}

	@Override
	public void handleEvent(final PersonEntersVehicleEvent event) {
		if (this.transitDrivers.contains(event.getPersonId()) || !this.transitVehicles.contains(event.getVehicleId())) {
			return; // ignore transit drivers or persons entering non-(analyzed-)transit vehicles
		}
		addPersonToVehicleContainer(event.getPersonId(), event.getVehicleId());
	}

	@Override
	public void handleEvent(final PersonLeavesVehicleEvent event) {
		if (this.transitDrivers.contains(event.getPersonId()) || !this.transitVehicles.contains(event.getVehicleId())) {
			return; // ignore transit drivers or persons entering non-(analyzed-)transit vehicles
		}
		removePersonFromVehicleContainer(event.getPersonId(), event.getVehicleId());
	}

	@Override
	public void handleEvent(final VehicleDepartsAtFacilityEvent event) {
		double time = event.getTime();
		Id vehId = event.getVehicleId();
		Id facId = event.getFacilityId();
		if (this.personsFromVehId.get(vehId) == null) {
			// (means nobody has entered the vehicle yet)
			return;
		}
		TransitStopFacility fac = this.schedule.getFacilities().get(facId);

		for (Id personId : this.personsFromVehId.get(vehId)) {
			// get the "Person" behind the id:
			Person person = this.sc.getPopulation().getPersons().get(personId);

			// get the selected plan:
			Plan selectedPlan = person.getSelectedPlan();

			// get the planStepFactory for the plan (or create one):
			PlanBuilder<TransitStopFacility> tmpPlanStepFactory = getPlanStepFactoryForPlan(selectedPlan);

			if (tmpPlanStepFactory != null) {
				// add the "turn" to the planStepfactory
				tmpPlanStepFactory.addTurn(fac, (int) time);
			}
		}
	}

	// ###################################################################################
	// only private functions below here (low level functionality)

	private void addPersonToVehicleContainer(final Id personId, final Id vehId) {
		// get the personsContainer that belongs to the vehicle:
		Collection<Id> personsInVehicle = this.personsFromVehId.get(vehId);

		if (personsInVehicle == null) {
			// means does not exist yet
			personsInVehicle = new ArrayList<Id>();
			this.personsFromVehId.put(vehId, personsInVehicle);
		}

		personsInVehicle.add(personId);
	}

	private void removePersonFromVehicleContainer(final Id personId, final Id vehId) {
		// get the personsContainer that belongs to the vehicle:
		Collection<Id> personsInVehicle = this.personsFromVehId.get(vehId);

		if (personsInVehicle == null) {
			throw new RuntimeException("should not be possible: person should enter before leaving, and then construct the container");
		}

		// remove the person from the personsContainer:
		personsInVehicle.remove(personId); // linear time operation; a HashMap might be better.
	}

	private PlanBuilder<TransitStopFacility> getPlanStepFactoryForPlan(final Plan selectedPlan) {
		PlanBuilder<TransitStopFacility> planStepFactory = null;

		planStepFactory = (PlanBuilder<TransitStopFacility>) selectedPlan.getCustomAttributes().get(STR_PLANSTEPFACTORY);
		Integer factoryIteration = (Integer) selectedPlan.getCustomAttributes().get(STR_ITERATION);
		if (planStepFactory == null || factoryIteration == null || factoryIteration != this.iteration) {
			// attach the iteration number to the plan:
			selectedPlan.getCustomAttributes().put(STR_ITERATION, this.iteration);

			// construct a new PlanBulder and attach it to the plan:
			planStepFactory = new PlanBuilder<TransitStopFacility>();
			selectedPlan.getCustomAttributes().put(STR_PLANSTEPFACTORY, planStepFactory);

			// memorize the plan as being seen:
			this.plansEverSeen.add(selectedPlan);
		}

		return planStepFactory;
	}

	static void printCadytsPlan(final cadyts.demand.Plan<TransitStopFacility> cadytsPlan) {
		// prints Cadyts plan
		String sepCadStr = "==printing Cadyts Plan==";
		System.err.println(sepCadStr);
		if (cadytsPlan != null) {
			for (int ii = 0; ii < cadytsPlan.size(); ii++) {
				cadyts.demand.PlanStep<TransitStopFacility> cadytsPlanStep = cadytsPlan.getStep(ii);
				System.err.println("stopId" + cadytsPlanStep.getLink().getId() + " time: " + cadytsPlanStep.getEntryTime_s());
			}
		} else {
			System.err.println(" cadyts plan is null ");
		}
	}

}
