/* *********************************************************************** *
 * project: org.matsim.*
 * EvacuationPopulationAgentSource.java
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

package playground.christoph.evacuation.mobsim;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.api.experimental.facilities.ActivityFacility;
import org.matsim.core.mobsim.framework.AgentSource;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.agents.AgentFactory;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.households.Household;
import org.matsim.utils.objectattributes.ObjectAttributes;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.Vehicles;

/**
 * @author cdobler
 */
public class EvacuationPopulationAgentSource implements AgentSource {

	private static Logger log = Logger.getLogger(EvacuationPopulationAgentSource.class);
	
    private final Scenario scenario;
    private final ObjectAttributes householdObjectAttributes;
    private final AgentFactory agentFactory;
	private final QSim qsim;

    public EvacuationPopulationAgentSource(Scenario scenario, AgentFactory agentFactory, QSim qsim, 
    		ObjectAttributes householdObjectAttributes) {
        this.scenario = scenario;
        this.agentFactory = agentFactory;
        this.qsim = qsim;
        this.householdObjectAttributes = householdObjectAttributes;
    }
	
	@Override
	public void insertAgentsIntoMobsim() {
		
        Vehicles vehicles = ((ScenarioImpl) scenario).getVehicles();
        
        for (Household household : ((ScenarioImpl) scenario).getHouseholds().getHouseholds().values()) {
        	
        	for (Id personId : household.getMemberIds()) {
        		Person p = scenario.getPopulation().getPersons().get(personId);
        		MobsimAgent agent = this.agentFactory.createMobsimAgentFromPerson(p);
        		qsim.insertAgentIntoMobsim(agent);
    			
    			checkVehicleId(p.getSelectedPlan());
        	}

        	// get household's home facility
			String homeFacilityIdString = this.householdObjectAttributes.getAttribute(household.getId().toString(), "homeFacilityId").toString();
			Id homeFacilityId = this.scenario.createId(homeFacilityIdString);
			ActivityFacility homeFacility = this.scenario.getActivityFacilities().getFacilities().get(homeFacilityId);
        	        	
        	// get id of link where the home facility is attached to the network
        	Id homeLinkId = homeFacility.getLinkId();       	
        	
        	// add vehicles to QSim
        	for (Id vehicleId : household.getVehicleIds()) {
        		Vehicle veh = vehicles.getVehicles().get(vehicleId);
        		qsim.createAndParkVehicleOnLink(veh, homeLinkId);
        	}
        }
	}
	
	private void checkVehicleId(Plan plan) {
		for (PlanElement planElement : plan.getPlanElements()) {
			if (planElement instanceof Leg) {
				Leg leg = (Leg) planElement;
				if (leg.getMode().equals(TransportMode.car)) {
					NetworkRoute route = (NetworkRoute) leg.getRoute();
					Id vehicleId = route.getVehicleId();
					if (vehicleId == null) {
						log.warn("Person " + plan.getPerson().getId().toString() + ": Vehicle Id is null!");
					} else if(!vehicleId.toString().contains("_veh")) {
						log.warn("Person " + plan.getPerson().getId().toString() + " has an unexpected vehicle Id: " + vehicleId.toString());
					}
				}
			}
		}
	}
}
