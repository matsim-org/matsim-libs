/* *********************************************************************** *
 * project: org.matsim.*
 * ParkingAgentSource.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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

package playground.christoph.parking.core.mobsim.agents;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.config.groups.GlobalConfigGroup;
import org.matsim.core.mobsim.framework.AgentSource;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.agents.AgentFactory;
import org.matsim.core.mobsim.qsim.agents.WithinDayAgentUtils;
import org.matsim.core.replanning.ReplanningContext;
import org.matsim.core.replanning.modules.AbstractMultithreadedModule;
import org.matsim.population.algorithms.PlanAlgorithm;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleUtils;

import playground.christoph.parking.core.mobsim.InsertParkingActivities;
import playground.christoph.parking.core.mobsim.ParkingInfrastructure;
import playground.christoph.parking.core.mobsim.UpdateRoutes;
import playground.christoph.parking.withinday.utils.ParkingRouter;
import playground.christoph.parking.withinday.utils.ParkingRouterFactory;

public class ParkingAgentSource implements AgentSource {

	private static final Logger log = Logger.getLogger(ParkingAgentSource.class);
	
	private Scenario scenario;
	private AgentFactory agentFactory;
	private QSim qsim;
	private final ParkingInfrastructure parkingInfrastructure;
	private final ParkingRouterFactory parkingRouterFactory;

	public ParkingAgentSource(Scenario scenario, AgentFactory agentFactory, QSim qsim,
			ParkingInfrastructure parkingInfrastructure, ParkingRouterFactory parkingRouterFactory) {
		this.scenario = scenario;
		this.agentFactory = agentFactory;
		this.qsim = qsim;
		this.parkingInfrastructure = parkingInfrastructure;
		this.parkingRouterFactory = parkingRouterFactory;
	}

	@Override
	public void insertAgentsIntoMobsim() {
		
//		parkingInfrastructure.resetParkingFacilityForNewIteration();
		
		PlanAlgorithm insertParkingActivities = new InsertParkingActivities(scenario, this.parkingInfrastructure);
		
		ReplanningContext replanningContext = null;	// we do not need this here
		RouteUpdater routeUpdater = new RouteUpdater(scenario.getConfig().global(), this.parkingRouterFactory);
		routeUpdater.prepareReplanning(replanningContext);
		
		for (Person p : scenario.getPopulation().getPersons().values()) {
			
			// Create agent...
			MobsimAgent agent = this.agentFactory.createMobsimAgentFromPerson(p);
			Plan modifiablePlan = WithinDayAgentUtils.getModifiablePlan(agent);
			
			// ... and then insert parking activities into its plan.
			insertParkingActivities.run(modifiablePlan);
			
			// Insert the agent into the mobsim...
			qsim.insertAgentIntoMobsim(agent);
			
			// ... and park its vehicle at the link where the first parking activity is performed.
			this.parkVehicleAtFirstParking(modifiablePlan);

			/*
			 * Finally, Update agent's routes which is necessary since their plans have been 
			 * altered by adding parking activities.
			 */
			routeUpdater.handlePlan(modifiablePlan);
		}
		
		// Finish replanning starts the replanning threads and waits until they a finished.
		routeUpdater.finishReplanning();
	}

	/**
	 * Registers the agent's vehicle at the very first parking facility in the
	 * agent's plan. If the agent has no parking activities scheduled, its plan
	 * does not contain car legs and therefore no vehicles is required.
	 * 
	 * @param plan
	 *            agent's executed plan
	 */
	private void parkVehicleAtFirstParking(Plan plan) {
		
		for (PlanElement planElement : plan.getPlanElements()) {
			if (planElement instanceof Activity) {
				Activity activity = (Activity) planElement;
				if (InsertParkingActivities.PARKINGACTIVITY.equals(activity.getType())) {
					Id<Vehicle> vehicleId = this.parkingInfrastructure.getVehicleId(plan.getPerson());
					
//					log.info("Park car for agent " + plan.getPerson().getId() +
//							" in facility " + activity.getFacilityId() +
//							" on link " + activity.getLinkId() + ".");
					
					this.parkingInfrastructure.parkVehicle(vehicleId, activity.getFacilityId());
					
					Vehicle vehicle = VehicleUtils.getFactory().createVehicle(Id.create(plan.getPerson().getId(), Vehicle.class), VehicleUtils.getDefaultVehicleType());
					qsim.createAndParkVehicleOnLink(vehicle, activity.getLinkId());

					return;
				}
			}
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
				if (InsertParkingActivities.PARKINGACTIVITY.equals(activity.getType())) return activity.getLinkId();
			}
		}
		return ((Activity) plan.getPlanElements().get(0)).getLinkId();
	}
	
	private static class RouteUpdater extends AbstractMultithreadedModule {

		private final ParkingRouterFactory parkingRouterFactory;
		
		public RouteUpdater(GlobalConfigGroup globalConfigGroup, ParkingRouterFactory parkingRouterFactory) {
			super(globalConfigGroup);
			this.parkingRouterFactory = parkingRouterFactory;
		}

		@Override
		public PlanAlgorithm getPlanAlgoInstance() {
			
			ParkingRouter parkingRouter = this.parkingRouterFactory.createParkingRouter();
			PlanAlgorithm updateRoutes = new UpdateRoutes(parkingRouter);
						
			return updateRoutes;
		}
	}
}