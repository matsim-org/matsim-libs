/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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

package org.matsim.core.mobsim.qsim.agents;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.mobsim.framework.AgentSource;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;

import java.util.*;

public final class PopulationAgentSource implements AgentSource {

	private final Population population;
	private final AgentFactory agentFactory;
	private final QSim qsim;
	private Map<String, VehicleType> modeVehicleTypes;
	private final Collection<String> mainModes;

    public PopulationAgentSource(Population population, AgentFactory agentFactory, QSim qsim) {
		this.population = population;
		this.agentFactory = agentFactory;
		this.qsim = qsim;  
		this.modeVehicleTypes = new HashMap<>();
		this.mainModes = qsim.getScenario().getConfig().qsim().getMainModes();
		for (String mode : mainModes) {
			// initialize each mode with default vehicle type:
			modeVehicleTypes.put(mode, VehicleUtils.getDefaultVehicleType());
		}
	}

	@Override
	public void insertAgentsIntoMobsim() {
		for (Person p : population.getPersons().values()) {
			MobsimAgent agent = this.agentFactory.createMobsimAgentFromPerson(p);
			qsim.insertAgentIntoMobsim(agent);
		}
        for (Person p : population.getPersons().values()) {
            insertVehicles(p);
        }
    }

	private void insertVehicles(Person p) {
		Plan plan = p.getSelectedPlan();
		Set<String> seenModes = new HashSet<>();
		for (PlanElement planElement : plan.getPlanElements()) {
			if (planElement instanceof Leg) {

				// to through all legs:
				Leg leg = (Leg) planElement;
				if (this.mainModes.contains(leg.getMode())) { // only simulated modes get vehicles
					if (!seenModes.contains(leg.getMode())) { // create one vehicle per simulated mode, put it on the home location
                        NetworkRoute route = (NetworkRoute) leg.getRoute();
                        Id<Vehicle> vehicleId;
                        if (route != null) {
                            vehicleId = route.getVehicleId();
                        } else {
                            vehicleId = null;
                        }
                        if (vehicleId == null) {
                            if (qsim.getScenario().getConfig().qsim().getUsePersonIdForMissingVehicleId()) {
                                vehicleId = Id.create(p.getId(), Vehicle.class);
                            } else {
                                throw new IllegalStateException("Found a network route without a vehicle id.");
                            }
                        }
                        Vehicle vehicle = null ;
                        switch ( qsim.getScenario().getConfig().qsim().getVehiclesSource() ) {
                        case DefaultVehicle:
                              vehicle = VehicleUtils.getFactory().createVehicle(vehicleId, modeVehicleTypes.get(leg.getMode()));
                        	break;
                        case FromVehiclesFile:
                              vehicle = qsim.getScenario().getVehicles().getVehicles().get(vehicleId);
                              if (vehicle == null) {
                                  throw new IllegalStateException("Expecting a vehicle id which is missing in the vehicles database: " + vehicleId);
                              }
                        	break;
                        default:
                        	throw new RuntimeException("not implemented") ;
                        }
                        Id<Link> vehicleLink = findVehicleLink(p);
                        qsim.createAndParkVehicleOnLink(vehicle, vehicleLink);
						seenModes.add(leg.getMode());
					}
				}
			}
		}
	}

	/**
	 *	A more careful way to decide where this agent should have its vehicles created
	 *  than to ask agent.getCurrentLinkId() after creation.
	 */
	private static Id<Link> findVehicleLink(Person p) {
		// hope it is ok to make this public as long as it is static final. kai, mar'14
		
		for (PlanElement planElement : p.getSelectedPlan().getPlanElements()) {
			if (planElement instanceof Activity) {
				Activity activity = (Activity) planElement;
				if (activity.getLinkId() != null) {
					return activity.getLinkId();
				}
			} else if (planElement instanceof Leg) {
				Leg leg = (Leg) planElement;
				if (leg.getRoute().getStartLinkId() != null) {
					return leg.getRoute().getStartLinkId();
				}
			}
		}
		throw new RuntimeException("Don't know where to put a vehicle for this agent.");
	}

	public void setModeVehicleTypes(Map<String, VehicleType> modeVehicleTypes) {
		this.modeVehicleTypes = modeVehicleTypes;
	}

}
