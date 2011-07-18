/* *********************************************************************** *
 * project: org.matsim.*
 * PtPlanToPlanStepBasedOnEvents.java
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
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.events.PersonEntersVehicleEvent;
import org.matsim.core.events.PersonEntersVehicleEventImpl;
import org.matsim.core.events.PersonLeavesVehicleEvent;
import org.matsim.core.events.PersonLeavesVehicleEventImpl;
import org.matsim.core.events.VehicleArrivesAtFacilityEvent;
import org.matsim.core.events.VehicleDepartsAtFacilityEvent;
import org.matsim.core.events.handler.PersonEntersVehicleEventHandler;
import org.matsim.core.events.handler.PersonLeavesVehicleEventHandler;
import org.matsim.core.events.handler.VehicleArrivesAtFacilityEventHandler;
import org.matsim.core.events.handler.VehicleDepartsAtFacilityEventHandler;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.pt.routes.ExperimentalTransitRoute;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

import playground.mmoyo.utils.ExpTransRouteUtils;
import cadyts.demand.PlanBuilder;

class PtPlanToPlanStepBasedOnEvents implements PersonEntersVehicleEventHandler, PersonLeavesVehicleEventHandler,
VehicleArrivesAtFacilityEventHandler, VehicleDepartsAtFacilityEventHandler
{
	private static final Logger log = Logger.getLogger(PtPlanToPlanStepBasedOnEvents.class);

	private final Network net;
	private final Scenario sc ;
	private final TransitSchedule schedule;

	private final Map< Id, Collection<Id> > personsFromVehId = new HashMap< Id, Collection<Id> >() ;

	private int iteration = -1 ;

	// this is _only_ there for output:
	Set<Plan> plansEverSeen = new HashSet<Plan>() ;

	// these are there so that print functions can be called from the outside (for debugging):
	private static Network NET ;
	private static TransitSchedule SCHEDULE ;

	//private PtBseOccupancyAnalyzer delegOcupAnalizer;  //it is not needed 18.jul.2011

	private final static String STR_M44 = "M44";
	private final String STR_PLANSTEPFACTORY = "planStepFactory";
	private final String STR_ITERATION = "iteration";
	
	PtPlanToPlanStepBasedOnEvents(Scenario sc/*, PtBseOccupancyAnalyzer delOcupAnalizer 18.jul.2011*/) {
		this.sc = sc ;
		this.net = sc.getNetwork();
		
		this.schedule = ((ScenarioImpl) sc).getTransitSchedule() ;
		
		NET = this.net ;
		SCHEDULE = this.schedule ;
		//this.delegOcupAnalizer = delOcupAnalizer;   //it is not needed 18.jul.2011
	}

	private long plansFound = 0 ;
	private long plansNotFound = 0 ;
	@SuppressWarnings("unchecked")
	final cadyts.demand.Plan<TransitStopFacility> getPlanSteps(Plan plan) {
		PlanBuilder<TransitStopFacility> planStepFactory = (PlanBuilder<TransitStopFacility>) plan.getCustomAttributes().get(STR_PLANSTEPFACTORY) ;
		if ( planStepFactory == null ) {
			this.plansNotFound ++ ;
			return null ;
		}
		this.plansFound ++ ;
		final cadyts.demand.Plan<TransitStopFacility> planSteps = planStepFactory.getResult();
		return planSteps;
	}

	@Override
	public void reset(int iteration) {
		this.iteration = iteration ;

		log.warn("found " + this.plansFound + " out of " + (this.plansFound+this.plansNotFound)
				+ " (" + (100.*this.plansFound/(this.plansFound+this.plansNotFound)) + "%)" ) ;
		log.warn("(above values may both be at zero for a couple of iterations if multiple plans per agent all have no score)") ;

		personsFromVehId.clear();
		//this.delegOcupAnalizer.reset(iteration);    //it is not needed 18.jul.2011
	}

	@Override
	public void handleEvent(PersonEntersVehicleEvent event) {
		Id transitLineId = ((PersonEntersVehicleEventImpl) event).getTransitRouteId() ;
		if ( !transitLineId.toString().contains(STR_M44)) {
			return ;
		}
		addPersonToVehicleContainer(event.getPersonId(), event.getVehicleId());
		//this.delegOcupAnalizer.handleEvent(event);     //it is not needed 18.jul.2011
	}

	@Override
	public void handleEvent(PersonLeavesVehicleEvent event) {
		Id transitLineId = ((PersonLeavesVehicleEventImpl) event).getTransitRouteId() ;
		if ( !transitLineId.toString().contains(STR_M44)) {
			return ;
		}
		removePersonFromVehicleContainer(event.getPersonId(), event.getVehicleId());
		//this.delegOcupAnalizer.handleEvent(event);      //it is not needed 18.jul.2011
	}

	@Override
	public void handleEvent(VehicleArrivesAtFacilityEvent event) {
		//this.delegOcupAnalizer.handleEvent(event);      //it is not needed 18.jul.2011
	}

	@Override
	public void handleEvent(VehicleDepartsAtFacilityEvent event) {
		double time = event.getTime() ;
		Id vehId = event.getVehicleId();
		Id facId = event.getFacilityId() ;
		if ( this.personsFromVehId.get(vehId)==null ) {
			// (means nobody has entered the vehicle yet)
			return ;
		}

		for ( Id personId : this.personsFromVehId.get(vehId) ) {
			// get the "Person" behind the id:
			Person person = this.sc.getPopulation().getPersons().get( personId ) ;

			// get the selected plan:
			Plan selectedPlan = person.getSelectedPlan() ;

			// get the planStepFactory for the plan (or create one):

			PlanBuilder<TransitStopFacility> tmpPlanStepFactory = getPlanStepFactoryForPlan(selectedPlan); 

			if ( tmpPlanStepFactory != null ) {

				// add the "turn" to the planStepfactory
				TransitStopFacility fac = this.schedule.getFacilities().get( facId ) ;

				tmpPlanStepFactory.addTurn( fac, (int) time ) ;
			}
		}
		//this.delegOcupAnalizer.handleEvent(event);       //it is not needed 18.jul.2011
	}


	// ###################################################################################
	// only private functions below here (low level functionality)

	private void addPersonToVehicleContainer(Id personId, Id vehId) {
		// get the personsContainer that belongs to the vehicle:
		Collection<Id> personsInVehicle = this.personsFromVehId.get( vehId ) ;

		if ( personsInVehicle == null ) {
			// means does not exist yet
			personsInVehicle = new ArrayList<Id>();
			this.personsFromVehId.put( vehId, personsInVehicle ) ;
		}

		personsInVehicle.add( personId ) ;
	}

	private void removePersonFromVehicleContainer(Id personId, Id vehId) {
		// get the personsContainer that belongs to the vehicle:
		Collection<Id> personsInVehicle = this.personsFromVehId.get( vehId ) ;

		if ( personsInVehicle == null ) {
			Gbl.errorMsg( "should not be possible: person should enter before leaving, and then construct the container" ) ;
		}

		// remove the person from the personsContainer:
		personsInVehicle.remove( personId ) ; // linear time operation; a HashMap might be better.
	}

	@SuppressWarnings("unchecked")
	private PlanBuilder<TransitStopFacility> getPlanStepFactoryForPlan(Plan selectedPlan) {
		PlanBuilder<TransitStopFacility> planStepFactory = null ;

		planStepFactory = (PlanBuilder<TransitStopFacility>) selectedPlan.getCustomAttributes().get(STR_PLANSTEPFACTORY) ;
		Integer factoryIteration = (Integer) selectedPlan.getCustomAttributes().get(STR_ITERATION) ;
		if ( planStepFactory == null
				// (means there is not yet a plansStepFactory for this plan
				|| factoryIteration==null || factoryIteration != iteration
				// (means the iteration for which the plansStepFactory was build is over)
				) {
			// attach the iteration number to the plan:
			selectedPlan.getCustomAttributes().put( STR_ITERATION, iteration ) ;

			// construct a new PlanBulder and attach it to the plan:
			planStepFactory = new PlanBuilder<TransitStopFacility>() ;
			selectedPlan.getCustomAttributes().put( STR_PLANSTEPFACTORY, planStepFactory ) ;

			// memorize the plan as being seen:
			plansEverSeen.add( selectedPlan ) ;
		}

		return planStepFactory;
	}

	@SuppressWarnings("unchecked")
	private void printCadytsAndMatsimPlans() {
		String separator = 	"\n==============================";
		System.err.println("results:") ;

		for ( Plan matsimPlan : this.plansEverSeen ) {
			PlanBuilder<TransitStopFacility> planStepFactory = (PlanBuilder<TransitStopFacility>) matsimPlan.getCustomAttributes().get(STR_PLANSTEPFACTORY) ;
			cadyts.demand.Plan<TransitStopFacility> cadytsPlan = planStepFactory.getResult() ;

			System.err.println(separator);

			if ( printMatsimPlanPtLinks(matsimPlan) ) {
				printCadytsPlan(cadytsPlan);
			}

		}
	}
	static void printCadytsPlan(cadyts.demand.Plan<TransitStopFacility> cadytsPlan) {
		//prints Cadyts plan
		String sepCadStr = 	"==printing Cadyts Plan==";
		System.err.println(sepCadStr);
		if ( cadytsPlan!= null ) {
			for( int ii=0 ; ii<cadytsPlan.size(); ii++ ) {
				cadyts.demand.PlanStep<TransitStopFacility> cadytsPlanStep = cadytsPlan.getStep(ii) ;
				System.err.println("stopId" + cadytsPlanStep.getLink().getId() + " time: " + cadytsPlanStep.getEntryTime_s() ) ;
			}
		} else {
			System.err.println( " cadyts plan is null ") ;
		}
	}
	static boolean printMatsimPlanPtLinks(Plan matsimPlan) {
		//prints MATSim plan
		final String sepMatStr = 	"==printing MATSim plan exp transit routes==";
		final String personIdStr = "person Id: ";
		boolean containsM44 = false ;
		System.err.println(sepMatStr);
		System.err.println(personIdStr + matsimPlan.getPerson().getId());
		for (PlanElement planElement : matsimPlan.getPerson().getSelectedPlan().getPlanElements()){
			if ((planElement instanceof Leg)) {
				Leg leg= (Leg)planElement;
				if (leg.getRoute()!= null && (leg.getMode().equals("pt")) ){
					ExperimentalTransitRoute exptr = (ExperimentalTransitRoute)leg.getRoute();
					if ( exptr.getRouteDescription().contains(STR_M44) ) {
						containsM44 = true ;
						System.err.print( exptr.getRouteDescription() + ": " ) ;

						ExpTransRouteUtils expTransRouteUtils = new ExpTransRouteUtils(NET, SCHEDULE, exptr);
						for(Link link: expTransRouteUtils.getLinks()){
							System.err.print("-- " + link.getId() + " --");
						}
						System.err.println();
					}
				}
			}
		}
		return containsM44 ;
	}

}
