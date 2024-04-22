/* *********************************************************************** *
 * project: org.matsim.*
 * PopulationAgentSourceWithVehicles.java
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
package org.matsim.contrib.socnetsim.sharedvehicles.qsim;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.mobsim.framework.AgentSource;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.agents.AgentFactory;
import org.matsim.core.mobsim.qsim.qnetsimengine.QVehicle;
import org.matsim.core.mobsim.qsim.qnetsimengine.QVehicleImpl;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;
import org.matsim.vehicles.Vehicles;
import org.matsim.vehicles.VehiclesFactory;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Inserts agent into the QSim, providing them with the vehicles specified in their
 * routes, or one vehicule per agent if nothing is specified in the routes.
 *
 * <br>
 * If the population contains agents with vehicles in their routes, and others without,
 * it will result in a failure.
 *
 * @author thibautd
 */
public class PopulationAgentSourceWithVehicles implements AgentSource {
	private final  Population population;
	private final AgentFactory agentFactory;
	private final QSim qsim;
	private final Map<String, VehicleType> modeVehicleTypes;
	private final Collection<String> mainModes;

	public PopulationAgentSourceWithVehicles(
			final Population population,
			final AgentFactory agentFactory,
			final QSim qsim) {
		this.population = population;
		this.agentFactory = agentFactory;
		this.qsim = qsim;  
		this.modeVehicleTypes = new HashMap<>();
		this.mainModes = qsim.getScenario().getConfig().qsim().getMainModes();
		for (String mode : mainModes) {
			modeVehicleTypes.put(mode, VehicleUtils.createDefaultVehicleType());
		}
	}

	@Override
	public void insertAgentsIntoMobsim() {
		Vehicles vehicles = this.qsim.getScenario().getVehicles();
		VehiclesFactory vehiclesFactory = vehicles.getFactory();
		final Set<Id> alreadyParked = new HashSet<>();
		for (Person p : population.getPersons().values()) {
			final MobsimAgent agent = this.agentFactory.createMobsimAgentFromPerson(p);
			final Plan plan = p.getSelectedPlan();
			for (PlanElement planElement : plan.getPlanElements()) {
				if (planElement instanceof Leg) {
					Leg leg = (Leg) planElement;
					if (this.mainModes.contains(leg.getMode())) { // only simulated modes get vehicles
						final Id vehicleId = getVehicleId( p , leg );
						if ( !alreadyParked.add( vehicleId ) ) {
							// only park each vehicle once
							continue;
						}

						final Id vehicleLink = findVehicleLink(p);
						final Vehicle basicVehicle = vehiclesFactory.createVehicle(
								vehicleId,
								modeVehicleTypes.get( leg.getMode() ) );
						
//						qsim.createAndParkVehicleOnLink( basicVehicle, vehicleLink);
						QVehicle veh = new QVehicleImpl( basicVehicle ) ;
						// yyyyyy better use the (new) QVehicleFactory. kai, nov'18
						qsim.addParkedVehicle( veh, vehicleLink );
					}
				}
			}
			// this MUST be the last action, because stuff already happens at
			// insertion (even if run was not called yet...)
			qsim.insertAgentIntoMobsim(agent);
		}
	}

	private boolean usedPersonId = false;
	private boolean usedRouteField = false;
	private Id getVehicleId(
			final Person p,
			final Leg leg) {
		final Route route = leg.getRoute();

		if (route instanceof NetworkRoute &&
				((NetworkRoute) route).getVehicleId() != null) {
			if ( usedPersonId ) throw new InconsistentVehiculeSpecificationsException();
			usedRouteField = true;
			return ((NetworkRoute) route).getVehicleId();
		}

		if ( usedRouteField )  throw new InconsistentVehiculeSpecificationsException();
		usedPersonId = true;
		return p.getId();
	}

	private Id findVehicleLink(final Person p) {
		// A more careful way to decide where this agent should have its vehicles created
		// than to ask agent.getCurrentLinkId() after creation.
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

	public void setModeVehicleTypes(final Map<String, VehicleType> modeVehicleTypes) {
		this.modeVehicleTypes.putAll( modeVehicleTypes );
	}

	public void setModeVehicleType(final String mode, final VehicleType type) {
		this.modeVehicleTypes.put( mode , type );
	}

	public static class InconsistentVehiculeSpecificationsException extends RuntimeException {
		private static final long serialVersionUID = 1L;
	}
}
