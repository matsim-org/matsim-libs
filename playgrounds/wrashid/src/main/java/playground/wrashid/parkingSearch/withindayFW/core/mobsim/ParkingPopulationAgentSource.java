/* *********************************************************************** *
 * project: org.matsim.*
 * ParkingPopulationAgentSource.java
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

package playground.wrashid.parkingSearch.withindayFW.core.mobsim;

import java.util.HashMap;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.mobsim.framework.AgentSource;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.agents.AgentFactory;
import org.matsim.core.mobsim.qsim.agents.WithinDayAgentUtils;
import org.matsim.facilities.ActivityFacility;
import org.matsim.population.algorithms.AbstractPersonAlgorithm;
import org.matsim.population.algorithms.ParallelPersonAlgorithmRunner;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleUtils;

import playground.wrashid.parkingSearch.withindayFW.core.InsertParkingActivities;
import playground.wrashid.parkingSearch.withindayFW.core.ParkingInfrastructure;

/**
 * Creates agents for the QSim. Agents' vehicles are add to the parking
 * facilities where they perform their first parking activities. Therefore,
 * teleportation of vehicles should not be necessary anymore.
 * 
 * @author cdobler
 */
public class ParkingPopulationAgentSource implements AgentSource {

	private final Population population;
	private final AgentFactory agentFactory;
	private final QSim qsim;
	private final InsertParkingActivities insertParkingActivities;
	private final ParkingInfrastructure parkingInfrastructure;
	private final int numOfThreads;

	public ParkingPopulationAgentSource(Population population, AgentFactory agentFactory, QSim qsim,
			InsertParkingActivities insertParkingActivities, ParkingInfrastructure parkingInfrastructure, int numOfThreads) {
		this.population = population;
		this.agentFactory = agentFactory;
		this.qsim = qsim;
		this.insertParkingActivities = insertParkingActivities;
		this.parkingInfrastructure = parkingInfrastructure;
		this.numOfThreads = numOfThreads;
	}

	@Override
	public void insertAgentsIntoMobsim() {
		parkingInfrastructure.resetParkingFacilityForNewIteration();
		
		ParallelPersonAlgorithmRunner.run(population, numOfThreads, new ParkingAgentInsertParkingActs(this.agentFactory));
		
		
//		for (Person p : population.getPersons().values()) {
//
//			reserveInitialParking(p);
//
//			MobsimAgent agent = this.agentFactory.createMobsimAgentFromPerson(p);
//			qsim.insertAgentIntoMobsim(agent);
//
//			/*
//			 * If it is a within-day replanning agent, we use its plan instead
//			 * of the person's plan because the agent's plan may have already
//			 * been altered.
//			 */
//			Plan plan;
//			if (agent instanceof ExperimentalBasicWithindayAgent) {
//				plan = ((ExperimentalBasicWithindayAgent) agent).getSelectedPlan();
//			} else
//				plan = p.getSelectedPlan();
//
//			/*
//			 * Insert parking activities into the plan
//			 */
//			
//
//			insertParkingActivities.run(plan);
//			
//			qsim.createAndParkVehicleOnLink(
//					VehicleUtils.getFactory().createVehicle(agent.getId(), VehicleUtils.getDefaultVehicleType()),
//					getParkingLinkId(plan));
//			
//			
//
//		}
	}
	
	private class ParkingAgentInsertParkingActs extends AbstractPersonAlgorithm{

		private final AgentFactory agentFac;

		public ParkingAgentInsertParkingActs(AgentFactory agentFactory){
			agentFac = agentFactory;
			
		}
		
		@Override
		public void run(Person person) {
			MobsimAgent agent=null;
			synchronized(parkingInfrastructure){
				reserveInitialParking(person);
			}
				
			synchronized(qsim){
				agent = agentFac.createMobsimAgentFromPerson(person);
				qsim.insertAgentIntoMobsim(agent);
			}
			
			Plan plan = WithinDayAgentUtils.getModifiablePlan(agent);

			/*
			 * Insert parking activities into the plan
			 */
			
			insertParkingActivities.run(plan);
			
			synchronized(qsim){
			qsim.createAndParkVehicleOnLink(
					VehicleUtils.getFactory().createVehicle(Id.create(agent.getId(), Vehicle.class), VehicleUtils.getDefaultVehicleType()),
					getParkingLinkId(plan));
			}
		
		
	}

	private void reserveInitialParking(Person p) {

		HashMap<Id, ActivityFacility> initialParkingFacilityOfAgent = parkingInfrastructure.getInitialParkingFacilityOfAgent();
		Id<Person> personId = p.getId();
		initInitialParkingFacilityIfRequired(p, initialParkingFacilityOfAgent, personId);

		parkingInfrastructure.parkVehicle(initialParkingFacilityOfAgent.get(personId).getId());

	}

	private void initInitialParkingFacilityIfRequired(Person p, HashMap<Id, ActivityFacility> initialParkingFacilityOfAgent,
			Id<Person> personId) {
		if (initialParkingFacilityOfAgent.get(personId) == null) {

			Activity firstActivity = (Activity) p.getSelectedPlan().getPlanElements().get(0);

			ActivityFacility closestFreeParkingFacility = parkingInfrastructure.getClosestFreeParkingFacility(firstActivity
					.getCoord());
			initialParkingFacilityOfAgent.put(personId, closestFreeParkingFacility);
		}
	}

	/**
	 * Returns link's id where an agent performs its first parking activity. It
	 * is the link where the facility is attached to, where the agent has
	 * initially parked its car. If no parking activity is found, the agent does
	 * not perform a car trip. In that case we assume that the agent's car is at
	 * his home facility.
	 * 
	 * @param plan
	 *            agent's executed plan
	 * @return id of the link where agent's home parking facility is attached to
	 */
	private Id getParkingLinkId(Plan plan) {
		for (PlanElement planElement : plan.getPlanElements()) {
			if (planElement instanceof Activity) {
				Activity activity = (Activity) planElement;
				if (activity.getType().equals("parking"))
					return activity.getLinkId();
			}
		}
		return ((Activity) plan.getPlanElements().get(0)).getLinkId();
	}

	/**
	 * Registers the agent's vehicle at the very first parking facility in the
	 * agent's plan. If the agent has no parking activities scheduled, its plan
	 * does not contain car legs and therefore no vehicles is required.
	 * 
	 * @param plan
	 *            agent's executed plan
	 */
	private void parkVehicle(Plan plan) {
		for (PlanElement planElement : plan.getPlanElements()) {
			if (planElement instanceof Activity) {
				Activity activity = (Activity) planElement;
				if (activity.getType().equals("parking")) {
					parkingInfrastructure.parkVehicle(activity.getFacilityId());
					return;
				}
			}
		}
	}
}}