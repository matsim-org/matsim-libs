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
	private boolean insertVehicles = true;

	public PopulationAgentSource(Population population, AgentFactory agentFactory, QSim qsim) {
		this.population = population;
		this.agentFactory = agentFactory;
		this.qsim = qsim;  
		this.modeVehicleTypes = new HashMap<>();
		this.mainModes = qsim.getScenario().getConfig().qsim().getMainModes();
		for (String mode : mainModes) {
			modeVehicleTypes.put(mode, VehicleUtils.getDefaultVehicleType());
		}
	}

	@Override
	public void insertAgentsIntoMobsim() {
		for (Person p : population.getPersons().values()) {
			MobsimAgent agent = this.agentFactory.createMobsimAgentFromPerson(p);
			qsim.insertAgentIntoMobsim(agent);
		}
		if (insertVehicles) {
			for (Person p : population.getPersons().values()) {
				insertVehicles(p);
			}
		}
	}

<<<<<<< HEAD
	void insertVehicles(Person p) {
=======
	private void insertVehicles(Person p) {
>>>>>>> some formatting; one final
		Plan plan = p.getSelectedPlan();
		Set<String> seenModes = new HashSet<>();
		for (PlanElement planElement : plan.getPlanElements()) {
			if (planElement instanceof Leg) {
				Leg leg = (Leg) planElement;
				if (this.mainModes.contains(leg.getMode())) { // only simulated modes get vehicles
					if (!seenModes.contains(leg.getMode())) { // create one vehicle per simulated mode, put it on the home location
						Id<Link> vehicleLink = findVehicleLink(p);
						qsim.createAndParkVehicleOnLink(
								VehicleUtils.getFactory().createVehicle(Id.create(p.getId(), Vehicle.class), modeVehicleTypes.get(leg.getMode())), 
								vehicleLink);
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
	
	public void setInsertVehicles(boolean insertVehicles) {
		this.insertVehicles = insertVehicles;
	}

}
