/* *********************************************************************** *
 * project: org.matsim.*
 * SelectHouseholdMeetingPointRunner.java
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

package playground.christoph.evacuation.withinday.replanning.utils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.framework.MobsimAgent.State;
import org.matsim.core.mobsim.qsim.agents.WithinDayAgentUtils;
import org.matsim.core.mobsim.qsim.qnetsimengine.PassengerQNetsimEngine;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.router.PlanRouter;
import org.matsim.core.router.TripRouter;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.utils.misc.Time;
import org.matsim.facilities.ActivityFacility;
import org.matsim.households.Household;
import org.matsim.vehicles.Vehicle;
import org.matsim.withinday.mobsim.MobsimDataProvider;

import playground.christoph.evacuation.analysis.CoordAnalyzer;
import playground.christoph.evacuation.mobsim.AgentPosition;
import playground.christoph.evacuation.mobsim.Tracker.Position;
import playground.christoph.evacuation.mobsim.decisiondata.DecisionDataProvider;
import playground.christoph.evacuation.mobsim.decisiondata.HouseholdDecisionData;
import playground.christoph.evacuation.mobsim.decisiondata.PersonDecisionData;
import playground.christoph.evacuation.mobsim.decisionmodel.EvacuationDecisionModel.EvacuationDecision;
import playground.christoph.evacuation.mobsim.decisionmodel.EvacuationDecisionModel.Participating;
import playground.christoph.evacuation.network.AddExitLinksToNetwork;

/**
 * Decision logic for SelectHouseholdMeetingPoint class. Can be executed parallel
 * on multiple threads.
 *  
 * @author cdobler
 */
public class SelectHouseholdMeetingPointRunner implements Runnable {

	private static final Logger log = Logger.getLogger(SelectHouseholdMeetingPointRunner.class);
	
	private final Scenario scenario;
	private final CoordAnalyzer coordAnalyzer;
	private final MobsimDataProvider mobsimDataProvider;
	private final ModeAvailabilityChecker modeAvailabilityChecker;
	private final DecisionDataProvider decisionDataProvider;
	private final TripRouter toHomeFacilityRouter;
	private final TripRouter evacuationRouter;
	
	private final List<Id> householdsToCheck;
	private volatile double time;
	
	private final CyclicBarrier startBarrier;
	private final CyclicBarrier endBarrier;
	private final AtomicBoolean allMeetingsPointsSelected;
	private final Random random;
	private final Id exitLinkId;
	
	private int handledHouseholds = 0;
	
	/*package*/ final List<Person> toHomePlans;
	/*package*/ final List<Person> directEvacuationPlans;
	
	public SelectHouseholdMeetingPointRunner(Scenario scenario, CoordAnalyzer coordAnalyzer, MobsimDataProvider mobsimDataProvider, 
			ModeAvailabilityChecker modeAvailabilityChecker, DecisionDataProvider decisionDataProvider, 
			CyclicBarrier startBarrier, CyclicBarrier endBarrier, AtomicBoolean allMeetingsPointsSelected,
			TripRouter toHomeFacilityRouter, TripRouter fromHomeFacilityRouter) {
		this.scenario = scenario;
		
		this.toHomeFacilityRouter = toHomeFacilityRouter;
		this.evacuationRouter = fromHomeFacilityRouter;

		this.mobsimDataProvider = mobsimDataProvider;
		this.coordAnalyzer = coordAnalyzer;
		this.modeAvailabilityChecker = modeAvailabilityChecker;
		this.decisionDataProvider = decisionDataProvider;

		this.startBarrier = startBarrier;
		this.endBarrier = endBarrier;
		this.allMeetingsPointsSelected = allMeetingsPointsSelected;
		
		this.random = new Random();
		this.exitLinkId = Id.create(AddExitLinksToNetwork.exitLink, Link.class);
		this.householdsToCheck = new ArrayList<Id>();
		
		if (SelectHouseholdMeetingPoint.writeRoutesToFiles) {
			this.toHomePlans = new ArrayList<Person>();
			this.directEvacuationPlans = new ArrayList<Person>();
		} else {
			this.toHomePlans = null;
			this.directEvacuationPlans = null;			
		}
	}
	
	public void setTime(double time) {
		this.time = time;
	}
	
	public void addHouseholdToCheck(Id householdId) {
		this.householdsToCheck.add(householdId);
	}
	
	@Override
	public void run() {
		
		/*
		 * The loop is ended when the "allMeetingsPointsSelected" Flag is set to false.
		 */
		while(true) {
			try {
				/*
				 * The Threads wait at the startBarrier until they are triggered in the 
				 * next TimeStep by the run() method in SelectHouseholdMeetingPoint.
				 */
				this.startBarrier.await();

				/*
				 * Check if all meeting points have selected. If yes, we can terminate the threads.
				 */
				if (this.allMeetingsPointsSelected.get()) {
					log.info("Handled " + this.handledHouseholds + " households in thread " + Thread.currentThread().getName());
					this.handledHouseholds = 0;
					Gbl.printCurrentThreadCpuTime();
					return;
				}
				
				/*
				 * Check all households that have been informed in the current time step
				 * and therefore have to be checked.
				 */
				for (Id householdId : householdsToCheck) {
					
					Household household = ((ScenarioImpl) this.scenario).getHouseholds().getHouseholds().get(householdId);
					
					/*
					 * Set seed in random object based on households id and current simulation time.
					 * This should be deterministic and not depend on the thread that handles the
					 * household.
					 */
					this.random.setSeed(householdId.hashCode() + (long) this.time);
					
					HouseholdDecisionData hdd = this.decisionDataProvider.getHouseholdDecisionData(householdId);
					
					boolean householdParticipates;
					Participating participating = this.decisionDataProvider.getHouseholdDecisionData(householdId).getParticipating();
					if (participating == Participating.TRUE) householdParticipates = true;
					else if (participating == Participating.FALSE) householdParticipates = false;
					else throw new RuntimeException("Households participation state is undefined: " + householdId.toString());
					
					/*
					 * If the household does not evacuate, set its meeting point to its home facility.
					 */
					if (!householdParticipates) {
						hdd.setMeetingPointFacilityId(hdd.getHomeFacilityId());
						continue;
					}
					
					/*
					 * So far we assume that all households who's home facility is not affected
					 * follow the evacuation order and evacuate directly to their home facility.
					 */
					if (!hdd.isHomeFacilityIsAffected()) {
						hdd.setMeetingPointFacilityId(hdd.getHomeFacilityId());
						continue;
					}
					
					/*
					 * The household evacuates but its home facility is located inside the affected area.
					 * 
					 * Therefore we compare the evacuation times "current location -> home -> rescue" to
					 * "current location -> rescue". The first option allows households to meet at home and
					 * evacuate united which is preferred by them. Households might be even willing to accept
					 * longer evacuation times if this allows them to meet at home.
					 */
					calculateHouseholdMemberTimes(household, hdd, time);
//					calculateLatestAcceptedLeaveTime(household);
					
					calculateHouseholdReturnHomeTime(household, hdd);
					calculateEvacuationTimeFromHome(household, hdd, hdd.getHouseholdReturnHomeTime());
					calculateHouseholdDirectEvacuationTime(household);
					
					selectHouseholdMeetingPoint(household);
				}
				
				// count number of handled households
				this.handledHouseholds += this.householdsToCheck.size();
				
				// clear list for next time step
				this.householdsToCheck.clear();
				
				/*
				 * The End of the Moving is synchronized with the endBarrier. If all Threads 
				 * reach this Barrier the main Thread can go on.
				 */
				this.endBarrier.await();
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			} catch (BrokenBarrierException e) {
            	throw new RuntimeException(e);
            }
		}
	}
	
	private void selectHouseholdMeetingPoint(Household household) {
		
		HouseholdDecisionData hdd = this.decisionDataProvider.getHouseholdDecisionData(household.getId());
		EvacuationDecision evacuationDecision = hdd.getEvacuationDecision();
		
		/*
		 * If the household does not evacuate, we do not check other influence factors like
		 * the household members' current positions. We set the meeting point to the
		 * household's home facility.
		 */
		if (evacuationDecision == EvacuationDecision.NEVER) {
			hdd.setMeetingPointFacilityId(hdd.getHomeFacilityId());
			return;
		}
		
		/*
		 * Else: the household compares its evacuation options - meet at home and leave together 
		 * or leave immediately - and probably split - and meet outside.
		 * 
		 * Also households where all members are located outside the affected area might decide
		 * to meet at home if they assume that there is enough time to meet and evacuate jointly.
		 */
		else {
			double fromHome = hdd.getHouseholdEvacuateFromHomeTime();
			double direct = hdd.getHouseholdDirectEvacuationTime();
			
			/*
			 * If the household can meet at home and leave the evacuation area before
			 * the latest accepted evacuation time, the household will meet at home.
			 * If not, the household selects the faster alternative between direct
			 * evacuation and evacuation via meeting at home.
			 */
			if (fromHome < hdd.getLatestAcceptedLeaveTime()) {
				hdd.setMeetingPointFacilityId(hdd.getHomeFacilityId());
			} else {
				
				// if evacuating from home is faster than directly, meet at home
				if (fromHome <= direct) {
					hdd.setMeetingPointFacilityId(hdd.getHomeFacilityId());
				}
				
				/*
				 * Check how much longer evacuation from home takes. If the time difference is
				 * not to large, it still might be an option.
				 */
				else {
					/*
					 * TODO: the household might be willing to meet at home even if that takes longer.
					 * How to define "longer"? E.g. 30 Minutes are always accepted, more than 60 Minutes
					 * never, in between a random value based on the household id is used. Take
					 * travel time into account? Take ratio difference/travel time into account?
					 * 
					 * So far:
					 * - 0..60 minutes in total
					 * - 0..30 minutes randomly based on households Id
					 * - 0..30 minutes based on ratio tDirect / tFromHome (for tDirect << tFromHome -> ~ 0 min) 
					 */
					double tDirect = direct - time;
					double tFromHome = fromHome - time;
					double ratio = tDirect / tFromHome;	// should be 0.0 .. 1.0
					if (ratio < 0.0 || ratio > 1.0) throw new RuntimeException("Unexpected ratio tDirect/tFromHome was found: " + ratio);
					
					double rand = this.random.nextDouble();
					double delta = rand * 1800.0 + ratio * 1800.0;
					
					if (direct < fromHome - delta) {
						hdd.setMeetingPointFacilityId(Id.create("rescueFacility", ActivityFacility.class));
					} else {
						hdd.setMeetingPointFacilityId(hdd.getHomeFacilityId());
					}
				}			
			}
		}
	}
	
	private void calculateHouseholdReturnHomeTime(Household household, HouseholdDecisionData hdd) {
		
		double latestReturnHomeTime = Double.MIN_VALUE;
		for (Id personId : household.getMemberIds()) {
			PersonDecisionData pdd = this.decisionDataProvider.getPersonDecisionData(personId);
			
			double t = pdd.getAgentReturnHomeTime();
			if (t > latestReturnHomeTime) latestReturnHomeTime = t;
		}
		hdd.setHouseholdReturnHomeTime(latestReturnHomeTime);
	}
	
	/*
	 * TODO: a household might be joined but not at home. Still its members would evacuate jointly.
	 * However, this option is not taken into account so far (neither here nor in the replanning code).
	 */
	private void calculateHouseholdDirectEvacuationTime(Household household) {
		HouseholdDecisionData hdd = this.decisionDataProvider.getHouseholdDecisionData(household.getId());
		
		double latestAgentDirectEvacuationTime = Double.MIN_VALUE;
		for (Id personId : household.getMemberIds()) {
			PersonDecisionData pdd = this.decisionDataProvider.getPersonDecisionData(personId);
			
			double t = pdd.getAgentDirectEvacuationTime();
			if (t > latestAgentDirectEvacuationTime) latestAgentDirectEvacuationTime = t;
		}
		hdd.setHouseholdDirectEvacuationTime(latestAgentDirectEvacuationTime);
	}	
	
	/*
	 * Calculates (estimates) the time until all members of a household have returned home.
	 */
	private void calculateHouseholdMemberTimes(Household household, HouseholdDecisionData hdd, double time) {
		
		for (Id personId : household.getMemberIds()) {
			calculateAgentTimes(personId, hdd.getHomeLinkId(), hdd.getHomeFacilityId(), time);
		}
	}
	
	/*
	 * Calculates (estimates) the time until a person has returned home.
	 */
	private void calculateAgentTimes(Id personId, Id homeLinkId, Id homeFacilityId, double time) {

		PersonDecisionData pdd = this.decisionDataProvider.getPersonDecisionData(personId);
		AgentPosition agentPosition = pdd.getAgentPosition();
		Position positionType = agentPosition.getPositionType();
		
		/*
		 * Identify the transport mode that the agent could use for its trip home.
		 */
		Id fromLinkId = null;
		String mode = null;
		boolean needsHomeRouting = true;
		if (positionType == Position.LINK) {
			mode = agentPosition.getTransportMode();
			fromLinkId = agentPosition.getPositionId();
			
			Link link = this.scenario.getNetwork().getLinks().get(fromLinkId);
			pdd.setAffected(this.coordAnalyzer.isLinkAffected(link));
		} else if (positionType == Position.FACILITY) {

			Id facilityId = agentPosition.getPositionId();
			ActivityFacility facility = this.scenario.getActivityFacilities().getFacilities().get(facilityId);
			pdd.setAffected(this.coordAnalyzer.isFacilityAffected(facility));
			
			/*
			 * If the current Activity is performed ad the homeFacilityId, the return
			 * home time is 0.0.
			 */
			if (facilityId.equals(homeFacilityId)) needsHomeRouting = false;
			
			/*
			 * Otherwise the activity is performed at another facility. Get the link where
			 * the facility is attached to the network.
			 */
			// get the index of the currently performed activity in the selected plan
			MobsimAgent mobsimAgent = this.mobsimDataProvider.getAgent(personId);
			
			Plan executedPlan = WithinDayAgentUtils.getModifiablePlan(mobsimAgent);
			
			if(!mobsimAgent.getState().equals(State.ACTIVITY)) {
				throw new RuntimeException("Expected agent to perform an activity but this is not true. Aborting!");
			}
			int currentActivityIndex = WithinDayAgentUtils.getCurrentPlanElementIndex(mobsimAgent);
			
			Id possibleVehicleId = getVehicleId(executedPlan);
			mode = this.modeAvailabilityChecker.identifyTransportMode(currentActivityIndex, executedPlan, possibleVehicleId);
			fromLinkId = this.scenario.getActivityFacilities().getFacilities().get(agentPosition.getPositionId()).getLinkId();
			
			/*
			 * If the agent has a vehicle available, the car will be available at the
			 * home location for the evacuation. 
			 */
			if (mode.equals(TransportMode.car)) {
				pdd.setAgentReturnHomeVehicleId(possibleVehicleId);
			}	
		} else if (positionType == Position.VEHICLE) {
			mode = agentPosition.getTransportMode();
			
			if (!mode.equals(TransportMode.car)) {
				throw new RuntimeException("Agent's position is VEHICLE but its transport mode is " + mode);
			}
			
			Id vehicleId = agentPosition.getPositionId();
			pdd.setAgentReturnHomeVehicleId(vehicleId);
			fromLinkId = this.mobsimDataProvider.getVehicle(vehicleId).getCurrentLink().getId();
			
			Link link = this.scenario.getNetwork().getLinks().get(fromLinkId);
			pdd.setAffected(this.coordAnalyzer.isLinkAffected(link));
		} else {
			log.error("Found unknown position type: " + positionType.toString());
			return;						
		}
		
		/*
		 * Calculate the travel time from the agents current position to its home facility.
		 */
		if (needsHomeRouting) {
			double travelTime = calculateTravelTime(toHomeFacilityRouter, personId, fromLinkId, homeLinkId, mode, time);			
			pdd.setAgentReturnHomeTime(time + travelTime);
		} else pdd.setAgentReturnHomeTime(time);
		pdd.setAgentTransportMode(mode);
		
		/*
		 * Calculate the travel time from the agent's current position to a secure place.
		 * If the agent is not inside the affected area, its evacuation travel time is 0, therefore
		 * its direct evacuation time is the actual time.
		 * Otherwise its the actual time + the travel time to leave the affected area.
		 */
		if (!pdd.isAffected()) pdd.setAgentDirectEvacuationTime(time);
		else {
			Id toLinkId = this.exitLinkId;
			double travelTime = calculateTravelTime(evacuationRouter, personId, fromLinkId, toLinkId, mode, time);			
			pdd.setAgentDirectEvacuationTime(time + travelTime);
		}
	}
	
	/*
	 * Calculates (estimates) a households evacuation time from home to a rescue facility.
	 */
	private void calculateEvacuationTimeFromHome(Household household, HouseholdDecisionData hdd, double departureTime) {
		
		/*
		 * Get all vehicles that are already located at the home facility.
		 * Then add all vehicles that are used by agents to return to
		 * the home facility.
		 */
		Set<Id<Vehicle>> availableVehicles = new HashSet<Id<Vehicle>>();
		availableVehicles.addAll(this.modeAvailabilityChecker.getAvailableCars(household, hdd.getHomeFacilityId()));
		for (Id personId : household.getMemberIds()) {
			PersonDecisionData pdd = this.decisionDataProvider.getPersonDecisionData(personId);
			if (pdd.getAgentReturnHomeVehicleId() != null) {
				availableVehicles.add(pdd.getAgentReturnHomeVehicleId());
			}
		}
		
		if (availableVehicles.size() > household.getVehicleIds().size()) {
			throw new RuntimeException("To many available vehicles identified!");
		}
		
		HouseholdModeAssignment assignment = this.modeAvailabilityChecker.getHouseholdModeAssignment(household.getMemberIds(), 
				availableVehicles, hdd.getHomeFacilityId());
		
		Id toLinkId = this.exitLinkId;
		double vehicularTravelTime = Double.MIN_VALUE;
		double nonVehicularTravelTime = Double.MIN_VALUE;
		
		for (Entry<Id, String> entry : assignment.getTransportModeMap().entrySet()) {
			String mode = entry.getValue();
			if (mode.equals(TransportMode.car)) {
				// Calculate a car travel time only once since it should not be person dependent.
				if (vehicularTravelTime == Double.MIN_VALUE) {
					vehicularTravelTime = calculateTravelTime(evacuationRouter, entry.getKey(), hdd.getHomeLinkId(), toLinkId, mode, departureTime);
				} else continue;
			}
			else if (mode.equals(TransportMode.ride)) continue;
			else if (mode.equals(PassengerQNetsimEngine.PASSENGER_TRANSPORT_MODE)) continue;
			else {
				double tt = calculateTravelTime(evacuationRouter, entry.getKey(), hdd.getHomeLinkId(), toLinkId, mode, departureTime);
				if (tt > nonVehicularTravelTime) nonVehicularTravelTime = tt;
			}
		}
		
		hdd.setHouseholdEvacuateFromHomeTime(departureTime + Math.max(vehicularTravelTime, nonVehicularTravelTime));
	}
	
	/*
	 * Calculates (estimates) the travel time from a fromLink to a toLink using a given mode.
	 */
	private double calculateTravelTime(TripRouter tripRouter, Id personId, Id fromLinkId, Id toLinkId, String mode, double departureTime) {
		
		PopulationFactory factory = scenario.getPopulation().getFactory();
		Plan plan = factory.createPlan();
		plan.setPerson(scenario.getPopulation().getPersons().get(personId));
		Activity fromActivity = factory.createActivityFromLinkId("current", fromLinkId);
		Activity toActivity = factory.createActivityFromLinkId("home", toLinkId);
		Leg leg = factory.createLeg(mode);
		
		fromActivity.setEndTime(departureTime);
		leg.setDepartureTime(departureTime);
		
		plan.addActivity(fromActivity);
		plan.addLeg(leg);
		plan.addActivity(toActivity);
		
		PlanRouter planRouter = new PlanRouter(tripRouter);
		
		planRouter.run(plan);
		
		// Cannot take the travel time from the Leg since the PlanRouter creates a new Leg Object.
		double travelTime = 0.0;
		for (PlanElement planElement : plan.getPlanElements()) {
			if (planElement instanceof Leg) {
				travelTime += ((Leg) planElement).getTravelTime();
			}
		}

		if (travelTime == Time.UNDEFINED_TIME) {
			throw new RuntimeException("Seems that the PlanRouter did not calculate a valid travel time.");			
		}
		
		if (SelectHouseholdMeetingPoint.writeRoutesToFiles) {
			Person person = factory.createPerson(personId);
			person.addPlan(plan);
			if (tripRouter == this.toHomeFacilityRouter) this.toHomePlans.add(person);
			else if (tripRouter == this.evacuationRouter) this.directEvacuationPlans.add(person);
			else throw new RuntimeException("Unknown TripRouter was found. Aborting!");
		}
		
		return Math.round(travelTime);
	}
	
	/*
	 * Returns the id of the first vehicle used by the agent. Without Within-Day Replanning, 
	 * an agent will use the same vehicle during the whole day. When Within-Day Replanning
	 * is enabled, this method should not be called anymore...
	 */
	private Id getVehicleId(Plan plan) {
		for (PlanElement planElement : plan.getPlanElements()) {
			if (planElement instanceof Leg) {
				Leg leg = (Leg) planElement;
				if (leg.getMode().equals(TransportMode.car)) {
					Route route = leg.getRoute();
					if (route instanceof NetworkRoute) {
						NetworkRoute networkRoute = (NetworkRoute) route;
						return networkRoute.getVehicleId();
					}					
				}
			}
		}
		return null;
	}

}