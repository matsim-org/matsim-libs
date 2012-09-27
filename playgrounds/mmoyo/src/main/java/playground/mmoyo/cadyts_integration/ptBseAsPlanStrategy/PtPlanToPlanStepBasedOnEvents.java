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

package playground.mmoyo.cadyts_integration.ptBseAsPlanStrategy;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.api.experimental.events.PersonEntersVehicleEvent;
import org.matsim.core.api.experimental.events.PersonLeavesVehicleEvent;
import org.matsim.core.api.experimental.events.TransitDriverStartsEvent;
import org.matsim.core.api.experimental.events.VehicleDepartsAtFacilityEvent;
import org.matsim.core.events.handler.PersonEntersVehicleEventHandler;
import org.matsim.core.events.handler.PersonLeavesVehicleEventHandler;
import org.matsim.core.events.handler.TransitDriverStartsEventHandler;
import org.matsim.core.events.handler.VehicleDepartsAtFacilityEventHandler;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.pt.routes.ExperimentalTransitRoute;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

import cadyts.demand.PlanBuilder;

class PtPlanToPlanStepBasedOnEvents implements TransitDriverStartsEventHandler, PersonEntersVehicleEventHandler,
		PersonLeavesVehicleEventHandler, VehicleDepartsAtFacilityEventHandler {
	private static final Logger log = Logger.getLogger(PtPlanToPlanStepBasedOnEvents.class);

	private final Scenario sc;
	private final TransitSchedule schedule;

	private final Map<Id, Collection<Id>> personsFromVehId = new HashMap<Id, Collection<Id>>();

	private int iteration = -1;

	// this is _only_ there for output:
	Set<Plan> plansEverSeen = new HashSet<Plan>();

	private final static String STR_M44 = "M44";
	private final String STR_PLANSTEPFACTORY = "planStepFactory";
	private final String STR_ITERATION = "iteration";
	private final Map<Id, Id> vehToRouteId = new HashMap<Id, Id>();
	private final Set<Id> transitDrivers = new HashSet<Id>();
	private final Set<Id> transitVehicles = new HashSet<Id>();
	
	PtPlanToPlanStepBasedOnEvents(final Scenario sc) {
		this.sc = sc;
		this.schedule = ((ScenarioImpl) sc).getTransitSchedule();
	}

	private long plansFound = 0;
	private long plansNotFound = 0;

	final cadyts.demand.Plan<TransitStopFacility> getPlanSteps(final Plan plan) {
		PlanBuilder<TransitStopFacility> planStepFactory = (PlanBuilder<TransitStopFacility>) plan.getCustomAttributes().get(
				this.STR_PLANSTEPFACTORY);
		if (planStepFactory == null) {
			this.plansNotFound++;
			return null;
		}
		this.plansFound++;
		final cadyts.demand.Plan<TransitStopFacility> planSteps = planStepFactory.getResult();
		return planSteps;
	}

	@Override
	public void reset(final int iteration) {
		this.iteration = iteration;

		log.warn("found " + this.plansFound + " out of " + (this.plansFound + this.plansNotFound) + " ("
				+ (100. * this.plansFound / (this.plansFound + this.plansNotFound)) + "%)");
		log.warn("(above values may both be at zero for a couple of iterations if multiple plans per agent all have no score)");

		this.personsFromVehId.clear();
		this.vehToRouteId.clear();
		
		plansEverSeen.clear(); //clear stored data Manuel apr2012
		this.transitDrivers.clear();   //clear stored data Manuel apr2012
		this.transitVehicles.clear();   //clear stored data Manuel apr2012
	}

	@Override
	public void handleEvent(final TransitDriverStartsEvent event) {
		this.vehToRouteId.put(event.getVehicleId(), event.getTransitRouteId());
		this.transitDrivers.add(event.getDriverId()); //store transit drivers as in org.matsim.pt.counts.OccupancyAnalyzer Manuel apr2012
		this.transitVehicles.add(event.getVehicleId()); //store transit vehicles as in org.matsim.pt.counts.OccupancyAnalyzer Manuel apr2012
	}

	@Override
	public void handleEvent(final PersonEntersVehicleEvent event) {
		if (this.transitDrivers.contains(event.getPersonId()) || !this.transitVehicles.contains(event.getVehicleId())) {
			return; // ignore transit drivers or persons entering non-transit vehicles, as in org.matsim.pt.counts.OccupancyAnalyzer Manuel apr2012
		}
		
		Id transitLineId = this.vehToRouteId.get(event.getVehicleId());
		if (!transitLineId.toString().contains(STR_M44)) {
			return;
		}

		addPersonToVehicleContainer(event.getPersonId(), event.getVehicleId());
	}

	@Override
	public void handleEvent(final PersonLeavesVehicleEvent event) {
		if (this.transitDrivers.contains(event.getPersonId()) || !this.transitVehicles.contains(event.getVehicleId())) {
			return; // ignore transit drivers or persons entering non-transit vehicles, as in org.matsim.pt.counts.OccupancyAnalyzer Manuel apr2012
		}
		
		Id transitLineId = this.vehToRouteId.get(event.getVehicleId());
		if (!transitLineId.toString().contains(STR_M44)) {
			return;
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
			Gbl.errorMsg("should not be possible: person should enter before leaving, and then construct the container");
		}

		// remove the person from the personsContainer:
		personsInVehicle.remove(personId); // linear time operation; a HashMap might be better.
	}

	private PlanBuilder<TransitStopFacility> getPlanStepFactoryForPlan(final Plan selectedPlan) {
		PlanBuilder<TransitStopFacility> planStepFactory = null;

		planStepFactory = (PlanBuilder<TransitStopFacility>) selectedPlan.getCustomAttributes().get(this.STR_PLANSTEPFACTORY);
		Integer factoryIteration = (Integer) selectedPlan.getCustomAttributes().get(this.STR_ITERATION);
		if (planStepFactory == null || factoryIteration == null || factoryIteration != this.iteration) {
			// attach the iteration number to the plan:
			selectedPlan.getCustomAttributes().put(this.STR_ITERATION, this.iteration);

			// construct a new PlanBulder and attach it to the plan:
			planStepFactory = new PlanBuilder<TransitStopFacility>();
			selectedPlan.getCustomAttributes().put(this.STR_PLANSTEPFACTORY, planStepFactory);

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

	static boolean printMatsimPlanPtLinks(final Plan matsimPlan) {
		// prints MATSim plan
		final String sepMatStr = "==printing MATSim plan exp transit routes==";
		final String personIdStr = "person Id: ";
		boolean containsM44 = false;
		System.err.println(sepMatStr);
		System.err.println(personIdStr + matsimPlan.getPerson().getId());
		for (PlanElement planElement : matsimPlan.getPerson().getSelectedPlan().getPlanElements()) {
			if ((planElement instanceof Leg)) {
				Leg leg = (Leg) planElement;
				if (leg.getRoute() != null && (leg.getMode().equals("pt"))) {
					ExperimentalTransitRoute exptr = (ExperimentalTransitRoute) leg.getRoute();
					if (exptr.getRouteDescription().contains(STR_M44)) {
						containsM44 = true;
						System.err.print(exptr.getRouteDescription() + ": ");

						// ExpTransRouteUtils expTransRouteUtils = new ExpTransRouteUtils(NET, SCHEDULE, exptr);
						// for(Link link: expTransRouteUtils.getLinks()){
						// System.err.print("-- " + link.getId() + " --");
						// }
						System.err.println();
					}
				}
			}
		}
		return containsM44;
	}

}
