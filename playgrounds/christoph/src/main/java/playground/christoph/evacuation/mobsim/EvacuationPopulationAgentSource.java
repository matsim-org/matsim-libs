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

import java.io.BufferedWriter;
import java.io.IOException;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.mobsim.framework.AgentSource;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.agents.AgentFactory;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.facilities.ActivityFacility;
import org.matsim.households.Household;
import org.matsim.utils.objectattributes.ObjectAttributes;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.Vehicles;

import playground.christoph.evacuation.controler.EvacuationConstants;

/**
 * @author cdobler
 */
public class EvacuationPopulationAgentSource implements AgentSource {

	private static Logger log = Logger.getLogger(EvacuationPopulationAgentSource.class);
	
	public static String parkedVehiclesFileName = "parkedVehiclesFile.xml.gz";
	public static String agentsVehiclesFileName = "agentsVehiclesFile.xml.gz";
	
    private final Scenario scenario;
    private final ObjectAttributes householdObjectAttributes;
    private final AgentFactory agentFactory;
	private final QSim qsim;
	private final String agentsVehiclesFile;
	private final String parkedVehiclesFile;
	
    public EvacuationPopulationAgentSource(Scenario scenario, AgentFactory agentFactory, QSim qsim, 
    		ObjectAttributes householdObjectAttributes, String agentsVehiclesFile, String parkedVehiclesFile) {
        this.scenario = scenario;
        this.agentFactory = agentFactory;
        this.qsim = qsim;
        this.householdObjectAttributes = householdObjectAttributes;
        this.agentsVehiclesFile = agentsVehiclesFile;
        this.parkedVehiclesFile = parkedVehiclesFile;
    }
	
	@Override
	public void insertAgentsIntoMobsim() {
		
		try {
			BufferedWriter agentsWriter = null;
			BufferedWriter parkedWriter = null;
			if (this.agentsVehiclesFile != null) {
				agentsWriter = IOUtils.getBufferedWriter(this.agentsVehiclesFile);
				agentsWriter.write("agentId");
				agentsWriter.write("\t");
				agentsWriter.write("vehicleId");
				agentsWriter.write("\n");
			}
			if (this.parkedVehiclesFile != null) {
				parkedWriter = IOUtils.getBufferedWriter(this.parkedVehiclesFile);
				parkedWriter.write("vehicleId");
				parkedWriter.write("\t");
				parkedWriter.write("linkId");
				parkedWriter.write("\n");
			}
			
			Logger.getLogger(this.getClass()).fatal("cannot say if the following should be vehicles or transit vehicles; aborting ... .  kai, feb'15");
			System.exit(-1); 

			Vehicles vehicles = ((ScenarioImpl) scenario).getTransitVehicles();
			
			for (Household household : ((ScenarioImpl) scenario).getHouseholds().getHouseholds().values()) {
				
				// get household's home facility
				String homeFacilityIdString = this.householdObjectAttributes.getAttribute(household.getId().toString(), EvacuationConstants.HOUSEHOLD_HOMEFACILITYID).toString();
				Id<ActivityFacility> homeFacilityId = Id.create(homeFacilityIdString, ActivityFacility.class);
				ActivityFacility homeFacility = this.scenario.getActivityFacilities().getFacilities().get(homeFacilityId);
				
				// get id of link where the home facility is attached to the network
				Id<Link> homeLinkId = homeFacility.getLinkId();       	
				
				// add vehicles to QSim
				for (Id<Vehicle> vehicleId : household.getVehicleIds()) {
					Vehicle veh = vehicles.getVehicles().get(vehicleId);
					qsim.createAndParkVehicleOnLink(veh, homeLinkId);
					
					if (parkedWriter != null) {
						parkedWriter.write(vehicleId.toString());
						parkedWriter.write("\t");
						parkedWriter.write(homeLinkId.toString());
						parkedWriter.write("\n");
					}
				}
				
				/*
				 * Agents have to be added after the vehicles to the qsim since they could
				 * depart immediately after insertion, if the end time of their first activity
				 * is <= the simulation start time.
				 */
				for (Id personId : household.getMemberIds()) {
					Person p = scenario.getPopulation().getPersons().get(personId);
					MobsimAgent agent = this.agentFactory.createMobsimAgentFromPerson(p);
					qsim.insertAgentIntoMobsim(agent);
					
					Id agentsVehicleId = checkVehicleId(p.getSelectedPlan());
					if (agentsWriter != null) {
						agentsWriter.write(agent.getId().toString());
						agentsWriter.write("\t");
						if (agentsVehicleId != null) agentsWriter.write(agentsVehicleId.toString());
						else agentsWriter.write("null");
						agentsWriter.write("\n");
					}
				}
			}
			if (agentsWriter != null) {
				agentsWriter.flush();
				agentsWriter.close();
			}
			if (parkedWriter != null) {
				parkedWriter.flush();
				parkedWriter.close();
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	private Id<Vehicle> checkVehicleId(Plan plan) {
		Id<Vehicle> agentsVehicleId = null;
		for (PlanElement planElement : plan.getPlanElements()) {
			if (planElement instanceof Leg) {
				Leg leg = (Leg) planElement;
				if (leg.getMode().equals(TransportMode.car)) {
					NetworkRoute route = (NetworkRoute) leg.getRoute();
					Id<Vehicle> vehicleId = route.getVehicleId();
					if (vehicleId == null) {
						log.warn("Person " + plan.getPerson().getId().toString() + ": Vehicle Id is null!");
					} else if(!vehicleId.toString().contains("_veh")) {
						log.warn("Person " + plan.getPerson().getId().toString() + " has an unexpected vehicle Id: " + vehicleId.toString());
					} else {
						if (agentsVehicleId == null) agentsVehicleId = vehicleId;
						else if (!vehicleId.equals(agentsVehicleId)) {
							log.warn("Person " + plan.getPerson().getId().toString() + " used different vehicles. Ids: " + vehicleId.toString() +
									" and " + agentsVehicleId.toString());
						}
					}
				}
			}
		}
		return agentsVehicleId;
	}
}