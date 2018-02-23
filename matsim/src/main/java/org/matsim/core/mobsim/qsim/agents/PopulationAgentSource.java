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

import java.util.*;
import javax.inject.Inject;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.mobsim.framework.AgentSource;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;
import org.matsim.vehicles.Vehicles;

public final class PopulationAgentSource implements AgentSource {
	private static final Logger log = Logger.getLogger( PopulationAgentSource.class );

	private final Population population;
	private final AgentFactory agentFactory;
	private final QSim qsim;
	private final Collection<String> mainModes;
	private Map<Id<Vehicle>,Id<Link>> seenVehicleIds = new HashMap<>() ;
	final private Scenario scenario;
	final private Config config;

	@Inject
	public PopulationAgentSource(QSim qsim, Population population, AgentFactory agentFactory, Config config, Scenario scenario ) {
		Vehicles vehicles = scenario.getVehicles() ;
		QSimConfigGroup qsimConfig = config.qsim() ;
		
		this.population = population;
		this.agentFactory = agentFactory;
		this.qsim = qsim;  
		this.mainModes = config.qsim().getMainModes();
		this.scenario = scenario;
		this.config = config;
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
		Map<String,Id<Vehicle>> seenModes = new HashMap<>();
		for (PlanElement planElement : plan.getPlanElements()) {
			if (planElement instanceof Leg) {
				Leg leg = (Leg) planElement;
				if (this.mainModes.contains(leg.getMode())) { // only simulated modes get vehicles
					NetworkRoute route = (NetworkRoute) leg.getRoute();
					Id<Vehicle> vehicleId = null ;
					if (route != null) {
						vehicleId = route.getVehicleId();
					}
					if (!seenModes.keySet().contains(leg.getMode())) { // create one vehicle per simulated mode, put it on the home location
						// yyyy this is already getting rather messy; need to consider simplifications ...  kai/amit, sep'16
						
						if (vehicleId == null) {
							// if mode choice is allowed, it is possible that a new mode is assigned to an agent and then the route does not have the vehicle
							// but scenario must have that vehicle; however, in order to find the vehicle, we need to identify the vehicle id. Amit July'17
							vehicleId = createAutomaticVehicleId (p.getId(), leg.getMode());
							route.setVehicleId(vehicleId);
						}

						// so here we have a vehicle id, now try to find or create a physical vehicle:
						Vehicle vehicle = scenario.getVehicles().getVehicles().get(vehicleId);
						
						// place the vehicle:
						Id<Link> vehicleLinkId = findVehicleLink(p);
						
						// Checking if the vehicle has been seen before:
						Id<Link> result = this.seenVehicleIds.get( vehicleId ) ;
						if ( result != null ) {
							// if seen before, but placed on same link, then it is ok:
							log.info( "have seen vehicle with id " + vehicleId + " before; not placing it again." );
							if ( result != vehicleLinkId ) {
								throw new RuntimeException("vehicle placement error: vehicleId=" + vehicleId + 
										"; previous placement link=" + vehicleLinkId + "; current placement link=" + result ) ; 
							}
						} else {
							this.seenVehicleIds.put( vehicleId, vehicleLinkId ) ;
							qsim.createAndParkVehicleOnLink(vehicle, vehicleLinkId);
						}
						seenModes.put(leg.getMode(),vehicleId);
					} else {
						if (vehicleId==null && route!=null) {
							vehicleId = seenModes.get(leg.getMode());
							route.setVehicleId( vehicleId );
						}
					}
				}
			}
		}
	}

	/**
	 *	A more careful way to decide where this agent should have its vehicles created
	 *  than to ask agent.getCurrentLinkId() after creation.
	 * @param leg TODO
	 */
	private Id<Link> findVehicleLink(Person p ) {
		/* Cases that come to mind:
		 * (1) multiple persons share car located at home, but possibly brought to different place by someone else.  
		 *      This is treated by the following algo.
		 * (2) person starts day with non-car leg and has car parked somewhere else.  This is NOT treated by the following algo.
		 *      It could be treated by placing the vehicle at the beginning of the first link where it is needed, but this would not
		 *      be compatible with variant (1).
		 */
		for (PlanElement planElement : p.getSelectedPlan().getPlanElements()) {
			if (planElement instanceof Activity) {
				Activity activity = (Activity) planElement;
				ActivityFacilities facilities = scenario.getActivityFacilities() ;
				final Id<Link> activityLinkId = PopulationUtils.computeLinkIdFromActivity(activity, facilities, config ) ;
				if (activityLinkId != null) {
					return activityLinkId;
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

	private Id<Vehicle> createAutomaticVehicleId(Id<Person> personId, String mode) {
		Id<Vehicle> vehicleId ;
		if (config.qsim().getUsePersonIdForMissingVehicleId()) {
			switch (config.qsim().getVehiclesSource()) {
				case defaultVehicle:
				case fromVehiclesData:
					vehicleId = Id.createVehicleId(personId);
					break;
				case modeVehicleTypesFromVehiclesData:
					if(! mode.equals(TransportMode.car)) {
						String vehIdString = personId.toString() + "_" + mode ;
						vehicleId = Id.create(vehIdString, Vehicle.class);
					} else {
						vehicleId = Id.createVehicleId(personId);
					}
					break;
				default:
					throw new RuntimeException("not implemented") ;
			}
		} else {
			throw new IllegalStateException("Found a network route without a vehicle id.");
		}
		return vehicleId;
	}
}
