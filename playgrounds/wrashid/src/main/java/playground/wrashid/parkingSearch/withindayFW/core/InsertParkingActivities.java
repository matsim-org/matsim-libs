/* *********************************************************************** *
 * project: org.matsim.*
 * InsertParkingActivitiesReplanner.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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

package playground.wrashid.parkingSearch.withindayFW.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.junit.Assert;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contrib.parking.lib.DebugLib;
import org.matsim.core.api.experimental.facilities.ActivityFacility;
import org.matsim.core.mobsim.qsim.agents.PlanBasedWithinDayAgent;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.router.PlanRouter;
import org.matsim.core.router.TripRouter;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.population.algorithms.PersonAlgorithm;
import org.matsim.population.algorithms.PersonPrepareForSim;
import org.matsim.population.algorithms.PlanAlgorithm;
import org.matsim.withinday.utils.EditRoutes;

public class InsertParkingActivities implements PlanAlgorithm {

	private final Scenario scenario;
	private final PersonAlgorithm personPrepareForSim;
	private final ParkingInfrastructure parkingInfrastructure;
	private final TripRouter tripRouter;

	
	// TODO: instead of selecting closest parking, we could select parking from previous day.
	/*
	 * use a InitialIdentifierImpl and set handleAllAgents to true
	 */
	public InsertParkingActivities(Scenario scenario, TripRouter tripRouter, ParkingInfrastructure parkingInfrastructure) {
		this.scenario = scenario;
		this.tripRouter = tripRouter;
		this.personPrepareForSim = new PersonPrepareForSim(new PlanRouter(tripRouter), (ScenarioImpl) scenario);
		this.parkingInfrastructure = parkingInfrastructure;
	}

	@Override
	public void run(Plan plan) {
		
		List<PlanElement> planElements = plan.getPlanElements();
		
		

		/*
		 * Changes to this plan will be executed but not written to the person
		 */
		List<Integer> carLegIndices = new ArrayList<Integer>();
		int index = 0;
		for (PlanElement planElement : planElements) {
			if (planElement instanceof Leg) {
				if (((Leg) planElement).getMode().equals(TransportMode.car)) {
					carLegIndices.add(index);
				}
			}
			index++;
		}

		// if no car legs are performed, no adaption of the plan is necessary
		if (carLegIndices.size() == 0)
			return;

		for (int i = carLegIndices.size(); i > 0; i--) {
			index = carLegIndices.get(i - 1);

			Activity previousActivity = (Activity) planElements.get(index - 1);
			Activity nextActivity = (Activity) planElements.get(index + 1);

			// TODO: uncomment if needed.
			// Link prevActLink =
			// scenario.getNetwork().getLinks().get(previousActivity.getLinkId());
			// Link nextActLink =
			// scenario.getNetwork().getLinks().get(nextActivity.getLinkId());

			// double
			// minDistanceBetweenConsecutiveActivitiesToPerfomNewParkingOperation
			// = 500;
			// if (GeneralLib.getDistance(prevActLink.getCoord(),
			// nextActLink.getCoord()) <
			// minDistanceBetweenConsecutiveActivitiesToPerfomNewParkingOperation)
			// {
			// Leg currentLeg = (Leg) planElements.get(index);
			// currentLeg.setMode(TransportMode.walk);
			// continue;
			// }

			planElements.add(index + 1, createWalkLeg());
			planElements.add(index + 1, createParkingActivity(((Activity) nextActivity).getFacilityId()));
			planElements.add(index, createParkingActivity(((Activity) previousActivity).getFacilityId()));

			// planElements.add(index + 1,
			// createParkingOnSameLinkAsActivity(((Activity)
			// nextActivity).getFacilityId()));
			// planElements.add(index,
			// createParkingOnSameLinkAsActivity(((Activity)
			// previousActivity).getFacilityId()));

			planElements.add(index, createWalkLeg());
		}

		/*
		 * In case, that for the following case work-walk-p1-car-p2-walk-shop,
		 * p1.link==p2.link, we get a problem, as due to missing enterLink event
		 * no search is performed. Therefore for such cases we assign p2
		 * initially to the second closest parking.
		 * 
		 * TODO: later we could further improve this whole operation here, as
		 * these parking really just need to be close the destination/one-road
		 * off to avoid this problem.
		 */

		initializeFirstParkingActivityOfDay(plan);
		
		

		/*
		 * Create initial routes for new legs
		 * 
		 * Since we only want the current plan to be prepared, we create a dummy
		 * person, add the plan, prepare this person and the reassign the plan
		 * to the person it really belongs to.
		 */
		Person person = plan.getPerson();
		//Person dummyPerson = this.scenario.getPopulation().getFactory().createPerson(scenario.createId("dummy"));
		//dummyPerson.addPlan(plan);
		//personPrepareForSim.run(dummyPerson);
		plan.setPerson(person);
	}
	
	//TODO: unrequired routings could be removed...
	// set the first parking facility of day as specified by parking infrastructure
	private void initializeFirstParkingActivityOfDay(Plan plan) {
		
		//DebugLib.traceAgent(plan.getPerson().getId(), 2);
		
		DebugLib.traceAgent(plan.getPerson().getId(), 3);
		
		List<PlanElement> planElements = plan.getPlanElements();
		
		for (int i=0;i<planElements.size();i++){
			if (planElements.get(i) instanceof Activity){
				ActivityImpl act=(ActivityImpl) planElements.get(i);
				
				if (act.getType().equalsIgnoreCase("parking")){
					synchronized(parkingInfrastructure) {
						HashMap<Id, ActivityFacility> initialParkingFacilityOfAgent = parkingInfrastructure.getInitialParkingFacilityOfAgent();
						Id personId = plan.getPerson().getId();
						ActivityFacility parkingFacility = initialParkingFacilityOfAgent.get(personId);
						
						act.setLinkId(parkingFacility.getLinkId());
						act.setFacilityId(parkingFacility.getId());
					}
					
					updateSecondParkingActivityLinkOfDayIfNeeded(parkingInfrastructure,plan,scenario);
					
					synchronized(this){
						EditRoutes editRoutes = new EditRoutes();
												
						//update walk leg
						Leg walkLeg = (Leg) plan.getPlanElements().get(i - 1);
						Assert.assertNotNull(walkLeg) ;
						Assert.assertNotNull(walkLeg.getRoute()) ;
						Assert.assertNotNull(act) ;
						Assert.assertNotNull(plan);
						Assert.assertNotNull(scenario) ;
						Assert.assertNotNull(tripRouter) ;
						editRoutes.relocateFutureLegRoute(walkLeg, walkLeg.getRoute().getStartLinkId(), 
								act.getLinkId(), plan.getPerson(), scenario.getNetwork(), tripRouter);

						//update car leg
						Leg carLeg = (Leg) plan.getPlanElements().get(i + 1);
						editRoutes.relocateFutureLegRoute(carLeg, act.getLinkId(), 
								carLeg.getRoute().getEndLinkId(), plan.getPerson(), scenario.getNetwork(), tripRouter);
						
						
						//EditPartialRoute editPartialRoute=new EditPartialRoute(scenario, routingAlgorithm);
						//editPartialRoute.replanFutureCarLegRoute(plan, i+1);
					}
					
					break;
				}
			}
		}
		
	}

	// if first parking act is on same link as second parking act, change the link id of the second
		// parking (else search cannot start).
	//TODO: find out, if through refactoring this can be merged with "updateNextParkingActivityIfNeededAndRouteDuringDay"
		private void updateSecondParkingActivityLinkOfDayIfNeeded(ParkingInfrastructure pi, Plan plan, Scenario sc){
			EditRoutes editRoutes=new EditRoutes();
			
			Activity currentParkingDepartureAct = null;
			ActivityImpl nextParkingArrivalAct = null;
			ActivityImpl nextParkingDepartureAct = null;
			
			List<PlanElement> planElements = plan.getPlanElements();
			int firstPossibleParkingActIndex = 2;
			for (int i = firstPossibleParkingActIndex; i < planElements.size(); i++) {
				if (planElements.get(i) instanceof Activity) {
					Activity act = (Activity) planElements.get(i);
					if (act.getType().equalsIgnoreCase("parking")) {
						if (currentParkingDepartureAct == null) {
							currentParkingDepartureAct = act;
						} else if (nextParkingArrivalAct == null) {
							nextParkingArrivalAct = (ActivityImpl) act;
						} else if (nextParkingDepartureAct == null) {
							nextParkingDepartureAct = (ActivityImpl) act;
						}
					}
				}
			}
			
//			PlanAlgorithm routingAlgo=routingAlgorithm;
			
			Id newParkingFacilityId=null;
			if (nextParkingArrivalAct!=null){
				if (currentParkingDepartureAct.getLinkId() == nextParkingArrivalAct.getLinkId()) {
					newParkingFacilityId = pi.getClosestParkingFacilityNotOnLink(nextParkingArrivalAct.getCoord(), nextParkingArrivalAct.getLinkId());
					nextParkingArrivalAct.setLinkId(((ScenarioImpl) scenario).getActivityFacilities().getFacilities().get(newParkingFacilityId).getLinkId());
					nextParkingArrivalAct.setFacilityId(newParkingFacilityId);

					
					// update car leg
					//editRoutes.replanFutureLegRoute(plan, indexNextParkingArrival-1, routingAlgo);
					
					// update walk leg
					//editRoutes.replanFutureLegRoute(plan, indexNextParkingArrival+1, routingAlgo);
					
				}
			}
			
			
			if (nextParkingDepartureAct!=null && newParkingFacilityId != null) {
				nextParkingDepartureAct.setLinkId(((ScenarioImpl) scenario).getActivityFacilities().getFacilities().get(newParkingFacilityId).getLinkId());
				nextParkingDepartureAct.setFacilityId(newParkingFacilityId);
				
				//update car leg
				//editRoutes.replanFutureLegRoute(plan, indexNextParkingDeparture + 1, routingAlgo);
				
				// update walk leg
				//editRoutes.replanFutureLegRoute(plan, indexNextParkingDeparture-1, routingAlgo);
				
			}
			
		}

	public static Activity createParkingActivity(Scenario sc, Id parkingFacilityId) {
		ActivityFacility facility = ((ScenarioImpl) sc).getActivityFacilities().getFacilities().get(parkingFacilityId);

		ActivityImpl activity = (ActivityImpl) sc.getPopulation().getFactory()
				.createActivityFromLinkId("parking", facility.getLinkId());
		activity.setMaximumDuration(180);
		activity.setCoord(facility.getCoord());
		activity.setFacilityId(parkingFacilityId);
		return activity;
	}

	private Activity createParkingActivity(Id facilityId) {

		// get the facility where the activity is performed
		ActivityFacility facility = ((ScenarioImpl) this.scenario).getActivityFacilities().getFacilities().get(facilityId);
		// Id parkingFacilityId =
		// this.parkingInfrastructure.getClosestFreeParkingFacility(facility.getCoord());
		Id parkingFacilityId = this.parkingInfrastructure.getClosestParkingFacility(facility.getCoord());

		// get the closest parking facility
		ActivityFacility parkingFacility = ((ScenarioImpl) this.scenario).getActivityFacilities().getFacilities()
				.get(parkingFacilityId);

		Id linkId = parkingFacility.getLinkId();
		ActivityImpl activity = (ActivityImpl) this.scenario.getPopulation().getFactory()
				.createActivityFromLinkId("parking", linkId);
		activity.setMaximumDuration(180);
		activity.setCoord(parkingFacility.getCoord());
		activity.setFacilityId(parkingFacilityId);
		return activity;
	}

	
	
	public static void updateNextParkingActivityIfNeededDuringDay(ParkingInfrastructure pi,
			PlanBasedWithinDayAgent withinDayAgent, Scenario sc, TripRouter tripRouter) {
		//EditPartialRoute editPartialRoute=new EditPartialRoute(sc, routeAlgo);
		

		EditRoutes editRoutes = new EditRoutes();

		Activity currentParkingArrivalAct = null;
		Activity currentParkingDepartureAct = null;
		ActivityImpl nextParkingArrivalAct = null;
		ActivityImpl nextParkingDepartureAct = null;

		Plan executedPlan = withinDayAgent.getSelectedPlan();
		int currentPlanElemIndex = withinDayAgent.getCurrentPlanElementIndex();

		List<PlanElement> planElements = withinDayAgent.getSelectedPlan().getPlanElements();
		for (int i = currentPlanElemIndex; i < planElements.size(); i++) {
			if (planElements.get(i) instanceof Activity) {
				Activity act = (Activity) planElements.get(i);
				if (act.getType().equalsIgnoreCase("parking")) {
					if (currentParkingArrivalAct == null) {
						currentParkingArrivalAct = act;
					} else if (currentParkingDepartureAct == null) {
						currentParkingDepartureAct = act;
					} else if (nextParkingArrivalAct == null) {
						nextParkingArrivalAct = (ActivityImpl) act;
					} else if (nextParkingDepartureAct == null) {
						nextParkingDepartureAct = (ActivityImpl) act;
					}
				}
			}
		}
		

//		Id newParkingFacilityId=null;
//		if (nextParkingArrivalAct!=null){
//			if (currentParkingArrivalAct.getLinkId() == nextParkingArrivalAct.getLinkId()) {
//				newParkingFacilityId = pi.getClosestParkingFacilityNotOnLink(nextParkingArrivalAct.getCoord(), nextParkingArrivalAct.getLinkId());
//				Activity newParkingAct = InsertParkingActivities.createParkingActivity(sc, newParkingFacilityId);
//				int indexNextParkingArrival = planElements.indexOf(nextParkingArrivalAct);
//				executedPlan.getPlanElements().remove(indexNextParkingArrival);
//				executedPlan.getPlanElements().add(indexNextParkingArrival, newParkingAct);
//				
//				// update car leg
//				editRoutes.replanFutureLegRoute(executedPlan, indexNextParkingArrival-1, routeAlgo);
//				
//				// update walk leg
//				editRoutes.replanFutureLegRoute(executedPlan, indexNextParkingArrival+1, routeAlgo);
//				
//			}
//		}
//		
//
//		if (nextParkingDepartureAct!=null && newParkingFacilityId != null) {
//			Activity newParkingAct = InsertParkingActivities.createParkingActivity(sc, newParkingFacilityId);
//			int indexNextParkingDeparture = planElements.indexOf(nextParkingDepartureAct);
//			executedPlan.getPlanElements().remove(indexNextParkingDeparture);
//			executedPlan.getPlanElements().add(indexNextParkingDeparture, newParkingAct);
//			
//			//update car leg
//			editRoutes.replanFutureLegRoute(executedPlan, indexNextParkingDeparture + 1, routeAlgo);
//			
//			// update walk leg
//			editRoutes.replanFutureLegRoute(executedPlan, indexNextParkingDeparture-1, routeAlgo);
//			
//		}
		
		Id newParkingFacilityId=null;
		if (nextParkingArrivalAct!=null){
			if (currentParkingDepartureAct.getLinkId() == nextParkingArrivalAct.getLinkId()) {
				newParkingFacilityId = pi.getClosestParkingFacilityNotOnLink(nextParkingArrivalAct.getCoord(), nextParkingArrivalAct.getLinkId());
				nextParkingArrivalAct.setLinkId(((ScenarioImpl) sc).getActivityFacilities().getFacilities().get(newParkingFacilityId).getLinkId());
				nextParkingArrivalAct.setFacilityId(newParkingFacilityId);

				int indexNextParkingArrival = planElements.indexOf(nextParkingArrivalAct);
				
				// update car leg
				//editRoutes.replanFutureLegRoute(plan, indexNextParkingArrival-1, routingAlgo);
				//editPartialRoute.replanFutureCarLegRoute(executedPlan, indexNextParkingArrival-1);
				
				// no need to update walk leg, as destination will change anyway.
				
				// update walk leg
				//editPartialRoute.replanFutureLegRoute(executedPlan, indexNextParkingArrival+1);
				//editRoutes.replanFutureLegRoute(plan, indexNextParkingArrival+1, routingAlgo);
				
			}
		}
		
		
		if (nextParkingDepartureAct!=null && newParkingFacilityId != null) {
			nextParkingDepartureAct.setLinkId(((ScenarioImpl) sc).getActivityFacilities().getFacilities().get(newParkingFacilityId).getLinkId());
			nextParkingDepartureAct.setFacilityId(newParkingFacilityId);
			
			//update car leg
			//editRoutes.replanFutureLegRoute(plan, indexNextParkingDeparture + 1, routingAlgo);
			
			// update walk leg
			//editRoutes.replanFutureLegRoute(plan, indexNextParkingDeparture-1, routingAlgo);
			
		}

	}

	private Leg createWalkLeg() {
		return this.scenario.getPopulation().getFactory().createLeg(TransportMode.walk);
	}

}
