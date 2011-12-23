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

import java.util.ArrayList;
import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.mobsim.framework.AgentSource;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.households.Household;
import org.matsim.ptproject.qsim.QSim;
import org.matsim.ptproject.qsim.agents.AgentFactory;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.Vehicles;

/**
 * @author cdobler
 */
public class EvacuationPopulationAgentSource implements AgentSource {

    private final Scenario scenario;
    private final AgentFactory agentFactory;
	private final QSim qsim;

    public EvacuationPopulationAgentSource(Scenario scenario, AgentFactory agentFactory, QSim qsim) {
        this.scenario = scenario;
        this.agentFactory = agentFactory;
        this.qsim = qsim;
    }
	
	@Override
	public List<MobsimAgent> insertAgentsIntoMobsim() {
        List<MobsimAgent> agents = new ArrayList<MobsimAgent>();
        Vehicles vehicles = ((ScenarioImpl) scenario).getVehicles();
        
        for (Household household : ((ScenarioImpl) scenario).getHouseholds().getHouseholds().values()) {
        	
        	if (household.getMemberIds().size() == 0) continue;
        	
        	for (Id personId : household.getMemberIds()) {
        		Person p = scenario.getPopulation().getPersons().get(personId);
        		MobsimAgent agent = this.agentFactory.createMobsimAgentFromPersonAndInsert(p);
    			agents.add(agent);
        	}
        	
        	// get first household member
        	Person p = scenario.getPopulation().getPersons().get(household.getMemberIds().get(0));
        	
        	// get id of the household's home facility
        	Id homeLinkId = ((Activity) p.getSelectedPlan().getPlanElements().get(0)).getLinkId();       	
        	
        	// add vehicles to QSim
        	for (Id vehicleId : household.getVehicleIds()) {
        		Vehicle veh = vehicles.getVehicles().get(vehicleId);
        		qsim.createAndParkVehicleOnLink(veh, homeLinkId);
        	}
        }
        return agents;
	}
}
