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

package playground.wrashid.parkingSearch.mobsim;

import java.util.ArrayList;
import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.mobsim.framework.AgentSource;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.ptproject.qsim.QSim;
import org.matsim.ptproject.qsim.agents.AgentFactory;
import org.matsim.ptproject.qsim.agents.ExperimentalBasicWithindayAgent;
import org.matsim.vehicles.VehicleUtils;

import playground.wrashid.parkingSearch.withinday.InsertParkingActivities;

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

    public ParkingPopulationAgentSource(Population population, AgentFactory agentFactory, 
    		QSim qsim, InsertParkingActivities insertParkingActivities) {
        this.population = population;
        this.agentFactory = agentFactory;
        this.qsim = qsim;
        this.insertParkingActivities = insertParkingActivities;
    }
	
	@Override
	public List<MobsimAgent> insertAgentsIntoMobsim() {
        List<MobsimAgent> agents = new ArrayList<MobsimAgent>();
		for (Person p : population.getPersons().values()) {
			MobsimAgent agent = this.agentFactory.createMobsimAgentFromPersonAndInsert(p);
			agents.add(agent);
			
			/*
			 * If it is a  within-day replanning agent, we use its plan instead of the
			 * person's plan because the agent's plan may have already been altered.  
			 */
	    	Plan plan;
	    	if (agent instanceof ExperimentalBasicWithindayAgent) {
	    		plan = ((ExperimentalBasicWithindayAgent) agent).getSelectedPlan();
	    	} else plan = p.getSelectedPlan();
	    	
	    	/*
	    	 * Insert parking activities into the plan
	    	 */
	    	insertParkingActivities.run(plan);
	    	
			qsim.createAndParkVehicleOnLink(VehicleUtils.getFactory().createVehicle(agent.getId(), 
					VehicleUtils.getDefaultVehicleType()), getParkingLinkId(plan));
		}
        return agents;
	}

	/**
	 * Returns link's id where an agent performs its first parking activity. It is
	 * the link where the facility is attached to, where the agent has initially 
	 * parked its car. If no parking activity is found, the agent does not perform 
	 * a car trip. In that case we assume that the agent's car is at his home facility.
	 * 
	 * @param agent
	 * @return id of the link where agent's home parking facility is attached to
	 */
    private Id getParkingLinkId(Plan plan) {
    	for (PlanElement planElement : plan.getPlanElements()) {
    		if (planElement instanceof Activity) {
    			Activity activity = (Activity) planElement;
    			if (activity.getType().equals("parking")) return activity.getLinkId();
    		}
    	}
    	return ((Activity) plan.getPlanElements().get(0)).getLinkId();
    }
}
