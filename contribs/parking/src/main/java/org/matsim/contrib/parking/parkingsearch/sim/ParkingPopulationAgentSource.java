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

package org.matsim.contrib.parking.parkingsearch.sim;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.mobsim.framework.AgentSource;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.agents.AgentFactory;
import org.matsim.core.mobsim.qsim.qnetsimengine.QVehicle;
import org.matsim.core.mobsim.qsim.qnetsimengine.QVehicleImpl;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;
import org.matsim.vehicles.Vehicles;
import org.matsim.vehicles.VehiclesFactory;

import jakarta.inject.Inject;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;


public final class ParkingPopulationAgentSource implements AgentSource {
	private static final Logger log = LogManager.getLogger( ParkingPopulationAgentSource.class );

	private final Population population;
	private final AgentFactory agentFactory;
	private final QSim qsim;
	private Map<String, VehicleType> modeVehicleTypes;
	private final Collection<String> mainModes;
	private Map<Id<Vehicle>,Id<Link>> seenVehicleIds = new HashMap<>() ;

	@Inject
	public ParkingPopulationAgentSource(Population population, AgentFactory agentFactory, QSim qsim ) {
		Vehicles vehicles = qsim.getScenario().getVehicles() ;
		QSimConfigGroup qsimConfig = qsim.getScenario().getConfig().qsim() ;

		this.population = population;
		this.agentFactory = agentFactory;
		this.qsim = qsim;
		this.modeVehicleTypes = new HashMap<>();
		this.mainModes = new HashSet<>(qsim.getScenario().getConfig().qsim().getMainModes());
		switch ( qsimConfig.getVehiclesSource() ) {
		case defaultVehicle:
			for (String mode : mainModes) {
				// initialize each mode with default vehicle type:
				modeVehicleTypes.put(mode, VehicleUtils.createDefaultVehicleType());
			}
			break;
		case fromVehiclesData:
			// don't do anything
			break;
		case modeVehicleTypesFromVehiclesData:
			for (String mode : mainModes) {
				VehicleType vehicleType = vehicles.getVehicleTypes().get( Id.create(mode, VehicleType.class) ) ;
				modeVehicleTypes.put(mode, vehicleType );
			}
			break;
		default:
			break;

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
		Vehicles vehicles = this.qsim.getScenario().getVehicles();
		VehiclesFactory vehiclesFactory = vehicles.getFactory();
		Plan plan = p.getSelectedPlan();
		Set<String> seenModes = new HashSet<>();
		boolean usePersonIdForMissingVehicleId = qsim.getScenario().getConfig().qsim().getUsePersonIdForMissingVehicleId();
		QSimConfigGroup.VehiclesSource vehiclesSource = qsim.getScenario().getConfig().qsim().getVehiclesSource();
		for (PlanElement planElement : plan.getPlanElements()) {
			if (planElement instanceof Leg) {
				Leg leg = (Leg) planElement;
				if (this.mainModes.contains(leg.getMode())) { // only simulated modes get vehicles
					if (!seenModes.contains(leg.getMode())) { // create one vehicle per simulated mode, put it on the home location
						Route route = leg.getRoute();
						Id<Vehicle> vehicleId = null ;
						if (route != null) {
							if (route instanceof NetworkRoute)
							vehicleId = ((NetworkRoute) route).getVehicleId(); // may be null!
						}
						if (vehicleId == null) {
							if (usePersonIdForMissingVehicleId) {
								vehicleId = Id.create(p.getId(), Vehicle.class);
							} else {
								throw new IllegalStateException("Found a network route without a vehicle id.");
							}
						}
						Vehicle vehicle = null ;
						switch ( vehiclesSource ) {
						case defaultVehicle:
						case modeVehicleTypesFromVehiclesData:
							vehicle = vehiclesFactory.createVehicle(vehicleId, modeVehicleTypes.get(leg.getMode()));
							break;
						case fromVehiclesData:
							vehicle = vehicles.getVehicles().get(vehicleId);
							if (vehicle == null) {
								throw new IllegalStateException("Expecting a vehicle id which is missing in the vehicles database: " + vehicleId);
							}
							break;
						default:
							throw new RuntimeException("not implemented") ;
						}
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
//							qsim.createAndParkVehicleOnLink(vehicle, vehicleLinkId);
							QVehicle qVehicle = new QVehicleImpl( vehicle ) ; // yyyyyy should use factory.  kai, nov'18
							qsim.addParkedVehicle( qVehicle, vehicleLinkId );

							// yyyy in fact, should rather try to use central method since the central method has moved on.  kai, nov'18
						}
						seenModes.add(leg.getMode());
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
				ActivityFacilities facilities = this.qsim.getScenario().getActivityFacilities() ;
				Config config = this.qsim.getScenario().getConfig() ;
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

	public void setModeVehicleTypes(Map<String, VehicleType> modeVehicleTypes) {
		this.modeVehicleTypes = modeVehicleTypes;
	}

}
